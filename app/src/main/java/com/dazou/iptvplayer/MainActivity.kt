package com.dazou.iptvplayer

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.dazou.iptvplayer.databinding.ActivityMainBinding
import com.dazou.iptvplayer.fragments.*
import com.dazou.iptvplayer.player.PlayerCallback
import com.dazou.iptvplayer.player.PlayerManager

class MainActivity : AppCompatActivity(), PlayerCallback {

    private lateinit var binding: ActivityMainBinding
    lateinit var playerManager: PlayerManager
    private var isFullscreen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ضروري لـ Android TV
        window.decorView.isFocusableInTouchMode = true
        window.decorView.requestFocus()

        playerManager = PlayerManager(this)
        binding.playerView.player = playerManager.player
        binding.fullscreenPlayerView.player = playerManager.player

        playerManager.setOnPlaybackEndedListener { onNextChannel() }

        setupBottomNav()
        setupPlayerControls()
        loadFragment(HomeFragment())

        // ضبط ارتفاع المشغل (نسبة 16:9 من عرض 30%)
        binding.playerView.post {
            val width = binding.playerContainer.width
            if (width > 0) {
                val height = (width * 9 / 16)
                binding.playerView.layoutParams.height = height
                binding.playerView.requestLayout()
            }
        }

        // طلب التركيز على أول زر
        Handler(Looper.getMainLooper()).postDelayed({
            binding.btnHome.isFocusable = true
            binding.btnHome.isFocusableInTouchMode = true
            binding.btnHome.requestFocus()
        }, 500)
    }

    private fun setupPlayerControls() {
        binding.btnPrevChannel.setOnClickListener { onPreviousChannel() }
        binding.btnPlayPause.setOnClickListener { togglePlayPause() }
        binding.btnNextChannel.setOnClickListener { onNextChannel() }
        binding.btnEpg.setOnClickListener { /* TODO: EPG */ }
        binding.btnFullscreen.setOnClickListener { toggleFullscreen() }
        binding.btnExitFullscreen.setOnClickListener { toggleFullscreen() }
    }

    private fun setupBottomNav() {
        binding.btnHome.setOnClickListener { loadFragment(HomeFragment()) }
        binding.btnLive.setOnClickListener { loadFragment(LiveFragment()) }
        binding.btnMovies.setOnClickListener { loadFragment(MoviesFragment()) }
        binding.btnSeries.setOnClickListener { loadFragment(SeriesFragment()) }
        binding.btnFavorites.setOnClickListener { loadFragment(FavoritesFragment()) }
        binding.btnAccounts.setOnClickListener { loadFragment(AccountsFragment()) }
        binding.btnSettings.setOnClickListener { loadFragment(SettingsFragment()) } // ← تمت الإضافة
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment)
            .commit()
    }

    private fun toggleFullscreen() {
        if (isFullscreen) {
            // الرجوع من ملء الشاشة
            binding.fullscreenContainer.visibility = View.GONE
            binding.mainContent.visibility = View.VISIBLE
            binding.playerView.player = playerManager.player
        } else {
            // الدخول إلى ملء الشاشة
            binding.fullscreenContainer.visibility = View.VISIBLE
            binding.mainContent.visibility = View.GONE
            binding.fullscreenPlayerView.player = playerManager.player
            binding.btnExitFullscreen.requestFocus()
        }
        isFullscreen = !isFullscreen
    }

    private fun togglePlayPause() {
        if (playerManager.isPlaying) playerManager.pause() else playerManager.resume()
    }

    override fun playStream(url: String, name: String, type: String) {
        playerManager.play(url, name, type)
        binding.tvChannelInfo.text = "🎬 $name"
        binding.tvChannelInfo.visibility = View.VISIBLE
        binding.controlsLayout.visibility = View.VISIBLE
    }

    override fun onNextChannel() { /* TODO */ }
    override fun onPreviousChannel() { /* TODO */ }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> { togglePlayPause(); true }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onDestroy() {
        playerManager.release()
        super.onDestroy()
    }
}