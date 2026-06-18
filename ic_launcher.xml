package it.apconsulting.fotocommesse

import android.content.Context
import android.util.Log

/**
 * Punto centrale che instrada le foto verso il provider di sincronizzazione attivo.
 *
 * Il resto dell'app (es. CameraActivity) chiama solo SyncManager.onPhotoSaved(...),
 * senza conoscere quale provider è configurato.
 *
 * Per ora gestisce LocalOnly (no-op). Quando aggiungeremo provider cloud,
 * qui introdurremo l'accodamento via WorkManager per upload resilienti in background.
 */
object SyncManager {

    private const val TAG = "SyncManager"

    /**
     * Restituisce l'istanza del provider attualmente selezionato nelle impostazioni.
     */
    fun providerFor(type: SyncProviderType): SyncProvider {
        return when (type) {
            SyncProviderType.LOCAL_ONLY -> LocalOnlyProvider()
            // SyncProviderType.PCLOUD -> PCloudProvider()
            // SyncProviderType.DROPBOX -> DropboxProvider()
            // ...
        }
    }

    fun activeProvider(context: Context): SyncProvider =
        providerFor(SettingsManager.getSyncProviderType(context))

    /**
     * Notifica che una nuova foto è stata salvata localmente.
     *
     * Per LocalOnly non fa nulla (la sync è esterna). Per i provider cloud futuri,
     * questo sarà il punto in cui la foto viene accodata per l'upload in background.
     */
    suspend fun onPhotoSaved(context: Context, item: SyncItem): SyncResult {
        val provider = activeProvider(context)
        if (!provider.isConfigured(context)) {
            Log.w(TAG, "Provider ${provider.type.id} non configurato, skip upload")
            return SyncResult.Failure("Provider non configurato", retryable = false)
        }
        return try {
            provider.upload(context, item)
        } catch (e: Exception) {
            Log.e(TAG, "Errore upload", e)
            SyncResult.Failure(e.message ?: "Errore sconosciuto", retryable = true)
        }
    }
}
