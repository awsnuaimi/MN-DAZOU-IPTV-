package com.dazou.iptvplayer

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
    private var currentType = "live"
    private var wasPlayingBeforeBackground = false
    private var lastCategories: List<XtreamCategory> = emptyList()
    private var currentChannelList: List<XtreamChannel> = emptyList()
    private var currentChannelIndex: Int = -1
    private var wasChannelsPanelOpenBeforeFullscreen = false
    private var isMuted = false
    private var currentCategoryId: String? = null
    private var currentCategoryName: String = ""
    private var pendingAutoPlayChannelId: Int? = null

    private val clockHandler = Handler(Looper.getMainLooper())
    private var clockRunnable: Runnable? = null

    private val controlsHandler = Handler(Looper.getMainLooper())
    private var controlsRunnable: Runnable? = null

    private val seekHandler = Handler(Looper.getMainLooper())
    private var seekRunnable: Runnable? = null
    private var userSeeking = false

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
            val pendingId = pendingAutoPlayChannelId
            if (pendingId != null) {
                pendingAutoPlayChannelId = null
                val index = channels.indexOfFirst { it.streamId == pendingId }
                if (index >= 0) playChannelAt(index)
            } else {
                buildChannelStrip()
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
        startSeekUpdater()

        val hasAccount = app.container.accountManager.getActiveAccount() != null

        if (hasAccount) {
            showMainUi()
            restoreLastSessionOrShowCategories()
            binding.menuHome.requestFocus()
        } else {
            showLoginUi()
            loadFragment(LoginFragment())
        }
    }

    private fun savePlaybackState(channel: XtreamChannel) {
        val prefs = getSharedPreferences("dazou_prefs", MODE_PRIVATE)
        prefs.edit()
            .putString("last_category_id", currentCategoryId)
            .putString("last_category_name", currentCategoryName)
            .putInt("last_channel_id", channel.streamId)
            .apply()
    }

    private fun restoreLastSessionOrShowCategories() {
        clearContentFragment()
        binding.sidebar.visibility = View.VISIBLE
        binding.videoPlayer.visibility = View.VISIBLE
        binding.channelInfo.visibility = View.VISIBLE
        binding.playerControls.visibility = View.VISIBLE
        binding.liveEpgPanel.visibility = View.VISIBLE
        if (lastCategories.isEmpty()) {
            liveViewModel.loadCategories()
        }

        val prefs = getSharedPreferences("dazou_prefs", MODE_PRIVATE)
        val savedCategoryId = prefs.getString("last_category_id", null)
        val savedCategoryName = prefs.getString("last_category_name", null)
        val savedChannelId = prefs.getInt("last_channel_id", -1)

        if (savedCategoryId != null && savedChannelId != -1) {
            currentCategoryId = savedCategoryId
            currentCategoryName = savedCategoryName ?: ""
            pendingAutoPlayChannelId = savedChannelId
            binding.channelsPanelTitle.text = currentCategoryName
            binding.channelsPanel.visibility = View.VISIBLE
            liveViewModel.loadChannels(savedCategoryId)
        }
    }

    fun playExternalMedia(url: String, name: String, type: String) {
        binding.videoPlayer.visibility = View.VISIBLE
        binding.channelInfo.visibility = View.VISIBLE
        binding.playerControls.visibility = View.VISIBLE
        playStream(url, name, type)
    }

    fun playChannelFromExternal(channel: XtreamChannel, sourceList: List<XtreamChannel>) {
        currentChannelList = sourceList
        currentChannelIndex = sourceList.indexOf(channel)
        val server = liveViewModel.getServer() ?: return
        val url = XtreamAPI.getStreamUrl(server, channel.streamId, channel.containerExtension, "live")
        playStream(url, channel.name, "live")
        updateNowPlayingPanel(channel)
        buildChannelStrip()
    }

    private fun updateNowPlayingPanel(channel: XtreamChannel) {
        val server = liveViewModel.getServer() ?: return
        binding.tvNowTitle.text = "⏳ جاري تحميل معلومات البرنامج..."
        binding.tvNowTime.text = ""
        binding.tvNextTitle.text = ""
        binding.pbNowProgress.progress = 0
        binding.tvControlsNow.text = "⏳ جاري تحميل معلومات البرنامج..."
        binding.pbControlsProgress.visibility = View.GONE

        XtreamAPI.getShortEpg(server, channel.streamId) { programs ->
            if (programs.isEmpty()) {
                binding.tvNowTitle.text = "📺 ${channel.name}"
                binding.tvNowTime.text = "لا توجد بيانات دليل برامج لهذه القناة"
                binding.tvNextTitle.text = ""
                binding.pbNowProgress.progress = 0
                binding.tvControlsNow.text = "📺 ${channel.name}"
                binding.pbControlsProgress.visibility = View.GONE
                return@getShortEpg
            }

            val now = programs.firstOrNull { it.nowPlaying } ?: programs.first()
            val nowIndex = programs.indexOf(now)
            val next = programs.getOrNull(nowIndex + 1)

            binding.tvNowTitle.text = "▶ الآن: ${now.title}"
            binding.pbNowProgress.progress = now.progressPercent()
            binding.tvNowTime.text = "${formatEpgTime(now.startTimestamp)} - ${formatEpgTime(now.stopTimestamp)}"
            binding.tvNextTitle.text = if (next != null)
                "⏭ التالي: ${next.title} (${formatEpgTime(next.startTimestamp)})"
            else ""

            binding.tvControlsNow.text = "${channel.name}  •  ${now.title}"
            binding.pbControlsProgress.visibility = View.VISIBLE
            binding.pbControlsProgress.progress = now.progressPercent()
        }
    }

    private fun formatEpgTime(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp * 1000))
    }

    private fun formatDuration(ms: Long): String {
        if (ms < 0) return "00:00"
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
    }

    private fun showControls() {
        binding.playerControls.visibility = View.VISIBLE
        controlsRunnable?.let { controlsHandler.removeCallbacks(it) }
        val runnable = Runnable {
            binding.playerControls.visibility = View.INVISIBLE
        }
        controlsRunnable = runnable
        controlsHandler.postDelayed(runnable, 5000)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && binding.videoPlayer.visibility == View.VISIBLE) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_CHANNEL_UP -> {
                    onNextChannel()
                    return true
                }
                KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                    onPreviousChannel()
                    return true
                }
                else -> showControls()
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun startSeekUpdater() {
        val runnable = object : Runnable {
            override fun run() {
                if (currentType != "live" && !userSeeking && playerManager.player.duration > 0) {
                    val pos = playerManager.player.currentPosition
                    val dur = playerManager.player.duration
                    val progress = ((pos.toFloat() / dur.toFloat()) * 1000).toInt()
                    binding.seekBar.progress = progress
                    binding.tvElapsed.text = formatDuration(pos)
                    binding.tvDuration.text = formatDuration(dur)
                }
                seekHandler.postDelayed(this, 500)
            }
        }
        seekRunnable = runnable
        seekHandler.post(runnable)
    }

    private fun updateMediaTypeUi() {
        val isLive = currentType == "live"
        binding.liveBadge.visibility = if (isLive) View.VISIBLE else View.GONE
        binding.seekBar.visibility = if (isLive) View.GONE else View.VISIBLE
        binding.tvElapsed.visibility = if (isLive) View.GONE else View.VISIBLE
        binding.tvDuration.visibility = if (isLive) View.GONE else View.VISIBLE
        binding.channelStripScroll.visibility = if (isLive) View.VISIBLE else View.GONE
    }

    private fun buildChannelStrip() {
        binding.channelStripTrack.removeAllViews()
        if (currentChannelList.isEmpty()) return

        val density = resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        val cells = mutableListOf<LinearLayout>()

        currentChannelList.forEachIndexed { index, channel ->
            val cell = LinearLayout(this)
            cell.id = View.generateViewId()
            cell.orientation = LinearLayout.VERTICAL
            cell.gravity = android.view.Gravity.CENTER
            val lp = LinearLayout.LayoutParams(dp(64), dp(56))
            lp.marginEnd = dp(6)
            cell.layoutParams = lp
            cell.setPadding(dp(4), dp(4), dp(4), dp(4))
            cell.setBackgroundResource(R.drawable.tv_button_selector)
            cell.isFocusable = true
            cell.isFocusableInTouchMode = true
            cell.isClickable = true
            cell.setOnClickListener { playChannelAt(index) }

            val idText = TextView(this)
            idText.text = "${index + 1}"
            idText.textSize = 9f
            idText.setTextColor(ContextCompat.getColor(this, R.color.text_gray))
            idText.gravity = android.view.Gravity.CENTER

            val nameText = TextView(this)
            nameText.text = channel.name
            nameText.textSize = 10f
            nameText.maxLines = 1
            nameText.ellipsize = android.text.TextUtils.TruncateAt.END
            nameText.gravity = android.view.Gravity.CENTER
            nameText.setTextColor(ContextCompat.getColor(this, R.color.text_white))

            cell.addView(idText)
            cell.addView(nameText)

            if (index == currentChannelIndex) {
                cell.background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = dp(6).toFloat()
                    setColor(ContextCompat.getColor(this@MainActivity, R.color.accent))
                }
            }

            binding.channelStripTrack.addView(cell)
            cells.add(cell)
        }

        for (i in cells.indices) {
            if (i > 0) cells[i].nextFocusRightId = cells[i - 1].id
            if (i < cells.size - 1) cells[i].nextFocusLeftId = cells[i + 1].id
            cells[i].nextFocusDownId = R.id.btn_play_pause
            cells[i].nextFocusUpId = R.id.menu_home
        }
        if (currentChannelIndex in cells.indices) {
            binding.btnPlayPause.nextFocusUpId = cells[currentChannelIndex].id
        }

        binding.channelStripScroll.post {
            val cellWidth = dp(70)
            val scrollX = (currentChannelIndex * cellWidth) - (binding.channelStripScroll.width / 2) + (cellWidth / 2)
            binding.channelStripScroll.smoothScrollTo(scrollX.coerceAtLeast(0), 0)
        }
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
        binding.liveEpgPanel.visibility = View.VISIBLE
        if (lastCategories.isEmpty()) {
            liveViewModel.loadCategories()
        }
    }

    private fun openCategory(category: XtreamCategory) {
        currentCategoryId = category.categoryId
        currentCategoryName = category.categoryName
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
        updateNowPlayingPanel(channel)
        savePlaybackState(channel)
        buildChannelStrip()
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
        binding.liveEpgPanel.visibility = View.GONE
        binding.videoPlayer.visibility = View.GONE
        binding.channelInfo.visibility = View.GONE
        binding.playerControls.visibility = View.GONE
    }

    private fun setupPlayerErrorHandling() {
        playerManager.player.addListener(object : Player.Listener {

            override fun onPlayerError(error: PlaybackException) {
                binding.playerLoading.visibility = View.GONE
                val isNetworkError = error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                    error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
                    error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS

                if (isNetworkError) {
                    binding.channelInfo.text = "⚠️ انقطع الاتصال، جاري إعادة المحاولة..."
                    playerManager.retryCurrent {
                        binding.channelInfo.text = "⚠️ تعذر الاتصال بالقناة بعد عدة محاولات"
                    }
                    return
                }

                val message = when (error.errorCode) {
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
                    Player.STATE_BUFFERING -> {
                        binding.channelInfo.text = "⏳ جاري التحميل..."
                        binding.playerLoading.visibility = View.VISIBLE
                    }
                    Player.STATE_READY -> {
                        playerManager.resetRetry()
                        binding.playerLoading.visibility = View.GONE
                        if (currentChannelName.isNotEmpty()) {
                            binding.channelInfo.text = "📺 $currentChannelName"
                        } else {
                            binding.channelInfo.text = ""
                        }
                    }
                    Player.STATE_ENDED -> {
                        binding.playerLoading.visibility = View.GONE
                    }
                }
            }
        })
    }

    private fun setupControls(){

        val focusShowListener = View.OnFocusChangeListener { _, hasFocus -> if (hasFocus) showControls() }

        binding.btnPlayPause.setOnClickListener {
            showControls()
            if(playerManager.isPlaying){
                playerManager.pause()
                binding.btnPlayPause.setImageResource(R.drawable.ic_play_small)
            }else{
                playerManager.resume()
                binding.btnPlayPause.setImageResource(R.drawable.ic_pause)
            }
        }
        binding.btnPlayPause.onFocusChangeListener = focusShowListener

        binding.btnFullscreen.setOnClickListener {
            showControls()
            toggleFullscreen()
        }
        binding.btnFullscreen.onFocusChangeListener = focusShowListener

        binding.btnPrev.setOnClickListener {
            showControls()
            onPreviousChannel()
        }
        binding.btnPrev.onFocusChangeListener = focusShowListener

        binding.btnNext.setOnClickListener {
            showControls()
            onNextChannel()
        }
        binding.btnNext.onFocusChangeListener = focusShowListener

        binding.btnVolume.setOnClickListener {
            showControls()
            toggleMute()
        }
        binding.btnVolume.onFocusChangeListener = focusShowListener

        binding.videoPlayer.setOnClickListener {
            showControls()
        }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                showControls()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) { userSeeking = true }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                userSeeking = false
                val dur = playerManager.player.duration
                if (dur > 0 && seekBar != null) {
                    val target = (dur * (seekBar.progress.toFloat() / 1000f)).toLong()
                    playerManager.player.seekTo(target)
                }
            }
        })
    }

    private fun toggleMute() {
        isMuted = !isMuted
        playerManager.player.volume = if (isMuted) 0f else 1f
        binding.btnVolume.setImageResource(if (isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_on)
    }

    private fun toggleFullscreen() {
        fullscreen = !fullscreen

        if (fullscreen) {
            wasChannelsPanelOpenBeforeFullscreen = binding.channelsPanel.visibility == View.VISIBLE

            binding.topBar.visibility = View.GONE
            binding.sidebar.visibility = View.GONE
            binding.channelsPanel.visibility = View.GONE
            binding.liveEpgPanel.visibility = View.GONE
            binding.fragmentContainer.visibility = View.GONE
            binding.btnFullscreen.setImageResource(R.drawable.ic_fullscreen_exit)

            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            )
        } else {
            binding.topBar.visibility = View.VISIBLE
            binding.sidebar.visibility = View.VISIBLE
            binding.btnFullscreen.setImageResource(R.drawable.ic_fullscreen_enter)
            if (wasChannelsPanelOpenBeforeFullscreen) {
                binding.channelsPanel.visibility = View.VISIBLE
            }
            if (supportFragmentManager.findFragmentById(binding.fragmentContainer.id) != null) {
                binding.fragmentContainer.visibility = View.VISIBLE
            } else {
                binding.liveEpgPanel.visibility = View.VISIBLE
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
        binding.liveEpgPanel.visibility = View.GONE
        binding.fragmentContainer.visibility = View.VISIBLE
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment)
            .commit()
    }

    override fun playStream(url:String, name:String, type:String){
        currentChannelName = name
        currentType = type
        playerManager.play(url, name, type)
        binding.channelInfo.text = "📺 $name"
        binding.btnPlayPause.setImageResource(R.drawable.ic_pause)
        updateMediaTypeUi()
        showControls()
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

    override fun onStop() {
        super.onStop()
        wasPlayingBeforeBackground = playerManager.isPlaying
        if (playerManager.isPlaying) {
            playerManager.pause()
        }
    }

    override fun onStart() {
        super.onStart()
        if (wasPlayingBeforeBackground) {
            playerManager.resume()
            wasPlayingBeforeBackground = false
        }
    }

    override fun onDestroy(){
        clockRunnable?.let { clockHandler.removeCallbacks(it) }
        controlsRunnable?.let { controlsHandler.removeCallbacks(it) }
        seekRunnable?.let { seekHandler.removeCallbacks(it) }
        networkCallback?.let {
            try { connectivityManager.unregisterNetworkCallback(it) } catch (_: Exception) {}
        }
        playerManager.release()
        super.onDestroy()
    }
}