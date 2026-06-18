package it.apconsulting.fotocommesse

import android.content.Context
import android.content.SharedPreferences

object SettingsManager {

    private const val PREFS_NAME = "foto_blocchi_prefs"

    // Existing key kept for backward compatibility with v1.x installs (= Blocchi folder)
    private const val KEY_FOLDER_BLOCCHI = "folder_name"
    private const val KEY_FOLDER_LASTRE = "folder_name_lastre"
    private const val KEY_SYNC_PROVIDER = "sync_provider"

    const val DEFAULT_FOLDER_BLOCCHI = "FotoBlocchi"
    const val DEFAULT_FOLDER_LASTRE = "FotoLastre"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSyncProviderType(context: Context): SyncProviderType =
        SyncProviderType.fromId(prefs(context).getString(KEY_SYNC_PROVIDER, null))

    fun setSyncProviderType(context: Context, type: SyncProviderType) {
        prefs(context).edit().putString(KEY_SYNC_PROVIDER, type.id).apply()
    }

    fun getBlocchiFolderName(context: Context): String =
        prefs(context).getString(KEY_FOLDER_BLOCCHI, DEFAULT_FOLDER_BLOCCHI)
            ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_FOLDER_BLOCCHI

    fun setBlocchiFolderName(context: Context, name: String) {
        prefs(context).edit().putString(KEY_FOLDER_BLOCCHI, name).apply()
    }

    fun getLastreFolderName(context: Context): String =
        prefs(context).getString(KEY_FOLDER_LASTRE, DEFAULT_FOLDER_LASTRE)
            ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_FOLDER_LASTRE

    fun setLastreFolderName(context: Context, name: String) {
        prefs(context).edit().putString(KEY_FOLDER_LASTRE, name).apply()
    }

    fun folderForMode(context: Context, mode: Mode): String = when (mode) {
        Mode.BLOCCHI -> getBlocchiFolderName(context)
        Mode.LASTRE -> getLastreFolderName(context)
    }
}
