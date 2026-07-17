package com.dazou.iptvplayer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dazou.iptvplayer.R
import com.dazou.iptvplayer.api.XtreamAPI
import com.dazou.iptvplayer.model.XtreamChannel
import com.dazou.iptvplayer.model.XtreamServer

class ChannelAdapter(
    private val channels: List<XtreamChannel>,
    private val server: XtreamServer?,
    private val onChannelClick: (XtreamChannel) -> Unit
) : RecyclerView.Adapter<ChannelAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvChannelName)
        val tvEpg: TextView = view.findViewById(R.id.tvChannelEpg)
        val pbEpg: ProgressBar = view.findViewById(R.id.pbChannelEpg)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_channel, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val channel = channels[position]
        holder.tvName.text = channel.name
        holder.tvEpg.visibility = View.GONE
        holder.pbEpg.visibility = View.GONE
        holder.itemView.setOnClickListener { onChannelClick(channel) }

        if (server != null) {
            XtreamAPI.getShortEpg(server, channel.streamId) { programs ->
                val current = programs.firstOrNull { it.nowPlaying } ?: programs.firstOrNull()
                if (current != null) {
                    holder.tvEpg.text = "📺 ${current.title}"
                    holder.tvEpg.visibility = View.VISIBLE
                    holder.pbEpg.progress = current.progressPercent()
                    holder.pbEpg.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun getItemCount() = channels.size
}