package org.example.project.alarm

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import java.io.File

data class AriaImportResult(
    val imported: List<File>,
    val skipped: Int
)

class AriaMusicRepository(context: Context) {

    private val appContext = context.applicationContext

    private val musicDir: File by lazy {
        File(appContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "AriaAlarm")
            .apply { mkdirs() }
    }

    fun importMp3Files(uris: List<Uri>): AriaImportResult {
        val imported = mutableListOf<File>()
        var skipped = 0

        for (uri in uris) {
            val displayName = queryDisplayName(uri)
            val mimeType = appContext.contentResolver.getType(uri)

            val looksLikeMp3 = displayName?.endsWith(".mp3", ignoreCase = true) == true
            val mimeLooksLikeMp3 = mimeType
                ?.lowercase()
                ?.let { it.contains("mpeg") || it.contains("mp3") } == true

            if (!looksLikeMp3 && !mimeLooksLikeMp3) {
                skipped++
                continue
            }

            val safeName = sanitizeFileName(displayName ?: "song_${System.currentTimeMillis()}.mp3")
            val targetFile = createUniqueFile(safeName)

            val success = appContext.contentResolver.openInputStream(uri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
                true
            } ?: false

            if (success && targetFile.length() > 0L) {
                imported.add(targetFile)
            } else {
                targetFile.delete()
                skipped++
            }
        }

        return AriaImportResult(imported, skipped)
    }

    fun songs(): List<File> {
        return musicDir.listFiles { file ->
            file.isFile && file.extension.equals("mp3", ignoreCase = true) && file.length() > 0L
        }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()
    }

    fun randomSong(): File? = songs().randomOrNull()

    fun deleteSong(file: File) {
        if (file.exists()) {
            file.delete()
        }
    }

    fun musicDirectory(): File = musicDir

    private fun queryDisplayName(uri: Uri): String? {
        return appContext.contentResolver.query(
            uri,
            null,
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) cursor.getString(index) else null
            } else {
                null
            }
        } ?: uri.lastPathSegment
    }

    private fun sanitizeFileName(name: String): String {
        val cleaned = name
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .trim()
            .take(180)
            .ifBlank { "song.mp3" }

        return if (cleaned.endsWith(".mp3", ignoreCase = true)) {
            cleaned
        } else {
            "$cleaned.mp3"
        }
    }

    private fun createUniqueFile(name: String): File {
        var candidate = File(musicDir, name)
        var index = 1

        val baseName = name.substringBeforeLast('.')
        val extension = name.substringAfterLast('.', "")

        while (candidate.exists()) {
            val newName = if (extension.isNotBlank()) {
                "${baseName}_$index.$extension"
            } else {
                "${baseName}_$index"
            }
            candidate = File(musicDir, newName)
            index++
        }

        return candidate
    }
}
