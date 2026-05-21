package it.apconsulting.fotocommesse

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import it.apconsulting.fotocommesse.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.settings_title)
        }

        val currentBlocchi = SettingsManager.getBlocchiFolderName(this)
        val currentLastre = SettingsManager.getLastreFolderName(this)
        binding.etFolderBlocchi.setText(currentBlocchi)
        binding.etFolderLastre.setText(currentLastre)
        updatePreview()

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updatePreview()
            }
        }
        binding.etFolderBlocchi.addTextChangedListener(watcher)
        binding.etFolderLastre.addTextChangedListener(watcher)

        binding.btnSave.setOnClickListener { onSave() }

        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "?"
        } catch (e: Exception) {
            "?"
        }
        binding.tvVersion.text = getString(R.string.settings_info_version, versionName)
    }

    private fun updatePreview() {
        val b = PhotoStorage.sanitize(binding.etFolderBlocchi.text?.toString().orEmpty())
        val l = PhotoStorage.sanitize(binding.etFolderLastre.text?.toString().orEmpty())
        binding.tvPreviewBlocchi.text = "Pictures/${if (b.isBlank()) "—" else b}/"
        binding.tvPreviewLastre.text = "Pictures/${if (l.isBlank()) "—" else l}/"
    }

    private fun onSave() {
        val rawB = binding.etFolderBlocchi.text?.toString().orEmpty()
        val rawL = binding.etFolderLastre.text?.toString().orEmpty()
        val sB = PhotoStorage.sanitize(rawB)
        val sL = PhotoStorage.sanitize(rawL)
        if (sB.isBlank() || sL.isBlank()) {
            Toast.makeText(this, R.string.toast_invalid_folder, Toast.LENGTH_SHORT).show()
            return
        }
        if (sB == sL) {
            Toast.makeText(this, "Le due cartelle devono essere diverse", Toast.LENGTH_LONG).show()
            return
        }
        SettingsManager.setBlocchiFolderName(this, sB)
        SettingsManager.setLastreFolderName(this, sL)
        Toast.makeText(this, "Impostazioni salvate", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
