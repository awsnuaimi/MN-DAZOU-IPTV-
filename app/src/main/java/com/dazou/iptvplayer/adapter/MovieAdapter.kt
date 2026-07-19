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
import com.dazou.iptvplayer.model.XtreamMovie
import com.dazou.iptvplayer.utils.FocusAnimator

class MovieAdapter(
    private val movies: List<XtreamMovie>,
    private val favoritesManager: FavoritesManager,
    private val onClick: (XtreamMovie) -> Unit
) : RecyclerView.Adapter<MovieAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val poster: ImageView = view.findViewById(R.id.ivPoster)
        val name: TextView = view.findViewById(R.id.tvMovieName)
        val favoriteBadge: ImageView = view.findViewById(R.id.ivFavoriteBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_movie, parent, false)
        FocusAnimator.attach(view)
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

        holder.favoriteBadge.visibility =
            if (favoritesManager.isFavorite("movie", movie.streamId)) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener { onClick(movie) }

        holder.itemView.setOnLongClickListener {
            val added = favoritesManager.toggleFavorite(
                FavoriteItem("movie", movie.streamId, movie.name, movie.streamIcon, movie.containerExtension)
            )
            holder.favoriteBadge.visibility = if (added) View.VISIBLE else View.GONE
            val ctx = holder.itemView.context
            Toast.makeText(
                ctx,
                if (added) ctx.getString(R.string.favorite_added) else ctx.getString(R.string.favorite_removed),
                Toast.LENGTH_SHORT
            ).show()
            true
        }
    }

    override fun getItemCount() = movies.size
}