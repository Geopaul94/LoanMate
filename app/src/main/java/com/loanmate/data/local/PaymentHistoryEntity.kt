package com.loanmate.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payment_history",
    foreignKeys = [ForeignKey(
        entity = LoanEntity::class,
        parentColumns = ["id"],
        childColumns = ["loanId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("loanId")]
)
data class PaymentHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: Long,
    val emiNumber: Int,
    val amountPaid: Double,
    val principalComponent: Double,
    val interestComponent: Double,
    val remainingBalance: Double,
    val paidDate: Long,
    val dueDate: Long,
    val note: String = ""
)
