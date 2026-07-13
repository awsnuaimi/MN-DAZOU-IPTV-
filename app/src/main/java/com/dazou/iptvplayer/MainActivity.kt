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

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun sp(value: Float) = value * resources.displayMetrics.scaledDensity
    private fun headerPadTop() = dp(if (isTv) 30 else 16)
    private fun headerPadBottom() = dp(if (isTv) 15 else 10)
    private fun playerHeight() = dp(if (isTv) 480 else 380)
    private fun titleSize() = sp(if (isTv) 26f else 18f)
    private fun tabSize() = sp(if (isTv) 15f else 12f)
    private fun itemSize() = sp(if (isTv) 17f else 14f)
    private fun itemPadV() = dp(if (isTv) 22 else 16)
    private fun itemPadH() = dp(if (isTv) 25 else 18)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        prefs = getSharedPreferences("mndazou_prefs", Context.MODE_PRIVATE)
        isTv = (getSystemService(Context.UI_MODE_SERVICE) as? android.app.UiModeManager)?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION

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
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(14), headerPadTop(), dp(14), headerPadBottom()); setBackgroundColor(t.bottomBar); gravity = Gravity.CENTER_VERTICAL }
        btnBack = Button(this).apply { text = "⬅️"; textSize = sp(if (isTv) 20f else 16f); setBackgroundColor(Color.TRANSPARENT); setTextColor(t.textWhite); visibility = View.GONE; setOnClickListener { goBack() } }
        header.addView(btnBack)
        tvTitle = TextView(this).apply { text = "MN-DAZOU IPTV"; textSize = titleSize(); setTextColor(t.accent); setTypeface(null, Typeface.BOLD); gravity = Gravity.CENTER; layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) }
        header.addView(tvTitle)
        val iconSize = sp(if (isTv) 20f else 14f)
        header.addView(Button(this).apply { text = "🎨"; textSize = iconSize; setBackgroundColor(Color.TRANSPARENT); setTextColor(t.textGray); setOnClickListener { showThemeDialog() } })
        header.addView(Button(this).apply { text = "⚙️"; textSize = iconSize; setBackgroundColor(Color.TRANSPARENT); setTextColor(t.textGray); setOnClickListener { showLoginDialog() } })
        root.addView(header)

        // Search
        searchLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(10), dp(5), dp(10), dp(5)); setBackgroundColor(t.bottomBar); gravity = Gravity.CENTER_VERTICAL }
        etSearch = EditText(this).apply { hint = "🔍 بحث..."; setHintTextColor(t.textGray); setTextColor(t.textWhite); setBackgroundColor(t.bg); setPadding(dp(18), dp(8), dp(18), dp(8)); layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f); textSize = sp(if (isTv) 16f else 13f) }
        searchLayout.addView(etSearch)
        searchLayout.addView(Button(this).apply { text = "بحث"; textSize = sp(if (isTv) 16f else 13f); setBackgroundColor(t.accent); setTextColor(Color.BLACK); setOnClickListener { performSearch() } })
        root.addView(searchLayout)

        // Player
        playerView = PlayerView(this).apply { layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, playerHeight()); setBackgroundColor(Color.BLACK); useController = true }
        root.addView(playerView)

        // Tabs
        val tabLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(6), dp(6), dp(6), dp(6)); setBackgroundColor(t.bottomBar); gravity = Gravity.CENTER }
        btnLive = createTabButton("📺 مباشر") { switchTab("live") }
        btnMovies = createTabButton("🎬 أفلام") { switchTab("movies") }
        btnSeries = createTabButton("🎭 مسلسلات") { switchTab("series") }
        btnFavorites = createTabButton("⭐ مفضلة") { switchTab("favorites") }
        tabLayout.addView(btnLive); tabLayout.addView(btnMovies); tabLayout.addView(btnSeries); tabLayout.addView(btnFavorites)
        root.addView(tabLayout)

        // Progress
        progressBar = ProgressBar(this).apply { visibility = View.GONE; layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(4)) }
        root.addView(progressBar)

        // List
        rv = RecyclerView(this).apply { layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f); layoutManager = LinearLayoutManager(this@MainActivity); setBackgroundColor(t.bg) }
        root.addView(rv)

        setContentView(root)

        player = ExoPlayer.Builder(this).build()
        playerView.player = player

        loadFavorites(); loadHistory()

        // ✅ فحص الاتصال أولاً
        val savedUrl = prefs.getString("server_url", "")
        if (!savedUrl.isNullOrEmpty()) {
            server = XtreamServer(savedUrl!!, prefs.getString("server_username", "")!!, prefs.getString("server_password", "")!!)
            // اختبار الاتصال
            testConnection()
        } else {
            showLoginDialog()
        }
    }

    // ✅ دالة فحص الاتصال
    private fun testConnection() {
        showLoading()
        tvTitle.text = "⏳ جاري الاتصال بالسيرفر..."
        thread {
            try {
                val url = "${server!!.url}/player_api.php?username=${server!!.username}&password=${server!!.password}"
                val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.requestMethod = "GET"
                val responseCode = conn.responseCode
                conn.disconnect()

                runOnUiThread {
                    hideLoading()
                    if (responseCode == 200) {
                        Toast.makeText(this, "✅ تم الاتصال بنجاح", Toast.LENGTH_SHORT).show()
                        switchTab("live")
                    } else {
                        showConnectionError("فشل الاتصال (كود: $responseCode)")
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    hideLoading()
                    showConnectionError("فشل الاتصال: ${e.message}")
                }
            }
        }
    }

    // ✅ عرض خطأ الاتصال مع خيارات
    private fun showConnectionError(message: String) {
        tvTitle.text = "❌ خطأ في الاتصال"
        rv.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val l = LinearLayout(parent.context).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(30), dp(30), dp(30), dp(30)); gravity = Gravity.CENTER }
                l.addView(TextView(parent.context).apply { text = message; textSize = sp(16f); setTextColor(Color.parseColor("#FF6B6B")); gravity = Gravity.CENTER; setTypeface(null, Typeface.BOLD) })
                l.addView(TextView(parent.context).apply { text = "تأكد من:\n- صحة رابط السيرفر\n- اتصال الانترنت\n- صلاحية الحساب"; textSize = sp(13f); setTextColor(Color.parseColor("#AAAAAA")); gravity = Gravity.CENTER; setPadding(0, dp(15), 0, dp(15)) })
                val btnRetry = Button(parent.context).apply {
                    text = "🔄 إعادة المحاولة"
                    setBackgroundColor(Color.parseColor("#2D2D5E"))
                    setTextColor(Color.WHITE)
                    textSize = sp(14f)
                    setOnClickListener { testConnection() }
                }
                l.addView(btnRetry)
                val btnNewAccount = Button(parent.context).apply {
                    text = "⚙️ إدخال حساب جديد"
                    setBackgroundColor(Color.parseColor("#FF6B6B"))
                    setTextColor(Color.WHITE)
                    textSize = sp(14f)
                    setOnClickListener { showLoginDialog() }
                }
                l.addView(btnNewAccount)
                return object : RecyclerView.ViewHolder(l) {}
            }
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {}
            override fun getItemCount() = 1
        }
    }

    private fun createTabButton(text: String, onClick: () -> Unit): Button {
        val t = themes[currentTheme]!!
        return Button(this).apply { this.text = text; textSize = tabSize(); setTextColor(t.textGray); setBackgroundColor(Color.TRANSPARENT); setTypeface(null, Typeface.BOLD); layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f); setOnClickListener { onClick() } }
    }

    private fun showThemeDialog() {
        AlertDialog.Builder(this).setTitle("🎨 اختر الثيم").setItems(themes.values.map { it.name }.toTypedArray()) { _, w ->
            prefs.edit().putString("theme", themes.keys.toList()[w]).apply(); Toast.makeText(this, "🔄 أعد تشغيل التطبيق", Toast.LENGTH_LONG).show()
        }.show()
    }

    private fun switchTab(tab: String) {
        currentCategory = tab; selectedCategoryId = null; isShowingCategories = true; btnBack.visibility = View.GONE
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
    private fun addToFavorites(type: String, id: Int, name: String, icon: String = "") { if (favorites.none { it.type == type && it.id == id }) { favorites.add(FavoriteItem(type, id, name, icon)); saveFavorites(); Toast.makeText(this, "⭐ تمت الإضافة", Toast.LENGTH_SHORT).show() } }
    private fun removeFavorite(item: FavoriteItem) { favorites.removeAll { it.type == item.type && it.id == item.id }; saveFavorites() }
    private fun saveFavorites() { val j = JSONArray(); favorites.forEach { val o = JSONObject(); o.put("type", it.type); o.put("id", it.id); o.put("name", it.name); o.put("icon", it.icon); j.put(o) }; prefs.edit().putString("favorites", j.toString()).apply() }
    private fun loadFavorites() { try { favorites.clear(); val s = prefs.getString("favorites", "[]") ?: "[]"; val j = JSONArray(s); for (i in 0 until j.length()) { val o = j.getJSONObject(i); favorites.add(FavoriteItem(o.getString("type"), o.getInt("id"), o.getString("name"), o.optString("icon", ""))) } } catch (e: Exception) { favorites.clear(); prefs.edit().remove("favorites").apply() } }
    private fun showFavorites() { isShowingCategories = true; val t = themes[currentTheme]!!; rv.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() { override fun onCreateViewHolder(p: ViewGroup, vt: Int): RecyclerView.ViewHolder { val l = LinearLayout(p.context).apply { orientation = LinearLayout.HORIZONTAL; setPadding(itemPadH(), itemPadV(), itemPadH(), itemPadV()); gravity = Gravity.CENTER_VERTICAL; setBackgroundColor(t.card) }; l.addView(TextView(p.context).apply { textSize = itemSize(); setTextColor(t.textWhite); setTypeface(null, Typeface.BOLD); layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) }); l.addView(Button(p.context).apply { text = "❌"; setBackgroundColor(Color.TRANSPARENT); setTextColor(Color.RED); textSize = sp(if (isTv) 18f else 14f) }); return object : RecyclerView.ViewHolder(l) {} } override fun onBindViewHolder(h: RecyclerView.ViewHolder, p: Int) { val l = (h.itemView as LinearLayout); val fav = favorites[p]; (l.getChildAt(0) as TextView).text = "⭐ ${fav.name}"; l.setOnClickListener { playFavoriteItem(fav) }; l.getChildAt(1).setOnClickListener { removeFavorite(fav); showFavorites() } } override fun getItemCount() = favorites.size } }

    // ===== HISTORY =====
    private fun addToHistory(type: String, id: Int, name: String, icon: String = "") { watchHistory.removeAll { it.type == type && it.id == id }; watchHistory.add(HistoryItem(type, id, name, System.currentTimeMillis(), icon)); if (watchHistory.size > 20) watchHistory.removeAt(0); saveHistory() }
    private fun saveHistory() { val j = JSONArray(); watchHistory.forEach { val o = JSONObject(); o.put("type", it.type); o.put("id", it.id); o.put("name", it.name); o.put("timestamp", it.timestamp); o.put("icon", it.icon); j.put(o) }; prefs.edit().putString("history", j.toString()).apply() }
    private fun loadHistory() { try { watchHistory.clear(); val s = prefs.getString("history", "[]") ?: "[]"; val j = JSONArray(s); for (i in 0 until j.length()) { val o = j.getJSONObject(i); watchHistory.add(HistoryItem(o.getString("type"), o.getInt("id"), o.getString("name"), o.getLong("timestamp"), o.optString("icon", ""))) } } catch (e: Exception) { watchHistory.clear(); prefs.edit().remove("history").apply() } }
    private fun playFavoriteItem(fav: FavoriteItem) { when (fav.type) { "live" -> { val url = XtreamAPI.getStreamUrl(server!!, fav.id); playStream(url, fav.name); addToHistory("live", fav.id, fav.name) }; "movie" -> { val url = XtreamAPI.getMovieUrl(server!!, fav.id); playStream(url, fav.name); addToHistory("movie", fav.id, fav.name) } } }

    // ===== SEARCH =====
    private fun performSearch() { val q = etSearch.text.toString().lowercase(); if (q.isEmpty()) return; when (currentCategory) { "live" -> { val filtered = liveChannels.filter { it.name.lowercase().contains(q) }; if (filtered.isNotEmpty()) { liveChannels.clear(); liveChannels.addAll(filtered); updateLiveList(); tvTitle.text = "🔍 $q (${filtered.size})" } else Toast.makeText(this, "لا نتائج", Toast.LENGTH_SHORT).show() } "movies" -> { val filtered = vodMovies.filter { it.name.lowercase().contains(q) }; if (filtered.isNotEmpty()) { vodMovies.clear(); vodMovies.addAll(filtered); updateMoviesList(); tvTitle.text = "🔍 $q (${filtered.size})" } else Toast.makeText(this, "لا نتائج", Toast.LENGTH_SHORT).show() } "series" -> { val filtered = seriesList.filter { it.name.lowercase().contains(q) }; if (filtered.isNotEmpty()) { seriesList.clear(); seriesList.addAll(filtered); updateSeriesList(); tvTitle.text = "🔍 $q (${filtered.size})" } else Toast.makeText(this, "لا نتائج", Toast.LENGTH_SHORT).show() } } }

    // ===== LOGIN =====
    private fun showLoginDialog() {
        val t = themes[currentTheme]!!
        val dialogSize = sp(if (isTv) 22f else 16f)
        val inputSize = sp(if (isTv) 18f else 14f)
        val inputPadding = dp(if (isTv) 30 else 20)

        val scrollView = ScrollView(this).apply { setPadding(dp(30), dp(30), dp(30), dp(30)) }
        val d = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(t.card) }

        d.addView(TextView(this).apply { text = "⚙️ إضافة حساب Xtream"; textSize = dialogSize; setTextColor(t.accent); setTypeface(null, Typeface.BOLD); gravity = Gravity.CENTER; setPadding(0, 0, 0, dp(25)) })
        d.addView(TextView(this).apply { text = "رابط السيرفر:"; textSize = sp(if (isTv) 16f else 13f); setTextColor(t.textGray); setPadding(0, dp(8), 0, dp(4)) })
        val es = EditText(this).apply { hint = "http://example.com:8080"; setHintTextColor(t.textGray); setTextColor(t.textWhite); setBackgroundColor(t.bg); setPadding(inputPadding, inputPadding, inputPadding, inputPadding); setText("http://"); textSize = inputSize }
        d.addView(es)
        d.addView(TextView(this).apply { text = "اسم المستخدم:"; textSize = sp(if (isTv) 16f else 13f); setTextColor(t.textGray); setPadding(0, dp(12), 0, dp(4)) })
        val eu = EditText(this).apply { hint = "username"; setHintTextColor(t.textGray); setTextColor(t.textWhite); setBackgroundColor(t.bg); setPadding(inputPadding, inputPadding, inputPadding, inputPadding); textSize = inputSize }
        d.addView(eu)
        d.addView(TextView(this).apply { text = "كلمة المرور:"; textSize = sp(if (isTv) 16f else 13f); setTextColor(t.textGray); setPadding(0, dp(12), 0, dp(4)) })
        val ep = EditText(this).apply { hint = "password"; setHintTextColor(t.textGray); setTextColor(t.textWhite); setBackgroundColor(t.bg); setPadding(inputPadding, inputPadding, inputPadding, inputPadding); textSize = inputSize }
        d.addView(ep)

        scrollView.addView(d)

        AlertDialog.Builder(this)
            .setView(scrollView)
            .setPositiveButton("حفظ واتصال") { _, _ ->
                server = XtreamServer(es.text.toString().trimEnd('/'), eu.text.toString(), ep.text.toString())
                prefs.edit().putString("server_url", server!!.url).putString("server_username", server!!.username).putString("server_password", server!!.password).apply()
                Toast.makeText(this, "⏳ جاري الاتصال...", Toast.LENGTH_SHORT).show()
                testConnection()
            }
            .setNegativeButton("إلغاء", null)
            .setCancelable(false)
            .show()
    }

    // ===== LIVE =====
    private fun loadLiveCategories() { server?.let { srv -> showLoading(); XtreamAPI.getLiveCategories(srv) { cats -> hideLoading(); if (cats.isNotEmpty()) { liveCategories.clear(); liveCategories.addAll(cats); showLiveCategories() } else { loadLiveStreams(null) } } } ?: run { Toast.makeText(this, "الرجاء تسجيل الدخول", Toast.LENGTH_SHORT).show(); showLoginDialog() } }
    private fun showLiveCategories() { isShowingCategories = true; tvTitle.text = "📺 المجموعات (${liveCategories.size})"; rv.adapter = createCategoryAdapter(liveCategories) { cat -> selectedCategoryId = cat.categoryId; isShowingCategories = false; btnBack.visibility = View.VISIBLE; tvTitle.text = "📺 ${cat.categoryName}"; loadLiveStreams(cat.categoryId) } }
    private fun loadLiveStreams(catId: String?) { server?.let { srv -> showLoading(); XtreamAPI.getLiveStreams(srv, catId) { channels -> hideLoading(); if (channels.isNotEmpty()) { liveChannels.clear(); liveChannels.addAll(channels); updateLiveList() } else { Toast.makeText(this, "لا توجد قنوات في هذا القسم", Toast.LENGTH_SHORT).show(); rv.adapter = null } } } }
    private fun updateLiveList() { tvTitle.text = "${tvTitle.text} (${liveChannels.size})"; rv.adapter = createChannelAdapter(liveChannels.map { it.name }) { name -> val ch = liveChannels.find { it.name == name }!!; val url = XtreamAPI.getStreamUrl(server!!, ch.streamId, ch.containerExtension); addToHistory("live", ch.streamId, ch.name); playStream(url, ch.name) } }

    // ===== VOD =====
    private fun loadVodCategories() { server?.let { srv -> showLoading(); XtreamAPI.getVodCategories(srv) { cats -> hideLoading(); if (cats.isNotEmpty()) { vodCategories.clear(); vodCategories.addAll(cats); showVodCategories() } else { loadMovies(null) } } } ?: run { Toast.makeText(this, "الرجاء تسجيل الدخول", Toast.LENGTH_SHORT).show(); showLoginDialog() } }
    private fun showVodCategories() { isShowingCategories = true; tvTitle.text = "🎬 المجموعات (${vodCategories.size})"; rv.adapter = createCategoryAdapter(vodCategories) { cat -> selectedCategoryId = cat.categoryId; isShowingCategories = false; btnBack.visibility = View.VISIBLE; tvTitle.text = "🎬 ${cat.categoryName}"; loadMovies(cat.categoryId) } }
    private fun loadMovies(catId: String?) { server?.let { srv -> showLoading(); XtreamAPI.getVodStreams(srv, catId) { movies -> hideLoading(); if (movies.isNotEmpty()) { vodMovies.clear(); vodMovies.addAll(movies); updateMoviesList() } else { Toast.makeText(this, "لا توجد أفلام في هذا القسم", Toast.LENGTH_SHORT).show(); rv.adapter = null } } } }
    private fun updateMoviesList() { tvTitle.text = "${tvTitle.text} (${vodMovies.size})"; rv.adapter = createChannelAdapter(vodMovies.map { it.name }) { name -> val m = vodMovies.find { it.name == name }!!; val url = XtreamAPI.getMovieUrl(server!!, m.streamId, m.containerExtension); addToHistory("movie", m.streamId, m.name); playStream(url, m.name) } }

    // ===== SERIES =====
    private fun loadSeriesCategories() { server?.let { srv -> showLoading(); XtreamAPI.getLiveCategories(srv) { cats -> hideLoading(); if (cats.isNotEmpty()) { seriesCategories.clear(); seriesCategories.addAll(cats); showSeriesCategories() } else { loadSeriesList(null) } } } ?: run { Toast.makeText(this, "الرجاء تسجيل الدخول", Toast.LENGTH_SHORT).show(); showLoginDialog() } }
    private fun showSeriesCategories() { isShowingCategories = true; tvTitle.text = "🎭 المجموعات (${seriesCategories.size})"; rv.adapter = createCategoryAdapter(seriesCategories) { cat -> selectedCategoryId = cat.categoryId; isShowingCategories = false; btnBack.visibility = View.VISIBLE; tvTitle.text = "🎭 ${cat.categoryName}"; loadSeriesList(cat.categoryId) } }
    private fun loadSeriesList(catId: String?) { server?.let { srv -> showLoading(); XtreamAPI.getSeries(srv, catId) { series -> hideLoading(); if (series.isNotEmpty()) { seriesList.clear(); seriesList.addAll(series); updateSeriesList() } else { Toast.makeText(this, "لا توجد مسلسلات في هذا القسم", Toast.LENGTH_SHORT).show(); rv.adapter = null } } } }
    private fun updateSeriesList() { tvTitle.text = "${tvTitle.text} (${seriesList.size})"; rv.adapter = createChannelAdapter(seriesList.map { it.name }) { name -> val s = seriesList.find { it.name == name }!!; XtreamAPI.getSeriesInfo(server!!, s.seriesId) { episodes -> showEpisodesDialog(s.name, episodes) } } }

    // ===== ADAPTERS =====
    private fun createCategoryAdapter(cats: List<XtreamCategory>, onClick: (XtreamCategory) -> Unit): RecyclerView.Adapter<RecyclerView.ViewHolder> {
        val t = themes[currentTheme]!!
        return object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val l = LinearLayout(parent.context).apply { orientation = LinearLayout.HORIZONTAL; setPadding(itemPadH(), itemPadV(), itemPadH(), itemPadV()); gravity = Gravity.CENTER_VERTICAL; setBackgroundColor(t.card) }
                l.addView(TextView(parent.context).apply { text = "📁"; textSize = sp(if (isTv) 26f else 22f) })
                l.addView(TextView(parent.context).apply { setPadding(dp(15), 0, 0, 0); textSize = sp(if (isTv) 18f else 15f); setTextColor(t.textWhite); setTypeface(null, Typeface.BOLD); layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) })
                l.addView(TextView(parent.context).apply { text = "→"; textSize = sp(if (isTv) 22f else 18f); setTextColor(t.accent) })
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
                val l = LinearLayout(parent.context).apply { orientation = LinearLayout.HORIZONTAL; setPadding(itemPadH(), itemPadV(), itemPadH(), itemPadV()); gravity = Gravity.CENTER_VERTICAL; setBackgroundColor(t.card) }
                l.addView(TextView(parent.context).apply { textSize = itemSize(); setTextColor(t.textWhite); setTypeface(null, Typeface.BOLD); layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) })
                l.addView(Button(parent.context).apply { text = "⭐"; setBackgroundColor(Color.TRANSPARENT); setTextColor(Color.parseColor("#FFD93D")); textSize = sp(if (isTv) 22f else 18f) })
                return object : RecyclerView.ViewHolder(l) {}
            }
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) { val l = (holder.itemView as LinearLayout); val name = names[pos]; (l.getChildAt(0) as TextView).text = name; l.setOnClickListener { onClick(name) }; l.getChildAt(1).setOnClickListener { val type = if (currentCategory == "movies") "movie" else "live"; val id = if (type == "live") liveChannels.getOrNull(pos)?.streamId ?: 0 else vodMovies.getOrNull(pos)?.streamId ?: 0; addToFavorites(type, id, name) } }
            override fun getItemCount() = names.size
        }
    }

    private fun showEpisodesDialog(name: String, episodes: List<XtreamEpisode>) {
        if (episodes.isEmpty()) { Toast.makeText(this, "لا توجد حلقات", Toast.LENGTH_SHORT).show(); return }
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