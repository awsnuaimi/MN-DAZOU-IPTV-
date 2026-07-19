package com.dazou.iptvplayer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dazou.iptvplayer.R
import com.dazou.iptvplayer.model.HistoryItem

class HistoryAdapter(
    private val items: List<HistoryItem>,
    private val onClick: (HistoryItem) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val poster: ImageView = view.findViewById(R.id.ivFavoritePoster)
        val type: TextView = view.findViewById(R.id.tvFavoriteType)
        val name: TextView = view.findViewById(R.id.tvFavoriteName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.name.text = item.name
        holder.type.text = when (item.type) {
            "live" -> "📡 قناة"
            "movie" -> "🎬 فيلم"
            "series" -> "📺 مسلسل"
            else -> item.type
        }

        Glide.with(holder.poster.context)
            .load(item.icon)
            .placeholder(R.drawable.ic_live_tv)
            .error(R.drawable.ic_live_tv)
            .into(holder.poster)

        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size
}