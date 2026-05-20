package it.apconsulting.fotocommesse

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore

object PhotoStorage {

    const val FOLDER_NAME = "FotoCommesse"
    val RELATIVE_PATH = "${Environment.DIRECTORY_PICTURES}/$FOLDER_NAME"

    /**
     * Returns the next progressive index for a given commessa, scanning existing
     * files in Pictures/FotoCommesse whose name matches `{commessa}_{N}.jpg`.
     * If none exist, returns 1.
     */
    fun nextIndex(context: Context, commessa: String): Int {
        val projection = arrayOf(MediaStore.Images.Media.DISPLAY_NAME)
        val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ? " +
                "AND ${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$FOLDER_NAME%", "${commessa}_%")

        var maxIdx = 0
        val pattern = Regex("^${Regex.escape(commessa)}_(\\d+)\\.[^.]+$")

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection, selection, selectionArgs, null
        )?.use { cursor ->
            val nameIdx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIdx) ?: continue
                pattern.find(name)?.groupValues?.get(1)?.toIntOrNull()?.let {
                    if (it > maxIdx) maxIdx = it
                }
            }
        }
        return maxIdx + 1
    }

    /**
     * Lists all commesse found in the FotoCommesse folder with their photo count.
     */
    fun listCommesseWithCount(context: Context): Map<String, Int> {
        val projection = arrayOf(MediaStore.Images.Media.DISPLAY_NAME)
        val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("%$FOLDER_NAME%")

        val counts = mutableMapOf<String, Int>()
        val pattern = Regex("^(.+)_(\\d+)\\.[^.]+$")

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection, selection, selectionArgs,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val nameIdx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIdx) ?: continue
                pattern.find(name)?.groupValues?.get(1)?.let { commessa ->
                    counts[commessa] = (counts[commessa] ?: 0) + 1
                }
            }
        }
        return counts
    }

    /**
     * Builds ContentValues for a new photo of the given commessa with the given index.
     */
    fun buildContentValues(commessa: String, index: Int): ContentValues {
        val fileName = "${commessa}_${index}.jpg"
        return ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, RELATIVE_PATH)
        }
    }

    /**
     * Sanitizes a commessa string: keeps alphanumerics, dash, underscore.
     * Replaces everything else with underscore and trims surrounding underscores.
     */
    fun sanitize(input: String): String {
        val cleaned = input.trim().replace(Regex("[^A-Za-z0-9_\\-]"), "_")
        return cleaned.trim('_').ifEmpty { "commessa" }
    }
}
