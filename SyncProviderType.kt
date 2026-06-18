package it.apconsulting.fotocommesse

import android.net.Uri

data class PhotoItem(
    val uri: Uri,
    val displayName: String,
    val dateAdded: Long,
    val sizeBytes: Long
)
