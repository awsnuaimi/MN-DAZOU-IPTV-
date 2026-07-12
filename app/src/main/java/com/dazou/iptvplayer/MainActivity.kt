package com.dazou.iptvplayer

import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView

class MainActivity : AppCompatActivity() {
    private var player: ExoPlayer? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val prefs = getSharedPreferences("IPTV_Prefs", Context.MODE_PRIVATE)
        val savedUrl = prefs.getString("url", "")

        if (savedUrl.isNullOrEmpty()) {
            showLoginForm(prefs)
        } else {
            startPlayer(savedUrl)
        }
    }

    private fun showLoginForm(prefs: android.content.SharedPreferences) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }

        val urlInput = EditText(this).apply { hint = "أدخل رابط M3U أو بيانات Xtream" }
        val loginButton = Button(this).apply { text = "تشغيل" }

        layout.addView(urlInput)
        layout.addView(loginButton)
        setContentView(layout)

        loginButton.setOnClickListener {
            val url = urlInput.text.toString()
            if (url.isNotEmpty()) {
                prefs.edit().putString("url", url).apply()
                startPlayer(url)
            }
        }
    }

    private fun startPlayer(url: String) {
        val playerView = PlayerView(this)
        setContentView(playerView)
        player = ExoPlayer.Builder(this).build()
        playerView.player = player
        player?.setMediaItem(MediaItem.fromUri(url))
        player?.prepare()
        player?.playWhenReady = true
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
    }
}
