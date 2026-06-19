package com.loanmate.data.repository

import com.loanmate.data.local.LoanEntity
import com.loanmate.data.local.dao.LoanDao
import com.loanmate.data.model.LoanStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoanRepository @Inject constructor(private val loanDao: LoanDao) {

    fun getAllLoans(): Flow<List<LoanEntity>> = loanDao.getAllLoans()

    fun getActiveLoans(): Flow<List<LoanEntity>> = loanDao.getLoansByStatus(LoanStatus.ACTIVE)

    fun getLoanById(id: Long): Flow<LoanEntity?> = loanDao.getLoanById(id)

    fun searchLoans(query: String): Flow<List<LoanEntity>> = loanDao.searchLoans(query)

    fun getActiveLoanCount(): Flow<Int> = loanDao.getActiveLoanCount()

    fun getTotalOutstanding(): Flow<Double?> = loanDao.getTotalOutstanding()

    fun getTotalMonthlyEmi(): Flow<Double?> = loanDao.getTotalMonthlyEmi()

    fun getNextUpcomingLoan(): Flow<LoanEntity?> = loanDao.getNextUpcomingLoan()

    suspend fun insertLoan(loan: LoanEntity): Long = loanDao.insertLoan(loan)

    suspend fun updateLoan(loan: LoanEntity) = loanDao.updateLoan(loan)

    // Soft delete — undoable
    suspend fun softDeleteLoan(id: Long) =
        loanDao.softDeleteLoan(id, System.currentTimeMillis())

    suspend fun restoreLoan(id: Long) = loanDao.restoreLoan(id)

    // Permanent delete — called by cleanup worker
    suspend fun hardDeleteLoan(id: Long) = loanDao.hardDeleteLoan(id)
}
