package com.dazou.iptvplayer.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

class PlayerManager(context: Context) {

    val player: ExoPlayer = ExoPlayer.Builder(context).build()
    var currentStreamUrl: String? = null
    var currentStreamName: String? = null
    var currentStreamType: String = "live"

    private var onPlaybackEndedListener: (() -> Unit)? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    onPlaybackEndedListener?.invoke()
                }
            }
        })
    }

    fun setOnPlaybackEndedListener(listener: () -> Unit) {
        onPlaybackEndedListener = listener
    }

    fun play(url: String, name: String, type: String = "live") {
        currentStreamUrl = url
        currentStreamName = name
        currentStreamType = type
        player.setMediaItem(MediaItem.fromUri(Uri.parse(url)))
        player.prepare()
        player.play()
    }

    fun pause() = player.pause()
    fun resume() = player.play()
    fun release() = player.release()
    val isPlaying: Boolean get() = player.isPlaying
}