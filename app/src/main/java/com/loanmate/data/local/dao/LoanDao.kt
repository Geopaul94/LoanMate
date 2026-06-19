package com.loanmate.data.local.dao

import androidx.room.*
import com.loanmate.data.local.LoanEntity
import com.loanmate.data.model.LoanStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanDao {

    @Query("SELECT * FROM loans WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllLoans(): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE isDeleted = 0 AND status = :status ORDER BY createdAt DESC")
    fun getLoansByStatus(status: LoanStatus): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE id = :id AND isDeleted = 0")
    fun getLoanById(id: Long): Flow<LoanEntity?>

    @Query("SELECT * FROM loans WHERE isDeleted = 0 AND (loanName LIKE '%' || :query || '%' OR bankName LIKE '%' || :query || '%')")
    fun searchLoans(query: String): Flow<List<LoanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: LoanEntity): Long

    @Update
    suspend fun updateLoan(loan: LoanEntity)

    // Soft delete — mark row as deleted, keep data for undo
    @Query("UPDATE loans SET isDeleted = 1, deletedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteLoan(id: Long, timestamp: Long)

    // Reverse soft delete
    @Query("UPDATE loans SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restoreLoan(id: Long)

    // Permanent delete — called by cleanup worker after undo window expires
    @Query("DELETE FROM loans WHERE id = :id AND isDeleted = 1")
    suspend fun hardDeleteLoan(id: Long)

    // Cleanup any stale soft-deleted rows older than the undo window
    @Query("DELETE FROM loans WHERE isDeleted = 1 AND deletedAt < :cutoff")
    suspend fun purgeOldDeleted(cutoff: Long)

    @Query("SELECT COUNT(*) FROM loans WHERE isDeleted = 0 AND status = 'ACTIVE'")
    fun getActiveLoanCount(): Flow<Int>

    @Query("SELECT SUM(outstandingAmount) FROM loans WHERE isDeleted = 0 AND status = 'ACTIVE'")
    fun getTotalOutstanding(): Flow<Double?>

    @Query("SELECT SUM(monthlyEmi) FROM loans WHERE isDeleted = 0 AND status = 'ACTIVE'")
    fun getTotalMonthlyEmi(): Flow<Double?>

    @Query("SELECT * FROM loans WHERE isDeleted = 0 AND status = 'ACTIVE' ORDER BY firstEmiDate ASC LIMIT 1")
    fun getNextUpcomingLoan(): Flow<LoanEntity?>
}
