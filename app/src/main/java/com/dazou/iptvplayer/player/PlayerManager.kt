package com.dazou.iptvplayer.player

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

class PlayerManager(context: Context) {

    private val userAgent = "Mozilla/5.0 (Linux; Android 10; DAZOU-IPTV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"

    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent(userAgent)
        .setConnectTimeoutMs(15000)
        .setReadTimeoutMs(15000)
        .setAllowCrossProtocolRedirects(true)

    private val mediaSourceFactory = DefaultMediaSourceFactory(context)
        .setDataSourceFactory(httpDataSourceFactory)

    private val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            15_000,
            50_000,
            2_500,
            5_000
        )
        .build()

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(mediaSourceFactory)
        .setLoadControl(loadControl)
        .build()

    var currentStreamUrl: String? = null
        private set

    private var currentName: String = ""
    private var currentType: String = ""

    val isPlaying: Boolean
        get() = player.isPlaying

    private var retryCount = 0
    private val maxRetries = 3
    private val retryHandler = Handler(Looper.getMainLooper())

    fun play(
        url: String,
        name: String = "",
        type: String = "",
        startPositionMs: Long = 0L
    ) {
        currentStreamUrl = url
        currentName = name
        currentType = type
        retryCount = 0

        val mediaItem = MediaItem.fromUri(Uri.parse(url))
        player.setMediaItem(mediaItem)
        player.prepare()
        if (startPositionMs > 0L) {
            player.seekTo(startPositionMs)
        }
        player.play()
    }

    fun retryCurrent(onExhausted: () -> Unit) {
        val url = currentStreamUrl ?: return
        if (retryCount >= maxRetries) {
            onExhausted()
            return
        }
        retryCount++
        retryHandler.postDelayed({
            play(url, currentName, currentType)
        }, 2000L * retryCount)
    }

    fun resetRetry() {
        retryCount = 0
    }

    fun pause() {
        player.pause()
    }

    fun resume() {
        player.play()
    }

    fun release() {
        retryHandler.removeCallbacksAndMessages(null)
        player.release()
    }
}