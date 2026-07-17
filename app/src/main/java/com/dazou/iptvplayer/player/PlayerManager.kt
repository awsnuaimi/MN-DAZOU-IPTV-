package com.dazou.iptvplayer.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

class PlayerManager(context: Context) {

    val player: ExoPlayer = ExoPlayer.Builder(context).build()

    var currentStreamUrl: String? = null
        private set

    val isPlaying: Boolean
        get() = player.isPlaying


    fun play(
        url: String,
        name: String = "",
        type: String = ""
    ) {

        currentStreamUrl = url

        val mediaItem = MediaItem.fromUri(
            Uri.parse(url)
        )

        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }


    fun pause() {

        player.pause()

    }


    fun resume() {

        player.play()

    }


    fun release() {

        player.release()

    }
}