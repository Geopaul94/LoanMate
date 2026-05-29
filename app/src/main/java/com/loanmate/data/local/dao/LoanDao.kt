package com.loanmate.data.local.dao

import androidx.room.*
import com.loanmate.data.local.LoanEntity
import com.loanmate.data.model.LoanStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanDao {

    @Query("SELECT * FROM loans ORDER BY createdAt DESC")
    fun getAllLoans(): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE status = :status ORDER BY createdAt DESC")
    fun getLoansByStatus(status: LoanStatus): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE id = :id")
    fun getLoanById(id: Long): Flow<LoanEntity?>

    @Query("SELECT * FROM loans WHERE loanName LIKE '%' || :query || '%' OR bankName LIKE '%' || :query || '%'")
    fun searchLoans(query: String): Flow<List<LoanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: LoanEntity): Long

    @Update
    suspend fun updateLoan(loan: LoanEntity)

    @Delete
    suspend fun deleteLoan(loan: LoanEntity)

    @Query("SELECT COUNT(*) FROM loans WHERE status = 'ACTIVE'")
    fun getActiveLoanCount(): Flow<Int>

    @Query("SELECT SUM(outstandingAmount) FROM loans WHERE status = 'ACTIVE'")
    fun getTotalOutstanding(): Flow<Double?>

    @Query("SELECT SUM(monthlyEmi) FROM loans WHERE status = 'ACTIVE'")
    fun getTotalMonthlyEmi(): Flow<Double?>

    @Query("SELECT * FROM loans WHERE status = 'ACTIVE' ORDER BY firstEmiDate ASC LIMIT 1")
    fun getNextUpcomingLoan(): Flow<LoanEntity?>
}
