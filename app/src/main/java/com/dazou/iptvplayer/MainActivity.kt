package com.dazou.iptvplayer

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView
    private lateinit var rv: RecyclerView
    private lateinit var tvTitle: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnHome: Button
    private lateinit var btnLive: Button
    private lateinit var btnMovies: Button
    private lateinit var btnSeries: Button
    private lateinit var btnFavorites: Button
    private lateinit var btnBack: Button
    private lateinit var searchLayout: LinearLayout
    private lateinit var etSearch: EditText
    private lateinit var playerControlsLayout: LinearLayout
    private lateinit var btnPrevChannel: Button
    private lateinit var btnPlayPause: Button
    private lateinit var btnNextChannel: Button
    private lateinit var btnFullscreen: Button
    private lateinit var btnDownload: Button
    private lateinit var btnQuality: Button
    private lateinit var btnAspectRatio: Button
    private lateinit var seekBar: SeekBar
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvTotalTime: TextView
    private lateinit var tvChannelInfo: TextView
    private lateinit var volumeSeekBar: SeekBar
    private lateinit var audioManager: AudioManager

    private var server: XtreamServer? = null
    private var liveChannels = mutableListOf<XtreamChannel>()
    private var vodMovies = mutableListOf<XtreamMovie>()
    private var seriesList = mutableListOf<XtreamSeries>()
    private var liveCategories = mutableListOf<XtreamCategory>()
    private var vodCategories = mutableListOf<XtreamCategory>()
    private var seriesCategories = mutableListOf<XtreamCategory>()
    private var favorites = mutableListOf<FavoriteItem>()
    private var watchHistory = mutableListOf<HistoryItem>()
    private var epgData = mutableListOf<EpgProgram>()
    private var currentStreamUrl: String? = null
    private var currentStreamName: String? = null
    private var currentStreamIndex = -1
    private var currentStreamType = "live"
    private var isPlayerPlaying = false
    private var currentAspectRatio = 0
    private var currentTheme = "dark"
    private var downloadId: Long = 0

    private var currentCategory = "home"
    private var selectedCategoryId: String? = null
    private var isShowingCategories = true

    private lateinit var prefs: SharedPreferences
    private lateinit var downloadManager: DownloadManager
    private lateinit var themes: Map<String, ThemeColors>

    data class FavoriteItem(val type: String, val id: Int, val name: String, val icon: String = "")
    data class HistoryItem(val type: String, val id: Int, val name: String, val timestamp: Long)
    data class EpgProgram(val channelId: String, val title: String, val startTime: String, val endTime: String, val description: String)
    data class ThemeColors(val name: String, val bg: Int, val card: Int, val accent: Int, val bottomBar: Int, val textWhite: Int, val textGray: Int, val activeTab: Int)

    private fun initThemes() {
        themes = mapOf(
            "dark" to ThemeColors("داكن", Color.parseColor("#0F0F1A"), Color.parseColor("#1A1A35"), Color.parseColor("#FF6B6B"), Color.parseColor("#12122A"), Color.parseColor("#FFFFFF"), Color.parseColor("#AAAAAA"), Color.parseColor("#2D2D5E")),
            "blue" to ThemeColors("أزرق", Color.parseColor("#0A1628"), Color.parseColor("#1B2D4A"), Color.parseColor("#4FC3F7"), Color.parseColor("#0D1F3C"), Color.parseColor("#FFFFFF"), Color.parseColor("#90CAF9"), Color.parseColor("#1565C0")),
            "green" to ThemeColors("أخضر", Color.parseColor("#0A1F0A"), Color.parseColor("#1A3A1A"), Color.parseColor("#66BB6A"), Color.parseColor("#0D2A0D"), Color.parseColor("#FFFFFF"), Color.parseColor("#A5D6A7"), Color.parseColor("#2E7D32")),
            "purple" to ThemeColors("بنفسجي", Color.parseColor("#1A0A2E"), Color.parseColor("#2D1B4E"), Color.parseColor("#CE93D8"), Color.parseColor("#1F0D3D"), Color.parseColor("#FFFFFF"), Color.parseColor("#E1BEE7"), Color.parseColor("#6A1B9A")),
            "red" to ThemeColors("أحمر", Color.parseColor("#1A0A0A"), Color.parseColor("#3A1A1A"), Color.parseColor("#EF5350"), Color.parseColor("#2A0D0D"), Color.parseColor("#FFFFFF"), Color.parseColor("#EF9A9A"), Color.parseColor("#C62828"))
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        prefs = getSharedPreferences("mndazou_prefs", Context.MODE_PRIVATE)
        downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        initThemes()
        currentTheme = prefs.getString("theme", "dark") ?: "dark"
        val theme = themes[currentTheme]!!

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(theme.bg)
        }

        // شريط العنوان
        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20, 45, 20, 20)
            setBackgroundColor(theme.bottomBar)
            gravity = Gravity.CENTER_VERTICAL
        }

        btnBack = Button(this).apply {
            text = "⬅️"; textSize = 18f; setBackgroundColor(Color.TRANSPARENT)
            setTextColor(theme.textWhite); visibility = View.GONE
            setOnClickListener { goBackToCategories() }
        }
        headerLayout.addView(btnBack)

        tvTitle = TextView(this).apply {
            text = "MN-DAZOU IPTV"; textSize = 22f; setTextColor(theme.accent)
            setTypeface(null, Typeface.BOLD); gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        headerLayout.addView(tvTitle)

        val btnTheme = Button(this).apply {
            text = "🎨"; textSize = 18f; setBackgroundColor(Color.TRANSPARENT)
            setTextColor(theme.textGray); setOnClickListener { showThemeDialog() }
        }
        headerLayout.addView(btnTheme)

        val btnSettings = Button(this).apply {
            text = "⚙️"; textSize = 18f; setBackgroundColor(Color.TRANSPARENT)
            setTextColor(theme.textGray); setOnClickListener { showSettingsDialog() }
        }
        headerLayout.addView(btnSettings)
        root.addView(headerLayout)

        // شريط البحث
        searchLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; setPadding(15, 10, 15, 10)
            setBackgroundColor(theme.bottomBar); gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
        }
        etSearch = EditText(this).apply {
            hint = "🔍 بحث..."; setHintTextColor(theme.textGray); setTextColor(theme.textWhite)
            setBackgroundColor(theme.card); setPadding(25, 15, 25, 15)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        searchLayout.addView(etSearch)
        val btnSearchGo = Button(this).apply {
            text = "بحث"; setBackgroundColor(theme.accent); setTextColor(Color.BLACK)
            setOnClickListener { performSearch() }
        }
        searchLayout.addView(btnSearchGo)
        root.addView(searchLayout)

        // مشغل الفيديو
        val playerCard = CardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 420)
            radius = 0f; setCardBackgroundColor(Color.BLACK); cardElevation = 8f
        }
        val playerContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        playerView = PlayerView(this).apply {
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.BLACK)
        }
        playerContainer.addView(playerView)

        // معلومات القناة الحالية
        tvChannelInfo = TextView(this).apply {
            text = ""; textSize = 13f; setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#CC000000")); setPadding(15, 8, 15, 8)
            visibility = View.GONE; gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP)
        }
        playerContainer.addView(tvChannelInfo)
        playerCard.addView(playerContainer)
        root.addView(playerCard)

        // عناصر تحكم المشغل - الصف الأول: التنقل
        val navControlsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; setPadding(15, 10, 15, 10)
            setBackgroundColor(Color.parseColor("#DD000000")); gravity = Gravity.CENTER
            visibility = View.GONE
        }
        btnPrevChannel = Button(this).apply { text = "⏪"; setBackgroundColor(Color.TRANSPARENT); setTextColor(Color.WHITE); textSize = 22f; setOnClickListener { playPreviousChannel() } }
        btnPlayPause = Button(this).apply { text = "▶️"; setBackgroundColor(Color.TRANSPARENT); setTextColor(Color.WHITE); textSize = 22f; setOnClickListener { togglePlayPause() } }
        btnNextChannel = Button(this).apply { text = "⏩"; setBackgroundColor(Color.TRANSPARENT); setTextColor(Color.WHITE); textSize = 22f; setOnClickListener { playNextChannel() } }
        navControlsLayout.addView(btnPrevChannel)
        navControlsLayout.addView(btnPlayPause)
        navControlsLayout.addView(btnNextChannel)
        root.addView(navControlsLayout)

        // عناصر تحكم المشغل - الصف الثاني: الأدوات
        playerControlsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; setPadding(15, 8, 15, 8)
            setBackgroundColor(Color.parseColor("#DD000000")); gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
        }
        btnQuality = Button(this).apply { text = "HD"; setBackgroundColor(Color.TRANSPARENT); setTextColor(Color.WHITE); setOnClickListener { showQualityDialog() } }
        btnAspectRatio = Button(this).apply { text = "🔲"; setBackgroundColor(Color.TRANSPARENT); setTextColor(Color.WHITE); setOnClickListener { changeAspectRatio() } }
        btnDownload = Button(this).apply { text = "⬇️"; setBackgroundColor(Color.TRANSPARENT); setTextColor(Color.WHITE); setOnClickListener { downloadCurrentStream() } }
        btnFullscreen = Button(this).apply { text = "⛶"; setBackgroundColor(Color.TRANSPARENT); setTextColor(Color.WHITE); setOnClickListener { toggleFullscreen() } }
        playerControlsLayout.addView(btnQuality)
        playerControlsLayout.addView(btnAspectRatio)
        playerControlsLayout.addView(btnDownload)
        playerControlsLayout.addView(btnFullscreen)
        root.addView(playerControlsLayout)

        // شريط التقدم
        val seekLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; setPadding(10, 5, 10, 5)
            setBackgroundColor(Color.parseColor("#DD000000")); gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
        }
        tvCurrentTime = TextView(this).apply { text = "00:00"; setTextColor(Color.WHITE); textSize = 12f }
        seekBar = SeekBar(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) { if (fromUser) player.seekTo(progress.toLong()) }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        tvTotalTime = TextView(this).apply { text = "00:00"; setTextColor(Color.WHITE); textSize = 12f }
        seekLayout.addView(tvCurrentTime)
        seekLayout.addView(seekBar)
        seekLayout.addView(tvTotalTime)
        root.addView(seekLayout)

        // شريط التحميل
        progressBar = ProgressBar(this).apply {
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 6)
        }
        root.addView(progressBar)

        // قائمة المحتوى
        rv = RecyclerView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            layoutManager = LinearLayoutManager(this@MainActivity)
            setBackgroundColor(theme.bg)
        }
        root.addView(rv)

        // شريط التنقل السفلي
        val bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; setPadding(5, 12, 5, 20)
            setBackgroundColor(theme.bottomBar); gravity = Gravity.CENTER
        }
        btnHome = createBottomButton("🏠", "الرئيسية", theme) { switchTab("home") }
        btnLive = createBottomButton("📺", "مباشر", theme) { switchTab("live") }
        btnMovies = createBottomButton("🎬", "أفلام", theme) { switchTab("movies") }
        btnSeries = createBottomButton("🎭", "مسلسلات", theme) { switchTab("series") }
        btnFavorites = createBottomButton("⭐", "مفضلة", theme) { switchTab("favorites") }
        bottomBar.addView(btnHome); bottomBar.addView(btnLive); bottomBar.addView(btnMovies)
        bottomBar.addView(btnSeries); bottomBar.addView(btnFavorites)
        root.addView(bottomBar)

        setContentView(root)
        initializePlayer()
        loadFavorites()
        loadHistory()
        registerDownloadReceiver()

        val savedUrl = prefs.getString("server_url", "")
        if (!savedUrl.isNullOrEmpty()) {
            server = XtreamServer(url = savedUrl!!, username = prefs.getString("server_username", "")!!, password = prefs.getString("server_password", "")!!)
            switchTab("home")
        } else {
            showLoginDialog()
        }

        // استعادة آخر قناة
        val lastChannelIndex = prefs.getInt("last_channel_index", -1)
        val lastChannelType = prefs.getString("last_channel_type", "live") ?: "live"
        if (lastChannelIndex >= 0) {
            currentStreamIndex = lastChannelIndex
            currentStreamType = lastChannelType
        }
    }

    private fun createBottomButton(icon: String, label: String, theme: ThemeColors, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = "$icon\n$label"; textSize = 10f; setTextColor(theme.textGray)
            setBackgroundColor(Color.TRANSPARENT); setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            gravity = Gravity.CENTER; setOnClickListener { onClick() }
        }
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(this).build()
        playerView.player = player
        playerView.useController = false
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                isPlayerPlaying = isPlaying
                btnPlayPause.text = if (isPlaying) "⏸️" else "▶️"
                showPlayerControls()
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    seekBar.max = player.duration.toInt()
                    tvTotalTime.text = formatTime(player.duration)
                } else if (playbackState == Player.STATE_ENDED) {
                    playNextChannel()
                }
            }
        })

        Thread {
            while (true) {
                if (player.isPlaying) {
                    runOnUiThread {
                        seekBar.progress = player.currentPosition.toInt()
                        tvCurrentTime.text = formatTime(player.currentPosition)
                    }
                }
                Thread.sleep(500)
            }
        }.start()
    }

    private fun showPlayerControls() {
        val anim = AlphaAnimation(0f, 1f)
        anim.duration = 300
        (playerControlsLayout.parent as? ViewGroup)?.let { vg ->
            for (i in 0 until vg.childCount) {
                val child = vg.getChildAt(i)
                if (child is LinearLayout && child != playerControlsLayout) {
                    child.visibility = View.VISIBLE
                    child.startAnimation(anim)
                }
            }
        }
        playerControlsLayout.visibility = View.VISIBLE
        playerControlsLayout.startAnimation(anim)
        tvChannelInfo.visibility = View.VISIBLE
        tvChannelInfo.startAnimation(anim)
        
        // إخفاء بعد 5 ثواني
        tvChannelInfo.postDelayed({
            hidePlayerControls()
        }, 5000)
    }

    private fun hidePlayerControls() {
        val anim = AlphaAnimation(1f, 0f)
        anim.duration = 300
        (playerControlsLayout.parent as? ViewGroup)?.let { vg ->
            for (i in 0 until vg.childCount) {
                val child = vg.getChildAt(i)
                if (child is LinearLayout) {
                    child.visibility = View.GONE
                }
            }
        }
        tvChannelInfo.visibility = View.GONE
    }

    private fun formatTime(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        val hours = millis / (1000 * 60 * 60)
        return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds)
        else String.format("%02d:%02d", minutes, seconds)
    }

    private fun playNextChannel() {
        val channels = when (currentStreamType) {
            "live" -> liveChannels; "movie" -> vodMovies; else -> emptyList()
        }
        if (channels.isEmpty() || currentStreamIndex < 0) return
        val nextIndex = (currentStreamIndex + 1) % channels.size
        playChannelAtIndex(nextIndex)
    }

    private fun playPreviousChannel() {
        val channels = when (currentStreamType) {
            "live" -> liveChannels; "movie" -> vodMovies; else -> emptyList()
        }
        if (channels.isEmpty() || currentStreamIndex < 0) return
        val prevIndex = if (currentStreamIndex - 1 < 0) channels.size - 1 else currentStreamIndex - 1
        playChannelAtIndex(prevIndex)
    }

    private fun playChannelAtIndex(index: Int) {
        val channels = when (currentStreamType) {
            "live" -> liveChannels; "movie" -> vodMovies; else -> emptyList()
        }
        if (index < 0 || index >= channels.size) return
        
        currentStreamIndex = index
        val channel = channels[index]
        val url = when (currentStreamType) {
            "live" -> XtreamAPI.getStreamUrl(server!!, (channel as XtreamChannel).streamId, channel.containerExtension)
            "movie" -> XtreamAPI.getMovieUrl(server!!, (channel as XtreamMovie).streamId, channel.containerExtension)
            else -> ""
        }
        playStream(url, channel.name)
        tvChannelInfo.text = "🎬 ${channel.name} (${index + 1}/${channels.size})"
        
        // حفظ آخر قناة
        prefs.edit().putInt("last_channel_index", index).putString("last_channel_type", currentStreamType).apply()
        
        // تمرير القائمة إلى القناة الحالية
        rv.smoothScrollToPosition(index)
    }

    private fun togglePlayPause() { if (isPlayerPlaying) player.pause() else player.play() }
    
    private fun changeAspectRatio() {
        currentAspectRatio = (currentAspectRatio + 1) % 3
        playerView.resizeMode = when (currentAspectRatio) {
            0 -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
            1 -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
            2 -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            else -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
        Toast.makeText(this, "📐 ${arrayOf("ملائم", "ملء", "تكبير")[currentAspectRatio]}", Toast.LENGTH_SHORT).show()
    }

    private fun toggleFullscreen() {
        if (playerView.layoutParams.height == 420) {
            playerView.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            Toast.makeText(this, "⛶ ملء الشاشة", Toast.LENGTH_SHORT).show()
        } else {
            playerView.layoutParams.height = 420
            Toast.makeText(this, "📱 الوضع العادي", Toast.LENGTH_SHORT).show()
        }
        playerView.requestLayout()
    }

    private fun showQualityDialog() {
        AlertDialog.Builder(this).setTitle("🎯 جودة الفيديو")
            .setItems(arrayOf("Auto", "4K", "1080p", "720p", "480p")) { _, _ ->
                Toast.makeText(this, "⚙️ الجودة التلقائية مفعلة", Toast.LENGTH_SHORT).show()
            }.show()
    }

    private fun downloadCurrentStream() {
        currentStreamUrl?.let { url ->
            try {
                val request = DownloadManager.Request(Uri.parse(url))
                    .setTitle("MN-DAZOU IPTV - تحميل")
                    .setDescription("جاري تحميل: ${currentStreamName ?: "فيديو"}")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES, "MN-DAZOU_${System.currentTimeMillis()}.mp4")
                downloadId = downloadManager.enqueue(request)
                Toast.makeText(this, "⬇️ جاري التحميل...", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) { Toast.makeText(this, "❌ فشل التحميل", Toast.LENGTH_SHORT).show() }
        } ?: Toast.makeText(this, "لا يوجد فيديو", Toast.LENGTH_SHORT).show()
    }

    private fun registerDownloadReceiver() {
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) == downloadId)
                    Toast.makeText(this@MainActivity, "✅ تم التحميل", Toast.LENGTH_SHORT).show()
            }
        }, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_NEXT -> { playNextChannel(); true }
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MEDIA_PREVIOUS -> { playPreviousChannel(); true }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> { togglePlayPause(); true }
            KeyEvent.KEYCODE_VOLUME_UP -> { audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI); true }
            KeyEvent.KEYCODE_VOLUME_DOWN -> { audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI); true }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(this).setTitle("⚙️ الإعدادات")
            .setItems(arrayOf("🎨 تغيير الثيم", "🔗 إعدادات Xtream", "📋 إضافة قائمة M3U", "📺 تحديث EPG", "🗑️ مسح البيانات")) { _, which ->
                when (which) {
                    0 -> showThemeDialog(); 1 -> showLoginDialog(); 2 -> showM3uDialog(); 3 -> loadEpg()
                    4 -> { prefs.edit().clear().apply(); Toast.makeText(this, "تم مسح البيانات", Toast.LENGTH_SHORT).show(); recreate() }
                }
            }.show()
    }

    private fun showThemeDialog() {
        AlertDialog.Builder(this).setTitle("🎨 اختر الثيم")
            .setItems(themes.values.map { it.name }.toTypedArray()) { _, which ->
                prefs.edit().putString("theme", themes.keys.toList()[which]).apply()
                Toast.makeText(this, "🔄 أعد تشغيل التطبيق", Toast.LENGTH_LONG).show()
            }.show()
    }

    private fun showM3uDialog() {
        val etM3u = EditText(this).apply { hint = "رابط M3U أو الصق المحتوى"; minLines = 3; setPadding(30, 20, 30, 20) }
        AlertDialog.Builder(this).setTitle("📋 إضافة M3U").setView(etM3u)
            .setPositiveButton("تحميل") { _, _ ->
                val input = etM3u.text.toString()
                if (input.startsWith("http")) loadM3uFromUrl(input) else parseM3uContent(input)
            }.setNegativeButton("إلغاء", null).show()
    }

    private fun loadM3uFromUrl(url: String) {
        showLoading(); Thread {
            try {
                val content = java.net.URL(url).readText()
                runOnUiThread { hideLoading(); parseM3uContent(content) }
            } catch (e: Exception) { runOnUiThread { hideLoading(); Toast.makeText(this, "❌ فشل التحميل", Toast.LENGTH_SHORT).show() } }
        }.start()
    }

    private fun parseM3uContent(content: String) {
        val lines = content.split("\n"); val channels = mutableListOf<XtreamChannel>(); var currentName = ""
        for (line in lines) {
            if (line.startsWith("#EXTINF")) currentName = Regex(",(.+)").find(line)?.groupValues?.get(1)?.trim() ?: "قناة"
            else if (line.startsWith("http")) channels.add(XtreamChannel(channels.size, currentName, "live", "", "", "", "m3u", "ts"))
        }
        liveChannels.addAll(channels)
        Toast.makeText(this, "✅ تم إضافة ${channels.size} قناة", Toast.LENGTH_SHORT).show()
        updateLiveList()
    }

    private fun loadEpg() {
        server?.let { s ->
            showLoading(); Thread {
                try {
                    val json = java.net.URL("${s.url}/player_api.php?username=${s.username}&password=${s.password}&action=get_short_epg").readText()
                    val jsonArray = JSONObject(json).getJSONArray("epg_listings"); epgData.clear()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        epgData.add(EpgProgram(obj.optString("epg_id",""), obj.optString("title",""), obj.optString("start",""), obj.optString("end",""), obj.optString("description","")))
                    }
                    runOnUiThread { hideLoading(); Toast.makeText(this, "📺 ${epgData.size} برنامج", Toast.LENGTH_SHORT).show(); showEpg() }
                } catch (e: Exception) { runOnUiThread { hideLoading(); Toast.makeText(this, "❌ فشل EPG", Toast.LENGTH_SHORT).show() } }
            }.start()
        } ?: Toast.makeText(this, "سجل دخول أولاً", Toast.LENGTH_SHORT).show()
    }

    private fun showEpg() {
        val grouped = epgData.groupBy { it.channelId }; val names = grouped.keys.toTypedArray()
        if (names.isEmpty()) { Toast.makeText(this, "لا توجد بيانات", Toast.LENGTH_SHORT).show(); return }
        AlertDialog.Builder(this).setTitle("📺 دليل البرامج").setItems(names) { _, i ->
            val programs = grouped[names[i]] ?: emptyList()
            AlertDialog.Builder(this).setTitle(names[i])
                .setItems(programs.map { "🕐 ${it.startTime}-${it.endTime}: ${it.title}" }.toTypedArray(), null)
                .setPositiveButton("إغلاق", null).show()
        }.setPositiveButton("إغلاق", null).show()
    }

    private fun switchTab(tab: String) {
        currentCategory = tab; selectedCategoryId = null; isShowingCategories = true
        btnBack.visibility = View.GONE; searchLayout.visibility = View.GONE
        val theme = themes[currentTheme]!!
        btnHome.setTextColor(theme.textGray); btnLive.setTextColor(theme.textGray)
        btnMovies.setTextColor(theme.textGray); btnSeries.setTextColor(theme.textGray); btnFavorites.setTextColor(theme.textGray)
        when (tab) {
            "home" -> { btnHome.setTextColor(theme.accent); tvTitle.text = "🏠 MN-DAZOU IPTV"; showHomeScreen() }
            "live" -> { btnLive.setTextColor(theme.accent); tvTitle.text = "📺 البث المباشر"; searchLayout.visibility = View.VISIBLE; currentStreamType = "live"; loadLiveCategories() }
            "movies" -> { btnMovies.setTextColor(theme.accent); tvTitle.text = "🎬 الأفلام"; searchLayout.visibility = View.VISIBLE; currentStreamType = "movie"; loadVodCategories() }
            "series" -> { btnSeries.setTextColor(theme.accent); tvTitle.text = "🎭 المسلسلات"; searchLayout.visibility = View.VISIBLE; loadSeriesCategories() }
            "favorites" -> { btnFavorites.setTextColor(theme.accent); tvTitle.text = "⭐ المفضلة"; showFavorites() }
        }
    }

    private fun showHomeScreen() {
        val theme = themes[currentTheme]!!; val allItems = mutableListOf<Any>()
        if (watchHistory.isNotEmpty()) { allItems.add("section_history"); allItems.addAll(watchHistory.takeLast(5).reversed()) }
        if (favorites.isNotEmpty()) { allItems.add("section_favorites"); allItems.addAll(favorites.take(5)) }
        allItems.add("section_quick"); allItems.add("quick_live"); allItems.add("quick_movies"); allItems.add("quick_series"); allItems.add("quick_epg")
        if (allItems.isEmpty()) allItems.add("empty")
        rv.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun getItemViewType(pos: Int) = if (allItems[pos] is String) 0 else 1
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                return if (viewType == 0) { val tv = TextView(parent.context).apply { setPadding(20,15,20,10); textSize=16f; setTextColor(theme.accent); setTypeface(null,Typeface.BOLD) }; object : RecyclerView.ViewHolder(tv) {} }
                else { val card = CardView(parent.context).apply { layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(15,6,15,6) }; radius=12f; setCardBackgroundColor(theme.card); cardElevation=3f }
                    val tv = TextView(parent.context).apply { setPadding(25,20,25,20); textSize=15f; setTextColor(theme.textWhite); setTypeface(null,Typeface.BOLD) }; card.addView(tv); object : RecyclerView.ViewHolder(card) {} }
            }
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
                val item = allItems[pos]
                if (item is String) { val tv = holder.itemView as TextView
                    when (item) {
                        "section_history" -> tv.text="🕐 آخر المشاهدات"; "section_favorites"-> tv.text="⭐ المفضلة"; "section_quick"-> tv.text="🚀 وصول سريع"
                        "quick_live"-> { tv.text="📺 البث المباشر"; tv.setOnClickListener{switchTab("live")} }
                        "quick_movies"-> { tv.text="🎬 الأفلام"; tv.setOnClickListener{switchTab("movies")} }
                        "quick_series"-> { tv.text="🎭 المسلسلات"; tv.setOnClickListener{switchTab("series")} }
                        "quick_epg"-> { tv.text="📋 دليل البرامج"; tv.setOnClickListener{loadEpg()} }
                        "empty"-> tv.text="👋 أهلاً بك!"
                    }
                } else if (item is HistoryItem) { (holder.itemView as CardView).let { (it.getChildAt(0) as TextView).text="🕐 ${item.name}"; it.setOnClickListener{playHistoryItem(item)} } }
                else if (item is FavoriteItem) { (holder.itemView as CardView).let { (it.getChildAt(0) as TextView).text="⭐ ${item.name}"; it.setOnClickListener{playFavoriteItem(item)} } }
            }
            override fun getItemCount() = allItems.size
        }
    }

    private fun showFavorites() {
        val theme = themes[currentTheme]!!
        rv.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val card = CardView(parent.context).apply { layoutParams=ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT).apply{setMargins(15,6,15,6)}; radius=12f; setCardBackgroundColor(theme.card); cardElevation=3f }
                val layout = LinearLayout(parent.context).apply { orientation=LinearLayout.HORIZONTAL; setPadding(25,20,25,20); gravity=Gravity.CENTER_VERTICAL }
                val tv = TextView(parent.context).apply { textSize=15f; setTextColor(theme.textWhite); setTypeface(null,Typeface.BOLD); layoutParams=LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f) }
                layout.addView(tv); layout.addView(Button(parent.context).apply{text="❌"; setBackgroundColor(Color.TRANSPARENT)})
                card.addView(layout); return object : RecyclerView.ViewHolder(card) {}
            }
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
                val layout = (holder.itemView as CardView).getChildAt(0) as LinearLayout
                val fav = favorites[pos]; (layout.getChildAt(0) as TextView).text = "⭐ ${fav.name}"
                holder.itemView.setOnClickListener { playFavoriteItem(fav) }
                layout.getChildAt(1).setOnClickListener { removeFavorite(fav); showFavorites() }
            }
            override fun getItemCount() = favorites.size
        }
    }

    private fun goBackToCategories() { isShowingCategories=true; selectedCategoryId=null; btnBack.visibility=View.GONE; when(currentCategory){"live"->showLiveCategories();"movies"->showVodCategories();"series"->showSeriesCategories()} }

    private fun showLoginDialog() {
        val theme = themes[currentTheme]!!; val dialogView = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(50,50,50,50); setBackgroundColor(theme.card) }
        dialogView.addView(TextView(this).apply{text="⚙️ Xtream Codes"; textSize=20f; setTextColor(theme.accent); setTypeface(null,Typeface.BOLD); gravity=Gravity.CENTER; setPadding(0,0,0,30)})
        val etServer = EditText(this).apply{hint="رابط السيرفر"; setHintTextColor(theme.textGray); setTextColor(theme.textWhite); setBackgroundColor(theme.bg); setPadding(30,20,30,20); setText("http://")}
        val etUsername = EditText(this).apply{hint="اسم المستخدم"; setHintTextColor(theme.textGray); setTextColor(theme.textWhite); setBackgroundColor(theme.bg); setPadding(30,20,30,20)}
        val etPassword = EditText(this).apply{hint="كلمة المرور"; setHintTextColor(theme.textGray); setTextColor(theme.textWhite); setBackgroundColor(theme.bg); setPadding(30,20,30,20)}
        dialogView.addView(etServer); dialogView.addView(createSpacer(10)); dialogView.addView(etUsername); dialogView.addView(createSpacer(10)); dialogView.addView(etPassword)
        AlertDialog.Builder(this).setView(dialogView).setPositiveButton("اتصال"){_,_-> server=XtreamServer(etServer.text.toString().trimEnd('/'),etUsername.text.toString(),etPassword.text.toString()); saveServerData(); Toast.makeText(this,"✅ تم",Toast.LENGTH_SHORT).show(); switchTab("home")}.setNegativeButton("إلغاء",null).show()
    }

    private fun saveServerData() { server?.let { prefs.edit().putString("server_url",it.url).putString("server_username",it.username).putString("server_password",it.password).apply() } }
    private fun createSpacer(h: Int) = View(this).apply { layoutParams=LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,h) }

    private fun performSearch() {
        val q = etSearch.text.toString().lowercase(); if(q.isEmpty()) return
        when(currentCategory) {
            "live" -> { val f = liveChannels.filter{it.name.lowercase().contains(q)}; if(f.isNotEmpty()){liveChannels.clear();liveChannels.addAll(f);updateLiveList();tvTitle.text="🔍 $q"} else Toast.makeText(this,"لا نتائج",Toast.LENGTH_SHORT).show() }
            "movies" -> { val f = vodMovies.filter{it.name.lowercase().contains(q)}; if(f.isNotEmpty()){vodMovies.clear();vodMovies.addAll(f);updateMoviesList();tvTitle.text="🔍 $q"} else Toast.makeText(this,"لا نتائج",Toast.LENGTH_SHORT).show() }
        }
    }

    private fun addToFavorites(type: String, id: Int, name: String) { if(favorites.none{it.type==type&&it.id==id}){favorites.add(FavoriteItem(type,id,name));saveFavorites();Toast.makeText(this,"⭐ تم",Toast.LENGTH_SHORT).show()} else Toast.makeText(this,"موجود",Toast.LENGTH_SHORT).show() }
    private fun removeFavorite(item: FavoriteItem) { favorites.removeAll{it.type==item.type&&it.id==item.id}; saveFavorites() }
    private fun saveFavorites() { val j=JSONArray(); favorites.forEach{val o=JSONObject();o.put("type",it.type);o.put("id",it.id);o.put("name",it.name);j.put(o)}; prefs.edit().putString("favorites",j.toString()).apply() }
    private fun loadFavorites() { val s=prefs.getString("favorites","[]")?:return; val j=JSONArray(s); for(i in 0 until j.length()){val o=j.getJSONObject(i);favorites.add(FavoriteItem(o.getString("type"),o.getInt("id"),o.getString("name")))} }
    private fun addToHistory(type: String, id: Int, name: String) { watchHistory.removeAll{it.type==type&&it.id==id}; watchHistory.add(HistoryItem(type,id,name,System.currentTimeMillis())); if(watchHistory.size>20)watchHistory.removeAt(0); saveHistory() }
    private fun saveHistory() { val j=JSONArray(); watchHistory.forEach{val o=JSONObject();o.put("type",it.type);o.put("id",it.id);o.put("name",it.name);o.put("timestamp",it.timestamp);j.put(o)}; prefs.edit().putString("history",j.toString()).apply() }
    private fun loadHistory() { val s=prefs.getString("history","[]")?:return; val j=JSONArray(s); for(i in 0 until j.length()){val o=j.getJSONObject(i);watchHistory.add(HistoryItem(o.getString("type"),o.getInt("id"),o.getString("name"),o.getLong("timestamp")))} }
    private fun playFavoriteItem(fav: FavoriteItem) { when(fav.type){"live"->{val u=XtreamAPI.getStreamUrl(server!!,fav.id);playStream(u,fav.name);addToHistory("live",fav.id,fav.name)} "movie"->{val u=XtreamAPI.getMovieUrl(server!!,fav.id);playStream(u,fav.name);addToHistory("movie",fav.id,fav.name)} } }
    private fun playHistoryItem(item: HistoryItem) { when(item.type){"live"->{playStream(XtreamAPI.getStreamUrl(server!!,item.id),item.name)} "movie"->{playStream(XtreamAPI.getMovieUrl(server!!,item.id),item.name)} } }

    private fun loadLiveCategories() { server?.let{s->showLoading();XtreamAPI.getLiveCategories(s){liveCategories.clear();liveCategories.addAll(it);hideLoading();if(it.isEmpty())loadLiveStreams(null)else showLiveCategories()}} ?: showLoginDialog() }
    private fun loadVodCategories() { server?.let{s->showLoading();XtreamAPI.getVodCategories(s){vodCategories.clear();vodCategories.addAll(it);hideLoading();if(it.isEmpty())loadMovies(null)else showVodCategories()}} ?: showLoginDialog() }
    private fun loadSeriesCategories() { server?.let{s->showLoading();XtreamAPI.getLiveCategories(s){seriesCategories.clear();seriesCategories.addAll(it);hideLoading();if(it.isEmpty())loadSeriesList(null)else showSeriesCategories()}} ?: showLoginDialog() }

    private fun showLiveCategories() { val t=themes[currentTheme]!!; tvTitle.text="📺 المجموعات (${liveCategories.size})"; rv.adapter=createCategoryAdapter(liveCategories,"📁",t.accent){selectedCategoryId=it.categoryId;isShowingCategories=false;btnBack.visibility=View.VISIBLE;tvTitle.text="📺 ${it.categoryName}";loadLiveStreams(it.categoryId)} }
    private fun showVodCategories() { val t=themes[currentTheme]!!; tvTitle.text="🎬 المجموعات (${vodCategories.size})"; rv.adapter=createCategoryAdapter(vodCategories,"🎬",t.accent){selectedCategoryId=it.categoryId;isShowingCategories=false;btnBack.visibility=View.VISIBLE;tvTitle.text="🎬 ${it.categoryName}";loadMovies(it.categoryId)} }
    private fun showSeriesCategories() { val t=themes[currentTheme]!!; tvTitle.text="🎭 المجموعات (${seriesCategories.size})"; rv.adapter=createCategoryAdapter(seriesCategories,"📺",t.accent){selectedCategoryId=it.categoryId;isShowingCategories=false;btnBack.visibility=View.VISIBLE;tvTitle.text="🎭 ${it.categoryName}";loadSeriesList(it.categoryId)} }

    private fun createCategoryAdapter(categories: List<XtreamCategory>, icon: String, color: Int, onClick: (XtreamCategory)->Unit): RecyclerView.Adapter<RecyclerView.ViewHolder> {
        val t=themes[currentTheme]!!
        return object: RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val card=CardView(parent.context).apply{layoutParams=ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT).apply{setMargins(15,8,15,8)};radius=12f;setCardBackgroundColor(t.card);cardElevation=4f}
                val l=LinearLayout(parent.context).apply{orientation=LinearLayout.HORIZONTAL;setPadding(30,25,30,25);gravity=Gravity.CENTER_VERTICAL}
                l.addView(TextView(parent.context).apply{text=icon;textSize=24f})
                l.addView(TextView(parent.context).apply{setPadding(20,0,0,0);textSize=16f;setTextColor(t.textWhite);setTypeface(null,Typeface.BOLD);layoutParams=LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f)})
                l.addView(TextView(parent.context).apply{text="→";textSize=20f;setTextColor(color)})
                card.addView(l); return object:RecyclerView.ViewHolder(card){}
            }
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
                val l=(holder.itemView as CardView).getChildAt(0) as LinearLayout
                (l.getChildAt(1) as TextView).text=categories[pos].categoryName
                holder.itemView.setOnClickListener{onClick(categories[pos])}
            }
            override fun getItemCount()=categories.size
        }
    }

    private fun loadLiveStreams(catId: String?) { server?.let{s->showLoading();XtreamAPI.getLiveStreams(s,catId){hideLoading();liveChannels.clear();liveChannels.addAll(it);currentStreamIndex=-1;updateLiveList()}} }
    private fun loadMovies(catId: String?) { server?.let{s->showLoading();XtreamAPI.getVodStreams(s,catId){hideLoading();vodMovies.clear();vodMovies.addAll(it);currentStreamIndex=-1;updateMoviesList()}} }
    private fun loadSeriesList(catId: String?) { server?.let{s->showLoading();XtreamAPI.getSeries(s,catId){hideLoading();seriesList.clear();seriesList.addAll(it);updateSeriesList()}} }

    private fun showLoading() { progressBar.visibility=View.VISIBLE }
    private fun hideLoading() { progressBar.visibility=View.GONE }

    private fun updateLiveList() { val t=themes[currentTheme]!!; rv.adapter=createContentAdapter(liveChannels.map{it.name},"📺",t){name->val idx=liveChannels.indexOfFirst{it.name==name}; currentStreamIndex=idx; currentStreamType="live"; val ch=liveChannels[idx]; val u=XtreamAPI.getStreamUrl(server!!,ch.streamId,ch.containerExtension); addToHistory("live",ch.streamId,ch.name); playStream(u,ch.name)} }
    private fun updateMoviesList() { val t=themes[currentTheme]!!; rv.adapter=createContentAdapter(vodMovies.map{it.name},"🎬",t){name->val idx=vodMovies.indexOfFirst{it.name==name}; currentStreamIndex=idx; currentStreamType="movie"; val m=vodMovies[idx]; val u=XtreamAPI.getMovieUrl(server!!,m.streamId,m.containerExtension); addToHistory("movie",m.streamId,m.name); playStream(u,m.name)} }
    private fun updateSeriesList() { val t=themes[currentTheme]!!; rv.adapter=createContentAdapter(seriesList.map{it.name},"📺",t){name->val s=seriesList.find{it.name==name}!!; XtreamAPI.getSeriesInfo(server!!,s.seriesId){showEpisodesDialog(s.name,it)}} }

    private fun createContentAdapter(items: List<String>, icon: String, theme: ThemeColors, onClick: (String)->Unit): RecyclerView.Adapter<RecyclerView.ViewHolder> {
        return object: RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val card=CardView(parent.context).apply{layoutParams=ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT).apply{setMargins(15,5,15,5)};radius=10f;setCardBackgroundColor(theme.card);cardElevation=2f}
                val l=LinearLayout(parent.context).apply{orientation=LinearLayout.HORIZONTAL;setPadding(25,20,25,20);gravity=Gravity.CENTER_VERTICAL}
                l.addView(TextView(parent.context).apply{textSize=15f;setTextColor(theme.textWhite);setTypeface(null,Typeface.BOLD);layoutParams=LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f)})
                l.addView(Button(parent.context).apply{text="⭐";setBackgroundColor(Color.TRANSPARENT)})
                card.addView(l); return object:RecyclerView.ViewHolder(card){}
            }
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
                val l=(holder.itemView as CardView).getChildAt(0) as LinearLayout
                (l.getChildAt(0) as TextView).text="$icon ${items[pos]}"
                holder.itemView.setOnClickListener{onClick(items[pos])}
                l.getChildAt(1).setOnClickListener{
                    val type=if(currentCategory=="live"||currentStreamType=="live")"live" else "movie"
                    val id=if(type=="live") liveChannels[pos].streamId else vodMovies[pos].streamId
                    addToFavorites(type,id,items[pos])
                }
            }
            override fun getItemCount()=items.size
        }
    }

    private fun showEpisodesDialog(name: String, episodes: List<XtreamEpisode>) {
        AlertDialog.Builder(this).setTitle(name)
            .setItems(episodes.map{"🎭 حلقة ${it.episodeNum}: ${it.title}"}.toTypedArray()){_,i->
                val e=episodes[i]; playStream(XtreamAPI.getSeriesEpisodeUrl(server!!,e.id,e.containerExtension),"$name - حلقة ${e.episodeNum}")
            }.setNegativeButton("إغلاق",null).show()
    }

    private fun playStream(url: String, name: String) {
        try {
            currentStreamUrl=url; currentStreamName=name
            player.setMediaItem(MediaItem.fromUri(url)); player.prepare(); player.play()
            tvChannelInfo.text="🎬 $name"
            Toast.makeText(this,"▶️ $name",Toast.LENGTH_SHORT).show()
        } catch(e: Exception) { Toast.makeText(this,"❌ ${e.message}",Toast.LENGTH_LONG).show() }
    }

    override fun onDestroy() { super.onDestroy(); player.release() }
}