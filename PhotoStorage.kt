package it.apconsulting.fotocommesse

import android.content.Context

/**
 * Provider "solo locale": non esegue alcun upload.
 *
 * La foto è già salvata localmente da MediaStore in Pictures/{cartella}/.
 * La sincronizzazione verso il cloud è demandata a un'app esterna (es. FolderSync),
 * esattamente come nel funzionamento attuale dell'app.
 *
 * upload() è quindi un no-op che ritorna sempre Success.
 */
class LocalOnlyProvider : SyncProvider {

    override val type: SyncProviderType = SyncProviderType.LOCAL_ONLY

    override fun isConfigured(context: Context): Boolean = true

    override suspend fun upload(context: Context, item: SyncItem): SyncResult {
        // Nessuna azione: la sync è gestita esternamente.
        return SyncResult.Success
    }
}
