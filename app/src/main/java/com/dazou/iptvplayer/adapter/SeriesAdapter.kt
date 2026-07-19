package com.dazou.iptvplayer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dazou.iptvplayer.R
import com.dazou.iptvplayer.data.FavoritesManager
import com.dazou.iptvplayer.model.FavoriteItem
import com.dazou.iptvplayer.model.XtreamSeries

class SeriesAdapter(
    private val seriesList: List<XtreamSeries>,
    private val favoritesManager: FavoritesManager,
    private val onClick: (XtreamSeries) -> Unit
) : RecyclerView.Adapter<SeriesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val poster: ImageView = view.findViewById(R.id.ivSeriesPoster)
        val name: TextView = view.findViewById(R.id.tvSeriesName)
        val favoriteBadge: ImageView = view.findViewById(R.id.ivFavoriteBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_series, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = seriesList[position]
        holder.name.text = item.name

        Glide.with(holder.poster.context)
            .load(item.cover)
            .placeholder(R.color.panel_darker)
            .error(R.color.panel_darker)
            .into(holder.poster)

        holder.favoriteBadge.visibility =
            if (favoritesManager.isFavorite("series", item.seriesId)) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener { onClick(item) }

        holder.itemView.setOnLongClickListener {
            val added = favoritesManager.toggleFavorite(
                FavoriteItem("series", item.seriesId, item.name, item.cover, "mp4")
            )
            holder.favoriteBadge.visibility = if (added) View.VISIBLE else View.GONE
            Toast.makeText(
                holder.itemView.context,
                if (added) "⭐ أُضيف للمفضلة" else "تم الحذف من المفضلة",
                Toast.LENGTH_SHORT
            ).show()
            true
        }
    }

    override fun getItemCount() = seriesList.size
}