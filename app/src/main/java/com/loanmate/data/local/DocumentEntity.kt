package com.loanmate.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class DocumentType { PDF, IMAGE, OTHER }

@Entity(
    tableName = "documents",
    foreignKeys = [ForeignKey(
        entity = LoanEntity::class,
        parentColumns = ["id"],
        childColumns = ["loanId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("loanId")]
)
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: Long,
    val fileName: String,
    val filePath: String,
    val documentType: DocumentType,
    val addedAt: Long = System.currentTimeMillis()
)
