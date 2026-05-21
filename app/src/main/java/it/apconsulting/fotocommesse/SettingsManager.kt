package it.apconsulting.fotocommesse

import android.content.Context
import android.content.SharedPreferences

/**
 * Wrapper for app preferences (folder name, future settings).
 * NOTE: package is kept as `fotocommesse` to allow upgrade-in-place over v1.0.
 * The user-facing app name is in strings.xml.
 */
object SettingsManager {

    private const val PREFS_NAME = "foto_blocchi_prefs"
    private const val KEY_FOLDER_NAME = "folder_name"

    const val DEFAULT_FOLDER_NAME = "FotoBlocchi"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getFolderName(context: Context): String =
        prefs(context).getString(KEY_FOLDER_NAME, DEFAULT_FOLDER_NAME)
            ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_FOLDER_NAME

    fun setFolderName(context: Context, name: String) {
        prefs(context).edit().putString(KEY_FOLDER_NAME, name).apply()
    }
}
