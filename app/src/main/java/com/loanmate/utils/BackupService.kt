package com.loanmate.utils

import android.net.Uri

/**
 * Reads and writes the backup package file.
 */
interface BackupService {

    /** Writes every stored loan, payment & document to [uri]. Returns how many loans were exported. */
    suspend fun exportToUri(uri: Uri): Result<Int>

    /** Reads backup package from [uri] and restores it. Returns how many loans were imported. */
    suspend fun importFromUri(uri: Uri): Result<Int>
}
