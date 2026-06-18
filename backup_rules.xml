package it.apconsulting.fotocommesse

import android.content.Context

/**
 * Contratto astratto per un metodo di sincronizzazione delle foto.
 *
 * Ogni implementazione concreta (LocalOnly, pCloud, Dropbox, OneDrive, REST...)
 * sa caricare una foto verso una destinazione, senza che il resto dell'app
 * debba conoscere i dettagli. Pattern Strategy.
 */
interface SyncProvider {

    /** Tipo del provider. */
    val type: SyncProviderType

    /**
     * Indica se il provider è configurato e pronto all'uso
     * (es. credenziali presenti, account collegato).
     * LocalOnly è sempre pronto.
     */
    fun isConfigured(context: Context): Boolean

    /**
     * Carica una foto verso la destinazione del provider.
     * È una funzione sospendibile: va invocata da una coroutine / worker.
     *
     * @return SyncResult.Success se l'upload è andato a buon fine (o non necessario),
     *         SyncResult.Failure altrimenti.
     */
    suspend fun upload(context: Context, item: SyncItem): SyncResult
}
