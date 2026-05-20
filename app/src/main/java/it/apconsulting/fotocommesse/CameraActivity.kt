package it.apconsulting.fotocommesse

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import it.apconsulting.fotocommesse.databinding.ActivityCameraBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    private lateinit var commessa: String
    private var photoCount: Int = 0
    private var toneGen: ToneGenerator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        commessa = intent.getStringExtra(EXTRA_COMMESSA).orEmpty()
        if (commessa.isBlank()) {
            finish()
            return
        }

        // photoCount = highest existing index for this commessa
        photoCount = PhotoStorage.nextIndex(this, commessa) - 1
        updateUi()

        cameraExecutor = Executors.newSingleThreadExecutor()
        toneGen = try {
            ToneGenerator(AudioManager.STREAM_MUSIC, 60)
        } catch (_: RuntimeException) {
            null
        }

        startCamera()

        binding.btnShoot.setOnClickListener { takePhoto() }
        binding.btnDone.setOnClickListener { finish() }
    }

    private fun updateUi() {
        binding.tvCommessa.text = "Commessa: $commessa"
        binding.tvCount.text =
            "Scattate: $photoCount\nProssima foto: #${photoCount + 1}"
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            val selector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, selector, preview, imageCapture)
            } catch (e: Exception) {
                Log.e(TAG, "Bind failed", e)
                Toast.makeText(this, "Errore inizializzazione fotocamera", Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val capture = imageCapture ?: return
        binding.btnShoot.isEnabled = false

        val nextIdx = photoCount + 1
        val contentValues = PhotoStorage.buildContentValues(this, commessa, nextIdx)

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Capture failed", exc)
                    Toast.makeText(
                        this@CameraActivity,
                        "Errore salvataggio: ${exc.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.btnShoot.isEnabled = true
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    photoCount = nextIdx
                    updateUi()
                    toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
                    Toast.makeText(
                        this@CameraActivity,
                        "Salvata ${commessa}_${nextIdx}.jpg",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.btnShoot.isEnabled = true
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        toneGen?.release()
        toneGen = null
    }

    companion object {
        private const val TAG = "CameraActivity"
        const val EXTRA_COMMESSA = "commessa"
    }
}
