package com.dazou.iptvplayer

import android.app.*
import android.content.*
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.media.AudioManager
import android.net.Uri
import android.os.*
import android.view.*
import android.view.animation.AlphaAnimation
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
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
    private lateinit var playerControlsLayout: LinearLayout
    private lateinit var btnPrevChannel: Button
    private lateinit var btnPlayPause: Button
    private lateinit var btnNextChannel: Button
    private lateinit var btnRecord: Button
    private lateinit var btnShare: Button
    private lateinit var btnFullscreen: Button
    private lateinit var btnDownload: Button
    private lateinit var btnQuality: Button
    private lateinit var btnAspectRatio: Button
    private lateinit var seekBar: SeekBar
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvTotalTime: TextView
    private lateinit var tvChannelInfo: TextView
    private lateinit var tvRecording: TextView
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
    private var imageCache = mutableMapOf<String, Bitmap>()
    private var currentStreamUrl: String? = null
    private var currentStreamName: String? = null
    private var currentStreamIndex = -1
    private var currentStreamType = "live"
    private var isPlayerPlaying = false
    private var currentAspectRatio = 0
    private var currentTheme = "dark"
    private var downloadId: Long = 0
    private var isRecording = false
    private var recordingFile: File? = null
    private var recordingThread: Thread? = null
    private var watchStartTime: Long = 0
    private var accounts = mutableListOf<XtreamServer>()
    private var currentAccountIndex = 0
    private var currentCategory = "home"
    private var selectedCategoryId: String? = null
    private var isShowingCategories = true

    private lateinit var prefs: SharedPreferences
    private lateinit var downloadManager: DownloadManager
    private lateinit var themes: Map<String, ThemeColors>
    private var isTv = false

    data class FavoriteItem(val type: String, val id: Int, val name: String, val icon: String = "")
    data class HistoryItem(val type: String, val id: Int, val name: String, val timestamp: Long, val icon: String = "")
    data class EpgProgram(val channelId: String, val title: String, val startTime: String, val endTime: String, val description: String)
    data class ThemeColors(val name: String, val bg: Int, val card: Int, val accent: Int, val bottomBar: Int, val textWhite: Int, val textGray: Int, val activeTab: Int)

    private fun isTelevision(): Boolean {
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        return uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            supportActionBar?.hide()
            isTv = isTelevision()
            prefs = getSharedPreferences("mndazou_prefs", Context.MODE_PRIVATE)
            downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

            themes = mapOf(
                "dark" to ThemeColors("داكن", Color.parseColor("#0F0F1A"), Color.parseColor("#1A1A35"), Color.parseColor("#FF6B6B"), Color.parseColor("#12122A"), Color.parseColor("#FFFFFF"), Color.parseColor("#AAAAAA"), Color.parseColor("#2D2D5E")),
                "blue" to ThemeColors("أزرق", Color.parseColor("#0A1628"), Color.parseColor("#1B2D4A"), Color.parseColor("#4FC3F7"), Color.parseColor("#0D1F3C"), Color.parseColor("#FFFFFF"), Color.parseColor("#90CAF9"), Color.parseColor("#1565C0")),
                "green" to ThemeColors("أخضر", Color.parseColor("#0A1F0A"), Color.parseColor("#1A3A1A"), Color.parseColor("#66BB6A"), Color.parseColor("#0D2A0D"), Color.parseColor("#FFFFFF"), Color.parseColor("#A5D6A7"), Color.parseColor("#2E7D32")),
                "purple" to ThemeColors("بنفسجي", Color.parseColor("#1A0A2E"), Color.parseColor("#2D1B4E"), Color.parseColor("#CE93D8"), Color.parseColor("#1F0D3D"), Color.parseColor("#FFFFFF"), Color.parseColor("#E1BEE7"), Color.parseColor("#6A1B9A")),
                "red" to ThemeColors("أحمر", Color.parseColor("#1A0A0A"), Color.parseColor("#3A1A1A"), Color.parseColor("#EF5350"), Color.parseColor("#2A0D0D"), Color.parseColor("#FFFFFF"), Color.parseColor("#EF9A9A"), Color.parseColor("#C62828"))
            )
            currentTheme = prefs.getString("theme", "dark") ?: "dark"
            val t = themes[currentTheme]!!

            val root = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(t.bg)
            }

            val header = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(20), dp(if (isTv) 30 else 45), dp(20), dp(20))
                setBackgroundColor(t.bottomBar)
                gravity = Gravity.CENTER_VERTICAL
            }
            btnBack = Button(this).apply {
                text = "⬅️"
                textSize = if (isTv) sp(22f) else sp(18f)
                setBackgroundColor(Color.TRANSPARENT)
                setTextColor(t.textWhite)
                visibility = View.GONE
                setOnClickListener { goBackToCategories() }
            }
            header.addView(btnBack)
            tvTitle = TextView(this).apply {
                text = "MN-DAZOU IPTV"
                textSize = if (isTv) sp(28f) else sp(20f)
                setTextColor(t.accent)
                setTypeface(null, Typeface.BOLD)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            header.addView(tvTitle)
            header.addView(Button(this).apply { text = "🎨"; textSize = if (isTv) sp(22f) else sp(16f); setBackgroundColor(Color.TRANSPARENT); setTextColor(t.textGray); setOnClickListener { showThemeDialog() } })
            header.addView(Button(this).apply { text = "⚙️"; textSize = if (isTv) sp(22f) else sp(16f); setBackgroundColor(Color.TRANSPARENT); setTextColor(t.textGray); setOnClickListener { showSettingsDialog() } })
            root.addView(header)

            searchLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(15), dp(10), dp(15), dp(10))
                setBackgroundColor(t.bottomBar)
                gravity = Gravity.CENTER_VERTICAL
                visibility = View.GONE
            }
            etSearch = EditText(this).apply {
                hint = "🔍 بحث..."
                setHintTextColor(t.textGray)
                setTextColor(t.textWhite)
                setBackgroundColor(t.card)
                setPadding(dp(25), dp(15), dp(25), dp(15))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                textSize = if (isTv) sp(18f) else sp(14f)
            }
            searchLayout.addView(etSearch)
            searchLayout.addView(Button(this).apply { text = "بحث"; setBackgroundColor(t.accent); setTextColor(Color.BLACK); textSize = if (isTv) sp(18f) else sp(14f); setOnClickListener { performSearch() } })
            root.addView(searchLayout)

            val playerCard = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(if (isTv) 500 else 420))
                setBackgroundColor(Color.BLACK)
            }
            playerView = PlayerView(this).apply {
                layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                setBackgroundColor(Color.BLACK)
                useController = false
            }
            tvChannelInfo = TextView(this).apply {
                text = ""
                textSize = sp(if (isTv) 16f else 13f)
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#CC000000"))
                setPadding(dp(15), dp(8), dp(15), dp(8))
                visibility = View.GONE
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP)
            }
            tvRecording = TextView(this).apply {
                text = "🔴"
                textSize = sp(if (isTv) 18f else 14f)
                setTextColor(Color.RED)
                setBackgroundColor(Color.parseColor("#CC000000"))
                setPadding(dp(15), dp(5), dp(15), dp(5))
                visibility = View.GONE
                gravity = Gravity.END
                layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.END)
            }
            playerCard.addView(playerView)
            playerCard.addView(tvChannelInfo)
            playerCard.addView(tvRecording)
            root.addView(playerCard)

            val navLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(15), dp(10), dp(15), dp(10))
                setBackgroundColor(Color.parseColor("#DD000000"))
                gravity = Gravity.CENTER
                visibility = View.GONE
            }
            val navTextSize = if (isTv) sp(26f) else sp(22f)
            btnPrevChannel = Button(this).apply { text = "⏪"; setBackgroundColor(Color.TRANSPARENT); setTextColor(Color.WHITE); textSize = navTextSize; setOnClickListener { playPreviousChannel() } }
            btnPlayPause = Button(this).apply { text = "▶️"; setBackgroundColor(Color.TRANSPARENT); setTextColor(Color.WHITE); textSize = navTextSize; setOnClickListener { togglePlayPause() } }
            btnNextChannel = Button(this).apply { text = "⏩"; setBackgroundColor(Color.TRANSPARENT); setTextColor(Color.WHITE); textSize = navTextSize; setOnClickListener { playNextChannel() } }
            btnRecord = Button(this).apply { text = "🔴"; setBackgroundColor(Color.TRANSPARENT); setTextColor(Color.WHITE); textSize = navTextSize; setOnClickListener { toggleRecording() } }
            navLayout.addView(btnPrevChannel)
            navLayout.addView(btnPlayPause)
            navLayout.addView(btnNextChannel)
            navLayout.addView(btnRecord)
            root.addView(navLayout)

            playerControlsLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(10), dp(8), dp(10), dp(8))
                setBackgroundColor(Color.parseColor("#DD000000"))
                gravity = Gravity.CENTER_VERTICAL
                visibility = View.GONE
            }
            val toolTextSize = if (isTv) sp(20f) else sp(16f)
            btnShare = Button(this).apply { text = "📤"; setBackgroundColor(Color.TRANSPARENT); setTextColor(Color.WHITE); textSize = toolTextSize; setOnClickListener { shareCurrentStream() } }
            btnQuality = Button(this).apply { text = "HD"; setBackgroundColor(Color.TRANSPARENT); setTextColor(Color.WHITE); textSize = toolTextSize; setOnClickListener { showQualityDialog() } }
            btnAspectRatio = Button(this).apply { text = "🔲"; setBackgroundColor(Color.TRANSPARENT); setTextColor(Color.WHITE); textSize = toolTextSize; setOnClickListener { changeAspectRatio() } }
            btnDownload = Button(this).apply { text = "⬇️"; setBackgroundColor(Color.TRANSPARENT); setTextColor(Color.WHITE); textSize = toolTextSize; setOnClickListener { downloadCurrentStream() } }
            btnFullscreen = Button(this).apply { text = "⛶"; setBackgroundColor(Color.TRANSPARENT); setTextColor(Color.WHITE); textSize = toolTextSize; setOnClickListener { toggleFullscreen() } }
            playerControlsLayout.addView(btnShare)
            playerControlsLayout.addView(btnQuality)
            playerControlsLayout.addView(btnAspectRatio)
            playerControlsLayout.addView(btnDownload)
            playerControlsLayout.addView(btnFullscreen)
            root.addView(playerControlsLayout)

            val seekLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(10), dp(5), dp(10), dp(5))
                setBackgroundColor(Color.parseColor("#DD000000"))
                gravity = Gravity.CENTER_VERTICAL
                visibility = View.GONE
            }
            val seekTextSize = if (isTv) sp(14f) else sp(12f)
            tvCurrentTime = TextView(this).apply { text = "00:00"; setTextColor(Color.WHITE); textSize = seekTextSize }
            seekBar = SeekBar(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { if (f) player.seekTo(p.toLong()) }
                    override fun onStartTrackingTouch(s: SeekBar?) {}
                    override fun onStopTrackingTouch(s: SeekBar?) {}
                })
            }
            tvTotalTime = TextView(this).apply { text = "00:00"; setTextColor(Color.WHITE); textSize = seekTextSize }
            seekLayout.addView(tvCurrentTime)
            seekLayout.addView(seekBar)
            seekLayout.addView(tvTotalTime)
            root.addView(seekLayout)

            progressBar = ProgressBar(this).apply {
                visibility = View.GONE
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(6))
            }
            root.addView(progressBar)

            rv = RecyclerView(this).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
                layoutManager = LinearLayoutManager(this@MainActivity)
                setBackgroundColor(t.bg)
            }
            root.addView(rv)

            val bottomBar = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(3), dp(10), dp(3), dp(if (isTv) 25 else 18))
                setBackgroundColor(t.bottomBar)
                gravity = Gravity.CENTER
            }
            val bottomTextSize = if (isTv) sp(11f) else sp(9f)
            btnHome = createBottomButton("🏠", "رئيسية", t, bottomTextSize) { switchTab("home") }
            btnLive = createBottomButton("📺", "مباشر", t, bottomTextSize) { switchTab("live") }
            btnMovies = createBottomButton("🎬", "أفلام", t, bottomTextSize) { switchTab("movies") }
            btnSeries = createBottomButton("🎭", "مسلسلات", t, bottomTextSize) { switchTab("series") }
            btnFavorites = createBottomButton("⭐", "مفضلة", t, bottomTextSize) { switchTab("favorites") }
            btnAccounts = createBottomButton("👤", "حسابات", t, bottomTextSize) { showAccountsDialog() }
            bottomBar.addView(btnHome)
            bottomBar.addView(btnLive)
            bottomBar.addView(btnMovies)
            bottomBar.addView(btnSeries)
            bottomBar.addView(btnFavorites)
            bottomBar.addView(btnAccounts)
            root.addView(bottomBar)

            setContentView(root)

            player = ExoPlayer.Builder(this).build()
            playerView.player = player
            player.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    isPlayerPlaying = isPlaying
                    btnPlayPause.text = if (isPlaying) "⏸️" else "▶️"
                    showPlayerControls()
                    if (isPlaying) watchStartTime = System.currentTimeMillis()
                    else if (watchStartTime > 0) {
                        saveWatchTime(System.currentTimeMillis() - watchStartTime)
                        watchStartTime = 0
                    }
                }
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        seekBar.max = player.duration.toInt()
                        tvTotalTime.text = formatTime(player.duration)
                    } else if (state == Player.STATE_ENDED) playNextChannel()
                }
            })
            thread {
                while (true) {
                    if (player.isPlaying) runOnUiThread {
                        seekBar.progress = player.currentPosition.toInt()
                        tvCurrentTime.text = formatTime(player.currentPosition)
                    }
                    Thread.sleep(500)
                }
            }

            loadFavorites()
            loadHistory()
            loadAccounts()
            registerDownloadReceiver()

            if (accounts.isNotEmpty()) {
                currentAccountIndex = prefs.getInt("current_account", 0)
                if (currentAccountIndex < accounts.size) {
                    server = accounts[currentAccountIndex]
                    switchTab("home")
                }
            } else showLoginDialog()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "حدث خطأ: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun createBottomButton(icon: String, label: String, theme: ThemeColors, size: Float, onClick: () -> Unit) = Button(this).apply {
        text = "$icon\n$label"
        textSize = size
        setTextColor(theme.textGray)
        setBackgroundColor(Color.TRANSPARENT)
        setTypeface(null, Typeface.BOLD)
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        gravity = Gravity.CENTER
        setOnClickListener { onClick() }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun sp(value: Float) = value * resources.displayMetrics.scaledDensity

    private fun formatTime(millis: Long): String {
        val s = (millis / 1000) % 60
        val m = (millis / (1000 * 60)) % 60
        val h = millis / (1000 * 60 * 60)
        return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
    }

    private fun toggleRecording() { if (isRecording) stopRecording() else startRecording() }

    private fun startRecording() {
        try {
            if (currentStreamUrl == null) { Toast.makeText(this, "لا يوجد بث للتسجيل", Toast.LENGTH_SHORT).show(); return }
            val dir = File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "Recordings"); dir.mkdirs()
            recordingFile = File(dir, "MN-DAZOU_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.ts")
            isRecording = true; btnRecord.text = "⏹️"; btnRecord.setTextColor(Color.RED); tvRecording.visibility = View.VISIBLE
            Toast.makeText(this, "🔴 بدأ التسجيل", Toast.LENGTH_SHORT).show()
            recordingThread = thread {
                try {
                    val input = URL(currentStreamUrl).openStream()
                    val output = FileOutputStream(recordingFile)
                    val buffer = ByteArray(4096)
                    var bytesRead = input.read(buffer)
                    while (isRecording && bytesRead != -1) { output.write(buffer, 0, bytesRead); bytesRead = input.read(buffer) }
                    output.close(); input.close()
                } catch (e: Exception) { runOnUiThread { Toast.makeText(this@MainActivity, "❌ خطأ في التسجيل", Toast.LENGTH_SHORT).show(); stopRecording() } }
            }
        } catch (e: Exception) { Toast.makeText(this, "❌ فشل بدء التسجيل", Toast.LENGTH_SHORT).show() }
    }

    private fun stopRecording() {
        isRecording = false; btnRecord.text = "🔴"; btnRecord.setTextColor(Color.WHITE); tvRecording.visibility = View.GONE
        recordingThread?.interrupt()
        recordingFile?.let { file -> val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE); intent.data = Uri.fromFile(file); sendBroadcast(intent); Toast.makeText(this, "✅ تم حفظ التسجيل: ${file.name}", Toast.LENGTH_LONG).show() }
    }

    private fun shareCurrentStream() {
        currentStreamName?.let { streamName ->
            val shareText = "شاهد معي على MN-DAZOU IPTV:\n📺 $streamName\n🔗 ${currentStreamUrl ?: ""}"
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, shareText); putExtra(Intent.EXTRA_SUBJECT, streamName) }, "مشاركة عبر"))
        } ?: Toast.makeText(this, "لا يوجد محتوى للمشاركة", Toast.LENGTH_SHORT).show()
    }

    private fun saveAccounts() { val json = JSONArray(); accounts.forEach { acc -> val obj = JSONObject(); obj.put("url", acc.url); obj.put("username", acc.username); obj.put("password", acc.password); json.put(obj) }; prefs.edit().putString("accounts", json.toString()).apply() }

    private fun loadAccounts() {
        try {
            val s = prefs.getString("accounts", "[]") ?: "[]"; val j = JSONArray(s); accounts.clear()
            for (i in 0 until j.length()) { val o = j.getJSONObject(i); accounts.add(XtreamServer(o.getString("url"), o.getString("username"), o.getString("password"))) }
        } catch (e: Exception) { accounts.clear(); prefs.edit().remove("accounts").apply() }
    }

    private fun showAccountsDialog() {
        if (accounts.isEmpty()) { showLoginDialog(); return }
        val names = accounts.mapIndexed { i, acc -> "👤 حساب ${i + 1}: ${acc.username}@${acc.url.removePrefix("http://").removePrefix("https://").substringBefore(":")}" }.toTypedArray()
        AlertDialog.Builder(this).setTitle("👤 إدارة الحسابات (${accounts.size})").setItems(names + arrayOf("➕ إضافة حساب", "🗑️ حذف كل الحسابات", "📊 إحصائيات المشاهدة")) { _, which ->
            when {
                which < accounts.size -> { currentAccountIndex = which; server = accounts[which]; prefs.edit().putInt("current_account", which).apply(); switchTab("home"); Toast.makeText(this, "✅ تم التبديل", Toast.LENGTH_SHORT).show() }
                which == accounts.size -> showLoginDialog()
                which == accounts.size + 1 -> { accounts.clear(); saveAccounts(); server = null; Toast.makeText(this, "🗑️ تم الحذف", Toast.LENGTH_SHORT).show(); showLoginDialog() }
                which == accounts.size + 2 -> showWatchStats()
            }
        }.show()
    }

    private fun saveWatchTime(duration: Long) {
        val today = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        prefs.edit().putLong("watch_$today", prefs.getLong("watch_$today", 0) + duration).apply()
        currentStreamName?.let { streamName -> prefs.edit().putInt("channel_count_$streamName", prefs.getInt("channel_count_$streamName", 0) + 1).apply() }
    }

    private fun showWatchStats() {
        val today = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val todayWatch = prefs.getLong("watch_$today", 0)
        val hours = todayWatch / (1000 * 60 * 60); val minutes = (todayWatch % (1000 * 60 * 60)) / (1000 * 60)
        val channelCounts = mutableMapOf<String, Int>()
        prefs.all.forEach { (key, value) -> if (key.startsWith("channel_count_") && value is Int) channelCounts[key.removePrefix("channel_count_")] = value }
        val topChannels = channelCounts.entries.sortedByDescending { entry -> entry.value }.take(5)
        val message = StringBuilder()
        message.appendLine("📊 إحصائيات اليوم:"); message.appendLine("⏱️ وقت المشاهدة: ${hours}h ${minutes}m"); message.appendLine()
        if (topChannels.isNotEmpty()) { message.appendLine("📺 الأكثر مشاهدة:"); topChannels.forEachIndexed { index, channelEntry -> message.appendLine("${index + 1}. ${channelEntry.key} (${channelEntry.value} مرة)") } }
        AlertDialog.Builder(this).setTitle("📊 إحصائيات المشاهدة").setMessage(message.toString()).setPositiveButton("حسناً", null).show()
    }

    private fun showPlayerControls() {
        val anim = AlphaAnimation(0f, 1f); anim.duration = 300
        (playerControlsLayout.parent as? ViewGroup)?.let { vg -> for (i in 0 until vg.childCount) { val c = vg.getChildAt(i); if (c is LinearLayout) { c.visibility = View.VISIBLE; c.startAnimation(anim) } } }
        tvChannelInfo.visibility = View.VISIBLE; tvChannelInfo.startAnimation(anim)
        tvChannelInfo.postDelayed({ hidePlayerControls() }, 5000)
    }

    private fun hidePlayerControls() {
        (playerControlsLayout.parent as? ViewGroup)?.let { vg -> for (i in 0 until vg.childCount) { val c = vg.getChildAt(i); if (c is LinearLayout) c.visibility = View.GONE } }
        tvChannelInfo.visibility = View.GONE
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?) = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_NEXT -> { playNextChannel(); true }
        KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MEDIA_PREVIOUS -> { playPreviousChannel(); true }
        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> { togglePlayPause(); true }
        KeyEvent.KEYCODE_VOLUME_UP -> { audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI); true }
        KeyEvent.KEYCODE_VOLUME_DOWN -> { audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI); true }
        KeyEvent.KEYCODE_MEDIA_RECORD -> { toggleRecording(); true }
        else -> super.onKeyDown(keyCode, event)
    }

    private fun togglePlayPause() { if (isPlayerPlaying) player.pause() else player.play() }
    private fun changeAspectRatio() { currentAspectRatio = (currentAspectRatio + 1) % 3; playerView.resizeMode = when (currentAspectRatio) { 0 -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT; 1 -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL; else -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM }; Toast.makeText(this, "📐 ${arrayOf("ملائم", "ملء", "تكبير")[currentAspectRatio]}", Toast.LENGTH_SHORT).show() }
    private fun toggleFullscreen() { if (playerView.layoutParams.height == dp(420)) { playerView.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT; Toast.makeText(this, "⛶ ملء الشاشة", Toast.LENGTH_SHORT).show() } else { playerView.layoutParams.height = dp(420); Toast.makeText(this, "📱 وضع عادي", Toast.LENGTH_SHORT).show() }; playerView.requestLayout() }
    private fun showQualityDialog() { AlertDialog.Builder(this).setTitle("🎯 جودة الفيديو").setItems(arrayOf("Auto", "4K", "1080p", "720p", "480p")) { _, _ -> Toast.makeText(this, "⚙️ تلقائي", Toast.LENGTH_SHORT).show() }.show() }
    private fun downloadCurrentStream() { currentStreamUrl?.let { url -> try { downloadId = downloadManager.enqueue(DownloadManager.Request(Uri.parse(url)).setTitle("MN-DAZOU IPTV").setDescription(currentStreamName ?: "فيديو").setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED).setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES, "MN-DAZOU_${System.currentTimeMillis()}.mp4")); Toast.makeText(this, "⬇️ جاري التحميل", Toast.LENGTH_SHORT).show() } catch (e: Exception) { Toast.makeText(this, "❌ فشل", Toast.LENGTH_SHORT).show() } } ?: Toast.makeText(this, "لا يوجد فيديو", Toast.LENGTH_SHORT).show() }

    private fun registerDownloadReceiver() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                if (i?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) == downloadId)
                    Toast.makeText(this@MainActivity, "✅ تم التحميل", Toast.LENGTH_SHORT).show()
            }
        }
        ContextCompat.registerReceiver(this, receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), ContextCompat.RECEIVER_EXPORTED)
    }

    private fun playNextChannel() { val channels = when (currentStreamType) { "live" -> liveChannels; "movie" -> vodMovies; else -> emptyList() }; if (channels.isEmpty() || currentStreamIndex < 0) return; playChannelAtIndex((currentStreamIndex + 1) % channels.size) }
    private fun playPreviousChannel() { val channels = when (currentStreamType) { "live" -> liveChannels; "movie" -> vodMovies; else -> emptyList() }; if (channels.isEmpty() || currentStreamIndex < 0) return; playChannelAtIndex(if (currentStreamIndex - 1 < 0) channels.size - 1 else currentStreamIndex - 1) }

    private fun playChannelAtIndex(index: Int) {
        val channels = when (currentStreamType) { "live" -> liveChannels; "movie" -> vodMovies; else -> emptyList() }
        if (index < 0 || index >= channels.size) return
        currentStreamIndex = index; val channelItem = channels[index]
        val channelName = when (currentStreamType) { "live" -> (channelItem as XtreamChannel).name; "movie" -> (channelItem as XtreamMovie).name; else -> "" }
        val url = when (currentStreamType) { "live" -> XtreamAPI.getStreamUrl(server!!, (channelItem as XtreamChannel).streamId, channelItem.containerExtension); "movie" -> XtreamAPI.getMovieUrl(server!!, (channelItem as XtreamMovie).streamId, channelItem.containerExtension); else -> "" }
        playStream(url, channelName); tvChannelInfo.text = "🎬 $channelName (${index + 1}/${channels.size})"
        prefs.edit().putInt("last_channel_index", index).putString("last_channel_type", currentStreamType).apply(); rv.smoothScrollToPosition(index)
    }

    private fun loadImage(url: String, callback: (Bitmap?) -> Unit) { imageCache[url]?.let { callback(it); return }; thread { try { val conn = URL(url).openConnection() as HttpURLConnection; conn.doInput = true; conn.connect(); val bitmap = BitmapFactory.decodeStream(conn.inputStream); bitmap?.let { imageCache[url] = it }; runOnUiThread { callback(bitmap) } } catch (e: Exception) { runOnUiThread { callback(null) } } } }

    private fun showSettingsDialog() { AlertDialog.Builder(this).setTitle("⚙️ الإعدادات").setItems(arrayOf("🎨 تغيير الثيم", "🔗 إعدادات Xtream", "📋 إضافة M3U", "📺 تحديث EPG", "📊 إحصائيات", "🗑️ مسح البيانات")) { _, w -> when (w) { 0 -> showThemeDialog(); 1 -> showLoginDialog(); 2 -> showM3uDialog(); 3 -> loadEpg(); 4 -> showWatchStats(); 5 -> { prefs.edit().clear().apply(); accounts.clear(); Toast.makeText(this, "تم المسح", Toast.LENGTH_SHORT).show(); recreate() } } }.show() }
    private fun showThemeDialog() { AlertDialog.Builder(this).setTitle("🎨 اختر الثيم").setItems(themes.values.map { themeItem -> themeItem.name }.toTypedArray()) { _, w -> prefs.edit().putString("theme", themes.keys.toList()[w]).apply(); Toast.makeText(this, "🔄 أعد التشغيل", Toast.LENGTH_LONG).show() }.show() }
    private fun showM3uDialog() { val e = EditText(this).apply { hint = "رابط M3U أو الصق المحتوى"; minLines = 3; setPadding(dp(30), dp(20), dp(30), dp(20)) }; AlertDialog.Builder(this).setTitle("📋 إضافة M3U").setView(e).setPositiveButton("تحميل") { _, _ -> val i = e.text.toString(); if (i.startsWith("http")) loadM3uFromUrl(i) else parseM3uContent(i) }.setNegativeButton("إلغاء", null).show() }
    private fun loadM3uFromUrl(url: String) { showLoading(); thread { try { val c = URL(url).readText(); runOnUiThread { hideLoading(); parseM3uContent(c) } } catch (ex: Exception) { runOnUiThread { hideLoading(); Toast.makeText(this, "❌ فشل", Toast.LENGTH_SHORT).show() } } } }
    private fun parseM3uContent(content: String) { val ch = mutableListOf<XtreamChannel>(); var nm = ""; for (l in content.split("\n")) { if (l.startsWith("#EXTINF")) nm = Regex(",(.+)").find(l)?.groupValues?.get(1)?.trim() ?: "قناة"; else if (l.startsWith("http")) ch.add(XtreamChannel(ch.size, nm, "live", "", "", "", "m3u", "ts")) }; liveChannels.addAll(ch); Toast.makeText(this, "✅ ${ch.size} قناة", Toast.LENGTH_SHORT).show(); updateLiveList() }

    private fun loadEpg() { server?.let { srv -> showLoading(); thread { try { val j = URL("${srv.url}/player_api.php?username=${srv.username}&password=${srv.password}&action=get_short_epg").readText(); epgData.clear(); val a = JSONObject(j).getJSONArray("epg_listings"); for (i in 0 until a.length()) { val o = a.getJSONObject(i); epgData.add(EpgProgram(o.optString("epg_id", ""), o.optString("title", ""), o.optString("start", ""), o.optString("end", ""), o.optString("description", ""))) }; runOnUiThread { hideLoading(); Toast.makeText(this, "📺 ${epgData.size} برنامج", Toast.LENGTH_SHORT).show(); showEpg() } } catch (ex: Exception) { runOnUiThread { hideLoading(); Toast.makeText(this, "❌ فشل EPG", Toast.LENGTH_SHORT).show() } } } } ?: Toast.makeText(this, "سجل دخول", Toast.LENGTH_SHORT).show() }

    private fun showEpg() {
        val grouped = epgData.groupBy { epgItem -> epgItem.channelId }; val keys = grouped.keys.toTypedArray()
        if (keys.isEmpty()) { Toast.makeText(this, "لا بيانات", Toast.LENGTH_SHORT).show(); return }
        AlertDialog.Builder(this).setTitle("📺 دليل البرامج").setItems(keys) { _, index ->
            val channel = keys[index]; val programs = grouped[channel] ?: emptyList()
            val titles = programs.map { prog -> "🕐 ${prog.startTime}-${prog.endTime}: ${prog.title}" }.toTypedArray()
            AlertDialog.Builder(this).setTitle(channel).setItems(titles, null).setPositiveButton("إغلاق", null).show()
        }.setPositiveButton("إغلاق", null).show()
    }

    private fun switchTab(tab: String) {
        currentCategory = tab; selectedCategoryId = null; isShowingCategories = true; btnBack.visibility = View.GONE; searchLayout.visibility = View.GONE
        val t = themes[currentTheme]!!
        btnHome.setTextColor(t.textGray); btnLive.setTextColor(t.textGray); btnMovies.setTextColor(t.textGray); btnSeries.setTextColor(t.textGray); btnFavorites.setTextColor(t.textGray)
        when (tab) { "home" -> { btnHome.setTextColor(t.accent); tvTitle.text = "🏠 MN-DAZOU IPTV"; showHomeScreen() }; "live" -> { btnLive.setTextColor(t.accent); tvTitle.text = "📺 البث المباشر"; searchLayout.visibility = View.VISIBLE; currentStreamType = "live"; loadLiveCategories() }; "movies" -> { btnMovies.setTextColor(t.accent); tvTitle.text = "🎬 الأفلام"; searchLayout.visibility = View.VISIBLE; currentStreamType = "movie"; loadVodCategories() }; "series" -> { btnSeries.setTextColor(t.accent); tvTitle.text = "🎭 المسلسلات"; searchLayout.visibility = View.VISIBLE; loadSeriesCategories() }; "favorites" -> { btnFavorites.setTextColor(t.accent); tvTitle.text = "⭐ المفضلة"; showFavorites() } }
    }

    private fun showHomeScreen() {
        val t = themes[currentTheme]!!; val items = mutableListOf<Any>()
        if (watchHistory.isNotEmpty()) { items.add("section_history"); items.addAll(watchHistory.takeLast(5).reversed()) }
        if (favorites.isNotEmpty()) { items.add("section_favorites"); items.addAll(favorites.take(5)) }
        items.add("section_quick"); items.add("quick_live"); items.add("quick_movies"); items.add("quick_series"); items.add("quick_epg"); items.add("quick_stats"); items.add("quick_recordings")
        if (items.isEmpty()) items.add("empty")
        rv.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun getItemViewType(p: Int) = if (items[p] is String) 0 else 1
            override fun onCreateViewHolder(p: ViewGroup, vt: Int): RecyclerView.ViewHolder {
                if (vt == 0) { val tv = TextView(p.context).apply { setPadding(dp(20), dp(15), dp(20), dp(10)); textSize = sp(if (isTv) 20f else 16f); setTextColor(t.accent); setTypeface(null, Typeface.BOLD) }; return object : RecyclerView.ViewHolder(tv) {} }
                else { val l = LinearLayout(p.context).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(20), dp(15), dp(20), dp(15)); gravity = Gravity.CENTER_VERTICAL; setBackgroundColor(t.card) }; val iv = ImageView(p.context).apply { layoutParams = LinearLayout.LayoutParams(dp(50), dp(50)); setBackgroundColor(t.activeTab) }; l.addView(iv); l.addView(TextView(p.context).apply { setPadding(dp(15), 0, 0, 0); textSize = sp(if (isTv) 18f else 15f); setTextColor(t.textWhite); setTypeface(null, Typeface.BOLD); layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) }); return object : RecyclerView.ViewHolder(l) {} }
            }
            override fun onBindViewHolder(h: RecyclerView.ViewHolder, p: Int) {
                val item = items[p]
                if (item is String) { val tv = h.itemView as TextView; when (item) { "section_history" -> tv.text = "🕐 آخر المشاهدات"; "section_favorites" -> tv.text = "⭐ المفضلة"; "section_quick" -> tv.text = "🚀 وصول سريع"; "quick_live" -> { tv.text = "📺 البث المباشر"; tv.setOnClickListener { switchTab("live") } }; "quick_movies" -> { tv.text = "🎬 الأفلام"; tv.setOnClickListener { switchTab("movies") } }; "quick_series" -> { tv.text = "🎭 المسلسلات"; tv.setOnClickListener { switchTab("series") } }; "quick_epg" -> { tv.text = "📋 دليل البرامج"; tv.setOnClickListener { loadEpg() } }; "quick_stats" -> { tv.text = "📊 إحصائيات المشاهدة"; tv.setOnClickListener { showWatchStats() } }; "quick_recordings" -> { tv.text = "📁 التسجيلات"; tv.setOnClickListener { showRecordings() } }; "empty" -> tv.text = "👋 أهلاً بك!" } }
                else if (item is HistoryItem) { (h.itemView as LinearLayout).let { l -> (l.getChildAt(1) as TextView).text = "🕐 ${item.name}"; l.setOnClickListener { playHistoryItem(item) }; if (item.icon.isNotEmpty()) loadImage(item.icon) { b -> (l.getChildAt(0) as ImageView).setImageBitmap(b ?: Bitmap.createBitmap(dp(50), dp(50), Bitmap.Config.ARGB_8888)) } } }
                else if (item is FavoriteItem) { (h.itemView as LinearLayout).let { l -> (l.getChildAt(1) as TextView).text = "⭐ ${item.name}"; l.setOnClickListener { playFavoriteItem(item) }; if (item.icon.isNotEmpty()) loadImage(item.icon) { b -> (l.getChildAt(0) as ImageView).setImageBitmap(b ?: Bitmap.createBitmap(dp(50), dp(50), Bitmap.Config.ARGB_8888)) } } }
            }
            override fun getItemCount() = items.size
        }
    }

    private fun showRecordings() {
        val dir = File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "Recordings"); dir.mkdirs()
        val files = dir.listFiles()?.filter { file -> file.isFile && file.name.endsWith(".ts") }?.sortedByDescending { file -> file.lastModified() } ?: emptyList()
        if (files.isEmpty()) { Toast.makeText(this, "لا توجد تسجيلات", Toast.LENGTH_SHORT).show(); return }
        AlertDialog.Builder(this).setTitle("📁 التسجيلات (${files.size})").setItems(files.map { file -> file.name }.toTypedArray()) { _, i ->
            val intent = Intent(Intent.ACTION_VIEW); intent.setDataAndType(Uri.fromFile(files[i]), "video/*"); intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            try { startActivity(intent) } catch (e: Exception) { Toast.makeText(this, "لا يوجد مشغل فيديو", Toast.LENGTH_SHORT).show() }
        }.setPositiveButton("إغلاق", null).show()
    }

    private fun showFavorites() { val t = themes[currentTheme]!!; rv.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() { override fun onCreateViewHolder(p: ViewGroup, vt: Int): RecyclerView.ViewHolder { val l = LinearLayout(p.context).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(20), dp(15), dp(20), dp(15)); gravity = Gravity.CENTER_VERTICAL; setBackgroundColor(t.card) }; l.addView(ImageView(p.context).apply { layoutParams = LinearLayout.LayoutParams(dp(45), dp(45)); setBackgroundColor(t.activeTab) }); l.addView(TextView(p.context).apply { setPadding(dp(15), 0, 0, 0); textSize = sp(if (isTv) 18f else 15f); setTextColor(t.textWhite); setTypeface(null, Typeface.BOLD); layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) }); l.addView(Button(p.context).apply { text = "❌"; setBackgroundColor(Color.TRANSPARENT) }); return object : RecyclerView.ViewHolder(l) {} } override fun onBindViewHolder(h: RecyclerView.ViewHolder, p: Int) { val l = (h.itemView as LinearLayout); val fav = favorites[p]; (l.getChildAt(1) as TextView).text = "⭐ ${fav.name}"; l.setOnClickListener { playFavoriteItem(fav) }; l.getChildAt(2).setOnClickListener { removeFavorite(fav); showFavorites() }; if (fav.icon.isNotEmpty()) loadImage(fav.icon) { b -> (l.getChildAt(0) as ImageView).setImageBitmap(b ?: Bitmap.createBitmap(dp(45), dp(45), Bitmap.Config.ARGB_8888)) } } override fun getItemCount() = favorites.size } }

    private fun goBackToCategories() { isShowingCategories = true; selectedCategoryId = null; btnBack.visibility = View.GONE; when (currentCategory) { "live" -> showLiveCategories(); "movies" -> showVodCategories(); "series" -> showSeriesCategories() } }

    private fun showLoginDialog() { val t = themes[currentTheme]!!; val d = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(50), dp(50), dp(50), dp(50)); setBackgroundColor(t.card) }; d.addView(TextView(this).apply { text = "⚙️ إضافة حساب Xtream"; textSize = sp(if (isTv) 24f else 20f); setTextColor(t.accent); setTypeface(null, Typeface.BOLD); gravity = Gravity.CENTER; setPadding(0, 0, 0, dp(30)) }); val es = EditText(this).apply { hint = "رابط السيرفر"; setHintTextColor(t.textGray); setTextColor(t.textWhite); setBackgroundColor(t.bg); setPadding(dp(30), dp(20), dp(30), dp(20)); setText("http://"); textSize = sp(if (isTv) 18f else 14f) }; val eu = EditText(this).apply { hint = "اسم المستخدم"; setHintTextColor(t.textGray); setTextColor(t.textWhite); setBackgroundColor(t.bg); setPadding(dp(30), dp(20), dp(30), dp(20)); textSize = sp(if (isTv) 18f else 14f) }; val ep = EditText(this).apply { hint = "كلمة المرور"; setHintTextColor(t.textGray); setTextColor(t.textWhite); setBackgroundColor(t.bg); setPadding(dp(30), dp(20), dp(30), dp(20)); textSize = sp(if (isTv) 18f else 14f) }; d.addView(es); d.addView(createSpacer(dp(10))); d.addView(eu); d.addView(createSpacer(dp(10))); d.addView(ep); AlertDialog.Builder(this).setView(d).setPositiveButton("حفظ") { _, _ -> val acc = XtreamServer(es.text.toString().trimEnd('/'), eu.text.toString(), ep.text.toString()); accounts.add(acc); currentAccountIndex = accounts.size - 1; server = acc; saveAccounts(); prefs.edit().putInt("current_account", currentAccountIndex).apply(); Toast.makeText(this, "✅ تم حفظ الحساب", Toast.LENGTH_SHORT).show(); switchTab("home") }.setNegativeButton("إلغاء", null).show() }
    private fun createSpacer(h: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h) }

    private fun performSearch() {
        val q = etSearch.text.toString().lowercase(); if (q.isEmpty()) return
        when (currentCategory) {
            "live" -> { val filtered = liveChannels.filter { liveChannel -> liveChannel.name.lowercase().contains(q) }; if (filtered.isNotEmpty()) { liveChannels.clear(); liveChannels.addAll(filtered); updateLiveList(); tvTitle.text = "🔍 $q" } else Toast.makeText(this, "لا نتائج", Toast.LENGTH_SHORT).show() }
            "movies" -> { val filtered = vodMovies.filter { movie -> movie.name.lowercase().contains(q) }; if (filtered.isNotEmpty()) { vodMovies.clear(); vodMovies.addAll(filtered); updateMoviesList(); tvTitle.text = "🔍 $q" } else Toast.makeText(this, "لا نتائج", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun addToFavorites(type: String, id: Int, name: String, icon: String = "") { if (favorites.none { fav -> fav.type == type && fav.id == id }) { favorites.add(FavoriteItem(type, id, name, icon)); saveFavorites(); Toast.makeText(this, "⭐ تم", Toast.LENGTH_SHORT).show() } else Toast.makeText(this, "موجود", Toast.LENGTH_SHORT).show() }
    private fun removeFavorite(item: FavoriteItem) { favorites.removeAll { fav -> fav.type == item.type && fav.id == item.id }; saveFavorites() }
    private fun saveFavorites() { val j = JSONArray(); favorites.forEach { fav -> val o = JSONObject(); o.put("type", fav.type); o.put("id", fav.id); o.put("name", fav.name); o.put("icon", fav.icon); j.put(o) }; prefs.edit().putString("favorites", j.toString()).apply() }

    private fun loadFavorites() {
        try { val s = prefs.getString("favorites", "[]") ?: "[]"; val j = JSONArray(s); favorites.clear(); for (i in 0 until j.length()) { val o = j.getJSONObject(i); favorites.add(FavoriteItem(o.getString("type"), o.getInt("id"), o.getString("name"), o.optString("icon", ""))) } } catch (e: Exception) { favorites.clear(); prefs.edit().remove("favorites").apply() }
    }

    private fun addToHistory(type: String, id: Int, name: String, icon: String = "") { watchHistory.removeAll { hist -> hist.type == type && hist.id == id }; watchHistory.add(HistoryItem(type, id, name, System.currentTimeMillis(), icon)); if (watchHistory.size > 20) watchHistory.removeAt(0); saveHistory() }
    private fun saveHistory() { val j = JSONArray(); watchHistory.forEach { hist -> val o = JSONObject(); o.put("type", hist.type); o.put("id", hist.id); o.put("name", hist.name); o.put("timestamp", hist.timestamp); o.put("icon", hist.icon); j.put(o) }; prefs.edit().putString("history", j.toString()).apply() }

    private fun loadHistory() {
        try { val s = prefs.getString("history", "[]") ?: "[]"; val j = JSONArray(s); watchHistory.clear(); for (i in 0 until j.length()) { val o = j.getJSONObject(i); watchHistory.add(HistoryItem(o.getString("type"), o.getInt("id"), o.getString("name"), o.getLong("timestamp"), o.optString("icon", ""))) } } catch (e: Exception) { watchHistory.clear(); prefs.edit().remove("history").apply() }
    }

    private fun playFavoriteItem(fav: FavoriteItem) { when (fav.type) { "live" -> { val u = XtreamAPI.getStreamUrl(server!!, fav.id); playStream(u, fav.name); addToHistory("live", fav.id, fav.name, fav.icon) }; "movie" -> { val u = XtreamAPI.getMovieUrl(server!!, fav.id); playStream(u, fav.name); addToHistory("movie", fav.id, fav.name, fav.icon) } } }
    private fun playHistoryItem(item: HistoryItem) { when (item.type) { "live" -> playStream(XtreamAPI.getStreamUrl(server!!, item.id), item.name); "movie" -> playStream(XtreamAPI.getMovieUrl(server!!, item.id), item.name) } }

    private fun loadLiveCategories() { server?.let { srv -> showLoading(); XtreamAPI.getLiveCategories(srv) { liveCategories.clear(); liveCategories.addAll(it); hideLoading(); if (it.isEmpty()) loadLiveStreams(null) else showLiveCategories() } } ?: showLoginDialog() }
    private fun loadVodCategories() { server?.let { srv -> showLoading(); XtreamAPI.getVodCategories(srv) { vodCategories.clear(); vodCategories.addAll(it); hideLoading(); if (it.isEmpty()) loadMovies(null) else showVodCategories() } } ?: showLoginDialog() }
    private fun loadSeriesCategories() { server?.let { srv -> showLoading(); XtreamAPI.getLiveCategories(srv) { seriesCategories.clear(); seriesCategories.addAll(it); hideLoading(); if (it.isEmpty()) loadSeriesList(null) else showSeriesCategories() } } ?: showLoginDialog() }

    private fun showLiveCategories() { val t = themes[currentTheme]!!; tvTitle.text = "📺 المجموعات (${liveCategories.size})"; rv.adapter = createCategoryAdapter(liveCategories, "📁", t.accent) { cat -> selectedCategoryId = cat.categoryId; isShowingCategories = false; btnBack.visibility = View.VISIBLE; tvTitle.text = "📺 ${cat.categoryName}"; loadLiveStreams(cat.categoryId) } }
    private fun showVodCategories() { val t = themes[currentTheme]!!; tvTitle.text = "🎬 المجموعات (${vodCategories.size})"; rv.adapter = createCategoryAdapter(vodCategories, "🎬", t.accent) { cat -> selectedCategoryId = cat.categoryId; isShowingCategories = false; btnBack.visibility = View.VISIBLE; tvTitle.text = "🎬 ${cat.categoryName}"; loadMovies(cat.categoryId) } }
    private fun showSeriesCategories() { val t = themes[currentTheme]!!; tvTitle.text = "🎭 المجموعات (${seriesCategories.size})"; rv.adapter = createCategoryAdapter(seriesCategories, "📺", t.accent) { cat -> selectedCategoryId = cat.categoryId; isShowingCategories = false; btnBack.visibility = View.VISIBLE; tvTitle.text = "🎭 ${cat.categoryName}"; loadSeriesList(cat.categoryId) } }

    private fun createCategoryAdapter(cats: List<XtreamCategory>, icon: String, color: Int, onClick: (XtreamCategory) -> Unit): RecyclerView.Adapter<RecyclerView.ViewHolder> { val t = themes[currentTheme]!!; return object : RecyclerView.Adapter<RecyclerView.ViewHolder>() { override fun onCreateViewHolder(p: ViewGroup, vt: Int): RecyclerView.ViewHolder { val l = LinearLayout(p.context).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(30), dp(25), dp(30), dp(25)); gravity = Gravity.CENTER_VERTICAL; setBackgroundColor(t.card) }; l.addView(TextView(p.context).apply { text = icon; textSize = sp(if (isTv) 28f else 24f) }); l.addView(TextView(p.context).apply { setPadding(dp(20), 0, 0, 0); textSize = sp(if (isTv) 20f else 16f); setTextColor(t.textWhite); setTypeface(null, Typeface.BOLD); layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) }); l.addView(TextView(p.context).apply { text = "→"; textSize = sp(if (isTv) 24f else 20f); setTextColor(color) }); return object : RecyclerView.ViewHolder(l) {} } override fun onBindViewHolder(h: RecyclerView.ViewHolder, p: Int) { val l = (h.itemView as LinearLayout); (l.getChildAt(1) as TextView).text = cats[p].categoryName; l.setOnClickListener { onClick(cats[p]) } } override fun getItemCount() = cats.size } }

    private fun loadLiveStreams(catId: String?) { server?.let { srv -> showLoading(); XtreamAPI.getLiveStreams(srv, catId) { hideLoading(); liveChannels.clear(); liveChannels.addAll(it); currentStreamIndex = -1; updateLiveList() } } }
    private fun loadMovies(catId: String?) { server?.let { srv -> showLoading(); XtreamAPI.getVodStreams(srv, catId) { hideLoading(); vodMovies.clear(); vodMovies.addAll(it); currentStreamIndex = -1; updateMoviesList() } } }
    private fun loadSeriesList(catId: String?) { server?.let { srv -> showLoading(); XtreamAPI.getSeries(srv, catId) { hideLoading(); seriesList.clear(); seriesList.addAll(it); updateSeriesList() } } }

    private fun showLoading() { progressBar.visibility = View.VISIBLE }
    private fun hideLoading() { progressBar.visibility = View.GONE }

    private fun updateLiveList() { val t = themes[currentTheme]!!; val mapped = liveChannels.map { ch -> Pair(ch.name, ch.streamIcon) }; rv.adapter = createContentAdapter(mapped, "📺", t) { itemName, imgUrl -> val idx = liveChannels.indexOfFirst { ch -> ch.name == itemName }; currentStreamIndex = idx; currentStreamType = "live"; val selectedChannel = liveChannels[idx]; val url = XtreamAPI.getStreamUrl(server!!, selectedChannel.streamId, selectedChannel.containerExtension); addToHistory("live", selectedChannel.streamId, selectedChannel.name, imgUrl); playStream(url, selectedChannel.name) } }
    private fun updateMoviesList() { val t = themes[currentTheme]!!; val mapped = vodMovies.map { m -> Pair(m.name, m.streamIcon) }; rv.adapter = createContentAdapter(mapped, "🎬", t) { itemName, imgUrl -> val idx = vodMovies.indexOfFirst { m -> m.name == itemName }; currentStreamIndex = idx; currentStreamType = "movie"; val selectedMovie = vodMovies[idx]; val url = XtreamAPI.getMovieUrl(server!!, selectedMovie.streamId, selectedMovie.containerExtension); addToHistory("movie", selectedMovie.streamId, selectedMovie.name, imgUrl); playStream(url, selectedMovie.name) } }
    private fun updateSeriesList() { val t = themes[currentTheme]!!; val mapped = seriesList.map { s -> Pair(s.name, s.cover) }; rv.adapter = createContentAdapter(mapped, "📺", t) { itemName, _ -> val series = seriesList.find { sr -> sr.name == itemName }!!; XtreamAPI.getSeriesInfo(server!!, series.seriesId) { episodes -> showEpisodesDialog(series.name, episodes) } } }

    private fun createContentAdapter(items: List<Pair<String, String>>, icon: String, theme: ThemeColors, onClick: (String, String) -> Unit): RecyclerView.Adapter<RecyclerView.ViewHolder> {
        return object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(p: ViewGroup, vt: Int): RecyclerView.ViewHolder {
                val l = LinearLayout(p.context).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(15), dp(12), dp(15), dp(12)); gravity = Gravity.CENTER_VERTICAL; setBackgroundColor(theme.card) }
                val iv = ImageView(p.context).apply { layoutParams = LinearLayout.LayoutParams(dp(55), dp(55)); setBackgroundColor(theme.activeTab) }
                l.addView(iv)
                l.addView(TextView(p.context).apply { setPadding(dp(12), 0, 0, 0); textSize = sp(if (isTv) 18f else 15f); setTextColor(theme.textWhite); setTypeface(null, Typeface.BOLD); layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) })
                l.addView(Button(p.context).apply { text = "⭐"; setBackgroundColor(Color.TRANSPARENT) })
                return object : RecyclerView.ViewHolder(l) {}
            }
            override fun onBindViewHolder(h: RecyclerView.ViewHolder, p: Int) {
                val l = (h.itemView as LinearLayout)
                val iv = l.getChildAt(0) as ImageView
                val tv = l.getChildAt(1) as TextView
                val itemName = items[p].first
                val imgUrl = items[p].second
                tv.text = "$icon $itemName"
                l.setOnClickListener { onClick(itemName, imgUrl) }
                l.getChildAt(2).setOnClickListener {
                    val type = if (currentStreamType == "live") "live" else "movie"
                    val id = if (type == "live") liveChannels.getOrNull(p)?.streamId ?: 0 else vodMovies.getOrNull(p)?.streamId ?: 0
                    addToFavorites(type, id, itemName, imgUrl)
                }
                if (imgUrl.isNotEmpty()) loadImage(imgUrl) { b -> iv.setImageBitmap(b ?: Bitmap.createBitmap(dp(55), dp(55), Bitmap.Config.ARGB_8888)) }
            }
            override fun getItemCount() = items.size
        }
    }

    private fun showEpisodesDialog(name: String, episodes: List<XtreamEpisode>) { AlertDialog.Builder(this).setTitle(name).setItems(episodes.map { ep -> "🎭 حلقة ${ep.episodeNum}: ${ep.title}" }.toTypedArray()) { _, i -> val e = episodes[i]; playStream(XtreamAPI.getSeriesEpisodeUrl(server!!, e.id, e.containerExtension), "$name - حلقة ${e.episodeNum}") }.setNegativeButton("إغلاق", null).show() }

    private fun playStream(url: String, name: String) {
        try {
            currentStreamUrl = url; currentStreamName = name
            player.setMediaItem(MediaItem.fromUri(url))
            player.prepare(); player.play()
            tvChannelInfo.text = "🎬 $name"
            watchStartTime = System.currentTimeMillis()
            Toast.makeText(this, "▶️ $name", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) { Toast.makeText(this, "❌ ${e.message}", Toast.LENGTH_LONG).show() }
    }

    override fun onDestroy() { if (isRecording) stopRecording(); super.onDestroy(); player.release() }
}