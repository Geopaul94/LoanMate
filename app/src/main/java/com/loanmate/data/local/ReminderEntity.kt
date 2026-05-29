package com.loanmate.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminders",
    foreignKeys = [ForeignKey(
        entity = LoanEntity::class,
        parentColumns = ["id"],
        childColumns = ["loanId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("loanId")]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: Long,
    val daysBeforeDue: Int,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val isEnabled: Boolean = true,
    val workManagerId: String = ""
)
