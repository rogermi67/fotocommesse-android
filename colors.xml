package it.apconsulting.fotocommesse

/**
 * Tipi di provider di sincronizzazione supportati dall'app.
 * L'id viene salvato nelle SharedPreferences; il displayName è mostrato nelle Impostazioni.
 *
 * Per aggiungere un nuovo provider (es. pCloud, Dropbox, OneDrive, REST):
 *  1. aggiungere una voce qui
 *  2. creare la classe che implementa SyncProvider
 *  3. registrarla in SyncManager.providerFor()
 */
enum class SyncProviderType(val id: String, val displayName: String) {
    LOCAL_ONLY("local", "Solo locale (sync esterna, es. FolderSync)");
    // PCLOUD("pcloud", "pCloud"),
    // DROPBOX("dropbox", "Dropbox"),
    // ONEDRIVE("onedrive", "OneDrive"),
    // REST("rest", "Server REST");

    companion object {
        fun fromId(id: String?): SyncProviderType =
            entries.firstOrNull { it.id == id } ?: LOCAL_ONLY
    }
}
