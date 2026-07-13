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
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
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
import java.io.File

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
    private lateinit var btnPlayPause: Button
    private lateinit var btnFullscreen: Button
    private lateinit var btnDownload: Button
    private lateinit var btnQuality: Button
    private lateinit var btnAspectRatio: Button
    private lateinit var seekBar: SeekBar
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvTotalTime: TextView

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

    // تعريف الثيمات
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
            text = "⬅️"
            textSize = 18f
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(theme.textWhite)
            visibility = View.GONE
            setOnClickListener { goBackToCategories() }
        }
        headerLayout.addView(btnBack)

        tvTitle = TextView(this).apply {
            text = "MN-DAZOU IPTV"
            textSize = 22f
            setTextColor(theme.accent)
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            gravity = Gravity.CENTER
        }
        headerLayout.addView(tvTitle)

        val btnTheme = Button(this).apply {
            text = "🎨"
            textSize = 18f
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(theme.textGray)
            setOnClickListener { showThemeDialog() }
        }
        headerLayout.addView(btnTheme)

        val btnSettings = Button(this).apply {
            text = "⚙️"
            textSize = 18f
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(theme.textGray)
            setOnClickListener { showSettingsDialog() }
        }
        headerLayout.addView(btnSettings)
        root.addView(headerLayout)

        // شريط البحث
        searchLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(15, 10, 15, 10)
            setBackgroundColor(theme.bottomBar)
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
        }
        etSearch = EditText(this).apply {
            hint = "🔍 بحث عن قناة أو فيلم..."
            setHintTextColor(theme.textGray)
            setTextColor(theme.textWhite)
            setBackgroundColor(theme.card)
            setPadding(25, 15, 25, 15)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        searchLayout.addView(etSearch)
        val btnSearchGo = Button(this).apply {
            text = "بحث"
            setBackgroundColor(theme.accent)
            setTextColor(Color.BLACK)
            setOnClickListener { performSearch() }
        }
        searchLayout.addView(btnSearchGo)
        root.addView(searchLayout)

        // مشغل الفيديو
        val playerCard = CardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 420)
            radius = 0f
            setCardBackgroundColor(Color.BLACK)
            cardElevation = 8f
        }
        val playerContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        playerView = PlayerView(this).apply {
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.BLACK)
        }
        playerContainer.addView(playerView)
        playerCard.addView(playerContainer)
        root.addView(playerCard)

        // عناصر تحكم المشغل
        playerControlsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(15, 10, 15, 10)
            setBackgroundColor(Color.parseColor("#CC000000"))
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
        }
        btnPlayPause = Button(this).apply { text = "▶️"; setBackgroundColor(Color.TRANSPARENT); setTextColor(Color.WHITE); setOnClickListener { togglePlayPause() } }
        btnQuality = Button(this).apply { text = "HD"; setBackgroundColor(Color.TRANSPARENT); setTextColor(Color.WHITE); setOnClickListener { showQualityDialog() } }
        btnAspectRatio = Button(this).apply { text = "🔲"; setBackgroundColor(Color.TRANSPARENT); setTextColor(Color.WHITE); setOnClickListener { changeAspectRatio() } }
        btnDownload = Button(this).apply { text = "⬇️"; setBackgroundColor(Color.TRANSPARENT); setTextColor(Color.WHITE); setOnClickListener { downloadCurrentStream() } }
        btnFullscreen = Button(this).apply { text = "⛶"; setBackgroundColor(Color.TRANSPARENT); setTextColor(Color.WHITE); setOnClickListener { toggleFullscreen() } }
        playerControlsLayout.addView(btnPlayPause)
        playerControlsLayout.addView(btnQuality)
        playerControlsLayout.addView(btnAspectRatio)
        playerControlsLayout.addView(btnDownload)
        playerControlsLayout.addView(btnFullscreen)
        root.addView(playerControlsLayout)

        // شريط التقدم
        val seekLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(10, 5, 10, 5)
            setBackgroundColor(Color.parseColor("#CC000000"))
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
        }
        tvCurrentTime = TextView(this).apply { text = "00:00"; setTextColor(Color.WHITE); textSize = 12f }
        seekBar = SeekBar(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) player.seekTo(progress.toLong())
                }
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
            orientation = LinearLayout.HORIZONTAL
            setPadding(5, 12, 5, 20)
            setBackgroundColor(theme.bottomBar)
            gravity = Gravity.CENTER
        }

        btnHome = createBottomButton("🏠", "الرئيسية", theme) { switchTab("home") }
        btnLive = createBottomButton("📺", "مباشر", theme) { switchTab("live") }
        btnMovies = createBottomButton("🎬", "أفلام", theme) { switchTab("movies") }
        btnSeries = createBottomButton("🎭", "مسلسلات", theme) { switchTab("series") }
        btnFavorites = createBottomButton("⭐", "مفضلة", theme) { switchTab("favorites") }

        bottomBar.addView(btnHome)
        bottomBar.addView(btnLive)
        bottomBar.addView(btnMovies)
        bottomBar.addView(btnSeries)
        bottomBar.addView(btnFavorites)
        root.addView(bottomBar)

        setContentView(root)
        initializePlayer()
        loadFavorites()
        loadHistory()
        registerDownloadReceiver()

        // استعادة بيانات السيرفر
        val savedUrl = prefs.getString("server_url", "")
        if (!savedUrl.isNullOrEmpty()) {
            server = XtreamServer(
                url = savedUrl!!,
                username = prefs.getString("server_username", "")!!,
                password = prefs.getString("server_password", "")!!
            )
            switchTab("home")
        } else {
            showLoginDialog()
        }
    }

    private fun createBottomButton(icon: String, label: String, theme: ThemeColors, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = "$icon\n$label"
            textSize = 10f
            setTextColor(theme.textGray)
            setBackgroundColor(Color.TRANSPARENT)
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            gravity = Gravity.CENTER
            setOnClickListener { onClick() }
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
                playerControlsLayout.visibility = View.VISIBLE
                (playerControlsLayout.parent as? View)?.let { parent ->
                    (parent as? ViewGroup)?.let { vg ->
                        for (i in 0 until vg.childCount) {
                            val child = vg.getChildAt(i)
                            if (child is LinearLayout && child != playerControlsLayout && child.childCount > 0 && child.getChildAt(0) is SeekBar) {
                                child.visibility = View.VISIBLE
                            }
                        }
                    }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    seekBar.max = player.duration.toInt()
                    tvTotalTime.text = formatTime(player.duration)
                }
            }
        })

        // تحديث شريط التقدم
        Thread {
            while (true) {
                if (player.isPlaying) {
                    runOnUiThread {
                        seekBar.progress = player.currentPosition.toInt()
                        tvCurrentTime.text = formatTime(player.currentPosition)
                    }
                }
                Thread.sleep(1000)
            }
        }.start()
    }

    private fun formatTime(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        val hours = millis / (1000 * 60 * 60)
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    private fun togglePlayPause() {
        if (isPlayerPlaying) player.pause() else player.play()
    }

    private fun changeAspectRatio() {
        currentAspectRatio = (currentAspectRatio + 1) % 3
        playerView.resizeMode = when (currentAspectRatio) {
            0 -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
            1 -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
            2 -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            else -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
        val modes = arrayOf("ملائم", "ملء", "تكبير")
        Toast.makeText(this, "📐 ${modes[currentAspectRatio]}", Toast.LENGTH_SHORT).show()
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
        val qualities = arrayOf("Auto", "4K", "1080p", "720p", "480p")
        AlertDialog.Builder(this)
            .setTitle("🎯 جودة الفيديو")
            .setItems(qualities) { _, _ ->
                Toast.makeText(this, "⚙️ الجودة التلقائية مفعلة", Toast.LENGTH_SHORT).show()
            }
            .show()
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
            } catch (e: Exception) {
                Toast.makeText(this, "❌ فشل التحميل", Toast.LENGTH_SHORT).show()
            }
        } ?: Toast.makeText(this, "لا يوجد فيديو للتحميل", Toast.LENGTH_SHORT).show()
    }

    private fun registerDownloadReceiver() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: -1
                if (id == downloadId) {
                    Toast.makeText(this@MainActivity, "✅ تم التحميل بنجاح", Toast.LENGTH_SHORT).show()
                }
            }
        }
        registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    private fun showSettingsDialog() {
        val items = arrayOf("🎨 تغيير الثيم", "🔗 إعدادات Xtream", "📋 إضافة قائمة M3U", "📺 تحديث EPG")
        AlertDialog.Builder(this)
            .setTitle("⚙️ الإعدادات")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> showThemeDialog()
                    1 -> showLoginDialog()
                    2 -> showM3uDialog()
                    3 -> loadEpg()
                }
            }
            .show()
    }

    private fun showThemeDialog() {
        val themeNames = themes.values.map { it.name }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("🎨 اختر الثيم")
            .setItems(themeNames) { _, which ->
                val themeKey = themes.keys.toList()[which]
                prefs.edit().putString("theme", themeKey).apply()
                Toast.makeText(this, "🔄 أعد تشغيل التطبيق لتطبيق الثيم", Toast.LENGTH_LONG).show()
            }
            .show()
    }

    private fun showM3uDialog() {
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }
        val etM3u = EditText(this).apply {
            hint = "رابط ملف M3U أو الصق المحتوى"
            minLines = 3
        }
        dialogView.addView(etM3u)
        AlertDialog.Builder(this)
            .setTitle("📋 إضافة قائمة M3U")
            .setView(dialogView)
            .setPositiveButton("تحميل") { _, _ ->
                val input = etM3u.text.toString()
                if (input.startsWith("http")) {
                    loadM3uFromUrl(input)
                } else {
                    parseM3uContent(input)
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun loadM3uFromUrl(url: String) {
        showLoading()
        Thread {
            try {
                val content = java.net.URL(url).readText()
                runOnUiThread {
                    hideLoading()
                    parseM3uContent(content)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    hideLoading()
                    Toast.makeText(this, "❌ فشل تحميل M3U", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun parseM3uContent(content: String) {
        val lines = content.split("\n")
        val channels = mutableListOf<XtreamChannel>()
        var currentName = ""
        for (line in lines) {
            when {
                line.startsWith("#EXTINF") -> {
                    val nameMatch = Regex(",(.+)").find(line)
                    currentName = nameMatch?.groupValues?.get(1)?.trim() ?: "قناة غير معروفة"
                }
                line.startsWith("http") -> {
                    channels.add(XtreamChannel(
                        streamId = channels.size,
                        name = currentName,
                        streamType = "live",
                        streamIcon = "",
                        epgChannelId = "",
                        added = "",
                        categoryId = "m3u",
                        containerExtension = "ts"
                    ))
                }
            }
        }
        liveChannels.addAll(channels)
        Toast.makeText(this, "✅ تم إضافة ${channels.size} قناة", Toast.LENGTH_SHORT).show()
        updateLiveList()
    }

    private fun loadEpg() {
        server?.let { s ->
            showLoading()
            Thread {
                try {
                    val url = "${s.url}/player_api.php?username=${s.username}&password=${s.password}&action=get_short_epg"
                    val json = java.net.URL(url).readText()
                    val jsonArray = JSONObject(json).getJSONArray("epg_listings")
                    epgData.clear()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        epgData.add(EpgProgram(
                            channelId = obj.optString("epg_id", ""),
                            title = obj.optString("title", "بدون عنوان"),
                            startTime = obj.optString("start", ""),
                            endTime = obj.optString("end", ""),
                            description = obj.optString("description", "")
                        ))
                    }
                    runOnUiThread {
                        hideLoading()
                        Toast.makeText(this, "📺 تم تحميل EPG (${epgData.size} برنامج)", Toast.LENGTH_SHORT).show()
                        showEpg()
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        hideLoading()
                        Toast.makeText(this, "❌ فشل تحميل EPG", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        } ?: Toast.makeText(this, "الرجاء تسجيل الدخول أولاً", Toast.LENGTH_SHORT).show()
    }

    private fun showEpg() {
        val epgGrouped = epgData.groupBy { it.channelId }
        val channelNames = epgGrouped.keys.toList()
        if (channelNames.isEmpty()) {
            Toast.makeText(this, "لا توجد بيانات EPG", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("📺 دليل البرامج (EPG)")
            .setItems(channelNames.toTypedArray()) { _, which ->
                val channelId = channelNames[which]
                val programs = epgGrouped[channelId] ?: emptyList()
                val programTexts = programs.map { "🕐 ${it.startTime}-${it.endTime}: ${it.title}" }.toTypedArray()
                AlertDialog.Builder(this)
                    .setTitle(channelId)
                    .setItems(programTexts, null)
                    .setPositiveButton("إغلاق", null)
                    .show()
            }
            .setPositiveButton("إغلاق", null)
            .show()
    }

    private fun switchTab(tab: String) {
        currentCategory = tab
        selectedCategoryId = null
        isShowingCategories = true
        btnBack.visibility = View.GONE
        searchLayout.visibility = View.GONE
        val theme = themes[currentTheme]!!

        btnHome.setTextColor(theme.textGray)
        btnLive.setTextColor(theme.textGray)
        btnMovies.setTextColor(theme.textGray)
        btnSeries.setTextColor(theme.textGray)
        btnFavorites.setTextColor(theme.textGray)

        when (tab) {
            "home" -> {
                btnHome.setTextColor(theme.accent)
                tvTitle.text = "🏠 MN-DAZOU IPTV"
                showHomeScreen()
            }
            "live" -> {
                btnLive.setTextColor(theme.accent)
                tvTitle.text = "📺 البث المباشر"
                searchLayout.visibility = View.VISIBLE
                loadLiveCategories()
            }
            "movies" -> {
                btnMovies.setTextColor(theme.accent)
                tvTitle.text = "🎬 الأفلام"
                searchLayout.visibility = View.VISIBLE
                loadVodCategories()
            }
            "series" -> {
                btnSeries.setTextColor(theme.accent)
                tvTitle.text = "🎭 المسلسلات"
                searchLayout.visibility = View.VISIBLE
                loadSeriesCategories()
            }
            "favorites" -> {
                btnFavorites.setTextColor(theme.accent)
                tvTitle.text = "⭐ المفضلة"
                showFavorites()
            }
        }
    }

    private fun showHomeScreen() {
        val theme = themes[currentTheme]!!
        val allItems = mutableListOf<Any>()
        if (watchHistory.isNotEmpty()) {
            allItems.add("section_history")
            allItems.addAll(watchHistory.takeLast(5).reversed())
        }
        if (favorites.isNotEmpty()) {
            allItems.add("section_favorites")
            allItems.addAll(favorites.take(5))
        }
        allItems.add("section_quick")
        allItems.add("quick_live")
        allItems.add("quick_movies")
        allItems.add("quick_series")
        allItems.add("quick_epg")
        if (allItems.isEmpty()) allItems.add("empty")

        rv.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun getItemViewType(position: Int): Int = when (allItems[position]) { is String -> 0; is HistoryItem -> 1; is FavoriteItem -> 2; else -> 0 }
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                return when (viewType) {
                    0 -> { val tv = TextView(parent.context).apply { setPadding(20, 15, 20, 10); textSize = 16f; setTextColor(theme.accent); setTypeface(null, Typeface.BOLD) }; object : RecyclerView.ViewHolder(tv) {} }
                    else -> { val card = CardView(parent.context).apply { layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(15, 6, 15, 6) }; radius = 12f; setCardBackgroundColor(theme.card); cardElevation = 3f }; val tv = TextView(parent.context).apply { setPadding(25, 20, 25, 20); textSize = 15f; setTextColor(theme.textWhite); setTypeface(null, Typeface.BOLD) }; card.addView(tv); object : RecyclerView.ViewHolder(card) {} }
                }
            }
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
                val item = allItems[pos]
                when (item) {
                    is String -> { val tv = holder.itemView as TextView
                        when (item) {
                            "section_history" -> tv.text = "🕐 آخر المشاهدات"
                            "section_favorites" -> tv.text = "⭐ المفضلة"
                            "section_quick" -> tv.text = "🚀 وصول سريع"
                            "quick_live" -> { tv.text = "📺 البث المباشر"; tv.setOnClickListener { switchTab("live") } }
                            "quick_movies" -> { tv.text = "🎬 الأفلام"; tv.setOnClickListener { switchTab("movies") } }
                            "quick_series" -> { tv.text = "🎭 المسلسلات"; tv.setOnClickListener { switchTab("series") } }
                            "quick_epg" -> { tv.text = "📋 دليل البرامج (EPG)"; tv.setOnClickListener { loadEpg() } }
                            "empty" -> tv.text = "👋 أهلاً بك! سجل دخولك للبدء"
                        }
                    }
                    is HistoryItem -> { val card = holder.itemView as CardView; val tv = card.getChildAt(0) as TextView; tv.text = "🕐  ${item.name}"; card.setOnClickListener { playHistoryItem(item) } }
                    is FavoriteItem -> { val card = holder.itemView as CardView; val tv = card.getChildAt(0) as TextView; tv.text = "⭐  ${item.name}"; card.setOnClickListener { playFavoriteItem(item) } }
                }
            }
            override fun getItemCount() = allItems.size
        }
    }

    private fun showFavorites() {
        val theme = themes[currentTheme]!!
        rv.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val card = CardView(parent.context).apply { layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(15, 6, 15, 6) }; radius = 12f; setCardBackgroundColor(theme.card); cardElevation = 3f }
                val layout = LinearLayout(parent.context).apply { orientation = LinearLayout.HORIZONTAL; setPadding(25, 20, 25, 20); gravity = Gravity.CENTER_VERTICAL }
                val tv = TextView(parent.context).apply { textSize = 15f; setTextColor(theme.textWhite); setTypeface(null, Typeface.BOLD); layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) }
                layout.addView(tv)
                val btnRemove = Button(parent.context).apply { text = "❌"; setBackgroundColor(Color.TRANSPARENT) }
                layout.addView(btnRemove)
                card.addView(layout)
                return object : RecyclerView.ViewHolder(card) {}
            }
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
                val card = holder.itemView as CardView; val layout = card.getChildAt(0) as LinearLayout; val tv = layout.getChildAt(0) as TextView; val btnRemove = layout.getChildAt(1) as Button
                val fav = favorites[pos]; tv.text = "⭐  ${fav.name}"; card.setOnClickListener { playFavoriteItem(fav) }; btnRemove.setOnClickListener { removeFavorite(fav); showFavorites() }
            }
            override fun getItemCount() = favorites.size
        }
    }

    private fun goBackToCategories() { isShowingCategories = true; selectedCategoryId = null; btnBack.visibility = View.GONE; when (currentCategory) { "live" -> showLiveCategories(); "movies" -> showVodCategories(); "series" -> showSeriesCategories() } }

    private fun showLoginDialog() {
        val theme = themes[currentTheme]!!
        val dialogView = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(50, 50, 50, 50); setBackgroundColor(theme.card) }
        val title = TextView(this).apply { text = "⚙️ إعدادات Xtream Codes"; textSize = 20f; setTextColor(theme.accent); setTypeface(null, Typeface.BOLD); gravity = Gravity.CENTER; setPadding(0, 0, 0, 30) }
        dialogView.addView(title)
        val etServer = EditText(this).apply { hint = "رابط السيرفر"; setHintTextColor(theme.textGray); setTextColor(theme.textWhite); setBackgroundColor(theme.bg); setPadding(30, 20, 30, 20); setText("http://") }
        val etUsername = EditText(this).apply { hint = "اسم المستخدم"; setHintTextColor(theme.textGray); setTextColor(theme.textWhite); setBackgroundColor(theme.bg); setPadding(30, 20, 30, 20) }
        val etPassword = EditText(this).apply { hint = "كلمة المرور"; setHintTextColor(theme.textGray); setTextColor(theme.textWhite); setBackgroundColor(theme.bg); setPadding(30, 20, 30, 20) }
        dialogView.addView(etServer); dialogView.addView(createSpacer(10)); dialogView.addView(etUsername); dialogView.addView(createSpacer(10)); dialogView.addView(etPassword)
        AlertDialog.Builder(this).setView(dialogView).setPositiveButton("اتصال") { _, _ ->
            server = XtreamServer(url = etServer.text.toString().trimEnd('/'), username = etUsername.text.toString(), password = etPassword.text.toString())
            saveServerData(); Toast.makeText(this, "✅ تم الاتصال", Toast.LENGTH_SHORT).show(); switchTab("home")
        }.setNegativeButton("إلغاء", null).setCancelable(false).show()
    }

    private fun saveServerData() { server?.let { prefs.edit().putString("server_url", it.url).putString("server_username", it.username).putString("server_password", it.password).apply() } }
    private fun createSpacer(height: Int): View = View(this).apply { layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height) }
    private fun performSearch() { val query = etSearch.text.toString().lowercase(); if (query.isEmpty()) return; val theme = themes[currentTheme]!!; when (currentCategory) { "live" -> { val filtered = liveChannels.filter { it.name.lowercase().contains(query) }; if (filtered.isNotEmpty()) { liveChannels.clear(); liveChannels.addAll(filtered); updateLiveList(); tvTitle.text = "🔍 بحث: $query" } else Toast.makeText(this, "لا توجد نتائج", Toast.LENGTH_SHORT).show() } "movies" -> { val filtered = vodMovies.filter { it.name.lowercase().contains(query) }; if (filtered.isNotEmpty()) { vodMovies.clear(); vodMovies.addAll(filtered); updateMoviesList(); tvTitle.text = "🔍 بحث: $query" } else Toast.makeText(this, "لا توجد نتائج", Toast.LENGTH_SHORT).show() } } }

    private fun addToFavorites(type: String, id: Int, name: String) { val exists = favorites.any { it.type == type && it.id == id }; if (!exists) { favorites.add(FavoriteItem(type, id, name)); saveFavorites(); Toast.makeText(this, "⭐ تمت الإضافة للمفضلة", Toast.LENGTH_SHORT).show() } else Toast.makeText(this, "موجود مسبقاً", Toast.LENGTH_SHORT).show() }
    private fun removeFavorite(item: FavoriteItem) { favorites.removeAll { it.type == item.type && it.id == item.id }; saveFavorites() }
    private fun saveFavorites() { val json = JSONArray(); favorites.forEach { val obj = JSONObject(); obj.put("type", it.type); obj.put("id", it.id); obj.put("name", it.name); json.put(obj) }; prefs.edit().putString("favorites", json.toString()).apply() }
    private fun loadFavorites() { val jsonStr = prefs.getString("favorites", "[]") ?: "[]"; val json = JSONArray(jsonStr); for (i in 0 until json.length()) { val obj = json.getJSONObject(i); favorites.add(FavoriteItem(obj.getString("type"), obj.getInt("id"), obj.getString("name"))) } }
    private fun addToHistory(type: String, id: Int, name: String) { watchHistory.removeAll { it.type == type && it.id == id }; watchHistory.add(HistoryItem(type, id, name, System.currentTimeMillis())); if (watchHistory.size > 20) watchHistory.removeAt(0); saveHistory() }
    private fun saveHistory() { val json = JSONArray(); watchHistory.forEach { val obj = JSONObject(); obj.put("type", it.type); obj.put("id", it.id); obj.put("name", it.name); obj.put("timestamp", it.timestamp); json.put(obj) }; prefs.edit().putString("history", json.toString()).apply() }
    private fun loadHistory() { val jsonStr = prefs.getString("history", "[]") ?: "[]"; val json = JSONArray(jsonStr); for (i in 0 until json.length()) { val obj = json.getJSONObject(i); watchHistory.add(HistoryItem(obj.getString("type"), obj.getInt("id"), obj.getString("name"), obj.getLong("timestamp"))) } }

    private fun playFavoriteItem(fav: FavoriteItem) { when (fav.type) { "live" -> { val url = XtreamAPI.getStreamUrl(server!!, fav.id); playStream(url, fav.name); addToHistory("live", fav.id, fav.name) } "movie" -> { val url = XtreamAPI.getMovieUrl(server!!, fav.id); playStream(url, fav.name); addToHistory("movie", fav.id, fav.name) } } }
    private fun playHistoryItem(item: HistoryItem) { when (item.type) { "live" -> { val url = XtreamAPI.getStreamUrl(server!!, item.id); playStream(url, item.name) } "movie" -> { val url = XtreamAPI.getMovieUrl(server!!, item.id); playStream(url, item.name) } } }

    private fun loadLiveCategories() { server?.let { s -> showLoading(); XtreamAPI.getLiveCategories(s) { categories -> liveCategories.clear(); liveCategories.addAll(categories); hideLoading(); if (categories.isEmpty()) loadLiveStreams(null) else showLiveCategories() } } ?: showLoginDialog() }
    private fun loadVodCategories() { server?.let { s -> showLoading(); XtreamAPI.getVodCategories(s) { categories -> vodCategories.clear(); vodCategories.addAll(categories); hideLoading(); if (categories.isEmpty()) loadMovies(null) else showVodCategories() } } ?: showLoginDialog() }
    private fun loadSeriesCategories() { server?.let { s -> showLoading(); XtreamAPI.getLiveCategories(s) { categories -> seriesCategories.clear(); seriesCategories.addAll(categories); hideLoading(); if (categories.isEmpty()) loadSeriesList(null) else showSeriesCategories() } } ?: showLoginDialog() }

    private fun showLiveCategories() { val theme = themes[currentTheme]!!; isShowingCategories = true; tvTitle.text = "📺 مجموعات البث (${liveCategories.size})"; rv.adapter = createCategoryAdapter(liveCategories, "📁", theme.accent) { cat -> selectedCategoryId = cat.categoryId; isShowingCategories = false; btnBack.visibility = View.VISIBLE; tvTitle.text = "📺 ${cat.categoryName}"; loadLiveStreams(cat.categoryId) } }
    private fun showVodCategories() { val theme = themes[currentTheme]!!; isShowingCategories = true; tvTitle.text = "🎬 مجموعات الأفلام (${vodCategories.size})"; rv.adapter = createCategoryAdapter(vodCategories, "🎬", theme.accent) { cat -> selectedCategoryId = cat.categoryId; isShowingCategories = false; btnBack.visibility = View.VISIBLE; tvTitle.text = "🎬 ${cat.categoryName}"; loadMovies(cat.categoryId) } }
    private fun showSeriesCategories() { val theme = themes[currentTheme]!!; isShowingCategories = true; tvTitle.text = "🎭 مجموعات المسلسلات (${seriesCategories.size})"; rv.adapter = createCategoryAdapter(seriesCategories, "📺", theme.accent) { cat -> selectedCategoryId = cat.categoryId; isShowingCategories = false; btnBack.visibility = View.VISIBLE; tvTitle.text = "🎭 ${cat.categoryName}"; loadSeriesList(cat.categoryId) } }

    private fun createCategoryAdapter(categories: List<XtreamCategory>, icon: String, color: Int, onClick: (XtreamCategory) -> Unit): RecyclerView.Adapter<RecyclerView.ViewHolder> {
        val theme = themes[currentTheme]!!
        return object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder { val card = CardView(parent.context).apply { layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(15, 8, 15, 8) }; radius = 12f; setCardBackgroundColor(theme.card); cardElevation = 4f }; val layout = LinearLayout(parent.context).apply { orientation = LinearLayout.HORIZONTAL; setPadding(30, 25, 30, 25); gravity = Gravity.CENTER_VERTICAL }; val iconView = TextView(parent.context).apply { text = icon; textSize = 24f }; layout.addView(iconView); val tv = TextView(parent.context).apply { setPadding(20, 0, 0, 0); textSize = 16f; setTextColor(theme.textWhite); setTypeface(null, Typeface.BOLD); layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) }; layout.addView(tv); val arrow = TextView(parent.context).apply { text = "→"; textSize = 20f; setTextColor(color) }; layout.addView(arrow); card.addView(layout); return object : RecyclerView.ViewHolder(card) {} }
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) { val card = holder.itemView as CardView; val layout = card.getChildAt(0) as LinearLayout; val tv = layout.getChildAt(1) as TextView; tv.text = categories[pos].categoryName; card.setOnClickListener { onClick(categories[pos]) } }
            override fun getItemCount() = categories.size
        }
    }

    private fun loadLiveStreams(categoryId: String?) { server?.let { s -> showLoading(); XtreamAPI.getLiveStreams(s, categoryId) { channels -> hideLoading(); liveChannels.clear(); liveChannels.addAll(channels); updateLiveList() } } }
    private fun loadMovies(categoryId: String?) { server?.let { s -> showLoading(); XtreamAPI.getVodStreams(s, categoryId) { movies -> hideLoading(); vodMovies.clear(); vodMovies.addAll(movies); updateMoviesList() } } }
    private fun loadSeriesList(categoryId: String?) { server?.let { s -> showLoading(); XtreamAPI.getSeries(s, categoryId) { series -> hideLoading(); seriesList.clear(); seriesList.addAll(series); updateSeriesList() } } }

    private fun showLoading() { progressBar.visibility = View.VISIBLE }
    private fun hideLoading() { progressBar.visibility = View.GONE }

    private fun updateLiveList() { val theme = themes[currentTheme]!!; rv.adapter = createContentAdapter(liveChannels.map { Pair(it.name, "live") }, "📺", theme) { name, _ -> val channel = liveChannels.find { it.name == name }!!; val url = XtreamAPI.getStreamUrl(server!!, channel.streamId, channel.containerExtension); addToHistory("live", channel.streamId, channel.name); playStream(url, channel.name) } }
    private fun updateMoviesList() { val theme = themes[currentTheme]!!; rv.adapter = createContentAdapter(vodMovies.map { Pair(it.name, "movie") }, "🎬", theme) { name, _ -> val movie = vodMovies.find { it.name == name }!!; val url = XtreamAPI.getMovieUrl(server!!, movie.streamId, movie.containerExtension); addToHistory("movie", movie.streamId, movie.name); playStream(url, movie.name) } }
    private fun updateSeriesList() { val theme = themes[currentTheme]!!; rv.adapter = createContentAdapter(seriesList.map { Pair(it.name, "series") }, "📺", theme) { name, _ -> val series = seriesList.find { it.name == name }!!; server?.let { s -> XtreamAPI.getSeriesInfo(s, series.seriesId) { episodes -> showEpisodesDialog(series.name, episodes) } } } }

    private fun createContentAdapter(items: List<Pair<String, String>>, icon: String, theme: ThemeColors, onClick: (String, String) -> Unit): RecyclerView.Adapter<RecyclerView.ViewHolder> {
        return object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder { val card = CardView(parent.context).apply { layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(15, 5, 15, 5) }; radius = 10f; setCardBackgroundColor(theme.card); cardElevation = 2f }; val layout = LinearLayout(parent.context).apply { orientation = LinearLayout.HORIZONTAL; setPadding(25, 20, 25, 20); gravity = Gravity.CENTER_VERTICAL }; val tv = TextView(parent.context).apply { textSize = 15f; setTextColor(theme.textWhite); setTypeface(null, Typeface.BOLD); layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) }; layout.addView(tv); val btnFav = Button(parent.context).apply { text = "⭐"; setBackgroundColor(Color.TRANSPARENT) }; layout.addView(btnFav); card.addView(layout); return object : RecyclerView.ViewHolder(card) {} }
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) { val card = holder.itemView as CardView; val layout = card.getChildAt(0) as LinearLayout; val tv = layout.getChildAt(0) as TextView; val btnFav = layout.getChildAt(1) as Button; val (name, type) = items[pos]; tv.text = "$icon  $name"; card.setOnClickListener { onClick(name, type) }; btnFav.setOnClickListener { val id = when (type) { "live" -> liveChannels.find { it.name == name }?.streamId ?: 0; "movie" -> vodMovies.find { it.name == name }?.streamId ?: 0; else -> 0 }; addToFavorites(type, id, name) } }
            override fun getItemCount() = items.size
        }
    }

    private fun showEpisodesDialog(seriesName: String, episodes: List<XtreamEpisode>) { val episodesArray = episodes.map { "🎭 حلقة ${it.episodeNum}: ${it.title}" }.toTypedArray(); AlertDialog.Builder(this).setTitle(seriesName).setItems(episodesArray) { _, which -> val episode = episodes[which]; val url = XtreamAPI.getSeriesEpisodeUrl(server!!, episode.id, episode.containerExtension); playStream(url, "${seriesName} - حلقة ${episode.episodeNum}") }.setNegativeButton("إغلاق", null).show() }

    private fun playStream(url: String, name: String) {
        try {
            currentStreamUrl = url
            currentStreamName = name
            val mediaItem = MediaItem.fromUri(url)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
            Toast.makeText(this, "▶️ $name", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "❌ خطأ: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() { super.onDestroy(); player.release() }
}