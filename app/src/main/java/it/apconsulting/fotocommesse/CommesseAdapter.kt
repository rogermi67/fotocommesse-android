package it.apconsulting.fotocommesse

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CommesseAdapter(
    private val items: List<Map.Entry<String, Int>>,
    private val notedKeys: Set<String>,
    private val onClick: (String) -> Unit,
    private val onLongClick: (String) -> Unit,
    private val onNoteClick: (String) -> Unit
) : RecyclerView.Adapter<CommesseAdapter.VH>() {

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCommessa: TextView = itemView.findViewById(R.id.tvItemCommessa)
        val tvCount: TextView = itemView.findViewById(R.id.tvItemCount)
        val btnNote: ImageButton = itemView.findViewById(R.id.btnItemNote)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_commessa, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val entry = items[position]
        val key = entry.key
        holder.tvCommessa.text = key
        holder.tvCount.text = "${entry.value} foto"

        val hasNote = notedKeys.contains(key)
        holder.btnNote.setImageResource(
            if (hasNote) R.drawable.ic_note_filled else R.drawable.ic_note
        )

        holder.itemView.setOnClickListener { onClick(key) }
        holder.itemView.setOnLongClickListener {
            onLongClick(key)
            true
        }
        holder.btnNote.setOnClickListener { onNoteClick(key) }
    }

    override fun getItemCount(): Int = items.size
}
