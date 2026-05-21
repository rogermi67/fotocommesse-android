package it.apconsulting.fotocommesse

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore

object PhotoStorage {

    fun folderName(context: Context, mode: Mode): String =
        SettingsManager.folderForMode(context, mode)

    fun relativePath(context: Context, mode: Mode): String =
        "${Environment.DIRECTORY_PICTURES}/${folderName(context, mode)}"

    /**
     * Validates input for the given mode.
     * - BLOCCHI: any non-blank alphanumeric/underscore/dash string
     * - LASTRE: must end with "-N" where N is numeric (formato codice-progressivo)
     */
    fun isValid(input: String, mode: Mode): Boolean {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return false
        return when (mode) {
            Mode.BLOCCHI -> true
            Mode.LASTRE -> Regex("^.+-\\d+$").matches(trimmed)
        }
    }

    /**
     * Returns the next filename for a new photo of the given key (commessa or lastra).
     * - BLOCCHI: "{key}_{N}.jpg" with N = max existing N + 1 (starting from 1)
     * - LASTRE: "{key}.jpg" if no existing, else "{key}+{N}.jpg" with N = max existing + 1
     */
    fun nextFileName(context: Context, key: String, mode: Mode): String {
        return when (mode) {
            Mode.BLOCCHI -> {
                val maxN = maxBlocchiIndex(context, key)
                "${key}_${maxN + 1}.jpg"
            }
            Mode.LASTRE -> {
                val maxN = maxLastreIndex(context, key)
                if (maxN < 0) "${key}.jpg"
                else "${key}+${maxN + 1}.jpg"
            }
        }
    }

    /**
     * Returns the count of photos existing for the given key in the given mode's folder.
     */
    fun photoCount(context: Context, key: String, mode: Mode): Int {
        return countMatchingPhotos(context, key, mode)
    }

    /**
     * Map of key -> photo count for all keys found in the given mode's folder.
     */
    fun listKeysWithCount(context: Context, mode: Mode): Map<String, Int> {
        val folder = folderName(context, mode)
        val projection = arrayOf(MediaStore.Images.Media.DISPLAY_NAME)
        val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("%$folder%")

        val counts = mutableMapOf<String, Int>()
        val pattern = when (mode) {
            Mode.BLOCCHI -> Regex("^(.+)_(\\d+)\\.[^.]+$")
            Mode.LASTRE -> Regex("^(.+?)(\\+\\d+)?\\.[^.]+$")
        }

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection, selection, selectionArgs,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val nameIdx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIdx) ?: continue
                pattern.find(name)?.groupValues?.get(1)?.let { key ->
                    counts[key] = (counts[key] ?: 0) + 1
                }
            }
        }
        return counts
    }

    /**
     * Lists photos in the given mode's folder, optionally filtered by key.
     */
    fun listAllPhotos(
        context: Context,
        mode: Mode,
        keyFilter: String? = null
    ): List<PhotoItem> {
        val folder = folderName(context, mode)
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.SIZE
        )

        val selection: String
        val selectionArgs: Array<String>
        if (!keyFilter.isNullOrBlank()) {
            selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ? " +
                    "AND ${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
            selectionArgs = arrayOf("%$folder%", "${keyFilter}%")
        } else {
            selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
            selectionArgs = arrayOf("%$folder%")
        }

        val items = mutableListOf<PhotoItem>()
        val matchPattern = keyFilter?.let { filter ->
            when (mode) {
                Mode.BLOCCHI -> Regex("^${Regex.escape(filter)}_(\\d+)\\.[^.]+$")
                Mode.LASTRE -> Regex("^${Regex.escape(filter)}(\\+\\d+)?\\.[^.]+$")
            }
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
                if (matchPattern != null && matchPattern.find(name) == null) continue
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

    /**
     * Builds ContentValues for a new photo using an exact filename.
     */
    fun buildContentValues(context: Context, fileName: String, mode: Mode): ContentValues {
        return ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, relativePath(context, mode))
        }
    }

    /**
     * Sanitizes a key (commessa/lastra) or folder name.
     */
    fun sanitize(input: String): String {
        val cleaned = input.trim().replace(Regex("[^A-Za-z0-9_\\-]"), "_")
        return cleaned.trim('_')
    }

    // -- internals ---------------------------------------------------------

    private fun maxBlocchiIndex(context: Context, key: String): Int {
        val folder = folderName(context, Mode.BLOCCHI)
        val projection = arrayOf(MediaStore.Images.Media.DISPLAY_NAME)
        val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ? " +
                "AND ${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$folder%", "${key}_%")

        var maxIdx = 0
        val pattern = Regex("^${Regex.escape(key)}_(\\d+)\\.[^.]+$")

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
        return maxIdx
    }

    /**
     * Returns:
     *  -1 if no photos exist for this lastra,
     *   0 if only "{key}.jpg" exists,
     *   N if "{key}+{N}.jpg" exists (with N = max existing suffix).
     */
    private fun maxLastreIndex(context: Context, key: String): Int {
        val folder = folderName(context, Mode.LASTRE)
        val projection = arrayOf(MediaStore.Images.Media.DISPLAY_NAME)
        val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ? " +
                "AND ${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$folder%", "${key}%")

        var found = false
        var maxIdx = 0
        val baseName = "${key}.jpg"
        val suffixPattern = Regex("^${Regex.escape(key)}\\+(\\d+)\\.[^.]+$")

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection, selection, selectionArgs, null
        )?.use { cursor ->
            val nameIdx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIdx) ?: continue
                if (name == baseName) {
                    found = true
                } else {
                    suffixPattern.find(name)?.groupValues?.get(1)?.toIntOrNull()?.let {
                        found = true
                        if (it > maxIdx) maxIdx = it
                    }
                }
            }
        }
        return if (!found) -1 else maxIdx
    }

    private fun countMatchingPhotos(context: Context, key: String, mode: Mode): Int {
        val folder = folderName(context, mode)
        val projection = arrayOf(MediaStore.Images.Media.DISPLAY_NAME)
        val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ? " +
                "AND ${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$folder%", "${key}%")

        val pattern = when (mode) {
            Mode.BLOCCHI -> Regex("^${Regex.escape(key)}_(\\d+)\\.[^.]+$")
            Mode.LASTRE -> Regex("^${Regex.escape(key)}(\\+\\d+)?\\.[^.]+$")
        }

        var count = 0
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection, selection, selectionArgs, null
        )?.use { cursor ->
            val nameIdx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIdx) ?: continue
                if (pattern.matches(name)) count++
            }
        }
        return count
    }
}
