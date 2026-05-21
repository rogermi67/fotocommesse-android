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

        val current = SettingsManager.getFolderName(this)
        binding.etFolderName.setText(current)
        binding.etFolderName.setSelection(current.length)
        updatePreview(current)

        binding.etFolderName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val raw = s?.toString().orEmpty()
                val sanitized = PhotoStorage.sanitize(raw)
                updatePreview(if (sanitized.isBlank()) "—" else sanitized)
            }
        })

        binding.btnSave.setOnClickListener { onSave() }

        // Mostro la versione
        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "?"
        } catch (e: Exception) {
            "?"
        }
        binding.tvVersion.text = getString(R.string.settings_info_version, versionName)
    }

    private fun updatePreview(folderName: String) {
        binding.tvPreview.text = "Pictures/$folderName/"
    }

    private fun onSave() {
        val raw = binding.etFolderName.text?.toString().orEmpty()
        val sanitized = PhotoStorage.sanitize(raw)
        if (sanitized.isBlank()) {
            Toast.makeText(this, R.string.toast_invalid_folder, Toast.LENGTH_SHORT).show()
            return
        }
        SettingsManager.setFolderName(this, sanitized)
        Toast.makeText(
            this,
            getString(R.string.toast_saved, "Pictures/$sanitized/"),
            Toast.LENGTH_SHORT
        ).show()
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
