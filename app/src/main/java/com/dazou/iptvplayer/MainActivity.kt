package com.dazou.iptvplayer

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.recyclerview.widget.LinearLayoutManager
import com.dazou.iptvplayer.adapter.CategoryAdapter
import com.dazou.iptvplayer.adapter.ChannelAdapter
import com.dazou.iptvplayer.api.XtreamAPI
import com.dazou.iptvplayer.databinding.ActivityMainBinding
import com.dazou.iptvplayer.model.XtreamCategory
import com.dazou.iptvplayer.player.PlayerCallback
import com.dazou.iptvplayer.player.PlayerManager
import com.dazou.iptvplayer.viewmodel.LiveViewModel
import com.dazou.iptvplayer.viewmodel.ViewModelFactory
import com.dazou.iptvplayer.fragments.*

class MainActivity : AppCompatActivity(), PlayerCallback {

    private lateinit var binding: ActivityMainBinding
    lateinit var playerManager: PlayerManager
    private lateinit var liveViewModel: LiveViewModel

    private var fullscreen = false
    private var currentChannelName = ""
    private var inChannelsMode = false
    private var lastCategories: List<XtreamCategory> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        playerManager = PlayerManager(this)
        binding.videoPlayer.player = playerManager.player

        val app = application as App
        liveViewModel = ViewModelProvider(this, ViewModelFactory(app.container.currentRepository))
            .get(LiveViewModel::class.java)

        binding.channelList.layoutManager = LinearLayoutManager(this)

        liveViewModel.categories.observe(this) { categories ->
            lastCategories = categories
            if (!inChannelsMode) {
                if (categories.isEmpty()) {
                    Toast.makeText(this, "لا توجد مجموعات – تأكد من الحساب", Toast.LENGTH_LONG).show()
                }
                binding.channelList.adapter = CategoryAdapter(categories) { category -> openCategory(category) }
            }
        }

        liveViewModel.channels.observe(this) { channels ->
            if (inChannelsMode) {
                binding.channelList.adapter = ChannelAdapter(channels) { channel ->
                    val server = liveViewModel.getServer()
                    if (server == null) {
                        Toast.makeText(this, "اختر حساب IPTV أولاً", Toast.LENGTH_SHORT).show()
                        return@ChannelAdapter
                    }
                    val url = XtreamAPI.getStreamUrl(server, channel.streamId, channel.containerExtension, "live")
                    playStream(url, channel.name, "live")
                }
            }
        }

        setupPlayerErrorHandling()
        setupControls()
        setupMenu()

        val hasAccount = app.container.accountManager.getActiveAccount() != null

        if (hasAccount) {
            showMainUi()
            showCategories()
            binding.menuHome.requestFocus()
        } else {
            showLoginUi()
            loadFragment(LoginFragment())
        }
    }

    private fun showCategories() {
        inChannelsMode = false
        if (lastCategories.isNotEmpty()) {
            binding.channelList.adapter = CategoryAdapter(lastCategories) { category -> openCategory(category) }
        } else {
            liveViewModel.loadCategories()
        }
    }

    private fun openCategory(category: XtreamCategory) {
        inChannelsMode = true
        liveViewModel.loadChannels(category.categoryId)
    }

    fun goToHome() {
        showMainUi()
        showCategories()
        binding.menuHome.requestFocus()
    }

    private fun showMainUi() {
        binding.topBar.visibility = View.VISIBLE
        binding.sidebar.visibility = View.VISIBLE
        binding.videoPlayer.visibility = View.VISIBLE
        binding.channelInfo.visibility = View.VISIBLE
        binding.playerControls.visibility = View.VISIBLE
    }

    private fun showLoginUi() {
        binding.topBar.visibility = View.GONE
        binding.sidebar.visibility = View.GONE
        binding.videoPlayer.visibility = View.GONE
        binding.channelInfo.visibility = View.GONE
        binding.playerControls.visibility = View.GONE
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
                        if (currentChannelName.isNotEmpty()) {
                            binding.channelInfo.text = "📺 $currentChannelName"
                        } else {
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
            binding.fragmentContainer.visibility = View.GONE

            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            )
        } else {
            binding.topBar.visibility = View.VISIBLE
            binding.sidebar.visibility = View.VISIBLE
            binding.fragmentContainer.visibility = View.VISIBLE

            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (fullscreen) {
            toggleFullscreen()
        } else if (inChannelsMode) {
            showCategories()
        } else {
            super.onBackPressed()
        }
    }

    private fun setupMenu(){
        binding.menuHome.setOnClickListener { loadFragment(HomeFragment()) }
        binding.menuLive.setOnClickListener { showCategories() }
        binding.menuMovies.setOnClickListener { loadFragment(MoviesFragment()) }
        binding.menuSeries.setOnClickListener { loadFragment(SeriesFragment()) }
        binding.menuEpg.setOnClickListener { loadFragment(EpgFragment()) }
        binding.settings.setOnClickListener { loadFragment(SettingsFragment()) }
        binding.account.setOnClickListener { loadFragment(AccountsFragment()) }
        binding.sidebarLiveButton.setOnClickListener { showCategories() }
    }

    private fun loadFragment(fragment: Fragment){
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment)
            .commit()
    }

    override fun playStream(url:String, name:String, type:String){
        currentChannelName = name
        playerManager.play(url, name, type)
        binding.channelInfo.text = "📺 $name"
        binding.btnPlayPause.setImageResource(R.drawable.ic_pause)
        binding.btnPlayPause.requestFocus()
    }

    override fun onNextChannel(){ }
    override fun onPreviousChannel(){ }

    override fun onDestroy(){
        playerManager.release()
        super.onDestroy()
    }
}