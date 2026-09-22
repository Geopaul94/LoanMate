package com.loanmate.data.drive

import com.loanmate.data.local.AchievementEntity
import com.loanmate.data.local.DocumentEntity
import com.loanmate.data.local.LoanEntity
import com.loanmate.data.local.PaymentHistoryEntity
import kotlinx.serialization.Serializable

@Serializable
data class BackupManifest(
    val version: Int = 1,
    val exportedAt: Long,
    val entryCount: Int,
    val appVersion: String,
    val deviceName: String = android.os.Build.MODEL
)

@Serializable
data class BackupData(
    val loans: List<LoanEntityDto>,
    val payments: List<PaymentHistoryEntityDto>,
    val achievements: List<AchievementEntityDto>,
    val documents: List<DocumentEntityDto>
)

@Serializable
data class LoanEntityDto(
    val id: Long,
    val loanName: String,
    val bankName: String,
    val loanType: String,
    val principalAmount: Double,
    val interestRate: Double,
    val interestType: String,
    val tenureValue: Int,
    val tenureUnit: String,
    val monthlyEmi: Double,
    val firstEmiDate: Long,
    val loanTakenDate: Long,
    val loanEndDate: Long,
    val processingFee: Double,
    val insuranceCharges: Double,
    val outstandingAmount: Double,
    val loanAccountNumber: String,
    val notes: String,
    val completedEmis: Int,
    val totalEmis: Int,
    val status: String,
    val createdAt: Long,
    val isDeleted: Boolean,
    val deletedAt: Long?
)

@Serializable
data class PaymentHistoryEntityDto(
    val id: Long,
    val loanId: Long,
    val emiNumber: Int,
    val amountPaid: Double,
    val principalComponent: Double,
    val interestComponent: Double,
    val remainingBalance: Double,
    val paidDate: Long,
    val dueDate: Long,
    val note: String
)

@Serializable
data class AchievementEntityDto(
    val type: String,
    val title: String,
    val description: String,
    val emoji: String,
    val earnedAt: Long?,
    val isEarned: Boolean
)

@Serializable
data class DocumentEntityDto(
    val id: Long,
    val loanId: Long,
    val fileName: String,
    val filePath: String, // Relative path in the ZIP
    val documentType: String,
    val addedAt: Long
)
