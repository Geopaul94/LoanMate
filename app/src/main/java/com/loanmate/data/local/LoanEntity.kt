package com.loanmate.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.loanmate.data.model.InterestType
import com.loanmate.data.model.LoanStatus
import com.loanmate.data.model.LoanType
import com.loanmate.data.model.TenureUnit

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanName: String,
    val bankName: String,
    val loanType: LoanType,
    val principalAmount: Double,
    val interestRate: Double,
    val interestType: InterestType,
    val tenureValue: Int,
    val tenureUnit: TenureUnit,
    val monthlyEmi: Double,
    val firstEmiDate: Long,
    val loanTakenDate: Long,
    val loanEndDate: Long,
    val processingFee: Double = 0.0,
    val insuranceCharges: Double = 0.0,
    val outstandingAmount: Double,
    val loanAccountNumber: String = "",
    val notes: String = "",
    val completedEmis: Int = 0,
    val totalEmis: Int,
    val status: LoanStatus = LoanStatus.ACTIVE,
    val createdAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)
