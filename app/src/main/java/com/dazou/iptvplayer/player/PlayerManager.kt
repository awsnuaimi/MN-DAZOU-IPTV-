package com.dazou.iptvplayer.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

class PlayerManager(context: Context) {
    val player: ExoPlayer = ExoPlayer.Builder(context).build()
    var currentStreamUrl: String? = null

    // إضافة Listener لتحديث الواجهة من الخارج
    fun setListener(listener: Player.Listener) {
        player.addListener(listener)
    }

    fun play(url: String) {
        currentStreamUrl = url
        player.setMediaItem(MediaItem.fromUri(Uri.parse(url)))
        player.prepare()
        player.play()
    }
    fun pause() = player.pause()
    fun resume() = player.play()
    fun release() = player.release()
}