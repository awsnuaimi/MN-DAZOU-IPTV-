package com.dazou.iptvplayer

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import androidx.activity.OnBackPressedCallback
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

    private val autoFullscreenHandler = Handler(Looper.getMainLooper())
    private var autoFullscreenRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.decorView.isFocusableInTouchMode = true
        window.decorView.requestFocus()

        playerManager = PlayerManager(this)
        binding.playerView.player = playerManager.player

        playerManager.setOnPlaybackEndedListener { onNextChannel() }

        setupBottomNav()
        setupPlayerControls()
        setupBackPress()
        loadFragment(HomeFragment())

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
        binding.btnClosePlayer.setOnClickListener { closeFloatingPlayer() }
    }

    private fun setupBottomNav() {
        binding.btnHome.setOnClickListener { loadFragment(HomeFragment()) }
        binding.btnLive.setOnClickListener { loadFragment(LiveFragment()) }
        binding.btnMovies.setOnClickListener { loadFragment(MoviesFragment()) }
        binding.btnSeries.setOnClickListener { loadFragment(SeriesFragment()) }
        binding.btnFavorites.setOnClickListener { loadFragment(FavoritesFragment()) }
        binding.btnAccounts.setOnClickListener { loadFragment(AccountsFragment()) }
        binding.btnSettings.setOnClickListener { loadFragment(SettingsFragment()) }
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    isFullscreen -> toggleFullscreen()
                    else -> {
                        val current = supportFragmentManager.findFragmentById(binding.fragmentContainer.id)
                        val handled = (current as? BackHandledFragment)?.onBackPressedInFragment() ?: false
                        if (!handled) {
                            isEnabled = false
                            onBackPressedDispatcher.onBackPressed()
                        }
                    }
                }
            }
        })
    }

    // ✅ اعتراض مركزي لسهم اليسار من الريموت - يعمل بغض النظر عن مكان الفوكس الحالي
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            val current = supportFragmentManager.findFragmentById(binding.fragmentContainer.id)
            if (current is BackHandledFragment && current.onBackPressedInFragment()) {
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment)
            .commit()
    }

    private fun closeFloatingPlayer() {
        cancelAutoFullscreen()
        playerManager.pause()
        binding.floatingPlayerCard.visibility = View.GONE
    }

    private fun toggleFullscreen() {
        cancelAutoFullscreen()
        if (isFullscreen) {
            binding.fullscreenContainer.visibility = View.GONE
            binding.floatingPlayerCard.visibility = View.VISIBLE
            binding.fullscreenPlayerView.player = null
            binding.playerView.player = playerManager.player
            binding.btnFullscreen.post { binding.btnFullscreen.requestFocus() }
        } else {
            binding.fullscreenContainer.visibility = View.VISIBLE
            binding.floatingPlayerCard.visibility = View.GONE
            binding.playerView.player = null
            binding.fullscreenPlayerView.player = playerManager.player
            binding.btnExitFullscreen.post { binding.btnExitFullscreen.requestFocus() }
        }
        isFullscreen = !isFullscreen
    }

    private fun togglePlayPause() {
        if (playerManager.isPlaying) playerManager.pause() else playerManager.resume()
    }

    override fun playStream(url: String, name: String, type: String) {
        if (isFullscreen) {
            binding.fullscreenContainer.visibility = View.GONE
            binding.fullscreenPlayerView.player = null
            binding.playerView.player = playerManager.player
            isFullscreen = false
        }

        playerManager.play(url, name, type)
        binding.floatingPlayerCard.visibility = View.VISIBLE
        binding.tvChannelInfo.text = "🎬 $name"
        binding.tvChannelInfo.visibility = View.VISIBLE
        binding.controlsLayout.visibility = View.VISIBLE

        // ✅ نطلب الفوكس بعد انتهاء رسم الطبقة فعليًا (post) حتى ينجح الطلب
        binding.btnPlayPause.post { binding.btnPlayPause.requestFocus() }

        // ✅ تكبير تلقائي لملء الشاشة بعد 10 ثواني من بدء التشغيل
        scheduleAutoFullscreen()
    }

    private fun scheduleAutoFullscreen() {
        cancelAutoFullscreen()
        val runnable = Runnable {
            if (!isFullscreen) {
                toggleFullscreen()
            }
        }
        autoFullscreenRunnable = runnable
        autoFullscreenHandler.postDelayed(runnable, 10_000)
    }

    private fun cancelAutoFullscreen() {
        autoFullscreenRunnable?.let { autoFullscreenHandler.removeCallbacks(it) }
        autoFullscreenRunnable = null
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
        cancelAutoFullscreen()
        playerManager.release()
        super.onDestroy()
    }
}