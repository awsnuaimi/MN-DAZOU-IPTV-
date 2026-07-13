package com.dazou.iptvplayer

import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class MainActivity : AppCompatActivity() {

    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        playerView = PlayerView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 400
            )
        }
        root.addView(playerView)

        val btnPlay = Button(this).apply {
            text = "تشغيل بث اختباري"
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                playTestStream()
            }
        }
        root.addView(btnPlay)

        setContentView(root)

        // إعداد المشغل
        player = ExoPlayer.Builder(this).build()
        playerView.player = player
    }

    private fun playTestStream() {
        try {
            // رابط بث اختباري HLS (قناة مفتوحة)
            val testUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
            val mediaItem = MediaItem.fromUri(Uri.parse(testUrl))
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
            Toast.makeText(this, "تم التشغيل", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "خطأ: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}