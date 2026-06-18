package it.apconsulting.fotocommesse

import android.net.Uri

/**
 * Descrive una singola foto da sincronizzare.
 * @param uri URI MediaStore del file salvato localmente
 * @param fileName nome del file (es. "12345_1.jpg" o "12345-3+2.jpg")
 * @param mode sezione di appartenenza (BLOCCHI/LASTRE), determina la cartella remota
 */
data class SyncItem(
    val uri: Uri,
    val fileName: String,
    val mode: Mode
)

/**
 * Esito di un'operazione di upload.
 */
sealed class SyncResult {
    /** Upload completato con successo (o non necessario, es. LocalOnly). */
    data object Success : SyncResult()

    /**
     * Upload fallito.
     * @param reason descrizione leggibile dell'errore
     * @param retryable true se ha senso ritentare (es. rete assente), false se è un errore definitivo (es. credenziali errate)
     */
    data class Failure(val reason: String, val retryable: Boolean) : SyncResult()
}
