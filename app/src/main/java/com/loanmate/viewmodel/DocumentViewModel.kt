package com.loanmate.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loanmate.data.local.DocumentEntity
import com.loanmate.data.local.DocumentType
import com.loanmate.data.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

data class DocumentUiState(
    val documents: List<DocumentEntity> = emptyList(),
    val isBusy: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DocumentViewModel @Inject constructor(
    private val repository: DocumentRepository
) : ViewModel() {

    private val _isBusy = MutableStateFlow(false)
    val isBusy = _isBusy.asStateFlow()

    fun getDocuments(loanId: Long): Flow<List<DocumentEntity>> =
        repository.getDocumentsForLoan(loanId)

    fun addDocument(context: Context, loanId: Long, uri: Uri, fileName: String) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val file = copyUriToInternalStorage(context, uri, fileName)
                val type = when {
                    fileName.endsWith(".pdf", ignoreCase = true) -> DocumentType.PDF
                    fileName.endsWith(".jpg", ignoreCase = true) ||
                            fileName.endsWith(".jpeg", ignoreCase = true) ||
                            fileName.endsWith(".png", ignoreCase = true) -> DocumentType.IMAGE
                    else -> DocumentType.OTHER
                }
                val entity = DocumentEntity(
                    loanId = loanId,
                    fileName = fileName,
                    filePath = file.absolutePath,
                    documentType = type
                )
                repository.addDocument(entity)
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun deleteDocument(document: DocumentEntity) {
        viewModelScope.launch {
            try {
                File(document.filePath).delete()
                repository.deleteDocument(document)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun copyUriToInternalStorage(context: Context, uri: Uri, fileName: String): File {
        val dir = File(context.filesDir, "loan_documents")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "${System.currentTimeMillis()}_$fileName")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        return file
    }
}
