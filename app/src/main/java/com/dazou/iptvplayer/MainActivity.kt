package com.dazou.iptvplayer

import android.os.Bundle
import android.view.View
import android.widget.Toast
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

        // تغيير الحقول بناء على اختيار M3U أو Xtream
        binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val isXtream = (checkedId == R.id.rbXtream)
            binding.etUser.visibility = if (isXtream) View.VISIBLE else View.GONE
            binding.etPass.visibility = if (isXtream) View.VISIBLE else View.GONE
            binding.etUrl.hint = if (isXtream) "رابط الخادم (مثال: http://host:port)" else "أدخل رابط M3U"
        }

        binding.btnConnect.setOnClickListener {
            if (binding.rbXtream.isChecked) {
                val host = binding.etUrl.text.toString()
                val user = binding.etUser.text.toString()
                val pass = binding.etPass.text.toString()
                
                if (host.isNotEmpty() && user.isNotEmpty() && pass.isNotEmpty()) {
                    // رسالة مؤقتة لتأكيد عمل الواجهة، سنقوم ببرمجة الاتصال لاحقاً
                    Toast.makeText(this, "جاري تجهيز بيانات Xtream...", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "يرجى إدخال جميع البيانات", Toast.LENGTH_SHORT).show()
                }
            } else {
                val url = binding.etUrl.text.toString()
                if (url.isNotEmpty()) {
                    prefs.edit().putString("url", url).apply()
                    setupPlayer(url)
                }
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
