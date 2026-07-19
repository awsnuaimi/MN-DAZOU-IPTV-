package com.dazou.iptvplayer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dazou.iptvplayer.R
import com.dazou.iptvplayer.model.XtreamChannel

class ChannelAdapter(
    private val channels: List<XtreamChannel>,
    private val onChannelClick: (XtreamChannel) -> Unit
) : RecyclerView.Adapter<ChannelAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivLogo: ImageView = view.findViewById(R.id.ivChannelLogo)
        val tvName: TextView = view.findViewById(R.id.tvChannelName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_channel, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val channel = channels[position]
        holder.tvName.text = channel.name

        Glide.with(holder.ivLogo.context)
            .load(channel.streamIcon)
            .placeholder(R.drawable.ic_live_tv)
            .error(R.drawable.ic_live_tv)
            .into(holder.ivLogo)

        holder.itemView.setOnClickListener { onChannelClick(channel) }
    }

    override fun getItemCount() = channels.size
}