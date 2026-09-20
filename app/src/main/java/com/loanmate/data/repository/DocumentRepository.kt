package com.loanmate.data.repository

import com.loanmate.data.local.DocumentEntity
import com.loanmate.data.local.dao.DocumentDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentRepository @Inject constructor(
    private val documentDao: DocumentDao
) {
    fun getDocumentsForLoan(loanId: Long): Flow<List<DocumentEntity>> =
        documentDao.getDocumentsByLoanId(loanId)

    suspend fun addDocument(document: DocumentEntity) =
        documentDao.insertDocument(document)

    suspend fun deleteDocument(document: DocumentEntity) =
        documentDao.deleteDocument(document)
}
