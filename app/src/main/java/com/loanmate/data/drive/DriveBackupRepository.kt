package com.loanmate.data.drive

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File as DriveFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps the Drive REST API for the `appDataFolder` scope.
 * Every method must be called off the main thread.
 * Files live in the app's hidden data folder — invisible in Drive's web UI.
 */
@Singleton
class DriveBackupRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val APP_DATA_FOLDER = "appDataFolder"
        private const val MIME_JSON = "application/json"
        private const val APP_NAME = "LoanMate"
        const val MAX_KEPT_BACKUPS = 5
    }

    data class RemoteBackup(
        val id: String,
        val name: String,
        val modifiedTimeMs: Long,
        val sizeBytes: Long
    )

    sealed class Outcome<out T> {
        data class Success<T>(val value: T) : Outcome<T>()
        data class Failure(val reason: String) : Outcome<Nothing>()
    }

    private fun driveService(account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_APPDATA)
        ).apply { selectedAccount = account.account }

        @Suppress("DEPRECATION")
        return Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName(APP_NAME).build()
    }

    suspend fun upload(account: GoogleSignInAccount, fileName: String, jsonBytes: ByteArray):
            Outcome<RemoteBackup> = withContext(Dispatchers.IO) {
        try {
            val service = driveService(account)
            val metadata = DriveFile().apply {
                name = fileName
                parents = listOf(APP_DATA_FOLDER)
                mimeType = MIME_JSON
            }
            val content = ByteArrayContent(MIME_JSON, jsonBytes)
            val created = service.files().create(metadata, content)
                .setFields("id, name, modifiedTime, size")
                .execute()
            pruneOldBackups(service)
            Outcome.Success(
                RemoteBackup(
                    id = created.id,
                    name = created.name,
                    modifiedTimeMs = created.modifiedTime?.value ?: System.currentTimeMillis(),
                    sizeBytes = created.getSize() ?: 0L
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
            val result = service.files().list()
                .setSpaces(APP_DATA_FOLDER)
                .setFields("files(id, name, modifiedTime, size)")
                .setOrderBy("modifiedTime desc")
                .setPageSize(20)
                .execute()
            val backups = result.files.orEmpty().map { f ->
                RemoteBackup(
                    id = f.id,
                    name = f.name ?: "(untitled)",
                    modifiedTimeMs = f.modifiedTime?.value ?: 0L,
                    sizeBytes = f.getSize() ?: 0L
                )
            }
            Outcome.Success(backups)
        } catch (e: Exception) {
            Outcome.Failure(e.message ?: e.javaClass.simpleName)
        }
    }

    suspend fun download(account: GoogleSignInAccount, fileId: String):
            Outcome<String> = withContext(Dispatchers.IO) {
        try {
            val service = driveService(account)
            val out = ByteArrayOutputStream()
            service.files().get(fileId).executeMediaAndDownloadTo(out)
            Outcome.Success(out.toString("UTF-8"))
        } catch (e: Exception) {
            Outcome.Failure(e.message ?: e.javaClass.simpleName)
        }
    }

    private fun pruneOldBackups(service: Drive) {
        try {
            val all = service.files().list()
                .setSpaces(APP_DATA_FOLDER)
                .setFields("files(id, modifiedTime)")
                .setOrderBy("modifiedTime desc")
                .execute()
                .files.orEmpty()
            all.drop(MAX_KEPT_BACKUPS).forEach { stale ->
                runCatching { service.files().delete(stale.id).execute() }
            }
        } catch (_: Exception) {
            // pruning is best-effort; don't fail the upload over it
        }
    }
}
