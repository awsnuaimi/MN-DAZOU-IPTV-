package com.dazou.iptvplayer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dazou.iptvplayer.R
import com.dazou.iptvplayer.model.XtreamSeries

class SeriesAdapter(
    private val seriesList: List<XtreamSeries>,
    private val onClick: (XtreamSeries) -> Unit
) : RecyclerView.Adapter<SeriesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val poster: ImageView = view.findViewById(R.id.ivSeriesPoster)
        val name: TextView = view.findViewById(R.id.tvSeriesName)
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

        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = seriesList.size
}