package it.apconsulting.fotocommesse

import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GalleryAdapter(
    private val onTap: (PhotoItem) -> Unit,
    private val onLongPress: (PhotoItem) -> Unit
) : ListAdapter<PhotoItem, GalleryAdapter.VH>(DIFF) {

    private val selected = mutableSetOf<android.net.Uri>()
    private val thumbnailJobs = mutableMapOf<Int, Job>()

    fun setSelection(uris: Set<android.net.Uri>) {
        selected.clear()
        selected.addAll(uris)
        notifyDataSetChanged()
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.ivThumbnail)
        val name: TextView = itemView.findViewById(R.id.tvName)
        val selectionOverlay: View = itemView.findViewById(R.id.vSelectionOverlay)
        val checkBadge: View = itemView.findViewById(R.id.vCheckBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gallery_photo, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.name.text = item.displayName

        val isSelected = selected.contains(item.uri)
        holder.selectionOverlay.visibility = if (isSelected) View.VISIBLE else View.GONE
        holder.checkBadge.visibility = if (isSelected) View.VISIBLE else View.GONE

        // Cancel previous job for this slot
        thumbnailJobs[holder.bindingAdapterPosition]?.cancel()
        holder.image.setImageBitmap(null)

        val ctx = holder.itemView.context
        val job = CoroutineScope(Dispatchers.Main).launch {
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    ctx.contentResolver.loadThumbnail(item.uri, Size(300, 300), null)
                } catch (_: Exception) {
                    null
                }
            }
            if (bitmap != null) {
                holder.image.setImageBitmap(bitmap)
            }
        }
        thumbnailJobs[holder.bindingAdapterPosition] = job

        holder.itemView.setOnClickListener { onTap(item) }
        holder.itemView.setOnLongClickListener {
            onLongPress(item)
            true
        }
    }

    override fun onViewRecycled(holder: VH) {
        super.onViewRecycled(holder)
        thumbnailJobs[holder.bindingAdapterPosition]?.cancel()
        holder.image.setImageBitmap(null)
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<PhotoItem>() {
            override fun areItemsTheSame(oldItem: PhotoItem, newItem: PhotoItem): Boolean =
                oldItem.uri == newItem.uri
            override fun areContentsTheSame(oldItem: PhotoItem, newItem: PhotoItem): Boolean =
                oldItem == newItem
        }
    }
}
