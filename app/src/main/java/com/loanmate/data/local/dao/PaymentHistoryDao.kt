package com.loanmate.data.local.dao

import androidx.room.*
import com.loanmate.data.local.PaymentHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentHistoryDao {

    @Query("SELECT * FROM payment_history WHERE loanId = :loanId ORDER BY emiNumber ASC")
    fun getPaymentsByLoanId(loanId: Long): Flow<List<PaymentHistoryEntity>>

    @Query("SELECT COUNT(*) FROM payment_history")
    suspend fun getTotalPaymentCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentHistoryEntity)

    @Delete
    suspend fun deletePayment(payment: PaymentHistoryEntity)

    @Query("DELETE FROM payment_history WHERE loanId = :loanId")
    suspend fun deletePaymentsByLoanId(loanId: Long)
}
