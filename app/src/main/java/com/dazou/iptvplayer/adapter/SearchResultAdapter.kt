package com.dazou.iptvplayer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dazou.iptvplayer.R
import com.dazou.iptvplayer.model.SearchResultItem

class SearchResultAdapter(
    private val items: List<SearchResultItem>,
    private val liveLabel: String,
    private val movieLabel: String,
    private val seriesLabel: String,
    private val onClick: (SearchResultItem) -> Unit
) : RecyclerView.Adapter<SearchResultAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.ivResultIcon)
        val name: TextView = view.findViewById(R.id.tvResultName)
        val meta: TextView = view.findViewById(R.id.tvResultMeta)
        val type: TextView = view.findViewById(R.id.tvResultType)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        when (item) {
            is SearchResultItem.Channel -> {
                holder.name.text = item.channel.name
                holder.meta.text = ""
                holder.meta.visibility = View.GONE
                holder.type.text = liveLabel
                Glide.with(holder.icon).load(item.channel.streamIcon)
                    .placeholder(R.color.panel_darker)
                    .into(holder.icon)
            }
            is SearchResultItem.Movie -> {
                holder.name.text = item.movie.name
                val metaParts = mutableListOf<String>()
                if (item.movie.year.isNotBlank()) metaParts.add(item.movie.year)
                if (item.movie.genre.isNotBlank()) metaParts.add(item.movie.genre)
                holder.meta.text = metaParts.joinToString("  •  ")
                holder.meta.visibility = if (metaParts.isEmpty()) View.GONE else View.VISIBLE
                holder.type.text = movieLabel
                Glide.with(holder.icon).load(item.movie.streamIcon)
                    .placeholder(R.color.panel_darker)
                    .into(holder.icon)
            }
            is SearchResultItem.Series -> {
                holder.name.text = item.series.name
                val metaParts = mutableListOf<String>()
                if (item.series.year.isNotBlank()) metaParts.add(item.series.year)
                if (item.series.genre.isNotBlank()) metaParts.add(item.series.genre)
                holder.meta.text = metaParts.joinToString("  •  ")
                holder.meta.visibility = if (metaParts.isEmpty()) View.GONE else View.VISIBLE
                holder.type.text = seriesLabel
                Glide.with(holder.icon).load(item.series.cover)
                    .placeholder(R.color.panel_darker)
                    .into(holder.icon)
            }
        }
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size
}