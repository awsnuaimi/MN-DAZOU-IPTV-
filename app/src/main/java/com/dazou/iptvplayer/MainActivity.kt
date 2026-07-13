package com.dazou.iptvplayer

import android.app.*
import android.content.*
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.*
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
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

// ✅ معالج الأخطاء العام
class GlobalExceptionHandler(private val context: Context, private val defaultHandler: Thread.UncaughtExceptionHandler) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val logFile = File(context.getExternalFilesDir(null), "crash_log.txt")
            val writer = PrintWriter(FileWriter(logFile, true))
            writer.println("========== ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())} ==========")
            throwable.printStackTrace(writer)
            writer.close()
        } catch (_: Exception) {}
        defaultHandler.uncaughtException(thread, throwable)
    }
}

class MainActivity : AppCompatActivity() {

    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView
    private lateinit var rv: RecyclerView
    private lateinit var tvTitle: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnLive: Button
    private lateinit var btnMovies: Button
    private lateinit var btnSeries: Button
    private lateinit var searchLayout: LinearLayout
    private lateinit var etSearch: EditText
    private lateinit var playerControlsLayout: LinearLayout
    private lateinit var btnPlayPause: Button
    private lateinit var btnFullscreen: Button
    private lateinit var seekBar: SeekBar
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvTotalTime: TextView
    private lateinit var tvChannelInfo: TextView

    private var server: XtreamServer? = null
    private var liveChannels = mutableListOf<XtreamChannel>()
    private var vodMovies = mutableListOf<XtreamMovie>()
    private var seriesList = mutableListOf<XtreamSeries>()
    private var liveCategories = mutableListOf<XtreamCategory>()
    private var vodCategories = mutableListOf<XtreamCategory>()
    private var seriesCategories = mutableListOf<XtreamCategory>()
    private var currentStreamUrl: String? = null
    private var currentStreamName: String? = null
    private var currentStreamIndex = -1
    private var currentStreamType = "live"
    private var isPlayerPlaying = false
    private var currentCategory = "live"
    private var selectedCategoryId: String? = null
    private var isShowingCategories = true

    private lateinit var prefs: SharedPreferences
    private var isTv = false

