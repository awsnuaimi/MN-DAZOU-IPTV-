package com.dazou.iptvplayer

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class MainActivity : AppCompatActivity() {

    private lateinit var player: ExoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val playerView = PlayerView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 400
            )
        }
        root.addView(playerView)

        val btnPlay = Button(this).apply {
            text = "تشغيل بث اختباري"
            setOnClickListener {
                playTestStream()
            }
        }
        root.addView(btnPlay)

        val btnXtream = Button(this).apply {
            text = "فتح إعدادات Xtream"
            setOnClickListener {
                Toast.makeText(this@MainActivity, "سيتم إضافة الإعدادات لاحقاً", Toast.LENGTH_SHORT).show()
            }
        }
        root.addView(btnXtream)

        setContentView(root)

        player = ExoPlayer.Builder(this).build()
        playerView.player = player
    }

    private fun playTestStream() {
        try {
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