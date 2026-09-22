package com.loanmate.data.drive

import android.content.Context
import android.util.Log
import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.loanmate.data.local.LoanDatabase
import com.loanmate.data.repository.LoanRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import java.io.File as JavaFile

enum class BackupOutcome {
    SUCCESS, SKIPPED_EMPTY, SKIPPED_SHRINK, FAILED
}

enum class RestoreResult {
    SUCCESS, NO_BACKUP_FOUND, FAILED
}

@Singleton
class DriveBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val loanRepository: LoanRepository,
    private val db: LoanDatabase
) {
    private val fixedBackupFileName = "loanmate_backup.zip"
    private val previousBackupFileName = "loanmate_backup_previous.zip"
    private val backupFolderName = "LoanMate Backups"

    suspend fun createOrReplaceBackup(driveService: Drive, allowShrink: Boolean = false): BackupOutcome =
        withContext(Dispatchers.IO) {
            try {
                // Layer 1: Empty Data Guard
                val localCount = loanRepository.getAllLoansOnce().size
                if (localCount <= 0) return@withContext BackupOutcome.SKIPPED_EMPTY

                // Get Folder ID
                val folderId = getOrCreateBackupFolder(driveService) ?: return@withContext BackupOutcome.FAILED

                // Check for existing backup and its metadata
                val existingFile = findFile(driveService, fixedBackupFileName, folderId)

                // Layer 2: Shrink Guard
                if (!allowShrink && existingFile != null) {
                    val cloudCount = existingFile.getAppProperties()?.get("entryCount")?.toIntOrNull() ?: 0
                    if (cloudCount > localCount) {
                        return@withContext BackupOutcome.SKIPPED_SHRINK
                    }
                }

                // Layer 3: Keep 1-deep rollback copy
                if (existingFile != null) {
                    keepPreviousCopy(driveService, existingFile.id, folderId)
                }

                // Create ZIP locally
                val zipFile = createLocalZip() ?: return@withContext BackupOutcome.FAILED

                val metadata = File().apply {
                    name = fixedBackupFileName
                    appProperties = mapOf("entryCount" to localCount.toString())
                    parents = listOf(folderId)
                }

                val content = FileContent("application/zip", zipFile)

                if (existingFile != null) {
                    driveService.files().update(existingFile.id, metadata, content).execute()
                } else {
                    driveService.files().create(metadata, content).execute()
                }

                zipFile.delete()
                BackupOutcome.SUCCESS
            } catch (e: Exception) {
                Log.e("DriveBackup", "Backup failed", e)
                BackupOutcome.FAILED
            }
        }

    suspend fun restoreLatestBackup(driveService: Drive): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val folderId = getOrCreateBackupFolder(driveService) ?: return@withContext RestoreResult.FAILED
            val existingFile = findFile(driveService, fixedBackupFileName, folderId)
                ?: return@withContext RestoreResult.NO_BACKUP_FOUND

            val tempFile = JavaFile(context.cacheDir, "temp_restore.zip")
            driveService.files().get(existingFile.id)
                .executeMediaAndDownloadTo(FileOutputStream(tempFile))

            val success = restoreFromZip(tempFile)
            tempFile.delete()
            if (success) RestoreResult.SUCCESS else RestoreResult.FAILED
        } catch (e: Exception) {
            Log.e("DriveBackup", "Restore failed", e)
            RestoreResult.FAILED
        }
    }

    private fun createLocalZip(): JavaFile? {
        val dbFile = context.getDatabasePath("loanmate.db")
        if (!dbFile.exists()) return null

        // Flush WAL
        try {
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").close()
        } catch (e: Exception) {
            Log.w("DriveBackup", "WAL checkpoint failed", e)
        }

        val zipFile = JavaFile(context.cacheDir, "backup.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            // Add DB file
            addFileToZip(zos, dbFile, "loanmate.db")
            listOf("-wal", "-shm").forEach { ext ->
                val sidecar = JavaFile(dbFile.path + ext)
                if (sidecar.exists()) addFileToZip(zos, sidecar, "loanmate.db$ext")
            }

            // Add documents directory
            val docDir = JavaFile(context.filesDir, "loan_documents")
            if (docDir.exists()) {
                docDir.listFiles()?.forEach { doc ->
                    if (doc.isFile) {
                        addFileToZip(zos, doc, "loan_documents/${doc.name}")
                    }
                }
            }
        }
        return zipFile
    }

    private fun addFileToZip(zos: ZipOutputStream, file: JavaFile, entryName: String) {
        zos.putNextEntry(ZipEntry(entryName))
        FileInputStream(file).use { it.copyTo(zos) }
        zos.closeEntry()
    }

    private fun restoreFromZip(zipFile: JavaFile): Boolean {
        // 1. Close active DB
        db.close()

        val dbFile = context.getDatabasePath("loanmate.db")

        // CRITICAL: Delete local sidecars
        listOf("-wal", "-shm").forEach { ext ->
            JavaFile(dbFile.path + ext).delete()
        }

        val docDir = JavaFile(context.filesDir, "loan_documents").apply { mkdirs() }

        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name.startsWith("loan_documents/")) {
                    val fileName = entry.name.removePrefix("loan_documents/")
                    if (fileName.isNotEmpty()) {
                        val outFile = JavaFile(docDir, fileName)
                        FileOutputStream(outFile).use { zis.copyTo(it) }
                    }
                } else if (entry.name.startsWith("loanmate.db")) {
                    val outFile = JavaFile(context.getDatabasePath(entry.name).path)
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { zis.copyTo(it) }
                }
                entry = zis.nextEntry
            }
        }
        return true
    }

    private fun getOrCreateBackupFolder(driveService: Drive): String? {
        val query =
            "name = '$backupFolderName' and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
        val result = driveService.files().list().setQ(query).setSpaces("drive").execute()
        val folder = result.files.firstOrNull()
        if (folder != null) return folder.id

        val metadata = File().apply {
            name = backupFolderName
            mimeType = "application/vnd.google-apps.folder"
        }
        return driveService.files().create(metadata).execute().id
    }

    private fun findFile(driveService: Drive, name: String, folderId: String): File? {
        val query = "name = '$name' and '$folderId' in parents and trashed = false"
        val result = driveService.files().list().setQ(query).setSpaces("drive")
            .setFields("files(id, name, appProperties)").execute()
        return result.files.firstOrNull()
    }

    private fun keepPreviousCopy(driveService: Drive, currentId: String, folderId: String) {
        try {
            // Delete old previous copy
            val oldPrevious = findFile(driveService, previousBackupFileName, folderId)
            if (oldPrevious != null) {
                driveService.files().delete(oldPrevious.id).execute()
            }
            // Copy current to previous
            val copyMetadata = File().apply {
                name = previousBackupFileName
                parents = listOf(folderId)
            }
            driveService.files().copy(currentId, copyMetadata).execute()
        } catch (e: Exception) {
            Log.e("DriveBackup", "Rollback copy failed", e)
        }
    }
}
