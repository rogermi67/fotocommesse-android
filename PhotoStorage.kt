package it.apconsulting.fotocommesse

import android.content.Intent

enum class Mode {
    BLOCCHI, LASTRE;

    companion object {
        const val EXTRA_MODE = "section_mode"

        fun fromIntent(intent: Intent?): Mode {
            val name = intent?.getStringExtra(EXTRA_MODE) ?: return BLOCCHI
            return runCatching { valueOf(name) }.getOrDefault(BLOCCHI)
        }
    }
}
