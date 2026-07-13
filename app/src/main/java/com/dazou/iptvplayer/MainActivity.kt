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
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
    private lateinit var btnAccounts: Button
    private lateinit var btnBack: Button
    private lateinit var searchLayout: LinearLayout
    private lateinit var etSearch: EditText

    private lateinit var controlsLayout: LinearLayout
    private lateinit var btnPrevChannel: Button
    private lateinit var btnPlayPause: Button
    private lateinit var btnNextChannel: Button
    private lateinit var btnEpg: Button
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
    private var accounts = mutableListOf<XtreamServer>()
    private var currentAccountIndex = 0
    private var currentCategory = "home"
    private var selectedCategoryId: String? = null
    private var isShowingCategories = true
    private var currentTheme = "dark"

    private var currentStreamIndex = -1
    private var currentStreamType = "live"
    private var currentStreamName: String? = null
    private var currentStreamUrl: String? = null
    private var isRecording = false
    private var recordingFile: File? = null
    private var recordingThread: Thread? = null
    private var downloadId: Long = 0
    private var aspectRatioMode = 0

    private lateinit var prefs: SharedPreferences
    private lateinit var downloadManager: DownloadManager
    private lateinit var themes: Map<String, ThemeColors>

    data class FavoriteItem(val type: String, val id: Int, val name: String)
    data class HistoryItem(val type: String, val id: Int, val name: String, val timestamp: Long)
    data class ThemeColors(val name: String, val bg: Int, val card: Int, val accent: Int, val bottomBar: Int, val textWhite: Int, val textGray: Int, val activeTab: Int)

    private fun dimen(id: Int): Int = try { resources.getDimensionPixelSize(id) } catch (e: Exception) { 20 }
    private fun dimenSp(id: Int): Float = try { resources.getDimension(id) / resources.displayMetrics.scaledDensity } catch (e: Exception) { 12f }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
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
            header.addView(Button(this).apply { text = "⚙️"; textSize = dimenSp(R.dimen.header_icon_size); setBackgroundColor(Color.TRANSPARENT); setTextColor(t.textGray); setOnClickListener { showSettingsDialog() } })
            root.addView(header)

            searchLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dimen(R.dimen.search_padding_h), dimen(R.dimen.search_padding_v), dimen(R.dimen.search_padding_h), dimen(R.dimen.search_padding_v)); setBackgroundColor(t.bottomBar); gravity = Gravity.CENTER_VERTICAL; visibility = View.GONE }
            etSearch = EditText(this).apply { hint = "🔍 بحث..."; setHintTextColor(t.textGray); setTextColor(t.textWhite); setBackgroundColor(t.bg); setPadding(12, 4, 12, 4); layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f); textSize = dimenSp(R.dimen.search_text_size) }
            searchLayout.addView(etSearch)
            searchLayout.addView(Button(this).apply { text = "بحث"; textSize = dimenSp(R.dimen.search_text_size); setBackgroundColor(t.accent); setTextColor(Color.BLACK); setOnClickListener { performSearch() } })
            root.addView(searchLayout)

            playerView = PlayerView(this).apply { layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dimen(R.dimen.player_height)); setBackgroundColor(Color.BLACK); useController = false }
            root.addView(playerView)

            tvChannelInfo = TextView(this).apply { text = ""; textSize = dimenSp(R.dimen.player_info_text_size); setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#CC000000")); setPadding(12, 4, 12, 4); visibility = View.GONE; gravity = Gravity.CENTER }
            root.addView(tvChannelInfo)

            controlsLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(2, dimen(R.dimen.player_control_padding), 2, dimen(R.dimen.player_control_padding)); setBackgroundColor(Color.parseColor("#DD000000")); gravity = Gravity.CENTER; visibility = View.GONE }
            val ctrlTextSize = dimenSp(R.dimen.player_control_text_size)
            btnPrevChannel = createCtrlButton("⏪", ctrlTextSize) { playPreviousChannel() }
            btnPlayPause = createCtrlButton("▶️", ctrlTextSize) { togglePlayPause() }
            btnNextChannel = createCtrlButton("⏩", ctrlTextSize) { playNextChannel() }
            btnEpg = createCtrlButton("📋", ctrlTextSize) { showEpgForCurrentChannel() }
            btnAspectRatio = createCtrlButton("🔲", ctrlTextSize) { changeAspectRatio() }
            btnToggleFullscreen = createCtrlButton("⛶", ctrlTextSize) { toggleFullscreen() }
            btnRecord = createCtrlButton("🔴", ctrlTextSize) { toggleRecording() }
            btnShare = createCtrlButton("📤", ctrlTextSize) { shareCurrentStream() }
            btnDownload = createCtrlButton("⬇️", ctrlTextSize) { downloadCurrentStream() }
            controlsLayout.addView(btnPrevChannel); controlsLayout.addView(btnPlayPause); controlsLayout.addView(btnNextChannel); controlsLayout.addView(btnEpg)
            controlsLayout.addView(btnAspectRatio); controlsLayout.addView(btnToggleFullscreen); controlsLayout.addView(btnRecord)
            controlsLayout.addView(btnShare); controlsLayout.addView(btnDownload)
            root.addView(controlsLayout)

            val tabLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dimen(R.dimen.tab_padding), dimen(R.dimen.tab_padding), dimen(R.dimen.tab_padding), dimen(R.dimen.tab_padding)); setBackgroundColor(t.bottomBar); gravity = Gravity.CENTER }
            btnHome = createTabButton("🏠 الرئيسية") { switchTab("home") }
            btnLive = createTabButton("📺 مباشر") { switchTab("live") }
            btnMovies = createTabButton("🎬 أفلام") { switchTab("movies") }
            btnSeries = createTabButton("🎭 مسلسلات") { switchTab("series") }
            btnFavorites = createTabButton("⭐ مفضلة") { switchTab("favorites") }
            btnAccounts = createTabButton("👤 حسابات") { showAccountsDialog() }
            tabLayout.addView(btnHome); tabLayout.addView(btnLive); tabLayout.addView(btnMovies); tabLayout.addView(btnSeries); tabLayout.addView(btnFavorites); tabLayout.addView(btnAccounts)
            root.addView(tabLayout)

            progressBar = ProgressBar(this).apply { visibility = View.GONE; layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 4) }
            root.addView(progressBar)

            rv = RecyclerView(this).apply { layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f); layoutManager = LinearLayoutManager(this@MainActivity); setBackgroundColor(t.bg) }
            root.addView(rv)

            setContentView(root)

            player = ExoPlayer.Builder(this).build()
            playerView.player = player
            player.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) { btnPlayPause.text = if (isPlaying) "⏸️" else "▶️"; controlsLayout.visibility = View.VISIBLE; tvChannelInfo.visibility = View.VISIBLE }
                override fun onPlaybackStateChanged(state: Int) { if (state == Player.STATE_ENDED) playNextChannel() }
            })

            loadFavorites(); loadHistory(); loadAccounts()
            registerDownloadReceiver()

            if (accounts.isNotEmpty()) {
                currentAccountIndex = prefs.getInt("current_account", 0)
                if (currentAccountIndex < accounts.size) server = accounts[currentAccountIndex]
                switchTab("home")
            } else showLoginDialog()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "خطأ: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun createCtrlButton(text: String, size: Float, onClick: () -> Unit) = Button(this).apply { this.text = text; textSize = size; setBackgroundColor(Color.TRANSPARENT); setTextColor(Color.WHITE); layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f); setOnClickListener { onClick() } }
    private fun createTabButton(text: String, onClick: () -> Unit) = Button(this).apply { this.text = text; textSize = dimenSp(R.dimen.tab_text_size); setTextColor(themes[currentTheme]!!.textGray); setBackgroundColor(Color.TRANSPARENT); setTypeface(null, Typeface.BOLD); layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f); setOnClickListener { onClick() } }

    // ===== D-Pad Support =====
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_NEXT -> { playNextChannel(); true }
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MEDIA_PREVIOUS -> { playPreviousChannel(); true }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> { togglePlayPause(); true }
            KeyEvent.KEYCODE_BACK -> { if (!isShowingCategories) { goBack(); true } else super.onKeyDown(keyCode, event) }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(this).setTitle("⚙️ الإعدادات").setItems(arrayOf("🎨 تغيير الثيم", "👤 إدارة الحسابات", "📋 EPG", "🗑️ مسح البيانات")) { _, w ->
            when (w) { 0 -> showThemeDialog(); 1 -> showAccountsDialog(); 2 -> showEpgForCurrentChannel(); 3 -> { prefs.edit().clear().apply(); accounts.clear(); recreate() } }
        }.show()
    }

    // ===== ACCOUNTS =====
    private fun saveAccounts() { val j = JSONArray(); accounts.forEach { val o = JSONObject(); o.put("url", it.url); o.put("username", it.username); o.put("password", it.password); j.put(o) }; prefs.edit().putString("accounts", j.toString()).apply() }
    private fun loadAccounts() { try { val s = prefs.getString("accounts", "[]") ?: "[]"; val j = JSONArray(s); accounts.clear(); for (i in 0 until j.length()) { val o = j.getJSONObject(i); accounts.add(XtreamServer(o.getString("url"), o.getString("username"), o.getString("password"))) } } catch (_: Exception) { accounts.clear() } }

    private fun showAccountsDialog() {
        if (accounts.isEmpty()) { showLoginDialog(); return }
        val names = accounts.mapIndexed { i, a -> "👤 حساب ${i+1}: ${a.username}" }.toTypedArray()
        AlertDialog.Builder(this).setTitle("👤 إدارة الحسابات (${accounts.size})").setItems(names + arrayOf("➕ إضافة حساب", "🗑️ حذف الكل")) { _, w ->
            when {
                w < accounts.size -> { currentAccountIndex = w; server = accounts[w]; prefs.edit().putInt("current_account", w).apply(); switchTab("home"); Toast.makeText(this, "✅ تم التبديل", Toast.LENGTH_SHORT).show() }
                w == accounts.size -> showLoginDialog()
                w == accounts.size + 1 -> { accounts.clear(); saveAccounts(); server = null; showLoginDialog() }
            }
        }.show()
    }

    // ===== EPG =====
    private fun showEpgForCurrentChannel() {
        server?.let { srv ->
            val streamId = if (currentStreamType == "live" && currentStreamIndex in liveChannels.indices) liveChannels[currentStreamIndex].streamId else null
            showLoading()
            XtreamAPI.getEpg(srv, streamId) { epgList ->
                hideLoading()
                if (epgList.isNotEmpty()) showEpgDialog(epgList)
                else Toast.makeText(this, "لا توجد بيانات EPG", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEpgDialog(epgList: List<EpgProgram>) {
        val items = epgList.map { "${it.startTime} - ${it.endTime}: ${it.title}" }.toTypedArray()
        AlertDialog.Builder(this).setTitle("📋 دليل البرامج (${epgList.size})").setItems(items) { _, i ->
            val epg = epgList[i]
            AlertDialog.Builder(this).setTitle(epg.title).setMessage("🕐 ${epg.startTime} - ${epg.endTime}\n\n${epg.description}").setPositiveButton("إغلاق", null).show()
        }.setNegativeButton("إغلاق", null).show()
    }

    // ===== Player Controls (existing) =====
    private fun togglePlayPause() { if (player.isPlaying) player.pause() else player.play() }
    private fun playNextChannel() { val list = when (currentStreamType) { "live" -> liveChannels; "movie" -> vodMovies; else -> emptyList() }; if (list.isEmpty()) return; playChannelAtIndex((currentStreamIndex + 1) % list.size) }
    private fun playPreviousChannel() { val list = when (currentStreamType) { "live" -> liveChannels; "movie" -> vodMovies; else -> emptyList() }; if (list.isEmpty()) return; playChannelAtIndex(if (currentStreamIndex - 1 < 0) list.size - 1 else currentStreamIndex - 1) }
    private fun playChannelAtIndex(index: Int) { /* unchanged */ }
    private fun changeAspectRatio() { aspectRatioMode = (aspectRatioMode + 1) % 3; playerView.resizeMode = when (aspectRatioMode) { 0 -> AspectRatioFrameLayout.RESIZE_MODE_FIT; 1 -> AspectRatioFrameLayout.RESIZE_MODE_FILL; else -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM }; Toast.makeText(this, "📐 ${arrayOf("ملائم", "ملء", "تكبير")[aspectRatioMode]}", Toast.LENGTH_SHORT).show() }
    private fun toggleFullscreen() { /* unchanged */ }
    private fun toggleRecording() { if (isRecording) stopRecording() else startRecording() }
    private fun startRecording() { /* unchanged */ }
    private fun stopRecording() { /* unchanged */ }
    private fun shareCurrentStream() { /* unchanged */ }
    private fun downloadCurrentStream() { /* unchanged */ }
    private fun registerDownloadReceiver() { /* unchanged */ }

    private fun playStream(url: String, name: String, type: String = "live") { /* unchanged */ }

    // ===== Theme, Tabs, Home, Favorites, History, Search, Login, Loaders, Adapters =====
    // جميع الدوال المتبقية كما هي من الكود السابق (showThemeDialog, switchTab, goBack, showHomeScreen, addToFavorites, removeFavorite, saveFavorites, loadFavorites, showFavorites, addToHistory, saveHistory, loadHistory, playFavoriteItem, playHistoryItem, performSearch, showLoginDialog, loadLiveCategories, showLiveCategories, loadLiveStreams, updateLiveList, loadVodCategories, showVodCategories, loadMovies, updateMoviesList, loadSeriesCategories, showSeriesCategories, loadSeriesList, updateSeriesList, showEmptyError, refreshCurrentTab, createCategoryAdapter, createChannelAdapter, showEpisodesDialog, showLoading, hideLoading)

    override fun onDestroy() { if (isRecording) stopRecording(); super.onDestroy(); player.release() }
}