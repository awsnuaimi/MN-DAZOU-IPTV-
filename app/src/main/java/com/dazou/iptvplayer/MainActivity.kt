package com.dazou.iptvplayer

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.dazou.iptvplayer.databinding.ActivityMainBinding
import com.dazou.iptvplayer.player.PlayerCallback
import com.dazou.iptvplayer.player.PlayerManager
import com.dazou.iptvplayer.fragments.*

class MainActivity : AppCompatActivity(), PlayerCallback {

    private lateinit var binding: ActivityMainBinding
    lateinit var playerManager: PlayerManager

    private var fullscreen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        playerManager = PlayerManager(this)
        binding.videoPlayer.player = playerManager.player

        setupPlayerErrorHandling()
        setupControls()
        setupMenu()

        val app = application as App
        val hasAccount = app.container.accountManager.getActiveAccount() != null

        if (hasAccount) {
            binding.topBar.visibility = View.VISIBLE
            binding.sidebar.visibility = View.VISIBLE
            loadFragment(HomeFragment())
            binding.menuHome.requestFocus()
        } else {
            binding.topBar.visibility = View.GONE
            binding.sidebar.visibility = View.GONE
            loadFragment(LoginFragment())
        }
    }

    fun goToHome() {
        binding.topBar.visibility = View.VISIBLE
        binding.sidebar.visibility = View.VISIBLE
        loadFragment(HomeFragment())
        binding.menuHome.requestFocus()
    }

    private fun setupPlayerErrorHandling() {
        playerManager.player.addListener(object : Player.Listener {

            override fun onPlayerError(error: PlaybackException) {
                val message = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                        "لا يوجد اتصال بالإنترنت"
                    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
                    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
                        "رابط القناة غير متاح حاليًا"
                    PlaybackException.ERROR_CODE_DECODING_FAILED,
                    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ->
                        "صيغة القناة غير مدعومة على هذا الجهاز"
                    else ->
                        "تعذر تشغيل القناة (${error.errorCodeName})"
                }
                binding.channelInfo.text = "⚠️ $message"
            }

            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> binding.channelInfo.text = "⏳ جاري التحميل..."
                    Player.STATE_READY -> {
                        if (binding.channelInfo.text.toString().startsWith("⏳") ||
                            binding.channelInfo.text.toString().startsWith("⚠️")) {
                            binding.channelInfo.text = ""
                        }
                    }
                }
            }
        })
    }

    private fun setupControls(){

        binding.btnPlayPause.setOnClickListener {

            if(playerManager.isPlaying){
                playerManager.pause()
                binding.btnPlayPause.setImageResource(R.drawable.ic_play_small)
            }else{
                playerManager.resume()
                binding.btnPlayPause.setImageResource(R.drawable.ic_pause)
            }

        }

        binding.btnFullscreen.setOnClickListener {
            toggleFullscreen()
        }

        binding.btnPrev.setOnClickListener {
            onPreviousChannel()
        }

        binding.btnNext.setOnClickListener {
            onNextChannel()
        }
    }

    private fun toggleFullscreen() {
        fullscreen = !fullscreen

        if (fullscreen) {
            binding.topBar.visibility = View.GONE
            binding.sidebar.visibility = View.GONE

            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            )
        } else {
            binding.topBar.visibility = View.VISIBLE
            binding.sidebar.visibility = View.VISIBLE

            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (fullscreen) {
            toggleFullscreen()
        } else {
            super.onBackPressed()
        }
    }

    private fun setupMenu(){
        binding.menuHome.setOnClickListener { loadFragment(HomeFragment()) }
        binding.menuLive.setOnClickListener { loadFragment(LiveFragment()) }
        binding.menuMovies.setOnClickListener { loadFragment(MoviesFragment()) }
        binding.menuSeries.setOnClickListener { loadFragment(SeriesFragment()) }
        binding.menuEpg.setOnClickListener { loadFragment(EpgFragment()) }
        binding.settings.setOnClickListener { loadFragment(SettingsFragment()) }
        binding.account.setOnClickListener { loadFragment(AccountsFragment()) }
    }

    private fun loadFragment(fragment: Fragment){
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment)
            .commit()
    }

    override fun playStream(url:String, name:String, type:String){
        playerManager.play(url, name, type)
        binding.channelInfo.text = "📺 $name"
        binding.btnPlayPause.setImageResource(R.drawable.ic_pause)
        binding.videoPlayer.requestFocus()
    }

    override fun onNextChannel(){ }
    override fun onPreviousChannel(){ }

    override fun onDestroy(){
        playerManager.release()
        super.onDestroy()
    }
}