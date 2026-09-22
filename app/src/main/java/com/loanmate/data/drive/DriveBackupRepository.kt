package com.loanmate.data.drive

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File as DriveFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File as LocalFile
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps the Drive REST API for the `drive.file` scope.
 * Every method must be called off the main thread.
 */
@Singleton
class DriveBackupRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val MIME_ZIP = "application/zip"
        private const val APP_NAME = "LoanMate"
        private const val FIXED_BACKUP_NAME = "loanmate_backup.zip"
        private const val PREVIOUS_BACKUP_NAME = "loanmate_backup_previous.zip"
    }

    data class RemoteBackup(
        val id: String,
        val name: String,
        val modifiedTimeMs: Long,
        val sizeBytes: Long,
        val entryCount: Int? = null
    )

    sealed class Outcome<out T> {
        data class Success<T>(val value: T) : Outcome<T>()
        data class Failure(val reason: String) : Outcome<Nothing>()
    }

    private fun driveService(account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_FILE)
        ).apply { selectedAccount = account.account }

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName(APP_NAME).build()
    }

    private suspend fun getOrCreateBackupFolder(service: Drive): String = withContext(Dispatchers.IO) {
        val folderName = "loanmate backupfile"
        val query = "name = '$folderName' and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
        val result = service.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id)")
            .execute()

        val existingId = result.files.firstOrNull()?.id
        if (existingId != null) return@withContext existingId

        val metadata = DriveFile().apply {
            name = folderName
            mimeType = "application/vnd.google-apps.folder"
        }
        val created = service.files().create(metadata)
            .setFields("id")
            .execute()
        created.id
    }

    suspend fun upload(account: GoogleSignInAccount, localFile: LocalFile, entryCount: Int):
            Outcome<RemoteBackup> = withContext(Dispatchers.IO) {
        try {
            val service = driveService(account)
            val folderId = getOrCreateBackupFolder(service)

            // 1. Find existing backup
            val query = "name = '$FIXED_BACKUP_NAME' and '$folderId' in parents and trashed = false"
            val existing = service.files().list().setQ(query).setFields("files(id)").execute().files.orEmpty()
            
            // 2. Rollback protection: move current to previous
            if (existing.isNotEmpty()) {
                val currentId = existing.first().id
                
                // Delete any old previous backup
                val prevQuery = "name = '$PREVIOUS_BACKUP_NAME' and '$folderId' in parents and trashed = false"
                service.files().list().setQ(prevQuery).setFields("files(id)").execute().files?.forEach { 
                    service.files().delete(it.id).execute()
                }

                // Copy current to previous
                val copyMeta = DriveFile().apply {
                    name = PREVIOUS_BACKUP_NAME
                    parents = listOf(folderId)
                }
                service.files().copy(currentId, copyMeta).execute()
            }

            // 3. Upload new backup (replace or create)
            val metadata = DriveFile().apply {
                name = FIXED_BACKUP_NAME
                parents = if (existing.isEmpty()) listOf(folderId) else null
                appProperties = mapOf("entryCount" to entryCount.toString())
            }
            
            val content = FileContent(MIME_ZIP, localFile)
            val result = if (existing.isNotEmpty()) {
                service.files().update(existing.first().id, metadata, content)
                    .setFields("id, name, modifiedTime, size, appProperties")
                    .execute()
            } else {
                service.files().create(metadata, content)
                    .setFields("id, name, modifiedTime, size, appProperties")
                    .execute()
            }

            Outcome.Success(
                RemoteBackup(
                    id = result.id,
                    name = result.name,
                    modifiedTimeMs = result.modifiedTime?.value ?: System.currentTimeMillis(),
                    sizeBytes = result.getSize() ?: 0L,
                    entryCount = result.appProperties?.get("entryCount")?.toIntOrNull()
                )
            )
        } catch (e: Exception) {
            Outcome.Failure(e.message ?: e.javaClass.simpleName)
        }
    }

    suspend fun listBackups(account: GoogleSignInAccount):
            Outcome<List<RemoteBackup>> = withContext(Dispatchers.IO) {
        try {
            val service = driveService(account)
            val folderId = getOrCreateBackupFolder(service)
            
            val result = service.files().list()
                .setQ("'$folderId' in parents and trashed = false")
                .setSpaces("drive")
                .setFields("files(id, name, modifiedTime, size, appProperties)")
                .setOrderBy("modifiedTime desc")
                .execute()
            
            val backups = result.files.orEmpty().map { f ->
                RemoteBackup(
                    id = f.id,
                    name = f.name ?: "(untitled)",
                    modifiedTimeMs = f.modifiedTime?.value ?: 0L,
                    sizeBytes = f.getSize() ?: 0L,
                    entryCount = f.appProperties?.get("entryCount")?.toIntOrNull()
                )
            }
            Outcome.Success(backups)
        } catch (e: Exception) {
            Outcome.Failure(e.message ?: e.javaClass.simpleName)
        }
    }

    suspend fun download(account: GoogleSignInAccount, fileId: String):
            Outcome<LocalFile> = withContext(Dispatchers.IO) {
        try {
            val service = driveService(account)
            val tempFile = LocalFile(context.cacheDir, "downloaded_backup.zip")
            FileOutputStream(tempFile).use { output ->
                service.files().get(fileId).executeMediaAndDownloadTo(output)
            }
            Outcome.Success(tempFile)
        } catch (e: Exception) {
            Outcome.Failure(e.message ?: e.javaClass.simpleName)
        }
    }
}
