package it.apconsulting.fotocommesse

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore

object PhotoStorage {

    fun folderName(context: Context): String = SettingsManager.getFolderName(context)

    fun relativePath(context: Context): String =
        "${Environment.DIRECTORY_PICTURES}/${folderName(context)}"

    fun nextIndex(context: Context, commessa: String): Int {
        val folder = folderName(context)
        val projection = arrayOf(MediaStore.Images.Media.DISPLAY_NAME)
        val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ? " +
                "AND ${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$folder%", "${commessa}_%")

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

    fun listCommesseWithCount(context: Context): Map<String, Int> {
        val folder = folderName(context)
        val projection = arrayOf(MediaStore.Images.Media.DISPLAY_NAME)
        val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("%$folder%")

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
     * Lists photos in the configured folder, optionally filtered by commessa prefix.
     * If commessaFilter is non-null and non-blank, only photos whose name matches
     * `{commessaFilter}_{N}.{ext}` are returned.
     */
    fun listAllPhotos(context: Context, commessaFilter: String? = null): List<PhotoItem> {
        val folder = folderName(context)
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.SIZE
        )

        val selection: String
        val selectionArgs: Array<String>
        if (!commessaFilter.isNullOrBlank()) {
            selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ? " +
                    "AND ${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
            selectionArgs = arrayOf("%$folder%", "${commessaFilter}_%")
        } else {
            selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
            selectionArgs = arrayOf("%$folder%")
        }

        val items = mutableListOf<PhotoItem>()
        val filterPattern = commessaFilter?.let {
            Regex("^${Regex.escape(it)}_(\\d+)\\.[^.]+$")
        }

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection, selection, selectionArgs,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol) ?: continue
                // If filtering by commessa, ensure exact match of the prefix (not just LIKE)
                if (filterPattern != null && filterPattern.find(name) == null) continue
                val date = cursor.getLong(dateCol)
                val size = cursor.getLong(sizeCol)
                val uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                )
                items.add(PhotoItem(uri, name, date, size))
            }
        }
        return items
    }

    fun deletePhotos(context: Context, uris: List<Uri>): Int {
        var deleted = 0
        uris.forEach { uri ->
            try {
                val rows = context.contentResolver.delete(uri, null, null)
                if (rows > 0) deleted++
            } catch (_: SecurityException) {
            } catch (_: Exception) {
            }
        }
        return deleted
    }

    fun buildContentValues(context: Context, commessa: String, index: Int): ContentValues {
        val fileName = "${commessa}_${index}.jpg"
        return ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, relativePath(context))
        }
    }

    fun sanitize(input: String): String {
        val cleaned = input.trim().replace(Regex("[^A-Za-z0-9_\\-]"), "_")
        return cleaned.trim('_')
    }
}
