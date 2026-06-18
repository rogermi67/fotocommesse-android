package it.apconsulting.fotocommesse

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCameraForCurrentInput()
        else Toast.makeText(this, "Permesso fotocamera negato", Toast.LENGTH_SHORT).show()
    }

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            binding.etCommessa.setText(result.contents)
            binding.etCommessa.setSelection(result.contents.length)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mode = Mode.fromIntent(intent)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.title = when (mode) {
            Mode.BLOCCHI -> getString(R.string.title_blocchi)
            Mode.LASTRE -> getString(R.string.title_lastre)
        }
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.tilCommessa.hint = when (mode) {
            Mode.BLOCCHI -> getString(R.string.hint_commessa)
            Mode.LASTRE -> getString(R.string.hint_lastra)
        }
        binding.tilCommessa.setEndIconOnClickListener { launchBarcode() }

        binding.btnAvvia.setOnClickListener { startCameraForCurrentInput() }

        binding.rvRecenti.layoutManager = LinearLayoutManager(this)

        refreshList()
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_gallery -> {
                val intent = Intent(this, GalleryActivity::class.java).apply {
                    putExtra(Mode.EXTRA_MODE, mode.name)
                }
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun startCameraForCurrentInput() {
        val raw = binding.etCommessa.text?.toString().orEmpty()
        if (!PhotoStorage.isValid(raw, mode)) {
            val msg = when (mode) {
                Mode.BLOCCHI -> "Inserisci un numero commessa"
                Mode.LASTRE -> "Codice non valido. Formato richiesto: numero-progressivo (es. 12345-3)"
            }
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            return
        }
        val key = PhotoStorage.sanitize(raw)
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
            return
        }
        launchCamera(key)
    }

    private fun launchBarcode() {
        val options = ScanOptions()
            .setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
            .setPrompt("Inquadra il codice a barre o QR")
            .setBeepEnabled(true)
            .setOrientationLocked(false)
            .setCaptureActivity(OrientationCaptureActivity::class.java)
        barcodeLauncher.launch(options)
    }

    private fun launchCamera(key: String) {
        val intent = Intent(this, CameraActivity::class.java).apply {
            putExtra(CameraActivity.EXTRA_KEY, key)
            putExtra(Mode.EXTRA_MODE, mode.name)
        }
        startActivity(intent)
    }

    private fun openNoteEditor(key: String) {
        lifecycleScope.launch {
            val current = withContext(Dispatchers.IO) {
                NoteStorage.read(this@MainActivity, key, mode)
            }
            val input = android.widget.EditText(this@MainActivity).apply {
                setText(current)
                minLines = 4
                maxLines = 10
                gravity = android.view.Gravity.TOP or android.view.Gravity.START
                setHorizontallyScrolling(false)
                setPadding(48, 32, 48, 32)
                hint = getString(R.string.note_hint)
            }
            val title = if (mode == Mode.LASTRE) "Nota lastra $key" else "Nota commessa $key"
            androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                .setTitle(title)
                .setView(input)
                .setPositiveButton(R.string.btn_save) { _, _ ->
                    val text = input.text.toString()
                    lifecycleScope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            NoteStorage.write(this@MainActivity, key, mode, text)
                        }
                        if (!ok) {
                            Toast.makeText(
                                this@MainActivity,
                                "Errore salvataggio nota",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        refreshList()
                    }
                }
                .setNegativeButton("Annulla", null)
                .setNeutralButton(R.string.note_delete) { _, _ ->
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            NoteStorage.delete(this@MainActivity, key, mode)
                        }
                        refreshList()
                    }
                }
                .show()
        }
    }

    private fun refreshList() {
        lifecycleScope.launch {
            val (counts, noted) = withContext(Dispatchers.IO) {
                val c = PhotoStorage.listKeysWithCount(this@MainActivity, mode)
                val n = c.keys.filter {
                    NoteStorage.exists(this@MainActivity, it, mode)
                }.toSet()
                c to n
            }
            val items = counts.entries.sortedByDescending { it.key }
            binding.rvRecenti.adapter = CommesseAdapter(
                items,
                notedKeys = noted,
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
                },
                onNoteClick = { key -> openNoteEditor(key) }
            )
        }
    }
}
