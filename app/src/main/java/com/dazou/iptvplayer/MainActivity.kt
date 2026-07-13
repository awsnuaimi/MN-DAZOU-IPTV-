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
    private lateinit var btnLive: Button
    private lateinit var btnMovies: Button
    private lateinit var btnSeries: Button
    private lateinit var btnFavorites: Button
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
    private var favorites = mutableListOf<FavoriteItem>()
    private var watchHistory = mutableListOf<HistoryItem>()
    private var currentStreamIndex = -1
    private var currentCategory = "live"
    private var selectedCategoryId: String? = null
    private var isShowingCategories = true
    private var isTv = false
    private var currentTheme = "dark"

    private lateinit var prefs: SharedPreferences
    private lateinit var themes: Map<String, ThemeColors>

    data class FavoriteItem(val type: String, val id: Int, val name: String, val icon: String = "")
    data class HistoryItem(val type: String, val id: Int, val name: String, val timestamp: Long, val icon: String = "")
    data class ThemeColors(val name: String, val bg: Int, val card: Int, val accent: Int, val bottomBar: Int, val textWhite: Int, val textGray: Int, val activeTab: Int)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        prefs = getSharedPreferences("mndazou_prefs", Context.MODE_PRIVATE)
        isTv = (getSystemService(Context.UI_MODE_SERVICE) as? android.app.UiModeManager)?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION

        // Load theme
        currentTheme = prefs.getString("theme", "dark") ?: "dark"
        themes = mapOf(
            "dark" to ThemeColors("داكن", Color.parseColor("#0F0F1A"), Color.parseColor("#1A1A35"), Color.parseColor("#FF6B6B"), Color.parseColor("#12122A"), Color.parseColor("#FFFFFF"), Color.parseColor("#AAAAAA"), Color.parseColor("#2D2D5E")),
            "blue" to ThemeColors("أزرق", Color.parseColor("#0A1628"), Color.parseColor("#1B2D4A"), Color.parseColor("#4FC3F7"), Color.parseColor("#0D1F3C"), Color.parseColor("#FFFFFF"), Color.parseColor("#90CAF9"), Color.parseColor("#1565C0")),
            "green" to ThemeColors("أخضر", Color.parseColor("#0A1F0A"), Color.parseColor("#1A3A1A"), Color.parseColor("#66BB6A"), Color.parseColor("#0D2A0D"), Color.parseColor("#FFFFFF"), Color.parseColor("#A5D6A7"), Color.parseColor("#2E7D32")),
            "purple" to ThemeColors("بنفسجي", Color.parseColor("#1A0A2E"), Color.parseColor("#2D1B4E"), Color.parseColor("#CE93D8"), Color.parseColor("#1F0D3D"), Color.parseColor("#FFFFFF"), Color.parseColor("#E1BEE7"), Color.parseColor("#6A1B9A")),
            "red" to ThemeColors("أحمر", Color.parseColor("#1A0A0A"), Color.parseColor("#3A1A1A"), Color.parseColor("#EF5350"), Color.parseColor("#2A0D0D"), Color.parseColor("#FFFFFF"), Color.parseColor("#EF9A9A"), Color.parseColor("#C62828"))
        )
        val t = themes[currentTheme]!!

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(t.bg)
        }

        // Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(20), dp(if (isTv) 30 else 45), dp(20), dp(15))
            setBackgroundColor(t.bottomBar)
            gravity = Gravity.CENTER_VERTICAL
        }
        btnBack = Button(this).apply { text = "⬅️"; textSize = sp(if (isTv) 22f else 18f); setBackgroundColor(Color.TRANSPARENT); setTextColor(t.textWhite); visibility = View.GONE; setOnClickListener { goBack() } }
        header.addView(btnBack)
        tvTitle = TextView(this).apply { text = "MN-DAZOU IPTV"; textSize = sp(if (isTv) 26f else 20f); setTextColor(t.accent); setTypeface(null, Typeface.BOLD); gravity = Gravity.CENTER; layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) }
        header.addView(tvTitle)
        header.addView(Button(this).apply { text = "🎨"; textSize = sp(if (isTv) 22f else 16f); setBackgroundColor(Color.TRANSPARENT); setTextColor(t.textGray); setOnClickListener { showThemeDialog() } })
        header.addView(Button(this).apply { text = "⚙️"; textSize = sp(if (isTv) 22f else 16f); setBackgroundColor(Color.TRANSPARENT); setTextColor(t.textGray); setOnClickListener { showLoginDialog() } })
        root.addView(header)

        // Search
        searchLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(15), dp(8), dp(15), dp(8)); setBackgroundColor(t.bottomBar); gravity = Gravity.CENTER_VERTICAL }
        etSearch = EditText(this).apply { hint = "🔍 بحث..."; setHintTextColor(t.textGray); setTextColor(t.textWhite); setBackgroundColor(t.bg); setPadding(dp(25), dp(12), dp(25), dp(12)); layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f); textSize = sp(if (isTv) 18f else 14f) }
        searchLayout.addView(etSearch)
        searchLayout.addView(Button(this).apply { text = "بحث"; textSize = sp(if (isTv) 18f else 14f); setBackgroundColor(t.accent); setTextColor(Color.BLACK); setOnClickListener { performSearch() } })
        root.addView(searchLayout)

        // Player
        playerView = PlayerView(this).apply { layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(if (isTv) 480 else 400)); setBackgroundColor(Color.BLACK); useController = true }
        root.addView(playerView)

        // Tabs
        val tabLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(10), dp(10), dp(10), dp(10)); setBackgroundColor(t.bottomBar); gravity = Gravity.CENTER }
        val tabTextSize = sp(if (isTv) 16f else 14f)
        btnLive = createTabButton("📺 مباشر", tabTextSize, t.textGray) { switchTab("live") }
        btnMovies = createTabButton("🎬 أفلام", tabTextSize, t.textGray) { switchTab("movies") }
        btnSeries = createTabButton("🎭 مسلسلات", tabTextSize, t.textGray) { switchTab("series") }
        btnFavorites = createTabButton("⭐ مفضلة", tabTextSize, t.textGray) { switchTab("favorites") }
        tabLayout.addView(btnLive); tabLayout.addView(btnMovies); tabLayout.addView(btnSeries); tabLayout.addView(btnFavorites)
        root.addView(tabLayout)

        // Progress
        progressBar = ProgressBar(this).apply { visibility = View.GONE; layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(6)) }
        root.addView(progressBar)

        // List
        rv = RecyclerView(this).apply { layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f); layoutManager = LinearLayoutManager(this@MainActivity); setBackgroundColor(t.bg) }
        root.addView(rv)

        setContentView(root)

        player = ExoPlayer.Builder(this).build()
        playerView.player = player

        loadFavorites()
        loadHistory()

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

    private fun createTabButton(text: String, size: Float, color: Int, onClick: () -> Unit): Button {
        return Button(this).apply { this.text = text; textSize = size; setTextColor(color); setBackgroundColor(Color.TRANSPARENT); setTypeface(null, Typeface.BOLD); layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f); setOnClickListener { onClick() } }
    }

    // ===== THEME =====
    private fun showThemeDialog() {
        AlertDialog.Builder(this).setTitle("🎨 اختر الثيم")
            .setItems(themes.values.map { it.name }.toTypedArray()) { _, w ->
                val themeKey = themes.keys.toList()[w]
                prefs.edit().putString("theme", themeKey).apply()
                Toast.makeText(this, "🔄 أعد تشغيل التطبيق", Toast.LENGTH_LONG).show()
            }.show()
    }

    // ===== SWITCH TAB =====
    private fun switchTab(tab: String) {
        currentCategory = tab; selectedCategoryId = null; isShowingCategories = true
        btnBack.visibility = View.GONE
        val t = themes[currentTheme]!!
        btnLive.setTextColor(t.textGray); btnMovies.setTextColor(t.textGray); btnSeries.setTextColor(t.textGray); btnFavorites.setTextColor(t.textGray)
        when (tab) {
            "live" -> { btnLive.setTextColor(t.accent); tvTitle.text = "📺 البث المباشر"; loadLiveCategories() }
            "movies" -> { btnMovies.setTextColor(t.accent); tvTitle.text = "🎬 الأفلام"; loadVodCategories() }
            "series" -> { btnSeries.setTextColor(t.accent); tvTitle.text = "🎭 المسلسلات"; loadSeriesCategories() }
            "favorites" -> { btnFavorites.setTextColor(t.accent); tvTitle.text = "⭐ المفضلة"; showFavorites() }
        }
    }

    private fun goBack() { isShowingCategories = true; selectedCategoryId = null; btnBack.visibility = View.GONE; when (currentCategory) { "live" -> showLiveCategories(); "movies" -> showVodCategories(); "series" -> showSeriesCategories() } }
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean { if (keyCode == KeyEvent.KEYCODE_BACK && !isShowingCategories) { goBack(); return true }; return super.onKeyDown(keyCode, event) }

    // ===== FAVORITES =====
    private fun addToFavorites(type: String, id: Int, name: String, icon: String = "") {
        if (favorites.none { it.type == type && it.id == id }) {
            favorites.add(FavoriteItem(type, id, name, icon))
            saveFavorites()
            Toast.makeText(this, "⭐ تمت الإضافة للمفضلة", Toast.LENGTH_SHORT).show()
        }
    }

    private fun removeFavorite(item: FavoriteItem) { favorites.removeAll { it.type == item.type && it.id == item.id }; saveFavorites() }
    private fun saveFavorites() { val j = JSONArray(); favorites.forEach { val o = JSONObject(); o.put("type", it.type); o.put("id", it.id); o.put("name", it.name); o.put("icon", it.icon); j.put(o) }; prefs.edit().putString("favorites", j.toString()).apply() }

    private fun loadFavorites() {
        try { val s = prefs.getString("favorites", "[]") ?: "[]"; val j = JSONArray(s); favorites.clear(); for (i in 0 until j.length()) { val o = j.getJSONObject(i); favorites.add(FavoriteItem(o.getString("type"), o.getInt("id"), o.getString("name"), o.optString("icon", ""))) } } catch (e: Exception) { favorites.clear() }
    }

    private fun showFavorites() {
        isShowingCategories = true
        val t = themes[currentTheme]!!
        rv.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val l = LinearLayout(parent.context).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(30), dp(20), dp(30), dp(20)); gravity = Gravity.CENTER_VERTICAL; setBackgroundColor(t.card) }
                l.addView(TextView(parent.context).apply { textSize = sp(if (isTv) 18f else 15f); setTextColor(t.textWhite); setTypeface(null, Typeface.BOLD); layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) })
                l.addView(Button(parent.context).apply { text = "❌"; setBackgroundColor(Color.TRANSPARENT); setTextColor(Color.RED) })
                return object : RecyclerView.ViewHolder(l) {}
            }
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
                val l = (holder.itemView as LinearLayout)
                val fav = favorites[pos]
                (l.getChildAt(0) as TextView).text = "⭐ ${fav.name}"
                l.setOnClickListener { playFavoriteItem(fav) }
                l.getChildAt(1).setOnClickListener { removeFavorite(fav); showFavorites() }
            }
            override fun getItemCount() = favorites.size
        }
    }

    // ===== HISTORY =====
    private fun addToHistory(type: String, id: Int, name: String, icon: String = "") {
        watchHistory.removeAll { it.type == type && it.id == id }
        watchHistory.add(HistoryItem(type, id, name, System.currentTimeMillis(), icon))
        if (watchHistory.size > 20) watchHistory.removeAt(0)
        saveHistory()
    }

    private fun saveHistory() { val j = JSONArray(); watchHistory.forEach { val o = JSONObject(); o.put("type", it.type); o.put("id", it.id); o.put("name", it.name); o.put("timestamp", it.timestamp); o.put("icon", it.icon); j.put(o) }; prefs.edit().putString("history", j.toString()).apply() }

    private fun loadHistory() {
        try { val s = prefs.getString("history", "[]") ?: "[]"; val j = JSONArray(s); watchHistory.clear(); for (i in 0 until j.length()) { val o = j.getJSONObject(i); watchHistory.add(HistoryItem(o.getString("type"), o.getInt("id"), o.getString("name"), o.getLong("timestamp"), o.optString("icon", ""))) } } catch (e: Exception) { watchHistory.clear() }
    }

    private fun playFavoriteItem(fav: FavoriteItem) {
        when (fav.type) {
            "live" -> { val url = XtreamAPI.getStreamUrl(server!!, fav.id); playStream(url, fav.name); addToHistory("live", fav.id, fav.name) }
            "movie" -> { val url = XtreamAPI.getMovieUrl(server!!, fav.id); playStream(url, fav.name); addToHistory("movie", fav.id, fav.name) }
        }
    }

    // ===== SEARCH =====
    private fun performSearch() { /* نفس الكود السابق */ }

    // ===== LOGIN =====
    private fun showLoginDialog() { /* نفس الكود السابق */ }

    // ===== LIVE =====
    private fun loadLiveCategories() { server?.let { srv -> showLoading(); XtreamAPI.getLiveCategories(srv) { liveCategories.clear(); liveCategories.addAll(it); if (it.isEmpty()) loadLiveStreams(null) else showLiveCategories() } } ?: showLoginDialog() }
    private fun showLiveCategories() { isShowingCategories = true; hideLoading(); tvTitle.text = "📺 المجموعات (${liveCategories.size})"; rv.adapter = createCategoryAdapter(liveCategories) { cat -> selectedCategoryId = cat.categoryId; isShowingCategories = false; btnBack.visibility = View.VISIBLE; tvTitle.text = "📺 ${cat.categoryName}"; loadLiveStreams(cat.categoryId) } }
    private fun loadLiveStreams(catId: String?) { server?.let { srv -> showLoading(); XtreamAPI.getLiveStreams(srv, catId) { liveChannels.clear(); liveChannels.addAll(it); updateLiveList() } } }
    private fun updateLiveList() { hideLoading(); tvTitle.text = "${tvTitle.text} (${liveChannels.size})"; rv.adapter = createChannelAdapter(liveChannels.map { it.name }) { name -> val ch = liveChannels.find { it.name == name }!!; val url = XtreamAPI.getStreamUrl(server!!, ch.streamId, ch.containerExtension); addToHistory("live", ch.streamId, ch.name); playStream(url, ch.name) } }

    // ===== VOD =====
    private fun loadVodCategories() { server?.let { srv -> showLoading(); XtreamAPI.getVodCategories(srv) { vodCategories.clear(); vodCategories.addAll(it); if (it.isEmpty()) loadMovies(null) else showVodCategories() } } ?: showLoginDialog() }
    private fun showVodCategories() { isShowingCategories = true; hideLoading(); tvTitle.text = "🎬 المجموعات (${vodCategories.size})"; rv.adapter = createCategoryAdapter(vodCategories) { cat -> selectedCategoryId = cat.categoryId; isShowingCategories = false; btnBack.visibility = View.VISIBLE; tvTitle.text = "🎬 ${cat.categoryName}"; loadMovies(cat.categoryId) } }
    private fun loadMovies(catId: String?) { server?.let { srv -> showLoading(); XtreamAPI.getVodStreams(srv, catId) { vodMovies.clear(); vodMovies.addAll(it); updateMoviesList() } } }
    private fun updateMoviesList() { hideLoading(); tvTitle.text = "${tvTitle.text} (${vodMovies.size})"; rv.adapter = createChannelAdapter(vodMovies.map { it.name }) { name -> val m = vodMovies.find { it.name == name }!!; val url = XtreamAPI.getMovieUrl(server!!, m.streamId, m.containerExtension); addToHistory("movie", m.streamId, m.name); playStream(url, m.name) } }

    // ===== SERIES =====
    private fun loadSeriesCategories() { server?.let { srv -> showLoading(); XtreamAPI.getLiveCategories(srv) { seriesCategories.clear(); seriesCategories.addAll(it); if (it.isEmpty()) loadSeriesList(null) else showSeriesCategories() } } ?: showLoginDialog() }
    private fun showSeriesCategories() { isShowingCategories = true; hideLoading(); tvTitle.text = "🎭 المجموعات (${seriesCategories.size})"; rv.adapter = createCategoryAdapter(seriesCategories) { cat -> selectedCategoryId = cat.categoryId; isShowingCategories = false; btnBack.visibility = View.VISIBLE; tvTitle.text = "🎭 ${cat.categoryName}"; loadSeriesList(cat.categoryId) } }
    private fun loadSeriesList(catId: String?) { server?.let { srv -> showLoading(); XtreamAPI.getSeries(srv, catId) { seriesList.clear(); seriesList.addAll(it); updateSeriesList() } } }
    private fun updateSeriesList() { hideLoading(); tvTitle.text = "${tvTitle.text} (${seriesList.size})"; rv.adapter = createChannelAdapter(seriesList.map { it.name }) { name -> val s = seriesList.find { it.name == name }!!; XtreamAPI.getSeriesInfo(server!!, s.seriesId) { episodes -> showEpisodesDialog(s.name, episodes) } } }

    // ===== ADAPTERS =====
    private fun createCategoryAdapter(cats: List<XtreamCategory>, onClick: (XtreamCategory) -> Unit): RecyclerView.Adapter<RecyclerView.ViewHolder> {
        val t = themes[currentTheme]!!
        return object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val l = LinearLayout(parent.context).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(30), dp(25), dp(30), dp(25)); gravity = Gravity.CENTER_VERTICAL; setBackgroundColor(t.card) }
                l.addView(TextView(parent.context).apply { text = "📁"; textSize = sp(if (isTv) 28f else 24f) })
                l.addView(TextView(parent.context).apply { setPadding(dp(20), 0, 0, 0); textSize = sp(if (isTv) 20f else 16f); setTextColor(t.textWhite); setTypeface(null, Typeface.BOLD); layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) })
                l.addView(TextView(parent.context).apply { text = "→"; textSize = sp(if (isTv) 24f else 20f); setTextColor(t.accent) })
                return object : RecyclerView.ViewHolder(l) {}
            }
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) { val l = (holder.itemView as LinearLayout); (l.getChildAt(1) as TextView).text = cats[pos].categoryName; l.setOnClickListener { onClick(cats[pos]) } }
            override fun getItemCount() = cats.size
        }
    }

    private fun createChannelAdapter(names: List<String>, onClick: (String) -> Unit): RecyclerView.Adapter<RecyclerView.ViewHolder> {
        val t = themes[currentTheme]!!
        return object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val l = LinearLayout(parent.context).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(30), dp(20), dp(30), dp(20)); gravity = Gravity.CENTER_VERTICAL; setBackgroundColor(t.card) }
                l.addView(TextView(parent.context).apply { textSize = sp(if (isTv) 18f else 15f); setTextColor(t.textWhite); setTypeface(null, Typeface.BOLD); layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) })
                l.addView(Button(parent.context).apply { text = "⭐"; setBackgroundColor(Color.TRANSPARENT); setTextColor(Color.parseColor("#FFD93D")) })
                return object : RecyclerView.ViewHolder(l) {}
            }
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
                val l = (holder.itemView as LinearLayout)
                val name = names[pos]
                (l.getChildAt(0) as TextView).text = name
                l.setOnClickListener { onClick(name) }
                l.getChildAt(1).setOnClickListener {
                    val type = if (currentCategory == "movies") "movie" else "live"
                    val id = if (type == "live") liveChannels[pos].streamId else vodMovies[pos].streamId
                    addToFavorites(type, id, name)
                }
            }
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