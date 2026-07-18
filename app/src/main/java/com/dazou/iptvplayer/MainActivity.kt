package com.dazou.iptvplayer

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.dazou.iptvplayer.model.XtreamChannel
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
    private var lastCategories: List<XtreamCategory> = emptyList()
    private var currentChannelList: List<XtreamChannel> = emptyList()
    private var currentChannelIndex: Int = -1
    private var wasChannelsPanelOpenBeforeFullscreen = false

    private val clockHandler = Handler(Looper.getMainLooper())
    private var clockRunnable: Runnable? = null

    private lateinit var connectivityManager: ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        playerManager = PlayerManager(this)
        binding.videoPlayer.player = playerManager.player

        val app = application as App
        liveViewModel = ViewModelProvider(this, ViewModelFactory(app.container.currentRepository))
            .get(LiveViewModel::class.java)

        binding.categoryList.layoutManager = LinearLayoutManager(this)
        binding.channelList.layoutManager = LinearLayoutManager(this)

        liveViewModel.categories.observe(this) { categories ->
            lastCategories = categories
            if (categories.isEmpty()) {
                Toast.makeText(this, "لا توجد مجموعات – تأكد من الحساب", Toast.LENGTH_LONG).show()
            }
            binding.categoryList.adapter = CategoryAdapter(categories) { category -> openCategory(category) }
        }

        liveViewModel.channels.observe(this) { channels ->
            currentChannelList = channels
            binding.channelList.adapter = ChannelAdapter(channels) { channel ->
                val index = channels.indexOf(channel)
                playChannelAt(index)
            }
        }

        binding.channelsPanelBack.setOnClickListener {
            hideChannelsPanel()
        }

        startClock()
        setupWifiStatus()

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

    fun playChannelFromExternal(channel: XtreamChannel, sourceList: List<XtreamChannel>) {
        currentChannelList = sourceList
        currentChannelIndex = sourceList.indexOf(channel)
        val server = liveViewModel.getServer() ?: return
        val url = XtreamAPI.getStreamUrl(server, channel.streamId, channel.containerExtension, "live")
        playStream(url, channel.name, "live")
    }

    private fun setupWifiStatus() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        fun isConnected(): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return false
                val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            } else {
                @Suppress("DEPRECATION")
                connectivityManager.activeNetworkInfo?.isConnected == true
            }
        }

        fun updateIcon() {
            val connected = isConnected()
            binding.wifiIcon.setImageResource(if (connected) R.drawable.ic_wifi else R.drawable.ic_wifi_off)
            binding.wifiIcon.alpha = if (connected) 1f else 0.5f
        }

        updateIcon()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    runOnUiThread { updateIcon() }
                }
                override fun onLost(network: Network) {
                    runOnUiThread { updateIcon() }
                }
            }
            networkCallback = callback
            try {
                connectivityManager.registerDefaultNetworkCallback(callback)
            } catch (_: Exception) {}
        }
    }

    private fun startClock() {
        val runnable = object : Runnable {
            override fun run() {
                val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                binding.clockText.text = sdf.format(java.util.Date())
                clockHandler.postDelayed(this, 30000)
            }
        }
        clockRunnable = runnable
        clockHandler.post(runnable)
    }

    private fun clearContentFragment() {
        supportFragmentManager.findFragmentById(binding.fragmentContainer.id)?.let {
            supportFragmentManager.beginTransaction().remove(it).commitNowAllowingStateLoss()
        }
        binding.fragmentContainer.visibility = View.GONE
    }

    private fun showCategories() {
        clearContentFragment()
        binding.sidebar.visibility = View.VISIBLE
        binding.videoPlayer.visibility = View.VISIBLE
        binding.channelInfo.visibility = View.VISIBLE
        binding.playerControls.visibility = View.VISIBLE
        if (lastCategories.isEmpty()) {
            liveViewModel.loadCategories()
        }
    }

    private fun openCategory(category: XtreamCategory) {
        binding.channelsPanelTitle.text = category.categoryName
        binding.channelsPanel.visibility = View.VISIBLE
        liveViewModel.loadChannels(category.categoryId)
    }

    private fun hideChannelsPanel() {
        binding.channelsPanel.visibility = View.GONE
    }

    private fun playChannelAt(index: Int) {
        if (index < 0 || index >= currentChannelList.size) return
        val server = liveViewModel.getServer()
        if (server == null) {
            Toast.makeText(this, "اختر حساب IPTV أولاً", Toast.LENGTH_SHORT).show()
            return
        }
        currentChannelIndex = index
        val channel = currentChannelList[index]
        val url = XtreamAPI.getStreamUrl(server, channel.streamId, channel.containerExtension, "live")
        playStream(url, channel.name, "live")
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
        binding.channelsPanel.visibility = View.GONE
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
            wasChannelsPanelOpenBeforeFullscreen = binding.channelsPanel.visibility == View.VISIBLE

            binding.topBar.visibility = View.GONE
            binding.sidebar.visibility = View.GONE
            binding.channelsPanel.visibility = View.GONE
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
            if (wasChannelsPanelOpenBeforeFullscreen) {
                binding.channelsPanel.visibility = View.VISIBLE
            }
            if (supportFragmentManager.findFragmentById(binding.fragmentContainer.id) != null) {
                binding.fragmentContainer.visibility = View.VISIBLE
            }

            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (fullscreen) {
            toggleFullscreen()
        } else if (binding.channelsPanel.visibility == View.VISIBLE) {
            hideChannelsPanel()
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
        binding.videoPlayer.visibility = View.GONE
        binding.channelInfo.visibility = View.GONE
        binding.playerControls.visibility = View.GONE
        binding.sidebar.visibility = View.GONE
        binding.channelsPanel.visibility = View.GONE
        binding.fragmentContainer.visibility = View.VISIBLE
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

    override fun onNextChannel(){
        if (currentChannelList.isEmpty()) return
        val next = (currentChannelIndex + 1).coerceAtMost(currentChannelList.size - 1)
        playChannelAt(next)
    }

    override fun onPreviousChannel(){
        if (currentChannelList.isEmpty()) return
        val prev = (currentChannelIndex - 1).coerceAtLeast(0)
        playChannelAt(prev)
    }

    override fun onDestroy(){
        clockRunnable?.let { clockHandler.removeCallbacks(it) }
        networkCallback?.let {
            try { connectivityManager.unregisterNetworkCallback(it) } catch (_: Exception) {}
        }
        playerManager.release()
        super.onDestroy()
    }
}