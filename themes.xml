package it.apconsulting.fotocommesse

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CommesseAdapter(
    private val items: List<Map.Entry<String, Int>>,
    private val onClick: (String) -> Unit,
    private val onLongClick: (String) -> Unit
) : RecyclerView.Adapter<CommesseAdapter.VH>() {

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCommessa: TextView = itemView.findViewById(R.id.tvItemCommessa)
        val tvCount: TextView = itemView.findViewById(R.id.tvItemCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_commessa, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val entry = items[position]
        holder.tvCommessa.text = entry.key
        holder.tvCount.text = "${entry.value} foto"
        holder.itemView.setOnClickListener { onClick(entry.key) }
        holder.itemView.setOnLongClickListener {
            onLongClick(entry.key)
            true
        }
    }

    override fun getItemCount(): Int = items.size
}
