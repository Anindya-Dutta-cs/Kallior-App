package com.app.kallior.alarm

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

data class AriaImportResult(
    val imported: List<File>,
    val skipped: Int
)

/**
 * A song in the AriaAlarm library. [id] is the file name inside the music
 * directory and is what alarms store as their song reference.
 */
data class AriaSong(
    val id: String,
    val title: String,
    val file: File
)

class AriaMusicRepository(context: Context) {

    private val appContext = context.applicationContext
    private val artistCache = ConcurrentHashMap<String, String>()

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

    fun songs(): List<AriaSong> {
        return songFiles().map { it.toSong() }
    }

    fun song(id: String?): AriaSong? {
        if (id.isNullOrBlank()) return null
        if (id.contains(File.separatorChar) || id.contains("..")) return null

        val file = File(musicDir, id)
        val valid = file.isFile &&
            file.extension.equals("mp3", ignoreCase = true) &&
            file.length() > 0L

        return if (valid) file.toSong() else null
    }

    fun randomSong(): AriaSong? = songs().randomOrNull()

    /** Artist from the file's embedded metadata, if available. Cached per song. */
    suspend fun artistOf(song: AriaSong): String? {
        val cached = artistCache[song.id]
        if (cached != null) return cached.takeIf { it.isNotEmpty() }

        return withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(song.file.absolutePath)
                val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)

                // ConcurrentHashMap does not allow null values.
                // We use an empty string as a sentinel for "no metadata found".
                artistCache[song.id] = artist ?: ""
                artist
            } catch (_: Exception) {
                artistCache[song.id] = ""
                null
            } finally {
                retriever.release()
            }
        }
    }

    fun deleteSong(id: String) {
        artistCache.remove(id)
        song(id)?.file?.takeIf { it.exists() }?.delete()
    }

    fun musicDirectory(): File = musicDir

    private fun songFiles(): List<File> {
        return musicDir.listFiles { file ->
            file.isFile && file.extension.equals("mp3", ignoreCase = true) && file.length() > 0L
        }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()
    }

    private fun File.toSong() = AriaSong(
        id = name,
        title = nameWithoutExtension,
        file = this
    )

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
