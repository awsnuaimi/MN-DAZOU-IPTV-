package com.dazou.iptvplayer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dazou.iptvplayer.R
import com.dazou.iptvplayer.model.XtreamMovie

class MovieAdapter(
    private val movies: List<XtreamMovie>,
    private val onClick: (XtreamMovie) -> Unit
) : RecyclerView.Adapter<MovieAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val poster: ImageView = view.findViewById(R.id.ivPoster)
        val name: TextView = view.findViewById(R.id.tvMovieName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_movie, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val movie = movies[position]
        holder.name.text = movie.name

        Glide.with(holder.poster.context)
            .load(movie.streamIcon)
            .placeholder(R.color.panel_darker)
            .error(R.color.panel_darker)
            .into(holder.poster)

        holder.itemView.setOnClickListener { onClick(movie) }
    }

    override fun getItemCount() = movies.size
}