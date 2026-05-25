package it.apconsulting.fotocommesse

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import it.apconsulting.fotocommesse.databinding.ActivityLandingBinding

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
}
