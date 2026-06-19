package com.loanmate.utils

import android.content.Context
import androidx.room.withTransaction
import com.loanmate.data.local.AchievementEntity
import com.loanmate.data.local.AchievementType
import com.loanmate.data.local.LoanDatabase
import com.loanmate.data.local.LoanEntity
import com.loanmate.data.local.PaymentHistoryEntity
import com.loanmate.data.model.InterestType
import com.loanmate.data.model.LoanStatus
import com.loanmate.data.model.LoanType
import com.loanmate.data.model.TenureUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plain JSON backup of all user data.
 * Versioned schema so future restores can migrate old files.
 *
 * NOT encrypted — the file contains sensitive loan data. Treat the share
 * destination accordingly (private Drive folder, password-protected zip, etc.).
 */
@Singleton
class BackupManager @Inject constructor(
    private val db: LoanDatabase
) {

    companion object {
        const val SCHEMA_VERSION = 1
    }

    suspend fun export(context: Context): BackupResult = withContext(Dispatchers.IO) {
        val loans = db.loanDao().getAllLoansOnce()
        val payments = db.paymentHistoryDao().getAllPaymentsOnce()
        val achievements = db.achievementDao().getAllAchievementsOnce()

        val root = JSONObject().apply {
            put("schemaVersion", SCHEMA_VERSION)
            put("exportedAt", System.currentTimeMillis())
            put("loans", JSONArray().also { arr ->
                loans.forEach { arr.put(loanToJson(it)) }
            })
            put("payments", JSONArray().also { arr ->
                payments.forEach { arr.put(paymentToJson(it)) }
            })
            put("achievements", JSONArray().also { arr ->
                achievements.forEach { arr.put(achievementToJson(it)) }
            })
        }

        val outDir = File(context.getExternalFilesDir(null), "backups").apply { mkdirs() }
        val ts = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.getDefault())
            .format(java.util.Date())
        val outFile = File(outDir, "loanmate_backup_$ts.json")
        FileOutputStream(outFile).use { it.write(root.toString(2).toByteArray()) }
        BackupResult(
            file = outFile,
            loanCount = loans.size,
            paymentCount = payments.size,
            achievementCount = achievements.size
        )
    }

    /**
     * Restores from a JSON file. Wipes existing rows first to prevent ID
     * collisions, then re-inserts everything in a single transaction.
     */
    suspend fun restore(jsonText: String): RestoreOutcome = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonText)
            val version = root.optInt("schemaVersion", -1)
            if (version <= 0 || version > SCHEMA_VERSION) {
                return@withContext RestoreOutcome.Failure(
                    "Unsupported backup version $version (this app supports up to $SCHEMA_VERSION)."
                )
            }
            val loans = parseArray(root.optJSONArray("loans"), ::jsonToLoan)
            val payments = parseArray(root.optJSONArray("payments"), ::jsonToPayment)
            val achievements = parseArray(root.optJSONArray("achievements"), ::jsonToAchievement)

            db.withTransaction {
                db.paymentHistoryDao().deleteAllPayments()
                db.achievementDao().deleteAllAchievements()
                db.loanDao().deleteAllLoans()
                loans.forEach { db.loanDao().insertLoan(it) }
                payments.forEach { db.paymentHistoryDao().insertPayment(it) }
                achievements.forEach { db.achievementDao().insertAchievement(it) }
            }
            RestoreOutcome.Success(
                loanCount = loans.size,
                paymentCount = payments.size,
                achievementCount = achievements.size
            )
        } catch (e: Exception) {
            RestoreOutcome.Failure("Backup file is malformed: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    private fun <T> parseArray(arr: JSONArray?, mapper: (JSONObject) -> T): List<T> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).map { mapper(arr.getJSONObject(it)) }
    }

    // --- Loan ---
    private fun loanToJson(l: LoanEntity) = JSONObject().apply {
        put("id", l.id)
        put("loanName", l.loanName); put("bankName", l.bankName)
        put("loanType", l.loanType.name); put("principalAmount", l.principalAmount)
        put("interestRate", l.interestRate); put("interestType", l.interestType.name)
        put("tenureValue", l.tenureValue); put("tenureUnit", l.tenureUnit.name)
        put("monthlyEmi", l.monthlyEmi); put("firstEmiDate", l.firstEmiDate)
        put("loanTakenDate", l.loanTakenDate); put("loanEndDate", l.loanEndDate)
        put("processingFee", l.processingFee); put("insuranceCharges", l.insuranceCharges)
        put("outstandingAmount", l.outstandingAmount); put("loanAccountNumber", l.loanAccountNumber)
        put("notes", l.notes); put("completedEmis", l.completedEmis)
        put("totalEmis", l.totalEmis); put("status", l.status.name)
        put("createdAt", l.createdAt); put("isDeleted", l.isDeleted)
        l.deletedAt?.let { put("deletedAt", it) }
    }

    private fun jsonToLoan(o: JSONObject) = LoanEntity(
        id = o.getLong("id"),
        loanName = o.getString("loanName"),
        bankName = o.getString("bankName"),
        loanType = LoanType.valueOf(o.getString("loanType")),
        principalAmount = o.getDouble("principalAmount"),
        interestRate = o.getDouble("interestRate"),
        interestType = InterestType.valueOf(o.getString("interestType")),
        tenureValue = o.getInt("tenureValue"),
        tenureUnit = TenureUnit.valueOf(o.getString("tenureUnit")),
        monthlyEmi = o.getDouble("monthlyEmi"),
        firstEmiDate = o.getLong("firstEmiDate"),
        loanTakenDate = o.getLong("loanTakenDate"),
        loanEndDate = o.getLong("loanEndDate"),
        processingFee = o.optDouble("processingFee", 0.0),
        insuranceCharges = o.optDouble("insuranceCharges", 0.0),
        outstandingAmount = o.getDouble("outstandingAmount"),
        loanAccountNumber = o.optString("loanAccountNumber", ""),
        notes = o.optString("notes", ""),
        completedEmis = o.optInt("completedEmis", 0),
        totalEmis = o.getInt("totalEmis"),
        status = LoanStatus.valueOf(o.getString("status")),
        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
        isDeleted = o.optBoolean("isDeleted", false),
        deletedAt = if (o.has("deletedAt")) o.getLong("deletedAt") else null
    )

    // --- Payment ---
    private fun paymentToJson(p: PaymentHistoryEntity) = JSONObject().apply {
        put("id", p.id); put("loanId", p.loanId); put("emiNumber", p.emiNumber)
        put("amountPaid", p.amountPaid); put("principalComponent", p.principalComponent)
        put("interestComponent", p.interestComponent); put("remainingBalance", p.remainingBalance)
        put("paidDate", p.paidDate); put("dueDate", p.dueDate); put("note", p.note)
    }

    private fun jsonToPayment(o: JSONObject) = PaymentHistoryEntity(
        id = o.getLong("id"),
        loanId = o.getLong("loanId"),
        emiNumber = o.getInt("emiNumber"),
        amountPaid = o.getDouble("amountPaid"),
        principalComponent = o.getDouble("principalComponent"),
        interestComponent = o.getDouble("interestComponent"),
        remainingBalance = o.getDouble("remainingBalance"),
        paidDate = o.getLong("paidDate"),
        dueDate = o.getLong("dueDate"),
        note = o.optString("note", "")
    )

    // --- Achievement ---
    private fun achievementToJson(a: AchievementEntity) = JSONObject().apply {
        put("type", a.type.name); put("title", a.title); put("description", a.description)
        put("emoji", a.emoji); a.earnedAt?.let { put("earnedAt", it) }
        put("isEarned", a.isEarned)
    }

    private fun jsonToAchievement(o: JSONObject) = AchievementEntity(
        type = AchievementType.valueOf(o.getString("type")),
        title = o.getString("title"),
        description = o.getString("description"),
        emoji = o.getString("emoji"),
        earnedAt = if (o.has("earnedAt")) o.getLong("earnedAt") else null,
        isEarned = o.optBoolean("isEarned", false)
    )

    data class BackupResult(
        val file: File,
        val loanCount: Int,
        val paymentCount: Int,
        val achievementCount: Int
    )

    sealed class RestoreOutcome {
        data class Success(val loanCount: Int, val paymentCount: Int, val achievementCount: Int) : RestoreOutcome()
        data class Failure(val reason: String) : RestoreOutcome()
    }
}
