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

        // Barcode scanner via endIcon del TextInputLayout
        binding.tilCommessa.setEndIconOnClickListener { launchBarcodeScanner() }

        binding.rvRecenti.layoutManager = LinearLayoutManager(this)

        ensurePermissions()
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
            setOrientationLocked(true)
            setBarcodeImageEnabled(false)
        }
        barcodeScanner.launch(options)
    }

    private fun onStartClicked() {
        val raw = binding.etCommessa.text.toString().trim()
        if (raw.isBlank()) return
        val sanitized = PhotoStorage.sanitize(raw)

        if (sanitized != raw) {
            AlertDialog.Builder(this)
                .setTitle("Commessa con caratteri non validi")
                .setMessage("Verrà salvata come \"$sanitized\".\nProcedere?")
                .setPositiveButton("Conferma") { _, _ -> launchCamera(sanitized) }
                .setNegativeButton("Annulla", null)
                .show()
        } else {
            launchCamera(sanitized)
        }
    }

    private fun launchCamera(commessa: String) {
        val intent = Intent(this, CameraActivity::class.java).apply {
            putExtra(CameraActivity.EXTRA_COMMESSA, commessa)
        }
        startActivity(intent)
    }

    private fun refreshList() {
        lifecycleScope.launch {
            val counts = withContext(Dispatchers.IO) {
                PhotoStorage.listCommesseWithCount(this@MainActivity)
            }
            val items = counts.entries.sortedByDescending { it.key }
            binding.rvRecenti.adapter = CommesseAdapter(
                items,
                onClick = { commessa ->
                    binding.etCommessa.setText(commessa)
                    binding.etCommessa.setSelection(commessa.length)
                },
                onLongClick = { commessa ->
                    val intent = Intent(this@MainActivity, GalleryActivity::class.java).apply {
                        putExtra(GalleryActivity.EXTRA_COMMESSA, commessa)
                    }
                    startActivity(intent)
                }
            )
            binding.tvNoData.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            binding.tvTotalCount.text =
                "${items.size} commesse, ${counts.values.sum()} foto totali"
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_gallery -> {
                startActivity(Intent(this, GalleryActivity::class.java))
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
