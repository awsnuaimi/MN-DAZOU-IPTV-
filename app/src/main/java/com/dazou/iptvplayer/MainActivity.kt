package com.dazou.iptvplayer

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.dazou.iptvplayer.databinding.ActivityMainBinding
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences("IPTV_Prefs", MODE_PRIVATE)
        val savedUrl = prefs.getString("url", "")

        if (!savedUrl.isNullOrEmpty()) {
            binding.etUrl.setText(savedUrl)
        }

        binding.btnConnect.setOnClickListener {
            val url = binding.etUrl.text.toString()
            if (url.isNotEmpty()) {
                prefs.edit().putString("url", url).apply()
                setupPlayer(url)
            }
        }

        binding.btnLogout.setOnClickListener {
            prefs.edit().remove("url").apply()
            player?.release()
            player = null
            binding.playerContainer.visibility = View.GONE
            binding.loginLayout.visibility = View.VISIBLE
        }
    }

    private fun setupPlayer(url: String) {
        binding.loginLayout.visibility = View.GONE
        binding.playerContainer.visibility = View.VISIBLE
        
        player = ExoPlayer.Builder(this).build()
        binding.playerView.player = player
        player?.setMediaItem(MediaItem.fromUri(url))
        player?.prepare()
        player?.playWhenReady = true
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
    }
}
