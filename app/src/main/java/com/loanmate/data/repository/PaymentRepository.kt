package com.loanmate.data.repository

import com.loanmate.data.local.PaymentHistoryEntity
import com.loanmate.data.local.dao.PaymentHistoryDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentRepository @Inject constructor(private val dao: PaymentHistoryDao) {

    fun getPaymentsByLoanId(loanId: Long): Flow<List<PaymentHistoryEntity>> =
        dao.getPaymentsByLoanId(loanId)

    suspend fun getTotalPaymentCount(): Int = dao.getTotalPaymentCount()

    suspend fun insertPayment(payment: PaymentHistoryEntity) = dao.insertPayment(payment)

    suspend fun deletePayment(payment: PaymentHistoryEntity) = dao.deletePayment(payment)
}
