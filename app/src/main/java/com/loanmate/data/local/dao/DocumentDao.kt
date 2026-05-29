package com.loanmate.data.local.dao

import androidx.room.*
import com.loanmate.data.local.DocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Query("SELECT * FROM documents WHERE loanId = :loanId ORDER BY addedAt DESC")
    fun getDocumentsByLoanId(loanId: Long): Flow<List<DocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity): Long

    @Delete
    suspend fun deleteDocument(document: DocumentEntity)
}
