package it.apconsulting.fotocommesse

import com.journeyapps.barcodescanner.CaptureActivity

/**
 * CaptureActivity custom che NON forza l'orientamento landscape (default della libreria ZXing).
 * L'orientamento effettivo è definito nell'AndroidManifest con screenOrientation="fullSensor",
 * così la schermata di scansione segue l'orientamento fisico del dispositivo.
 */
class OrientationCaptureActivity : CaptureActivity()
