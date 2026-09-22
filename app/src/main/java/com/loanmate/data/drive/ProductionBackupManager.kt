package com.loanmate.data.drive

import android.content.Context
import androidx.room.withTransaction
import com.loanmate.data.local.AchievementEntity
import com.loanmate.data.local.AchievementType
import com.loanmate.data.local.DocumentEntity
import com.loanmate.data.local.DocumentType
import com.loanmate.data.local.LoanDatabase
import com.loanmate.data.local.LoanEntity
import com.loanmate.data.local.PaymentHistoryEntity
import com.loanmate.data.model.InterestType
import com.loanmate.data.model.LoanStatus
import com.loanmate.data.model.LoanType
import com.loanmate.data.model.TenureUnit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductionBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: LoanDatabase
) {
    private val jsonFormatter = Json { ignoreUnknownKeys = true; prettyPrint = true }

    suspend fun prepareBackupPackage(): File = withContext(Dispatchers.IO) {
        // 1. Flush WAL
        db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").close()

        val loans = db.loanDao().getAllLoansIncludeTrashOnce()
        val payments = db.paymentHistoryDao().getAllPaymentsOnce()
        val achievements = db.achievementDao().getAllAchievementsOnce()
        val documents = db.documentDao().getAllDocumentsOnce()

        val backupZip = File(context.cacheDir, "loanmate_backup.zip").apply { delete() }

        val data = BackupData(
            loans = loans.map { it.toDto() },
            payments = payments.map { it.toDto() },
            achievements = achievements.map { it.toDto() },
            documents = documents.map { it.toDto() }
        )

        ZipOutputStream(BufferedOutputStream(FileOutputStream(backupZip))).use { zos ->
            // Data JSON
            zos.putNextEntry(ZipEntry("data.json"))
            zos.write(jsonFormatter.encodeToString(data).toByteArray())
            zos.closeEntry()

            // Documents
            documents.forEach { doc ->
                val docFile = File(doc.filePath)
                if (docFile.exists()) {
                    zos.putNextEntry(ZipEntry("documents/${docFile.name}"))
                    docFile.inputStream().use { input -> input.copyTo(zos) }
                    zos.closeEntry()
                }
            }
            
            // Manifest
            val versionName = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            } catch (e: Exception) {
                "1.0.0"
            } ?: "1.0.0"

            val manifest = BackupManifest(
                exportedAt = System.currentTimeMillis(),
                entryCount = loans.size,
                appVersion = versionName
            )
            zos.putNextEntry(ZipEntry("manifest.json"))
            zos.write(jsonFormatter.encodeToString(manifest).toByteArray())
            zos.closeEntry()
        }

        backupZip
    }

    suspend fun restoreFromPackage(zipFile: File) = withContext(Dispatchers.IO) {
        val extractDir = File(context.cacheDir, "restore_staging").apply { deleteRecursively(); mkdirs() }
        
        ZipFile(zipFile).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                val outFile = File(extractDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        outFile.outputStream().use { output -> input.copyTo(output) }
                    }
                }
            }
        }

        val dataFile = File(extractDir, "data.json")
        val data = jsonFormatter.decodeFromString<BackupData>(dataFile.readText())

        db.withTransaction {
            db.paymentHistoryDao().deleteAllPayments()
            db.achievementDao().deleteAllAchievements()
            db.documentDao().deleteAllDocuments()
            db.loanDao().deleteAllLoans()

            // Restore files first
            val docDir = File(context.filesDir, "loan_documents").apply { mkdirs() }
            val newDocuments = data.documents.map { dto ->
                val originalFile = File(extractDir, dto.filePath)
                val newFile = File(docDir, "${System.currentTimeMillis()}_${originalFile.name}")
                if (originalFile.exists()) {
                    originalFile.copyTo(newFile, overwrite = true)
                }
                dto.toEntity(newFile.absolutePath)
            }

            data.loans.forEach { db.loanDao().insertLoan(it.toEntity()) }
            data.payments.forEach { db.paymentHistoryDao().insertPayment(it.toEntity()) }
            data.achievements.forEach { db.achievementDao().insertAchievement(it.toEntity()) }
            newDocuments.forEach { db.documentDao().insertDocument(it) }
        }
        
        extractDir.deleteRecursively()
    }

    // Mapper extensions
    private fun LoanEntity.toDto() = LoanEntityDto(
        id, loanName, bankName, loanType.name, principalAmount, interestRate, interestType.name,
        tenureValue, tenureUnit.name, monthlyEmi, firstEmiDate, loanTakenDate, loanEndDate,
        processingFee, insuranceCharges, outstandingAmount, loanAccountNumber, notes,
        completedEmis, totalEmis, status.name, createdAt, isDeleted, deletedAt
    )

    private fun LoanEntityDto.toEntity() = LoanEntity(
        id, loanName, bankName, LoanType.valueOf(loanType), principalAmount, interestRate,
        InterestType.valueOf(interestType), tenureValue, TenureUnit.valueOf(tenureUnit),
        monthlyEmi, firstEmiDate, loanTakenDate, loanEndDate, processingFee, insuranceCharges,
        outstandingAmount, loanAccountNumber, notes, completedEmis, totalEmis,
        LoanStatus.valueOf(status), createdAt, isDeleted, deletedAt
    )

    private fun PaymentHistoryEntity.toDto() = PaymentHistoryEntityDto(
        id, loanId, emiNumber, amountPaid, principalComponent, interestComponent, remainingBalance, paidDate, dueDate, note
    )

    private fun PaymentHistoryEntityDto.toEntity() = PaymentHistoryEntity(
        id, loanId, emiNumber, amountPaid, principalComponent, interestComponent, remainingBalance, paidDate, dueDate, note
    )

    private fun AchievementEntity.toDto() = AchievementEntityDto(
        type.name, title, description, emoji, earnedAt, isEarned
    )

    private fun AchievementEntityDto.toEntity() = AchievementEntity(
        AchievementType.valueOf(type), title, description, emoji, earnedAt, isEarned
    )

    private fun DocumentEntity.toDto() = DocumentEntityDto(
        id, loanId, fileName, "documents/${File(filePath).name}", documentType.name, addedAt
    )

    private fun DocumentEntityDto.toEntity(newPath: String) = DocumentEntity(
        id, loanId, fileName, newPath, DocumentType.valueOf(documentType), addedAt
    )
}
