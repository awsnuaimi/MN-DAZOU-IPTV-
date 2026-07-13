package com.dazou.iptvplayer

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class MainActivity : AppCompatActivity() {

    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView
    private lateinit var rv: RecyclerView
    private lateinit var tvTitle: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnLive: Button
    private lateinit var btnMovies: Button
    private lateinit var btnSeries: Button
    private lateinit var btnBack: Button
    private lateinit var searchLayout: LinearLayout
    private lateinit var etSearch: EditText

    private var server: XtreamServer? = null
    private var liveChannels = mutableListOf<XtreamChannel>()
    private var vodMovies = mutableListOf<XtreamMovie>()
    private var seriesList = mutableListOf<XtreamSeries>()
    private var liveCategories = mutableListOf<XtreamCategory>()
    private var vodCategories = mutableListOf<XtreamCategory>()
    private var seriesCategories = mutableListOf<XtreamCategory>()
    private var currentStreamIndex = -1
    private var currentCategory = "live"
    private var selectedCategoryId: String? = null
    private var isShowingCategories = true
    private var isTv = false

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        prefs = getSharedPreferences("mndazou_prefs", Context.MODE_PRIVATE)
        isTv = (getSystemService(Context.UI_MODE_SERVICE) as? android.app.UiModeManager)?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION

        val bgColor = Color.parseColor("#0F0F1A")
        val cardColor = Color.parseColor("#1A1A35")
        val accentColor = Color.parseColor("#FF6B6B")
        val textWhite = Color.parseColor("#FFFFFF")
        val textGray = Color.parseColor("#AAAAAA")
        val activeTabColor = Color.parseColor("#2D2D5E")

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgColor)
        }

        // Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(20), dp(if (isTv) 30 else 45), dp(20), dp(15))
            setBackgroundColor(cardColor)
            gravity = Gravity.CENTER_VERTICAL
        }

        btnBack = Button(this).apply {
            text = "⬅️"
            textSize = sp(if (isTv) 22f else 18f)
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(textWhite)
            visibility = View.GONE
            setOnClickListener { goBack() }
        }
        header.addView(btnBack)

        tvTitle = TextView(this).apply {
            text = "MN-DAZOU IPTV"
            textSize = sp(if (isTv) 26f else 20f)
            setTextColor(accentColor)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        header.addView(tvTitle)

        header.addView(Button(this).apply {
            text = "⚙️"
            textSize = sp(if (isTv) 22f else 16f)
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(textGray)
            setOnClickListener { showLoginDialog() }
        })
        root.addView(header)

        // Search
        searchLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(15), dp(8), dp(15), dp(8))
            setBackgroundColor(cardColor)
            gravity = Gravity.CENTER_VERTICAL
        }
        etSearch = EditText(this).apply {
            hint = "🔍 بحث..."
            setHintTextColor(textGray)
            setTextColor(textWhite)
            setBackgroundColor(bgColor)
            setPadding(dp(25), dp(12), dp(25), dp(12))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            textSize = sp(if (isTv) 18f else 14f)
        }
        searchLayout.addView(etSearch)
        searchLayout.addView(Button(this).apply {
            text = "بحث"
            textSize = sp(if (isTv) 18f else 14f)
            setBackgroundColor(accentColor)
            setTextColor(Color.BLACK)
            setOnClickListener { performSearch() }
        })
        root.addView(searchLayout)

        // Player
        playerView = PlayerView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(if (isTv) 480 else 400))
            setBackgroundColor(Color.BLACK)
            useController = true
        }
        root.addView(playerView)

        // Tabs
        val tabLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setBackgroundColor(cardColor)
            gravity = Gravity.CENTER
        }
        val tabTextSize = sp(if (isTv) 16f else 14f)
        btnLive = createTabButton("📺 مباشر", tabTextSize) { switchTab("live") }
        btnMovies = createTabButton("🎬 أفلام", tabTextSize) { switchTab("movies") }
        btnSeries = createTabButton("🎭 مسلسلات", tabTextSize) { switchTab("series") }
        tabLayout.addView(btnLive)
        tabLayout.addView(btnMovies)
        tabLayout.addView(btnSeries)
        root.addView(tabLayout)

        // Progress
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

        setContentView(root)

        // Player Init
        player = ExoPlayer.Builder(this).build()
        playerView.player = player

        // Load saved server
        val savedUrl = prefs.getString("server_url", "")
        if (!savedUrl.isNullOrEmpty()) {
            server = XtreamServer(savedUrl!!, prefs.getString("server_username", "")!!, prefs.getString("server_password", "")!!)
            switchTab("live")
        } else {
            showLoginDialog()
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun sp(value: Float) = value * resources.displayMetrics.scaledDensity

    private fun createTabButton(text: String, size: Float, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text; textSize = size; setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT); setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { onClick() }
        }
    }

    private fun switchTab(tab: String) {
        currentCategory = tab; selectedCategoryId = null; isShowingCategories = true
        btnBack.visibility = View.GONE
        btnLive.setBackgroundColor(Color.TRANSPARENT); btnMovies.setBackgroundColor(Color.TRANSPARENT); btnSeries.setBackgroundColor(Color.TRANSPARENT)
        val activeColor = Color.parseColor("#2D2D5E")
        when (tab) {
            "live" -> { btnLive.setBackgroundColor(activeColor); tvTitle.text = "📺 البث المباشر"; loadLiveCategories() }
            "movies" -> { btnMovies.setBackgroundColor(activeColor); tvTitle.text = "🎬 الأفلام"; loadVodCategories() }
            "series" -> { btnSeries.setBackgroundColor(activeColor); tvTitle.text = "🎭 المسلسلات"; loadSeriesCategories() }
        }
    }

    private fun goBack() {
        isShowingCategories = true
        selectedCategoryId = null
        btnBack.visibility = View.GONE
        when (currentCategory) {
            "live" -> showLiveCategories()
            "movies" -> showVodCategories()
            "series" -> showSeriesCategories()
        }
    }

    // ===== D-Pad Support =====
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && !isShowingCategories) {
            goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    // ===== SEARCH =====
    private fun performSearch() {
        val q = etSearch.text.toString().lowercase()
        if (q.isEmpty()) return
        when (currentCategory) {
            "live" -> {
                val filtered = liveChannels.filter { it.name.lowercase().contains(q) }
                if (filtered.isNotEmpty()) { liveChannels.clear(); liveChannels.addAll(filtered); updateLiveList(); tvTitle.text = "🔍 $q (${filtered.size})" }
                else Toast.makeText(this, "لا نتائج", Toast.LENGTH_SHORT).show()
            }
            "movies" -> {
                val filtered = vodMovies.filter { it.name.lowercase().contains(q) }
                if (filtered.isNotEmpty()) { vodMovies.clear(); vodMovies.addAll(filtered); updateMoviesList(); tvTitle.text = "🔍 $q (${filtered.size})" }
                else Toast.makeText(this, "لا نتائج", Toast.LENGTH_SHORT).show()
            }
            "series" -> {
                val filtered = seriesList.filter { it.name.lowercase().contains(q) }
                if (filtered.isNotEmpty()) { seriesList.clear(); seriesList.addAll(filtered); updateSeriesList(); tvTitle.text = "🔍 $q (${filtered.size})" }
                else Toast.makeText(this, "لا نتائج", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ===== LOGIN =====
    private fun showLoginDialog() {
        val d = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(50), dp(50), dp(50), dp(50)); setBackgroundColor(Color.parseColor("#1A1A35")) }
        d.addView(TextView(this).apply { text = "⚙️ إضافة حساب Xtream"; textSize = sp(if (isTv) 24f else 20f); setTextColor(Color.parseColor("#FF6B6B")); setTypeface(null, Typeface.BOLD); gravity = Gravity.CENTER; setPadding(0, 0, 0, dp(30)) })
        val es = EditText(this).apply { hint = "رابط السيرفر"; setHintTextColor(Color.parseColor("#AAAAAA")); setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#0F0F1A")); setPadding(dp(30), dp(20), dp(30), dp(20)); setText("http://"); textSize = sp(if (isTv) 18f else 14f) }
        val eu = EditText(this).apply { hint = "اسم المستخدم"; setHintTextColor(Color.parseColor("#AAAAAA")); setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#0F0F1A")); setPadding(dp(30), dp(20), dp(30), dp(20)); textSize = sp(if (isTv) 18f else 14f) }
        val ep = EditText(this).apply { hint = "كلمة المرور"; setHintTextColor(Color.parseColor("#AAAAAA")); setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#0F0F1A")); setPadding(dp(30), dp(20), dp(30), dp(20)); textSize = sp(if (isTv) 18f else 14f) }
        d.addView(es); d.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(10)) })
        d.addView(eu); d.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(10)) })
        d.addView(ep)
        AlertDialog.Builder(this).setView(d).setPositiveButton("حفظ") { _, _ ->
            server = XtreamServer(es.text.toString().trimEnd('/'), eu.text.toString(), ep.text.toString())
            prefs.edit().putString("server_url", server!!.url).putString("server_username", server!!.username).putString("server_password", server!!.password).apply()
            Toast.makeText(this, "✅ تم الحفظ", Toast.LENGTH_SHORT).show(); switchTab("live")
        }.setNegativeButton("إلغاء", null).setCancelable(false).show()
    }

    // ===== LIVE =====
    private fun loadLiveCategories() { server?.let { srv -> showLoading(); XtreamAPI.getLiveCategories(srv) { liveCategories.clear(); liveCategories.addAll(it); if (it.isEmpty()) loadLiveStreams(null) else showLiveCategories() } } ?: showLoginDialog() }
    private fun showLiveCategories() { isShowingCategories = true; hideLoading(); tvTitle.text = "📺 المجموعات (${liveCategories.size})"; rv.adapter = createCategoryAdapter(liveCategories) { cat -> selectedCategoryId = cat.categoryId; isShowingCategories = false; btnBack.visibility = View.VISIBLE; tvTitle.text = "📺 ${cat.categoryName}"; loadLiveStreams(cat.categoryId) } }
    private fun loadLiveStreams(catId: String?) { server?.let { srv -> showLoading(); XtreamAPI.getLiveStreams(srv, catId) { liveChannels.clear(); liveChannels.addAll(it); updateLiveList() } } }
    private fun updateLiveList() { hideLoading(); tvTitle.text = "${tvTitle.text} (${liveChannels.size})"; rv.adapter = createChannelAdapter(liveChannels.map { it.name }) { name -> val ch = liveChannels.find { it.name == name }!!; val url = XtreamAPI.getStreamUrl(server!!, ch.streamId, ch.containerExtension); playStream(url, ch.name) } }

    // ===== VOD =====
    private fun loadVodCategories() { server?.let { srv -> showLoading(); XtreamAPI.getVodCategories(srv) { vodCategories.clear(); vodCategories.addAll(it); if (it.isEmpty()) loadMovies(null) else showVodCategories() } } ?: showLoginDialog() }
    private fun showVodCategories() { isShowingCategories = true; hideLoading(); tvTitle.text = "🎬 المجموعات (${vodCategories.size})"; rv.adapter = createCategoryAdapter(vodCategories) { cat -> selectedCategoryId = cat.categoryId; isShowingCategories = false; btnBack.visibility = View.VISIBLE; tvTitle.text = "🎬 ${cat.categoryName}"; loadMovies(cat.categoryId) } }
    private fun loadMovies(catId: String?) { server?.let { srv -> showLoading(); XtreamAPI.getVodStreams(srv, catId) { vodMovies.clear(); vodMovies.addAll(it); updateMoviesList() } } }
    private fun updateMoviesList() { hideLoading(); tvTitle.text = "${tvTitle.text} (${vodMovies.size})"; rv.adapter = createChannelAdapter(vodMovies.map { it.name }) { name -> val m = vodMovies.find { it.name == name }!!; val url = XtreamAPI.getMovieUrl(server!!, m.streamId, m.containerExtension); playStream(url, m.name) } }

    // ===== SERIES =====
    private fun loadSeriesCategories() { server?.let { srv -> showLoading(); XtreamAPI.getLiveCategories(srv) { seriesCategories.clear(); seriesCategories.addAll(it); if (it.isEmpty()) loadSeriesList(null) else showSeriesCategories() } } ?: showLoginDialog() }
    private fun showSeriesCategories() { isShowingCategories = true; hideLoading(); tvTitle.text = "🎭 المجموعات (${seriesCategories.size})"; rv.adapter = createCategoryAdapter(seriesCategories) { cat -> selectedCategoryId = cat.categoryId; isShowingCategories = false; btnBack.visibility = View.VISIBLE; tvTitle.text = "🎭 ${cat.categoryName}"; loadSeriesList(cat.categoryId) } }
    private fun loadSeriesList(catId: String?) { server?.let { srv -> showLoading(); XtreamAPI.getSeries(srv, catId) { seriesList.clear(); seriesList.addAll(it); updateSeriesList() } } }
    private fun updateSeriesList() { hideLoading(); tvTitle.text = "${tvTitle.text} (${seriesList.size})"; rv.adapter = createChannelAdapter(seriesList.map { it.name }) { name -> val s = seriesList.find { it.name == name }!!; XtreamAPI.getSeriesInfo(server!!, s.seriesId) { episodes -> showEpisodesDialog(s.name, episodes) } } }

    // ===== ADAPTERS =====
    private fun createCategoryAdapter(cats: List<XtreamCategory>, onClick: (XtreamCategory) -> Unit): RecyclerView.Adapter<RecyclerView.ViewHolder> {
        return object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val l = LinearLayout(parent.context).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(30), dp(25), dp(30), dp(25)); gravity = Gravity.CENTER_VERTICAL; setBackgroundColor(Color.parseColor("#1A1A35")) }
                l.addView(TextView(parent.context).apply { text = "📁"; textSize = sp(if (isTv) 28f else 24f) })
                l.addView(TextView(parent.context).apply { setPadding(dp(20), 0, 0, 0); textSize = sp(if (isTv) 20f else 16f); setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD); layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) })
                l.addView(TextView(parent.context).apply { text = "→"; textSize = sp(if (isTv) 24f else 20f); setTextColor(Color.parseColor("#FF6B6B")) })
                return object : RecyclerView.ViewHolder(l) {}
            }
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) { val l = (holder.itemView as LinearLayout); (l.getChildAt(1) as TextView).text = cats[pos].categoryName; l.setOnClickListener { onClick(cats[pos]) } }
            override fun getItemCount() = cats.size
        }
    }

    private fun createChannelAdapter(names: List<String>, onClick: (String) -> Unit): RecyclerView.Adapter<RecyclerView.ViewHolder> {
        return object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val tv = TextView(parent.context).apply { setPadding(dp(30), dp(25), dp(30), dp(25)); textSize = sp(if (isTv) 18f else 15f); setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD); setBackgroundColor(Color.parseColor("#1A1A35")) }
                return object : RecyclerView.ViewHolder(tv) {}
            }
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) { val tv = holder.itemView as TextView; tv.text = names[pos]; tv.setOnClickListener { onClick(names[pos]) } }
            override fun getItemCount() = names.size
        }
    }

    private fun showEpisodesDialog(name: String, episodes: List<XtreamEpisode>) {
        AlertDialog.Builder(this).setTitle(name).setItems(episodes.map { "🎭 حلقة ${it.episodeNum}: ${it.title}" }.toTypedArray()) { _, i ->
            val e = episodes[i]; val url = XtreamAPI.getSeriesEpisodeUrl(server!!, e.id, e.containerExtension); playStream(url, "$name - حلقة ${e.episodeNum}")
        }.setNegativeButton("إغلاق", null).show()
    }

    private fun playStream(url: String, name: String) {
        try { player.setMediaItem(MediaItem.fromUri(Uri.parse(url))); player.prepare(); player.play(); Toast.makeText(this, "▶️ $name", Toast.LENGTH_SHORT).show() }
        catch (e: Exception) { Toast.makeText(this, "❌ ${e.message}", Toast.LENGTH_LONG).show() }
    }

    private fun showLoading() { progressBar.visibility = View.VISIBLE }
    private fun hideLoading() { progressBar.visibility = View.GONE }

    override fun onDestroy() { super.onDestroy(); player.release() }
}