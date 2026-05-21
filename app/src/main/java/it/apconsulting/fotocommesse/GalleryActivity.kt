package it.apconsulting.fotocommesse

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import it.apconsulting.fotocommesse.databinding.ActivityGalleryBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GalleryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGalleryBinding
    private lateinit var adapter: GalleryAdapter
    private var photos: List<PhotoItem> = emptyList()
    private val selectedUris = mutableSetOf<Uri>()
    private var inSelectionMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.gallery_title)
        }

        adapter = GalleryAdapter(
            onTap = { item -> onItemTap(item) },
            onLongPress = { item -> onItemLongPress(item) }
        )
        binding.recyclerView.layoutManager = GridLayoutManager(this, 3)
        binding.recyclerView.adapter = adapter

        binding.tvHelp.text = getString(R.string.gallery_help)
    }

    override fun onResume() {
        super.onResume()
        loadPhotos()
    }

    private fun loadPhotos() {
        lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) {
                PhotoStorage.listAllPhotos(this@GalleryActivity)
            }
            photos = items
            adapter.submitList(items)
            updateEmptyState()
            updateTitle()
        }
    }

    private fun updateEmptyState() {
        if (photos.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
            binding.tvHelp.visibility = View.GONE
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
            binding.tvHelp.visibility = View.VISIBLE
        }
    }

    private fun updateTitle() {
        supportActionBar?.title = if (inSelectionMode) {
            getString(R.string.gallery_selected, selectedUris.size)
        } else {
            getString(R.string.gallery_title)
        }
        supportActionBar?.subtitle = if (!inSelectionMode) {
            getString(R.string.gallery_total, photos.size)
        } else null
    }

    private fun onItemTap(item: PhotoItem) {
        if (inSelectionMode) {
            toggleSelection(item)
        } else {
            // Open in external viewer
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(item.uri, "image/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            try {
                startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(this, "Nessuna app per aprire l'immagine", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onItemLongPress(item: PhotoItem) {
        if (!inSelectionMode) {
            inSelectionMode = true
        }
        toggleSelection(item)
        invalidateOptionsMenu()
    }

    private fun toggleSelection(item: PhotoItem) {
        if (selectedUris.contains(item.uri)) {
            selectedUris.remove(item.uri)
        } else {
            selectedUris.add(item.uri)
        }
        if (selectedUris.isEmpty()) {
            inSelectionMode = false
            invalidateOptionsMenu()
        }
        adapter.setSelection(selectedUris)
        updateTitle()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.gallery_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.action_delete)?.isVisible = inSelectionMode
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_delete -> {
                confirmDelete()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        if (inSelectionMode) {
            exitSelection()
        } else {
            finish()
        }
        return true
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (inSelectionMode) {
            exitSelection()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    private fun exitSelection() {
        inSelectionMode = false
        selectedUris.clear()
        adapter.setSelection(emptySet())
        invalidateOptionsMenu()
        updateTitle()
    }

    private fun confirmDelete() {
        val count = selectedUris.size
        if (count == 0) return
        AlertDialog.Builder(this)
            .setTitle(R.string.gallery_delete_title)
            .setMessage(getString(R.string.gallery_delete_message, count))
            .setPositiveButton(R.string.gallery_delete_confirm) { _, _ -> performDelete() }
            .setNegativeButton(R.string.gallery_delete_cancel, null)
            .show()
    }

    private fun performDelete() {
        val toDelete = selectedUris.toList()
        lifecycleScope.launch {
            val deleted = withContext(Dispatchers.IO) {
                PhotoStorage.deletePhotos(this@GalleryActivity, toDelete)
            }
            Toast.makeText(
                this@GalleryActivity,
                getString(R.string.gallery_delete_done, deleted),
                Toast.LENGTH_SHORT
            ).show()
            exitSelection()
            loadPhotos()
        }
    }
}
