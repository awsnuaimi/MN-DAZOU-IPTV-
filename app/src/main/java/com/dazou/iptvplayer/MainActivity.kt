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
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

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

    // عناصر التحكم الجديدة
    private lateinit var controlsLayout: LinearLayout
    private lateinit var btnPrevChannel: Button
    private lateinit var btnPlayPause: Button
    private lateinit var btnNextChannel: Button
    private lateinit var btnAspectRatio: Button
    private lateinit var btnToggleFullscreen: Button
    private lateinit var btnRecord: Button
    private lateinit var btnShare: Button
    private lateinit var btnDownload: Button
    private lateinit var tvChannelInfo: TextView

    private var server: XtreamServer? = null
    private var liveChannels = mutableListOf<XtreamChannel>()
    private var vodMovies = mutableListOf<XtreamMovie>()
    private var seriesList = mutableListOf<XtreamSeries>()
    private var liveCategories = mutableListOf<XtreamCategory>()
    private var vodCategories = mutableListOf<XtreamCategory>()
    private var seriesCategories = mutableListOf<XtreamCategory>()
    private var favorites = mutableListOf<FavoriteItem>()
    private var watchHistory = mutableListOf<HistoryItem>()
    private var currentCategory = "home"
    private var selectedCategoryId: String? = null
    private var isShowingCategories = true
    private var currentTheme = "dark"

    // متغيرات المشغل
    private var currentStreamIndex = -1
    private var currentStreamType = "live"
    private var currentStreamName: String? = null
    private var currentStreamUrl: String? = null
    private var isRecording = false
    private var recordingFile: File? = null
    private var recordingThread: Thread? = null
    private var downloadId: Long = 0
    private var aspectRatioMode = 0 // 0=FIT, 1=FILL, 2=ZOOM

    private lateinit var prefs: SharedPreferences
    private lateinit var downloadManager: DownloadManager
    private lateinit var themes: Map<String, ThemeColors>

    data class FavoriteItem(val type: String, val id: Int, val name: String)
    data class HistoryItem(val type: String, val id: Int, val name: String, val timestamp: Long)
    data class ThemeColors(val name: String, val bg: Int, val card: Int, val accent: Int, val bottomBar: Int, val textWhite: Int, val textGray: Int, val activeTab: Int)

    private fun dimen(id: Int): Int = resources.getDimensionPixelSize(id)
    private fun dimenSp(id: Int): Float = resources.getDimension(id) / resources.displayMetrics.scaledDensity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        prefs = getSharedPreferences("mndazou_prefs", Context.MODE_PRIVATE)
        downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        currentTheme = prefs.getString("theme", "dark") ?: "dark"
        themes = mapOf(
            "dark" to ThemeColors("داكن", Color.parseColor("#0F0F1A"), Color.parseColor("#1A1A35"), Color.parseColor("#FF6B6B"), Color.parseColor("#12122A"), Color.parseColor("#FFFFFF"), Color.parseColor("#AAAAAA"), Color.parseColor("#2D2D5E")),
            "blue" to ThemeColors("أزرق", Color.parseColor("#0A1628"), Color.parseColor("#1B2D4A"), Color.parseColor("#4FC3F7"), Color.parseColor("#0D1F3C"), Color.parseColor("#FFFFFF"), Color.parseColor("#90CAF9"), Color.parseColor("#1565C0")),
            "green" to ThemeColors("أخضر", Color.parseColor("#0A1F0A"), Color.parseColor("#1A3A1A"), Color.parseColor("#66BB6A"), Color.parseColor("#0D2A0D"), Color.parseColor("#FFFFFF"), Color.parseColor("#A5D6A7"), Color.parseColor("#2E7D32")),
            "purple" to ThemeColors("بنفسجي", Color.parseColor("#1A0A2E"), Color.parseColor("#2D1B4E"), Color.parseColor("#CE93D8"), Color.parseColor("#1F0D3D"), Color.parseColor("#FFFFFF"), Color.parseColor("#E1BEE7"), Color.parseColor("#6A1B9A")),
            "red" to ThemeColors("أحمر", Color.parseColor("#1A0A0A"), Color.parseColor("#3A1A1A"), Color.parseColor("#EF5350"), Color.parseColor("#2A0D0D"), Color.parseColor("#FFFFFF"), Color.parseColor("#EF9A9A"), Color.parseColor("#C62828"))
        )
        val t = themes[currentTheme]!!

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(t.bg) }

        // Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dimen(R.dimen.header_padding_top), dimen(R.dimen.header_padding_top), dimen(R.dimen.header_padding_top), dimen(R.dimen.header_padding_bottom))
            setBackgroundColor(t.bottomBar); gravity = Gravity.CENTER_VERTICAL
        }
        btnBack = Button(this).apply { text = "⬅️"; textSize = dimenSp(R.dimen.header_icon_size); setBackgroundColor(Color.TRANSPARENT); setTextColor(t.textWhite); visibility = View.GONE; setOnClickListener { goBack() } }
        header.addView(btnBack)
        tvTitle = TextView(this).apply { text = "MN-DAZOU IPTV"; textSize = dimenSp(R.dimen.header_text_size); setTextColor(t.accent); setTypeface(null, Typeface.BOLD); gravity = Gravity.CENTER; layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) }
        header.addView(tvTitle)
        header.addView(Button(this).apply { text = "🎨"; textSize = dimenSp(R.dimen.header_icon_size); setBackgroundColor(Color.TRANSPARENT); setTextColor(t.textGray); setOnClickListener { showThemeDialog() } })
        header.addView(Button(this).apply { text = "⚙️"; textSize = dimenSp(R.dimen.header_icon_size); setBackgroundColor(Color.TRANSPARENT); setTextColor(t.textGray); setOnClickListener { showLoginDialog() } })
        root.addView(header)

        // Search
        searchLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dimen(R.dimen.search_padding_h), dimen(R.dimen.search_padding_v), dimen(R.dimen.search_padding_h), dimen(R.dimen.search_padding_v)); setBackgroundColor(t.bottomBar); gravity = Gravity.CENTER_VERTICAL; visibility = View.GONE }
        etSearch = EditText(this).apply { hint = "🔍 بحث..."; setHintTextColor(t.textGray); setTextColor(t.textWhite); setBackgroundColor(t.bg); setPadding(12, 4, 12, 4); layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f); textSize = dimenSp(R.dimen.search_text_size) }
        searchLayout.addView(etSearch)
        searchLayout.addView(Button(this).apply { text = "بحث"; textSize = dimenSp(R.dimen.search_text_size); setBackgroundColor(t.accent); setTextColor(Color.BLACK); setOnClickListener { performSearch() } })
        root.addView(searchLayout)

        // Player
        playerView = PlayerView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dimen(R.dimen.player_height))
            setBackgroundColor(Color.BLACK); useController = false
        }
        root.addView(playerView)

        // معلومات القناة الحالية
        tvChannelInfo = TextView(this).apply {
            text = ""; textSize = dimenSp(R.dimen.player_info_text_size); setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#CC000000")); setPadding(12, 4, 12, 4)
            visibility = View.GONE; gravity = Gravity.CENTER
        }
        root.addView(tvChannelInfo)

        // أزرار التحكم بالمشغل
        controlsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dimen(R.dimen.player_control_padding), dimen(R.dimen.player_control_padding), dimen(R.dimen.player_control_padding), dimen(R.dimen.player_control_padding))
            setBackgroundColor(Color.parseColor("#DD000000")); gravity = Gravity.CENTER
            visibility = View.GONE
        }
        val ctrlTextSize = dimenSp(R.dimen.player_control_text_size)
        btnPrevChannel = createCtrlButton("⏪", ctrlTextSize) { playPreviousChannel() }
        btnPlayPause = createCtrlButton("▶️", ctrlTextSize) { togglePlayPause() }
        btnNextChannel = createCtrlButton("⏩", ctrlTextSize) { playNextChannel() }
        btnAspectRatio = createCtrlButton("🔲", ctrlTextSize) { changeAspectRatio() }
        btnToggleFullscreen = createCtrlButton("⛶", ctrlTextSize) { toggleFullscreen() }
        btnRecord = createCtrlButton("🔴", ctrlTextSize) { toggleRecording() }
        btnShare = createCtrlButton("📤", ctrlTextSize) { shareCurrentStream() }
        btnDownload = createCtrlButton("⬇️", ctrlTextSize) { downloadCurrentStream() }

        controlsLayout.addView(btnPrevChannel)
        controlsLayout.addView(btnPlayPause)
        controlsLayout.addView(btnNextChannel)
        controlsLayout.addView(btnAspectRatio)
        controlsLayout.addView(btnToggleFullscreen)
        controlsLayout.addView(btnRecord)
        controlsLayout.addView(btnShare)
        controlsLayout.addView(btnDownload)
        root.addView(controlsLayout)

        // Tabs
        val tabLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dimen(R.dimen.tab_padding), dimen(R.dimen.tab_padding), dimen(R.dimen.tab_padding), dimen(R.dimen.tab_padding)); setBackgroundColor(t.bottomBar); gravity = Gravity.CENTER }
        btnHome = createTabButton("🏠 الرئيسية") { switchTab("home") }
        btnLive = createTabButton("📺 مباشر") { switchTab("live") }
        btnMovies = createTabButton("🎬 أفلام") { switchTab("movies") }
        btnSeries = createTabButton("🎭 مسلسلات") { switchTab("series") }
        btnFavorites = createTabButton("⭐ مفضلة") { switchTab("favorites") }
        tabLayout.addView(btnHome); tabLayout.addView(btnLive); tabLayout.addView(btnMovies); tabLayout.addView(btnSeries); tabLayout.addView(btnFavorites)
        root.addView(tabLayout)

        // Progress
        progressBar = ProgressBar(this).apply { visibility = View.GONE; layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 4) }
        root.addView(progressBar)

        // List
        rv = RecyclerView(this).apply { layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f); layoutManager = LinearLayoutManager(this@MainActivity); setBackgroundColor(t.bg) }
        root.addView(rv)

        setContentView(root)

        // Player Init
        player = ExoPlayer.Builder(this).build()
        playerView.player = player
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                btnPlayPause.text = if (isPlaying) "⏸️" else "▶️"
                controlsLayout.visibility = View.VISIBLE
                tvChannelInfo.visibility = View.VISIBLE
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) playNextChannel()
            }
        })

        loadFavorites(); loadHistory()
        registerDownloadReceiver()

        val savedUrl = prefs.getString("server_url", "")
        if (!savedUrl.isNullOrEmpty()) {
            server = XtreamServer(savedUrl!!, prefs.getString("server_username", "")!!, prefs.getString("server_password", "")!!)
            switchTab("home")
        } else showLoginDialog()
    }

    private fun createCtrlButton(text: String, size: Float, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text; textSize = size; setBackgroundColor(Color.TRANSPARENT); setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { onClick() }
        }
    }

    private fun createTabButton(text: String, onClick: () -> Unit): Button {
        val t = themes[currentTheme]!!
        return Button(this).apply { this.text = text; textSize = dimenSp(R.dimen.tab_text_size); setTextColor(t.textGray); setBackgroundColor(Color.TRANSPARENT); setTypeface(null, Typeface.BOLD); layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f); setOnClickListener { onClick() } }
    }

    // ===== PLAYER CONTROLS =====
    private fun togglePlayPause() { if (player.isPlaying) player.pause() else player.play() }

    private fun playNextChannel() {
        val list = when (currentStreamType) { "live" -> liveChannels; "movie" -> vodMovies; else -> emptyList() }
        if (list.isEmpty()) return
        val nextIndex = (currentStreamIndex + 1) % list.size
        playChannelAtIndex(nextIndex)
    }

    private fun playPreviousChannel() {
        val list = when (currentStreamType) { "live" -> liveChannels; "movie" -> vodMovies; else -> emptyList() }
        if (list.isEmpty()) return
        val prevIndex = if (currentStreamIndex - 1 < 0) list.size - 1 else currentStreamIndex - 1
        playChannelAtIndex(prevIndex)
    }

    private fun playChannelAtIndex(index: Int) {
        when (currentStreamType) {
            "live" -> {
                if (index in liveChannels.indices) {
                    currentStreamIndex = index
                    val ch = liveChannels[index]
                    val url = XtreamAPI.getStreamUrl(server!!, ch.streamId, ch.containerExtension)
                    playStream(url, ch.name, "live")
                }
            }
            "movie" -> {
                if (index in vodMovies.indices) {
                    currentStreamIndex = index
                    val m = vodMovies[index]
                    val url = XtreamAPI.getMovieUrl(server!!, m.streamId, m.containerExtension)
                    playStream(url, m.name, "movie")
                }
            }
        }
    }

    private fun changeAspectRatio() {
        aspectRatioMode = (aspectRatioMode + 1) % 3
        playerView.resizeMode = when (aspectRatioMode) {
            0 -> AspectRatioFrameLayout.RESIZE_MODE_FIT
            1 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            else -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        }
        val modes = arrayOf("ملائم", "ملء", "تكبير")
        Toast.makeText(this, "📐 ${modes[aspectRatioMode]}", Toast.LENGTH_SHORT).show()
    }

    private fun toggleFullscreen() {
        if (playerView.layoutParams.height == ViewGroup.LayoutParams.MATCH_PARENT) {
            playerView.layoutParams.height = dimen(R.dimen.player_height)
            Toast.makeText(this, "📱 الوضع العادي", Toast.LENGTH_SHORT).show()
        } else {
            playerView.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            Toast.makeText(this, "⛶ ملء الشاشة", Toast.LENGTH_SHORT).show()
        }
        playerView.requestLayout()
    }

    private fun toggleRecording() { if (isRecording) stopRecording() else startRecording() }

    private fun startRecording() {
        if (currentStreamUrl == null) { Toast.makeText(this, "لا يوجد بث للتسجيل", Toast.LENGTH_SHORT).show(); return }
        try {
            val dir = File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "Recordings"); dir.mkdirs()
            recordingFile = File(dir, "MN-DAZOU_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.ts")
            isRecording = true; btnRecord.text = "⏹️"; btnRecord.setTextColor(Color.RED)
            Toast.makeText(this, "🔴 بدأ التسجيل", Toast.LENGTH_SHORT).show()
            recordingThread = thread {
                try {
                    val input = java.net.URL(currentStreamUrl).openStream()
                    val output = java.io.FileOutputStream(recordingFile)
                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    while (isRecording) {
                        bytesRead = input.read(buffer)
                        if (bytesRead == -1) break
                        output.write(buffer, 0, bytesRead)
                    }
                    output.close(); input.close()
                } catch (e: Exception) { runOnUiThread { Toast.makeText(this@MainActivity, "❌ خطأ في التسجيل", Toast.LENGTH_SHORT).show(); stopRecording() } }
            }
        } catch (e: Exception) { Toast.makeText(this, "❌ فشل بدء التسجيل", Toast.LENGTH_SHORT).show() }
    }

    private fun stopRecording() {
        isRecording = false; btnRecord.text = "🔴"; btnRecord.setTextColor(Color.WHITE)
        recordingThread?.interrupt()
        recordingFile?.let {
            val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            intent.data = Uri.fromFile(it); sendBroadcast(intent)
            Toast.makeText(this, "✅ تم حفظ التسجيل: ${it.name}", Toast.LENGTH_LONG).show()
        }
    }

    private fun shareCurrentStream() {
        currentStreamName?.let { name ->
            val text = "شاهد معي على MN-DAZOU IPTV:\n📺 $name\n🔗 ${currentStreamUrl ?: ""}"
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }, "مشاركة عبر"))
        } ?: Toast.makeText(this, "لا يوجد محتوى للمشاركة", Toast.LENGTH_SHORT).show()
    }

    private fun downloadCurrentStream() {
        currentStreamUrl?.let { url ->
            try {
                val request = DownloadManager.Request(Uri.parse(url))
                    .setTitle("MN-DAZOU IPTV")
                    .setDescription(currentStreamName ?: "فيديو")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES, "MN-DAZOU_${System.currentTimeMillis()}.mp4")
                downloadId = downloadManager.enqueue(request)
                Toast.makeText(this, "⬇️ جاري التحميل...", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) { Toast.makeText(this, "❌ فشل التحميل", Toast.LENGTH_SHORT).show() }
        } ?: Toast.makeText(this, "لا يوجد فيديو للتحميل", Toast.LENGTH_SHORT).show()
    }

    private fun registerDownloadReceiver() {
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) == downloadId)
                    Toast.makeText(this@MainActivity, "✅ تم التحميل بنجاح", Toast.LENGTH_SHORT).show()
            }
        }, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    // ===== PLAY STREAM (updated) =====
    private fun playStream(url: String, name: String, type: String = "live") {
        try {
            currentStreamUrl = url; currentStreamName = name; currentStreamType = type
            player.setMediaItem(MediaItem.fromUri(Uri.parse(url)))
            player.prepare(); player.play()
            tvChannelInfo.text = "🎬 $name"
            Toast.makeText(this, "▶️ $name", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) { Toast.makeText(this, "❌ ${e.message}", Toast.LENGTH_LONG).show() }
    }

    // ===== THEME =====
    private fun showThemeDialog() {
        AlertDialog.Builder(this).setTitle("🎨 اختر الثيم").setItems(themes.values.map { it.name }.toTypedArray()) { _, w ->
            prefs.edit().putString("theme", themes.keys.toList()[w]).apply(); Toast.makeText(this, "🔄 أعد تشغيل التطبيق", Toast.LENGTH_LONG).show()
        }.show()
    }

    private fun switchTab(tab: String) {
        currentCategory = tab; selectedCategoryId = null; isShowingCategories = true; btnBack.visibility = View.GONE; searchLayout.visibility = View.GONE
        val t = themes[currentTheme]!!
        btnHome.setTextColor(t.textGray); btnLive.setTextColor(t.textGray); btnMovies.setTextColor(t.textGray); btnSeries.setTextColor(t.textGray); btnFavorites.setTextColor(t.textGray)
        when (tab) {
            "home" -> { btnHome.setTextColor(t.accent); tvTitle.text = "🏠 MN-DAZOU IPTV"; showHomeScreen() }
            "live" -> { btnLive.setTextColor(t.accent); tvTitle.text = "📺 البث المباشر"; searchLayout.visibility = View.VISIBLE; loadLiveCategories() }
            "movies" -> { btnMovies.setTextColor(t.accent); tvTitle.text = "🎬 الأفلام"; searchLayout.visibility = View.VISIBLE; loadVodCategories() }
            "series" -> { btnSeries.setTextColor(t.accent); tvTitle.text = "🎭 المسلسلات"; searchLayout.visibility = View.VISIBLE; loadSeriesCategories() }
            "favorites" -> { btnFavorites.setTextColor(t.accent); tvTitle.text = "⭐ المفضلة"; showFavorites() }
        }
    }

    private fun goBack() { isShowingCategories = true; selectedCategoryId = null; btnBack.visibility = View.GONE; when (currentCategory) { "live" -> showLiveCategories(); "movies" -> showVodCategories(); "series" -> showSeriesCategories() } }

    // ===== HOME SCREEN =====
    private fun showHomeScreen() {
        val t = themes[currentTheme]!!; val items = mutableListOf<Any>()
        if (watchHistory.isNotEmpty()) { items.add("section_history"); items.addAll(watchHistory.takeLast(5).reversed()) }
        if (favorites.isNotEmpty()) { items.add("section_favorites"); items.addAll(favorites.take(5)) }
        items.add("section_quick"); items.add("quick_live"); items.add("quick_movies"); items.add("quick_series"); items.add("quick_favorites")
        rv.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun getItemViewType(pos: Int): Int = if (items[pos] is String) 0 else 1
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                if (viewType == 0) { val tv = TextView(parent.context).apply { setPadding(dimen(R.dimen.item_padding_h), dimen(R.dimen.item_padding_v), dimen(R.dimen.item_padding_h), dimen(R.dimen.item_padding_v) / 2); textSize = dimenSp(R.dimen.category_text_size); setTextColor(t.accent); setTypeface(null, Typeface.BOLD) }; return object : RecyclerView.ViewHolder(tv) {} }
                else { val l = LinearLayout(parent.context).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dimen(R.dimen.item_padding_h), dimen(R.dimen.item_padding_v), dimen(R.dimen.item_padding_h), dimen(R.dimen.item_padding_v)); gravity = Gravity.CENTER_VERTICAL; setBackgroundColor(t.card) }; val icon = ImageView(parent.context).apply { layoutParams = LinearLayout.LayoutParams(dimen(R.dimen.category_icon_size).toInt(), dimen(R.dimen.category_icon_size).toInt()); setBackgroundColor(t.activeTab) }; l.addView(icon); l.addView(TextView(parent.context).apply { setPadding(12, 0, 0, 0); textSize = dimenSp(R.dimen.item_text_size); setTextColor(t.textWhite); setTypeface(null, Typeface.BOLD); layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) }); return object : RecyclerView.ViewHolder(l) {} }
            }
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
                val item = items[pos]
                if (item is String) { val tv = holder.itemView as TextView; when (item) { "section_history" -> tv.text = "🕐 آخر المشاهدات"; "section_favorites" -> tv.text = "⭐ المفضلة"; "section_quick" -> tv.text = "🚀 وصول سريع" } }
                else if (item is HistoryItem) { val l = holder.itemView as LinearLayout; (l.getChildAt(1) as TextView).text = "🕐 ${item.name}"; l.setOnClickListener { playHistoryItem(item) } }
                else if (item is FavoriteItem) { val l = holder.itemView as LinearLayout; (l.getChildAt(1) as TextView).text = "⭐ ${item.name}"; l.setOnClickListener { playFavoriteItem(item) } }
            }
            override fun getItemCount() = items.size
        }
    }

    // ===== FAVORITES =====
    private fun addToFavorites(type: String, id: Int, name: String) { if (favorites.none { it.type == type && it.id == id }) { favorites.add(FavoriteItem(type, id, name)); saveFavorites(); Toast.makeText(this, "⭐ تم", Toast.LENGTH_SHORT).show() } }
    private fun removeFavorite(item: FavoriteItem) { favorites.removeAll { it.type == item.type && it.id == item.id }; saveFavorites() }
    private fun saveFavorites() { val j = JSONArray(); favorites.forEach { val o = JSONObject(); o.put("type", it.type); o.put("id", it.id); o.put("name", it.name); j.put(o) }; prefs.edit().putString("favorites", j.toString()).apply() }
    private fun loadFavorites() { try { favorites.clear(); val s = prefs.getString("favorites", "[]") ?: "[]"; val j = JSONArray(s); for (i in 0 until j.length()) { val o = j.getJSONObject(i); favorites.add(FavoriteItem(o.getString("type"), o.getInt("id"), o.getString("name"))) } } catch (_: Exception) { favorites.clear() } }
    private fun showFavorites() { isShowingCategories = true; val t = themes[currentTheme]!!; rv.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() { override fun onCreateViewHolder(p: ViewGroup, vt: Int): RecyclerView.ViewHolder { val l = LinearLayout(p.context).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dimen(R.dimen.item_padding_h), dimen(R.dimen.item_padding_v), dimen(R.dimen.item_padding_h), dimen(R.dimen.item_padding_v)); gravity = Gravity.CENTER_VERTICAL; setBackgroundColor(t.card) }; l.addView(TextView(p.context).apply { textSize = dimenSp(R.dimen.item_text_size); setTextColor(t.textWhite); setTypeface(null, Typeface.BOLD); layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) }); l.addView(Button(p.context).apply { text = "❌"; setBackgroundColor(Color.TRANSPARENT); setTextColor(Color.RED); textSize = dimenSp(R.dimen.item_fav_icon_size) }); return object : RecyclerView.ViewHolder(l) {} } override fun onBindViewHolder(h: RecyclerView.ViewHolder, p: Int) { val l = (h.itemView as LinearLayout); val fav = favorites[p]; (l.getChildAt(0) as TextView).text = "⭐ ${fav.name}"; l.setOnClickListener { playFavoriteItem(fav) }; l.getChildAt(1).setOnClickListener { removeFavorite(fav); showFavorites() } } override fun getItemCount() = favorites.size } }

    // ===== HISTORY =====
    private fun addToHistory(type: String, id: Int, name: String) { watchHistory.removeAll { it.type == type && it.id == id }; watchHistory.add(HistoryItem(type, id, name, System.currentTimeMillis())); if (watchHistory.size > 20) watchHistory.removeAt(0); saveHistory() }
    private fun saveHistory() { val j = JSONArray(); watchHistory.forEach { val o = JSONObject(); o.put("type", it.type); o.put("id", it.id); o.put("name", it.name); o.put("timestamp", it.timestamp); j.put(o) }; prefs.edit().putString("history", j.toString()).apply() }
    private fun loadHistory() { try { watchHistory.clear(); val s = prefs.getString("history", "[]") ?: "[]"; val j = JSONArray(s); for (i in 0 until j.length()) { val o = j.getJSONObject(i); watchHistory.add(HistoryItem(o.getString("type"), o.getInt("id"), o.getString("name"), o.getLong("timestamp"))) } } catch (_: Exception) { watchHistory.clear() } }
    private fun playFavoriteItem(fav: FavoriteItem) { when (fav.type) { "live" -> { val url = XtreamAPI.getStreamUrl(server!!, fav.id); playStream(url, fav.name, "live"); addToHistory("live", fav.id, fav.name) } "movie" -> { val url = XtreamAPI.getMovieUrl(server!!, fav.id); playStream(url, fav.name, "movie"); addToHistory("movie", fav.id, fav.name) } } }
    private fun playHistoryItem(item: HistoryItem) { when (item.type) { "live" -> playStream(XtreamAPI.getStreamUrl(server!!, item.id), item.name, "live") "movie" -> playStream(XtreamAPI.getMovieUrl(server!!, item.id), item.name, "movie") } }

    // ===== SEARCH =====
    private fun performSearch() { /* unchanged */ }

    // ===== LOGIN =====
    private fun showLoginDialog() { /* unchanged */ }

    // ===== LIVE =====
    private fun loadLiveCategories() { /* unchanged */ }
    private fun showLiveCategories() { /* unchanged */ }
    private fun loadLiveStreams(catId: String?) { /* unchanged */ }
    private fun updateLiveList() { tvTitle.text = "${tvTitle.text} (${liveChannels.size})"; rv.adapter = createChannelAdapter(liveChannels.map { it.name }) { name -> val idx = liveChannels.indexOfFirst { it.name == name }; currentStreamIndex = idx; currentStreamType = "live"; val ch = liveChannels[idx]; val url = XtreamAPI.getStreamUrl(server!!, ch.streamId, ch.containerExtension); addToHistory("live", ch.streamId, ch.name); playStream(url, ch.name, "live") } }

    // ===== VOD =====
    private fun loadVodCategories() { /* unchanged */ }
    private fun showVodCategories() { /* unchanged */ }
    private fun loadMovies(catId: String?) { /* unchanged */ }
    private fun updateMoviesList() { tvTitle.text = "${tvTitle.text} (${vodMovies.size})"; rv.adapter = createChannelAdapter(vodMovies.map { it.name }) { name -> val idx = vodMovies.indexOfFirst { it.name == name }; currentStreamIndex = idx; currentStreamType = "movie"; val m = vodMovies[idx]; val url = XtreamAPI.getMovieUrl(server!!, m.streamId, m.containerExtension); addToHistory("movie", m.streamId, m.name); playStream(url, m.name, "movie") } }

    // ===== SERIES =====
    private fun loadSeriesCategories() { /* unchanged */ }
    private fun showSeriesCategories() { /* unchanged */ }
    private fun loadSeriesList(catId: String?) { /* unchanged */ }
    private fun updateSeriesList() { tvTitle.text = "${tvTitle.text} (${seriesList.size})"; rv.adapter = createChannelAdapter(seriesList.map { it.name }) { name -> val s = seriesList.find { it.name == name }!!; XtreamAPI.getSeriesInfo(server!!, s.seriesId) { episodes -> showEpisodesDialog(s.name, episodes) } } }

    // ===== ERROR =====
    private fun showEmptyError(message: String) { /* unchanged */ }
    private fun refreshCurrentTab() { switchTab(currentCategory) }

    // ===== ADAPTERS =====
    private fun createCategoryAdapter(cats: List<XtreamCategory>, onClick: (XtreamCategory) -> Unit): RecyclerView.Adapter<RecyclerView.ViewHolder> { /* unchanged */ }
    private fun createChannelAdapter(names: List<String>, onClick: (String) -> Unit): RecyclerView.Adapter<RecyclerView.ViewHolder> { /* unchanged */ }

    private fun showEpisodesDialog(name: String, episodes: List<XtreamEpisode>) {
        if (episodes.isEmpty()) { Toast.makeText(this, "لا حلقات", Toast.LENGTH_SHORT).show(); return }
        AlertDialog.Builder(this).setTitle(name).setItems(episodes.map { "🎭 حلقة ${it.episodeNum}: ${it.title}" }.toTypedArray()) { _, i -> val e = episodes[i]; playStream(XtreamAPI.getSeriesEpisodeUrl(server!!, e.id, e.containerExtension), "$name - حلقة ${e.episodeNum}", "series") }.setNegativeButton("إغلاق", null).show()
    }

    private fun showLoading() { progressBar.visibility = View.VISIBLE }
    private fun hideLoading() { progressBar.visibility = View.GONE }

    override fun onDestroy() { if (isRecording) stopRecording(); super.onDestroy(); player.release() }
}