    private fun isTelevision(): Boolean {
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        return uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ تفعيل التقاط الأخطاء
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        if (defaultHandler != null) {
            Thread.setDefaultUncaughtExceptionHandler(GlobalExceptionHandler(this, defaultHandler))
        }

        // ✅ فحص إذا فيه كراش سابق
        checkForCrashLog()

        try {
            supportActionBar?.hide()
            isTv = isTelevision()
            prefs = getSharedPreferences("mndazou_prefs", Context.MODE_PRIVATE)

            val bgColor = Color.parseColor("#0F0F1A")
            val cardColor = Color.parseColor("#1A1A35")
            val accentColor = Color.parseColor("#FF6B6B")
            val textWhite = Color.parseColor("#FFFFFF")
            val textGray = Color.parseColor("#AAAAAA")

            val root = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(bgColor)
            }

            // Header
            val header = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(20), dp(if (isTv) 30 else 45), dp(20), dp(20))
                setBackgroundColor(cardColor)
                gravity = Gravity.CENTER_VERTICAL
            }
            tvTitle = TextView(this).apply {
                text = "MN-DAZOU IPTV"
                textSize = if (isTv) sp(28f) else sp(20f)
                setTextColor(accentColor)
                setTypeface(null, Typeface.BOLD)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            header.addView(tvTitle)
            header.addView(Button(this).apply {
                text = "⚙️"
                textSize = if (isTv) sp(22f) else sp(16f)
                setBackgroundColor(Color.TRANSPARENT)
                setTextColor(textGray)
                setOnClickListener { showSettingsDialog() }
            })
            root.addView(header)

            // Search
            searchLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(15), dp(10), dp(15), dp(10))
                setBackgroundColor(cardColor)
                gravity = Gravity.CENTER_VERTICAL
                visibility = View.GONE
            }
            etSearch = EditText(this).apply {
                hint = "🔍 بحث..."
                setHintTextColor(textGray)
                setTextColor(textWhite)
                setBackgroundColor(bgColor)
                setPadding(dp(25), dp(15), dp(25), dp(15))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                textSize = if (isTv) sp(18f) else sp(14f)
            }
            searchLayout.addView(etSearch)
            searchLayout.addView(Button(this).apply {
                text = "بحث"
                setBackgroundColor(accentColor)
                setTextColor(Color.BLACK)
                textSize = if (isTv) sp(18f) else sp(14f)
                setOnClickListener { performSearch() }
            })
            root.addView(searchLayout)

            // Player
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
            playerCard.addView(playerView)
            playerCard.addView(tvChannelInfo)
            root.addView(playerCard)

            // Controls
            playerControlsLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(15), dp(10), dp(15), dp(10))
                setBackgroundColor(Color.parseColor("#DD000000"))
                gravity = Gravity.CENTER
                visibility = View.GONE
            }
            val ctrlTextSize = if (isTv) sp(26f) else sp(22f)
            btnPlayPause = Button(this).apply {
                text = "▶️"
                setBackgroundColor(Color.TRANSPARENT)
                setTextColor(Color.WHITE)
                textSize = ctrlTextSize
                setOnClickListener { togglePlayPause() }
            }
            btnFullscreen = Button(this).apply {
                text = "⛶"
                setBackgroundColor(Color.TRANSPARENT)
                setTextColor(Color.WHITE)
                textSize = ctrlTextSize
                setOnClickListener { toggleFullscreen() }
            }
            playerControlsLayout.addView(btnPlayPause)
            playerControlsLayout.addView(btnFullscreen)
            root.addView(playerControlsLayout)

            // Seek
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

            // List
            rv = RecyclerView(this).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
                layoutManager = LinearLayoutManager(this@MainActivity)
                setBackgroundColor(bgColor)
            }
            root.addView(rv)

            // Bottom Tabs
            val bottomBar = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(3), dp(10), dp(3), dp(if (isTv) 25 else 18))
                setBackgroundColor(cardColor)
                gravity = Gravity.CENTER
            }
            val bottomTextSize = if (isTv) sp(11f) else sp(9f)
            btnLive = createBottomButton("📺", "مباشر", textGray, bottomTextSize) { switchTab("live") }
            btnMovies = createBottomButton("🎬", "أفلام", textGray, bottomTextSize) { switchTab("movies") }
            btnSeries = createBottomButton("🎭", "مسلسلات", textGray, bottomTextSize) { switchTab("series") }
            bottomBar.addView(btnLive)
            bottomBar.addView(btnMovies)
            bottomBar.addView(btnSeries)
            root.addView(bottomBar)

            setContentView(root)

            // Player Init
            player = ExoPlayer.Builder(this).build()
            playerView.player = player
            player.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    isPlayerPlaying = isPlaying
                    btnPlayPause.text = if (isPlaying) "⏸️" else "▶️"
                    playerControlsLayout.visibility = View.VISIBLE
                    (seekLayout.parent as ViewGroup).let { (it).visibility = View.VISIBLE }
                    tvChannelInfo.visibility = View.VISIBLE
                }
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        seekBar.max = player.duration.toInt()
                        tvTotalTime.text = formatTime(player.duration)
                    }
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

            // Load saved server
            val savedUrl = prefs.getString("server_url", "")
            if (!savedUrl.isNullOrEmpty()) {
                server = XtreamServer(
                    savedUrl!!,
                    prefs.getString("server_username", "")!!,
                    prefs.getString("server_password", "")!!
                )
                switchTab("live")
            } else {
                showLoginDialog()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            saveCrashLog(e)
            Toast.makeText(this, "خطأ: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkForCrashLog() {
        try {
            val logFile = File(getExternalFilesDir(null), "crash_log.txt")
            if (logFile.exists() && logFile.length() > 0) {
                val content = logFile.readText()
                if (content.isNotBlank()) {
                    AlertDialog.Builder(this)
                        .setTitle("⚠️ تم اكتشاف خطأ سابق")
                        .setMessage("هل تريد عرض تقرير الخطأ؟")
                        .setPositiveButton("عرض") { _, _ ->
                            AlertDialog.Builder(this)
                                .setTitle("📋 تقرير الخطأ")
                                .setMessage(content.takeLast(2000))
                                .setPositiveButton("مسح التقرير") { _, _ -> logFile.delete() }
                                .setNegativeButton("إغلاق", null)
                                .show()
                        }
                        .setNegativeButton("مسح") { _, _ -> logFile.delete() }
                        .setNeutralButton("لاحقاً", null)
                        .show()
                }
            }
        } catch (_: Exception) {}
    }

    private fun saveCrashLog(throwable: Throwable) {
        try {
            val logFile = File(getExternalFilesDir(null), "crash_log.txt")
            val writer = PrintWriter(FileWriter(logFile, true))
            writer.println("========== ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())} ==========")
            throwable.printStackTrace(writer)
            writer.close()
        } catch (_: Exception) {}
    }

    private fun createBottomButton(icon: String, label: String, color: Int, size: Float, onClick: () -> Unit) = Button(this).apply {
        text = "$icon\n$label"
        textSize = size
        setTextColor(color)
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

    private fun togglePlayPause() { if (isPlayerPlaying) player.pause() else player.play() }
    private fun toggleFullscreen() {
        if (playerView.layoutParams.height == dp(420)) {
            playerView.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            Toast.makeText(this, "⛶ ملء الشاشة", Toast.LENGTH_SHORT).show()
        } else {
            playerView.layoutParams.height = dp(420)
            Toast.makeText(this, "📱 وضع عادي", Toast.LENGTH_SHORT).show()
        }
        playerView.requestLayout()
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("⚙️ الإعدادات")
            .setItems(arrayOf("🔗 إعدادات Xtream", "📋 عرض تقرير الأخطاء")) { _, which ->
                when (which) {
                    0 -> showLoginDialog()
                    1 -> {
                        val logFile = File(getExternalFilesDir(null), "crash_log.txt")
                        if (logFile.exists()) {
                            AlertDialog.Builder(this)
                                .setTitle("📋 تقرير الأخطاء")
                                .setMessage(logFile.readText().takeLast(3000))
                                .setPositiveButton("مسح", { _, _ -> logFile.delete() })
                                .setNegativeButton("إغلاق", null)
                                .show()
                        } else {
                            Toast.makeText(this, "لا توجد أخطاء مسجلة ✅", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }.show()
    }

    private fun performSearch() {
        val q = etSearch.text.toString().lowercase()
        if (q.isEmpty()) return
        when (currentCategory) {
            "live" -> {
                val filtered = liveChannels.filter { it.name.lowercase().contains(q) }
                if (filtered.isNotEmpty()) { liveChannels.clear(); liveChannels.addAll(filtered); updateLiveList(); tvTitle.text = "🔍 $q" }
                else Toast.makeText(this, "لا نتائج", Toast.LENGTH_SHORT).show()
            }
            "movies" -> {
                val filtered = vodMovies.filter { it.name.lowercase().contains(q) }
                if (filtered.isNotEmpty()) { vodMovies.clear(); vodMovies.addAll(filtered); updateMoviesList(); tvTitle.text = "🔍 $q" }
                else Toast.makeText(this, "لا نتائج", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLoginDialog() {
        val d = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(50), dp(50), dp(50), dp(50))
            setBackgroundColor(Color.parseColor("#1A1A35"))
        }
        d.addView(TextView(this).apply {
            text = "⚙️ إضافة حساب Xtream"
            textSize = sp(if (isTv) 24f else 20f)
            setTextColor(Color.parseColor("#FF6B6B"))
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(30))
        })
        val es = EditText(this).apply { hint = "رابط السيرفر"; setHintTextColor(Color.parseColor("#AAAAAA")); setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#0F0F1A")); setPadding(dp(30), dp(20), dp(30), dp(20)); setText("http://"); textSize = sp(if (isTv) 18f else 14f) }
        val eu = EditText(this).apply { hint = "اسم المستخدم"; setHintTextColor(Color.parseColor("#AAAAAA")); setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#0F0F1A")); setPadding(dp(30), dp(20), dp(30), dp(20)); textSize = sp(if (isTv) 18f else 14f) }
        val ep = EditText(this).apply { hint = "كلمة المرور"; setHintTextColor(Color.parseColor("#AAAAAA")); setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#0F0F1A")); setPadding(dp(30), dp(20), dp(30), dp(20)); textSize = sp(if (isTv) 18f else 14f) }
        d.addView(es); d.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(10)) })
        d.addView(eu); d.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(10)) })
        d.addView(ep)
        AlertDialog.Builder(this).setView(d).setPositiveButton("حفظ") { _, _ ->
            server = XtreamServer(es.text.toString().trimEnd('/'), eu.text.toString(), ep.text.toString())
            prefs.edit().putString("server_url", server!!.url).putString("server_username", server!!.username).putString("server_password", server!!.password).apply()
            Toast.makeText(this, "✅ تم حفظ الحساب", Toast.LENGTH_SHORT).show()
            switchTab("live")
        }.setNegativeButton("إلغاء", null).show()
    }

    private fun switchTab(tab: String) {
        currentCategory = tab; selectedCategoryId = null; isShowingCategories = true
        searchLayout.visibility = View.VISIBLE
        when (tab) {
            "live" -> { tvTitle.text = "📺 البث المباشر"; currentStreamType = "live"; loadLiveCategories() }
            "movies" -> { tvTitle.text = "🎬 الأفلام"; currentStreamType = "movie"; loadVodCategories() }
            "series" -> { tvTitle.text = "🎭 المسلسلات"; loadSeriesCategories() }
        }
    }

    private fun loadLiveCategories() { server?.let { srv -> showLoading(); XtreamAPI.getLiveCategories(srv) { liveCategories.clear(); liveCategories.addAll(it); hideLoading(); if (it.isEmpty()) loadLiveStreams(null) else showLiveCategories() } } ?: showLoginDialog() }
    private fun loadVodCategories() { server?.let { srv -> showLoading(); XtreamAPI.getVodCategories(srv) { vodCategories.clear(); vodCategories.addAll(it); hideLoading(); if (it.isEmpty()) loadMovies(null) else showVodCategories() } } ?: showLoginDialog() }
    private fun loadSeriesCategories() { server?.let { srv -> showLoading(); XtreamAPI.getLiveCategories(srv) { seriesCategories.clear(); seriesCategories.addAll(it); hideLoading(); if (it.isEmpty()) loadSeriesList(null) else showSeriesCategories() } } ?: showLoginDialog() }

    private fun showLiveCategories() { tvTitle.text = "📺 المجموعات (${liveCategories.size})"; rv.adapter = createCategoryAdapter(liveCategories, "📁", Color.parseColor("#FF6B6B")) { cat -> selectedCategoryId = cat.categoryId; isShowingCategories = false; tvTitle.text = "📺 ${cat.categoryName}"; loadLiveStreams(cat.categoryId) } }
    private fun showVodCategories() { tvTitle.text = "🎬 المجموعات (${vodCategories.size})"; rv.adapter = createCategoryAdapter(vodCategories, "🎬", Color.parseColor("#FF6B6B")) { cat -> selectedCategoryId = cat.categoryId; isShowingCategories = false; tvTitle.text = "🎬 ${cat.categoryName}"; loadMovies(cat.categoryId) } }
    private fun showSeriesCategories() { tvTitle.text = "🎭 المجموعات (${seriesCategories.size})"; rv.adapter = createCategoryAdapter(seriesCategories, "📺", Color.parseColor("#FF6B6B")) { cat -> selectedCategoryId = cat.categoryId; isShowingCategories = false; tvTitle.text = "🎭 ${cat.categoryName}"; loadSeriesList(cat.categoryId) } }

    private fun createCategoryAdapter(cats: List<XtreamCategory>, icon: String, color: Int, onClick: (XtreamCategory) -> Unit): RecyclerView.Adapter<RecyclerView.ViewHolder> {
        return object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(p: ViewGroup, vt: Int): RecyclerView.ViewHolder {
                val l = LinearLayout(p.context).apply {
                    orientation = LinearLayout.HORIZONTAL; setPadding(dp(30), dp(25), dp(30), dp(25))
                    gravity = Gravity.CENTER_VERTICAL; setBackgroundColor(Color.parseColor("#1A1A35"))
                }
                l.addView(TextView(p.context).apply { text = icon; textSize = sp(if (isTv) 28f else 24f) })
                l.addView(TextView(p.context).apply { setPadding(dp(20), 0, 0, 0); textSize = sp(if (isTv) 20f else 16f); setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD); layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) })
                l.addView(TextView(p.context).apply { text = "→"; textSize = sp(if (isTv) 24f else 20f); setTextColor(color) })
                return object : RecyclerView.ViewHolder(l) {}
            }
            override fun onBindViewHolder(h: RecyclerView.ViewHolder, p: Int) {
                val l = (h.itemView as LinearLayout); (l.getChildAt(1) as TextView).text = cats[p].categoryName
                l.setOnClickListener { onClick(cats[p]) }
            }
            override fun getItemCount() = cats.size
        }
    }

    private fun loadLiveStreams(catId: String?) { server?.let { srv -> showLoading(); XtreamAPI.getLiveStreams(srv, catId) { hideLoading(); liveChannels.clear(); liveChannels.addAll(it); currentStreamIndex = -1; updateLiveList() } } }
    private fun loadMovies(catId: String?) { server?.let { srv -> showLoading(); XtreamAPI.getVodStreams(srv, catId) { hideLoading(); vodMovies.clear(); vodMovies.addAll(it); currentStreamIndex = -1; updateMoviesList() } } }
    private fun loadSeriesList(catId: String?) { server?.let { srv -> showLoading(); XtreamAPI.getSeries(srv, catId) { hideLoading(); seriesList.clear(); seriesList.addAll(it); updateSeriesList() } } }

    private fun showLoading() { progressBar.visibility = View.VISIBLE }
    private fun hideLoading() { progressBar.visibility = View.GONE }

    private fun updateLiveList() {
        val mapped = liveChannels.map { ch -> Pair(ch.name, ch.streamIcon) }
        rv.adapter = createContentAdapter(mapped, "📺") { itemName, _ ->
            val idx = liveChannels.indexOfFirst { ch -> ch.name == itemName }
            currentStreamIndex = idx; currentStreamType = "live"
            val selectedChannel = liveChannels[idx]
            val url = XtreamAPI.getStreamUrl(server!!, selectedChannel.streamId, selectedChannel.containerExtension)
            playStream(url, selectedChannel.name)
        }
    }

    private fun updateMoviesList() {
        val mapped = vodMovies.map { m -> Pair(m.name, m.streamIcon) }
        rv.adapter = createContentAdapter(mapped, "🎬") { itemName, _ ->
            val idx = vodMovies.indexOfFirst { m -> m.name == itemName }
            currentStreamIndex = idx; currentStreamType = "movie"
            val selectedMovie = vodMovies[idx]
            val url = XtreamAPI.getMovieUrl(server!!, selectedMovie.streamId, selectedMovie.containerExtension)
            playStream(url, selectedMovie.name)
        }
    }

    private fun updateSeriesList() {
        val mapped = seriesList.map { s -> Pair(s.name, s.cover) }
        rv.adapter = createContentAdapter(mapped, "📺") { itemName, _ ->
            val series = seriesList.find { sr -> sr.name == itemName }!!
            XtreamAPI.getSeriesInfo(server!!, series.seriesId) { episodes -> showEpisodesDialog(series.name, episodes) }
        }
    }

    private fun createContentAdapter(items: List<Pair<String, String>>, icon: String, onClick: (String, String) -> Unit): RecyclerView.Adapter<RecyclerView.ViewHolder> {
        return object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(p: ViewGroup, vt: Int): RecyclerView.ViewHolder {
                val l = LinearLayout(p.context).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(15), dp(12), dp(15), dp(12)); gravity = Gravity.CENTER_VERTICAL; setBackgroundColor(Color.parseColor("#1A1A35")) }
                val iv = ImageView(p.context).apply { layoutParams = LinearLayout.LayoutParams(dp(55), dp(55)); setBackgroundColor(Color.parseColor("#2D2D5E")) }
                l.addView(iv)
                l.addView(TextView(p.context).apply { setPadding(dp(12), 0, 0, 0); textSize = sp(if (isTv) 18f else 15f); setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD); layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) })
                return object : RecyclerView.ViewHolder(l) {}
            }
            override fun onBindViewHolder(h: RecyclerView.ViewHolder, p: Int) {
                val l = (h.itemView as LinearLayout)
                val tv = l.getChildAt(1) as TextView
                val itemName = items[p].first
                tv.text = "$icon $itemName"
                l.setOnClickListener { onClick(itemName, items[p].second) }
            }
            override fun getItemCount() = items.size
        }
    }

    private fun showEpisodesDialog(name: String, episodes: List<XtreamEpisode>) {
        AlertDialog.Builder(this).setTitle(name).setItems(episodes.map { ep -> "🎭 حلقة ${ep.episodeNum}: ${ep.title}" }.toTypedArray()) { _, i ->
            val e = episodes[i]; playStream(XtreamAPI.getSeriesEpisodeUrl(server!!, e.id, e.containerExtension), "$name - حلقة ${e.episodeNum}")
        }.setNegativeButton("إغلاق", null).show()
    }

    private fun playStream(url: String, name: String) {
        try {
            currentStreamUrl = url; currentStreamName = name
            player.setMediaItem(MediaItem.fromUri(url))
            player.prepare(); player.play()
            tvChannelInfo.text = "🎬 $name"
            Toast.makeText(this, "▶️ $name", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            saveCrashLog(e)
            Toast.makeText(this, "❌ ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() { super.onDestroy(); player.release() }
}