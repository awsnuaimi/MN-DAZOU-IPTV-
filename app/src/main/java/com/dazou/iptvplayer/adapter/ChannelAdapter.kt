package com.dazou.iptvplayer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.dazou.iptvplayer.R
import com.dazou.iptvplayer.data.FavoritesManager
import com.dazou.iptvplayer.model.FavoriteItem
import com.dazou.iptvplayer.model.XtreamChannel
import com.dazou.iptvplayer.utils.FocusAnimator

class ChannelAdapter(
    private val channels: List<XtreamChannel>,
    private val favoritesManager: FavoritesManager,
    private val rightFocusTargetId: Int? = null,
    private val onRequestFocusLeft: (() -> Unit)? = null,
    private val onRequestFullscreen: (() -> Unit)? = null,
    private val onChannelClick: (XtreamChannel) -> Unit
) : RecyclerView.Adapter<ChannelAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivLogo: ImageView = view.findViewById(R.id.ivChannelLogo)
        val tvName: TextView = view.findViewById(R.id.tvChannelName)
        val favoriteBadge: ImageView = view.findViewById(R.id.ivChannelFavoriteBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_channel, parent, false)
        FocusAnimator.attach(view)

        rightFocusTargetId?.let { view.nextFocusRightId = it }

        view.setOnKeyListener { _, keyCode, event ->
            if (event.action != android.view.KeyEvent.ACTION_DOWN) return@setOnKeyListener false
            when (keyCode) {
                android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (onRequestFocusLeft != null) {
                        onRequestFocusLeft.invoke()
                        true
                    } else false
                }
                // ✅ ضغطة يمين وحدة: الفوكس يروح لزر ملء الشاشة ويفتحها بنفس اللحظة
                android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (onRequestFullscreen != null) {
                        onRequestFullscreen.invoke()
                        true
                    } else false
                }
                else -> false
            }
        }

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val channel = channels[position]
        holder.tvName.text = channel.name

        Glide.with(holder.ivLogo.context)
            .load(channel.streamIcon)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.ic_live_tv)
            .error(R.drawable.ic_live_tv)
            .into(holder.ivLogo)

        holder.favoriteBadge.visibility =
            if (favoritesManager.isFavorite("live", channel.streamId)) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener { onChannelClick(channel) }

        holder.itemView.setOnLongClickListener {
            val added = favoritesManager.toggleFavorite(
                FavoriteItem("live", channel.streamId, channel.name, channel.streamIcon, channel.containerExtension)
            )
            holder.favoriteBadge.visibility = if (added) View.VISIBLE else View.GONE
            val ctx = holder.itemView.context
            Toast.makeText(
                ctx,
                if (added) ctx.getString(R.string.favorite_added_channel) else ctx.getString(R.string.favorite_removed),
                Toast.LENGTH_SHORT
            ).show()
            true
        }
    }

    override fun getItemCount() = channels.size
}