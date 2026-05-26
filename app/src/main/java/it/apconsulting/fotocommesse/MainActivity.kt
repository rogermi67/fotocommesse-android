package it.apconsulting.fotocommesse

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import it.apconsulting.fotocommesse.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mode: Mode

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            val cameraGranted = results[Manifest.permission.CAMERA] ?: false
            if (!cameraGranted) {
                Toast.makeText(
                    this,
                    "Il permesso fotocamera è obbligatorio",
                    Toast.LENGTH_LONG
                ).show()
            }
            refreshList()
        }

    private val barcodeScanner = registerForActivityResult(ScanContract()) { result ->
        val contents = result.contents
        if (!contents.isNullOrBlank()) {
            binding.etCommessa.setText(contents)
            binding.etCommessa.setSelection(contents.length)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mode = Mode.fromIntent(intent)
        applyModeUi()

        setSupportActionBar(binding.toolbar)

        binding.btnStart.isEnabled = false
        binding.etCommessa.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.btnStart.isEnabled = !s.isNullOrBlank()
            }
        })

        binding.btnStart.setOnClickListener { onStartClicked() }
        binding.tilCommessa.setEndIconOnClickListener { launchBarcodeScanner() }

        binding.rvRecenti.layoutManager = LinearLayoutManager(this)

        ensurePermissions()
    }

    private fun applyModeUi() {
        val titleRes = when (mode) {
            Mode.BLOCCHI -> R.string.section_blocchi_title
            Mode.LASTRE -> R.string.section_lastre_title
        }
        title = getString(titleRes)
        binding.toolbar.title = getString(titleRes)

        binding.tvSubtitle.setText(
            when (mode) {
                Mode.BLOCCHI -> R.string.subtitle_blocchi
                Mode.LASTRE -> R.string.subtitle_lastre
            }
        )

        binding.tilCommessa.hint = when (mode) {
            Mode.BLOCCHI -> getString(R.string.hint_commessa)
            Mode.LASTRE -> getString(R.string.hint_lastra)
        }

        binding.tvSectionRecenti.setText(
            when (mode) {
                Mode.BLOCCHI -> R.string.section_recenti_blocchi
                Mode.LASTRE -> R.string.section_recenti_lastre
            }
        )
    }

    override fun onResume() {
        super.onResume()
        if (hasCameraPermission() && hasStoragePermission()) {
            refreshList()
        }
    }

    private fun launchBarcodeScanner() {
        if (!hasCameraPermission()) {
            requestPermissions.launch(arrayOf(Manifest.permission.CAMERA))
            return
        }
       val options = ScanOptions().apply {
            setPrompt(getString(R.string.barcode_prompt))
            setBeepEnabled(true)
            setOrientationLocked(false)
            setBarcodeImageEnabled(false)
            captureActivity = OrientationCaptureActivity::class.java
        }
        barcodeScanner.launch(options)
    }

    private fun onStartClicked() {
        val raw = binding.etCommessa.text.toString().trim()
        if (raw.isBlank()) return
        val sanitized = PhotoStorage.sanitize(raw)

        if (sanitized.isBlank()) {
            Toast.makeText(this, "Valore non valido", Toast.LENGTH_SHORT).show()
            return
        }

        if (!PhotoStorage.isValid(sanitized, mode)) {
            val msg = if (mode == Mode.LASTRE) {
                "Formato lastra non valido.\nUsa codice-progressivo (es. 12345-3)"
            } else {
                "Valore non valido"
            }
            AlertDialog.Builder(this)
                .setTitle("Input non valido")
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .show()
            return
        }

        if (sanitized != raw) {
            AlertDialog.Builder(this)
                .setTitle("Caratteri non consentiti")
                .setMessage("Verrà salvato come \"$sanitized\".\nProcedere?")
                .setPositiveButton("Conferma") { _, _ -> launchCamera(sanitized) }
                .setNegativeButton("Annulla", null)
                .show()
        } else {
            launchCamera(sanitized)
        }
    }

    private fun launchCamera(key: String) {
        val intent = Intent(this, CameraActivity::class.java).apply {
            putExtra(CameraActivity.EXTRA_KEY, key)
            putExtra(Mode.EXTRA_MODE, mode.name)
        }
        startActivity(intent)
    }

    private fun refreshList() {
        lifecycleScope.launch {
            val counts = withContext(Dispatchers.IO) {
                PhotoStorage.listKeysWithCount(this@MainActivity, mode)
            }
            val items = counts.entries.sortedByDescending { it.key }
            binding.rvRecenti.adapter = CommesseAdapter(
                items,
                onClick = { key ->
                    binding.etCommessa.setText(key)
                    binding.etCommessa.setSelection(key.length)
                },
                onLongClick = { key ->
                    val intent = Intent(this@MainActivity, GalleryActivity::class.java).apply {
                        putExtra(GalleryActivity.EXTRA_KEY, key)
                        putExtra(Mode.EXTRA_MODE, mode.name)
                    }
                    startActivity(intent)
                }
            )
            binding.tvNoData.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            val tot = counts.values.sum()
            binding.tvTotalCount.text = if (mode == Mode.LASTRE) {
                "${items.size} lastre, $tot foto totali"
            } else {
                "${items.size} commesse, $tot foto totali"
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_gallery -> {
                val intent = Intent(this, GalleryActivity::class.java).apply {
                    putExtra(Mode.EXTRA_MODE, mode.name)
                }
                startActivity(intent)
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun ensurePermissions() {
        val needed = mutableListOf<String>()
        if (!hasCameraPermission()) needed += Manifest.permission.CAMERA
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
            != PackageManager.PERMISSION_GRANTED
        ) {
            needed += Manifest.permission.READ_MEDIA_IMAGES
        }
        if (needed.isNotEmpty()) {
            requestPermissions.launch(needed.toTypedArray())
        }
    }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
