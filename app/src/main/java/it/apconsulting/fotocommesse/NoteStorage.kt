package it.apconsulting.fotocommesse

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File

/**
 * Gestisce note testuali associate a una chiave (commessa o lastra).
 *
 * Tenta di salvare il file `{key}_note.txt` nella stessa cartella delle foto
 * (es. Pictures/FotoBlocchi/12345_note.txt) usando MediaStore.Files.
 * Se Android rifiuta, fa fallback a uno storage privato dell'app.
 */
object NoteStorage {

    private const val TAG = "NoteStorage"
    private const val NOTE_SUFFIX = "_note.txt"

    private fun fileName(key: String): String = "$key$NOTE_SUFFIX"

    private fun privateNoteFile(context: Context, key: String, mode: Mode): File {
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val folder = PhotoStorage.folderName(context, mode)
        val dir = File(baseDir, "notes/$folder")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, fileName(key))
    }

    private fun findMediaStoreUri(context: Context, key: String, mode: Mode): Uri? {
        val folder = PhotoStorage.folderName(context, mode)
        val target = fileName(key)
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.RELATIVE_PATH
        )
        val selection = "${MediaStore.Files.FileColumns.RELATIVE_PATH} LIKE ? " +
                "AND ${MediaStore.Files.FileColumns.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf("%$folder%", target)
        val collection = MediaStore.Files.getContentUri("external")

        context.contentResolver.query(collection, projection, selection, selectionArgs, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                    val id = cursor.getLong(idCol)
                    return android.content.ContentUris.withAppendedId(collection, id)
                }
            }
        return null
    }

    fun read(context: Context, key: String, mode: Mode): String {
        try {
            val uri = findMediaStoreUri(context, key, mode)
            if (uri != null) {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    return stream.bufferedReader(Charsets.UTF_8).readText()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Read from MediaStore failed: ${e.message}")
        }
        val privateFile = privateNoteFile(context, key, mode)
        return if (privateFile.exists()) {
            try {
                privateFile.readText(Charsets.UTF_8)
            } catch (e: Exception) {
                Log.w(TAG, "Read from private file failed: ${e.message}")
                ""
            }
        } else ""
    }

    fun write(context: Context, key: String, mode: Mode, text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return delete(context, key, mode)
        }
        val mediaOk = writeToMediaStore(context, key, mode, trimmed)
        if (mediaOk) {
            privateNoteFile(context, key, mode).takeIf { it.exists() }?.delete()
            return true
        }
        return try {
            privateNoteFile(context, key, mode).writeText(trimmed, Charsets.UTF_8)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Write to private file failed", e)
            false
        }
    }

    private fun writeToMediaStore(
        context: Context,
        key: String,
        mode: Mode,
        text: String
    ): Boolean {
        return try {
            val existing = findMediaStoreUri(context, key, mode)
            if (existing != null) {
                context.contentResolver.openOutputStream(existing, "wt")?.use { os ->
                    os.write(text.toByteArray(Charsets.UTF_8))
                }
                return true
            }
            val folder = PhotoStorage.folderName(context, mode)
            val values = ContentValues().apply {
                put(MediaStore.Files.FileColumns.DISPLAY_NAME, fileName(key))
                put(MediaStore.Files.FileColumns.MIME_TYPE, "text/plain")
                put(
                    MediaStore.Files.FileColumns.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/$folder"
                )
            }
            val collection = MediaStore.Files.getContentUri("external")
            val uri = context.contentResolver.insert(collection, values) ?: return false
            context.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(text.toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "MediaStore write failed: ${e.message}")
            false
        }
    }

    fun exists(context: Context, key: String, mode: Mode): Boolean {
        if (findMediaStoreUri(context, key, mode) != null) return true
        return privateNoteFile(context, key, mode).exists()
    }

    fun delete(context: Context, key: String, mode: Mode): Boolean {
        var anyDeleted = false
        try {
            val uri = findMediaStoreUri(context, key, mode)
            if (uri != null) {
                val rows = context.contentResolver.delete(uri, null, null)
                if (rows > 0) anyDeleted = true
            }
        } catch (e: Exception) {
            Log.w(TAG, "MediaStore delete failed: ${e.message}")
        }
        val privateFile = privateNoteFile(context, key, mode)
        if (privateFile.exists()) {
            if (privateFile.delete()) anyDeleted = true
        }
        return anyDeleted
    }
}
