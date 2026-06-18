package it.apconsulting.fotocommesse

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import it.apconsulting.fotocommesse.databinding.ActivityLandingBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LandingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLandingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLandingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        binding.cardBlocchi.setOnClickListener {
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    putExtra(Mode.EXTRA_MODE, Mode.BLOCCHI.name)
                }
            )
        }

        binding.cardLastre.setOnClickListener {
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    putExtra(Mode.EXTRA_MODE, Mode.LASTRE.name)
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPhotoStats()
    }

    private fun refreshPhotoStats() {
        lifecycleScope.launch {
            val total = withContext(Dispatchers.IO) {
                PhotoStorage.totalPhotoCount(this@LandingActivity)
            }
            val provider = SettingsManager.getSyncProviderType(this@LandingActivity)
            binding.tvPhotoStats.text = when (provider) {
                SyncProviderType.LOCAL_ONLY ->
                    getString(R.string.landing_photo_stats_local, total)
            }
        }
    }
}
