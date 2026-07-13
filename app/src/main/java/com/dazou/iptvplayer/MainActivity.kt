package com.dazou.iptvplayer

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
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
    private lateinit var btnHome: Button
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

    private var currentCategory = "home"
    private var selectedCategoryId: String? = null
    private var isShowingCategories = true

    private lateinit var prefs: SharedPreferences

    data class FavoriteItem(val type: String, val id: Int, val name: String, val icon: String = "")
    data class HistoryItem(val type: String, val id: Int, val name: String, val timestamp: Long)

    // ألوان الثيم الداكن
    private val darkBg = Color.parseColor("#0F0F1A")
    private val darkCard = Color.parseColor("#1A1A35")
    private val accentColor = Color.parseColor("#FF6B6B")
    private val accentBlue = Color.parseColor("#4ECDC4")
    private val accentGold = Color.parseColor("#FFD93D")
    private val textWhite = Color.parseColor("#FFFFFF")
    private val textGray = Color.parseColor("#AAAAAA")
    private val activeTab = Color.parseColor("#2D2D5E")
    private val greenLive = Color.parseColor("#6BCB77")
    private val bottomBarBg = Color.parseColor("#12122A")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        prefs = getSharedPreferences("mndazou_prefs", Context.MODE_PRIVATE)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(darkBg)
        }

        // شريط العنوان
        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20, 45, 20, 20)
            setBackgroundColor(bottomBarBg)
            gravity = Gravity.CENTER_VERTICAL
        }

        btnBack = Button(this).apply {
            text = "⬅️"
            textSize = 18f
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(textWhite)
            visibility = View.GONE
            setOnClickListener { goBackToCategories() }
        }
        headerLayout.addView(btnBack)

        tvTitle = TextView(this).apply {
            text = "MN-DAZOU IPTV"
            textSize = 22f
            setTextColor(accentColor)
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            gravity = Gravity.CENTER
        }
        headerLayout.addView(tvTitle)

        val btnSettings = Button(this).apply {
            text = "⚙️"
            textSize = 18f
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(textGray)
            setOnClickListener { showLoginDialog() }
        }
        headerLayout.addView(btnSettings)
        root.addView(headerLayout)

        // شريط البحث
        searchLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(15, 10, 15, 10)
            setBackgroundColor(bottomBarBg)
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
        }
        etSearch = EditText(this).apply {
            hint = "🔍 بحث عن قناة أو فيلم..."
            setHintTextColor(textGray)
            setTextColor(textWhite)
            setBackgroundColor(darkCard)
            setPadding(25, 15, 25, 15)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        searchLayout.addView(etSearch)
        val btnSearchGo = Button(this).apply {
            text = "بحث"
            setBackgroundColor(accentBlue)
            setTextColor(Color.BLACK)
            setOnClickListener { performSearch() }
        }
        searchLayout.addView(btnSearchGo)
        root.addView(searchLayout)

        // مشغل الفيديو
        val playerCard = CardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 420
            )
            radius = 0f
            setCardBackgroundColor(Color.BLACK)
            cardElevation = 8f
        }

        playerView = PlayerView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.BLACK)
        }
        playerCard.addView(playerView)
        root.addView(playerCard)

        // شريط التحميل
        progressBar = ProgressBar(this).apply {
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 6
            )
        }
        root.addView(progressBar)

        // قائمة المحتوى
        rv = RecyclerView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            )
            layoutManager = LinearLayoutManager(this@MainActivity)
            setBackgroundColor(darkBg)
        }
        root.addView(rv)

        // شريط التنقل السفلي
        val bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(5, 12, 5, 20)
            setBackgroundColor(bottomBarBg)
            gravity = Gravity.CENTER
        }

        btnHome = createBottomButton("🏠", "الرئيسية") { switchTab("home") }
        btnLive = createBottomButton("📺", "مباشر") { switchTab("live") }
        btnMovies = createBottomButton("🎬", "أفلام") { switchTab("movies") }
        btnSeries = createBottomButton("🎭", "مسلسلات") { switchTab("series") }
        btnFavorites = createBottomButton("⭐", "مفضلة") { switchTab("favorites") }

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

        if (server != null) {
            switchTab("home")
        } else {
            showLoginDialog()
        }
    }

    private fun createBottomButton(icon: String, label: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = "$icon\n$label"
            textSize = 10f
            setTextColor(textGray)
            setBackgroundColor(Color.TRANSPARENT)
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            gravity = Gravity.CENTER
            setOnClickListener { onClick() }
        }
    }

    private fun switchTab(tab: String) {
        currentCategory = tab
        selectedCategoryId = null
        isShowingCategories = true
        btnBack.visibility = View.GONE
        searchLayout.visibility = View.GONE

        // إعادة تعيين ألوان الأزرار
        btnHome.setTextColor(textGray)
        btnLive.setTextColor(textGray)
        btnMovies.setTextColor(textGray)
        btnSeries.setTextColor(textGray)
        btnFavorites.setTextColor(textGray)

        when (tab) {
            "home" -> {
                btnHome.setTextColor(accentBlue)
                tvTitle.text = "🏠 MN-DAZOU IPTV"
                showHomeScreen()
            }
            "live" -> {
                btnLive.setTextColor(greenLive)
                tvTitle.text = "📺 البث المباشر"
                searchLayout.visibility = View.VISIBLE
                loadLiveCategories()
            }
            "movies" -> {
                btnMovies.setTextColor(accentGold)
                tvTitle.text = "🎬 الأفلام"
                searchLayout.visibility = View.VISIBLE
                loadVodCategories()
            }
            "series" -> {
                btnSeries.setTextColor(accentColor)
                tvTitle.text = "🎭 المسلسلات"
                searchLayout.visibility = View.VISIBLE
                loadSeriesCategories()
            }
            "favorites" -> {
                btnFavorites.setTextColor(accentGold)
                tvTitle.text = "⭐ المفضلة"
                showFavorites()
            }
        }
    }

    private fun showHomeScreen() {
        val allItems = mutableListOf<Any>()

        // قسم آخر المشاهدات
        if (watchHistory.isNotEmpty()) {
            allItems.add("section_history")
            allItems.addAll(watchHistory.takeLast(5).reversed())
        }

        // قسم المفضلة
        if (favorites.isNotEmpty()) {
            allItems.add("section_favorites")
            allItems.addAll(favorites.take(5))
        }

        // قسم سريع للبث المباشر
        allItems.add("section_quick")
        allItems.add("quick_live")
        allItems.add("quick_movies")
        allItems.add("quick_series")

        if (allItems.isEmpty()) {
            allItems.add("empty")
        }

        rv.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun getItemViewType(position: Int): Int {
                return when (allItems[position]) {
                    is String -> 0
                    is HistoryItem -> 1
                    is FavoriteItem -> 2
                    else -> 0
                }
            }

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                return when (viewType) {
                    0 -> {
                        val tv = TextView(parent.context).apply {
                            setPadding(20, 15, 20, 10)
                            textSize = 16f
                            setTextColor(accentBlue)
                            setTypeface(null, Typeface.BOLD)
                        }
                        object : RecyclerView.ViewHolder(tv) {}
                    }
                    else -> {
                        val card = CardView(parent.context).apply {
                            layoutParams = ViewGroup.MarginLayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                            ).apply { setMargins(15, 6, 15, 6) }
                            radius = 12f
                            setCardBackgroundColor(darkCard)
                            cardElevation = 3f
                        }
                        val tv = TextView(parent.context).apply {
                            setPadding(25, 20, 25, 20)
                            textSize = 15f
                            setTextColor(textWhite)
                            setTypeface(null, Typeface.BOLD)
                        }
                        card.addView(tv)
                        object : RecyclerView.ViewHolder(card) {}
                    }
                }
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
                val item = allItems[pos]
                when (item) {
                    is String -> {
                        val tv = holder.itemView as TextView
                        when (item) {
                            "section_history" -> tv.text = "🕐 آخر المشاهدات"
                            "section_favorites" -> tv.text = "⭐ المفضلة"
                            "section_quick" -> tv.text = "🚀 وصول سريع"
                            "empty" -> tv.text = "👋 أهلاً بك! سجل دخولك للبدء"
                        }
                    }
                    is HistoryItem -> {
                        val card = holder.itemView as CardView
                        val tv = card.getChildAt(0) as TextView
                        tv.text = "🕐  ${item.name}"
                        card.setOnClickListener { playHistoryItem(item) }
                    }
                    is FavoriteItem -> {
                        val card = holder.itemView as CardView
                        val tv = card.getChildAt(0) as TextView
                        tv.text = "⭐  ${item.name}"
                        card.setOnClickListener { playFavoriteItem(item) }
                    }
                }
            }

            override fun getItemCount() = allItems.size
        }
    }

    private fun showFavorites() {
        rv.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val card = CardView(parent.context).apply {
                    layoutParams = ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(15, 6, 15, 6) }
                    radius = 12f
                    setCardBackgroundColor(darkCard)
                    cardElevation = 3f
                }
                val layout = LinearLayout(parent.context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(25, 20, 25, 20)
                    gravity = Gravity.CENTER_VERTICAL
                }
                val tv = TextView(parent.context).apply {
                    textSize = 15f
                    setTextColor(textWhite)
                    setTypeface(null, Typeface.BOLD)
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }
                layout.addView(tv)
                val btnRemove = Button(parent.context).apply {
                    text = "❌"
                    setBackgroundColor(Color.TRANSPARENT)
                }
                layout.addView(btnRemove)
                card.addView(layout)
                return object : RecyclerView.ViewHolder(card) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
                val card = holder.itemView as CardView
                val layout = card.getChildAt(0) as LinearLayout
                val tv = layout.getChildAt(0) as TextView
                val btnRemove = layout.getChildAt(1) as Button
                val fav = favorites[pos]
                tv.text = "⭐  ${fav.name}"
                card.setOnClickListener { playFavoriteItem(fav) }
                btnRemove.setOnClickListener {
                    removeFavorite(fav)
                    showFavorites()
                }
            }

            override fun getItemCount() = favorites.size
        }
    }

    private fun goBackToCategories() {
        isShowingCategories = true
        selectedCategoryId = null
        btnBack.visibility = View.GONE
        when (currentCategory) {
            "live" -> showLiveCategories()
            "movies" -> showVodCategories()
            "series" -> showSeriesCategories()
        }
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(this).build()
        playerView.player = player
        playerView.useController = true
        playerView.showController()
    }

    private fun showLoginDialog() {
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            setBackgroundColor(darkCard)
        }

        val title = TextView(this).apply {
            text = "⚙️ إعدادات Xtream Codes"
            textSize = 20f
            setTextColor(accentColor)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 30)
        }
        dialogView.addView(title)

        val etServer = EditText(this).apply {
            hint = "رابط السيرفر"
            setHintTextColor(textGray)
            setTextColor(textWhite)
            setBackgroundColor(darkBg)
            setPadding(30, 20, 30, 20)
            setText("http://")
        }
        val etUsername = EditText(this).apply {
            hint = "اسم المستخدم"
            setHintTextColor(textGray)
            setTextColor(textWhite)
            setBackgroundColor(darkBg)
            setPadding(30, 20, 30, 20)
        }
        val etPassword = EditText(this).apply {
            hint = "كلمة المرور"
            setHintTextColor(textGray)
            setTextColor(textWhite)
            setBackgroundColor(darkBg)
            setPadding(30, 20, 30, 20)
        }

        dialogView.addView(etServer)
        dialogView.addView(createSpacer(10))
        dialogView.addView(etUsername)
        dialogView.addView(createSpacer(10))
        dialogView.addView(etPassword)

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("اتصال") { _, _ ->
                server = XtreamServer(
                    url = etServer.text.toString().trimEnd('/'),
                    username = etUsername.text.toString(),
                    password = etPassword.text.toString()
                )
                saveServerData()
                Toast.makeText(this, "✅ تم الاتصال", Toast.LENGTH_SHORT).show()
                switchTab("home")
            }
            .setNegativeButton("إلغاء", null)
            .setCancelable(false)
            .show()
    }

    private fun saveServerData() {
        server?.let {
            prefs.edit().apply {
                putString("server_url", it.url)
                putString("server_username", it.username)
                putString("server_password", it.password)
                apply()
            }
        }
    }

    private fun createSpacer(height: Int): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
        }
    }

    private fun performSearch() {
        val query = etSearch.text.toString().lowercase()
        if (query.isEmpty()) return

        when (currentCategory) {
            "live" -> {
                val filtered = liveChannels.filter { it.name.lowercase().contains(query) }
                if (filtered.isNotEmpty()) {
                    liveChannels.clear()
                    liveChannels.addAll(filtered)
                    updateLiveList()
                    tvTitle.text = "🔍 بحث: $query"
                } else {
                    Toast.makeText(this, "لا توجد نتائج", Toast.LENGTH_SHORT).show()
                }
            }
            "movies" -> {
                val filtered = vodMovies.filter { it.name.lowercase().contains(query) }
                if (filtered.isNotEmpty()) {
                    vodMovies.clear()
                    vodMovies.addAll(filtered)
                    updateMoviesList()
                    tvTitle.text = "🔍 بحث: $query"
                } else {
                    Toast.makeText(this, "لا توجد نتائج", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // دوال المفضلة
    private fun addToFavorites(type: String, id: Int, name: String) {
        val exists = favorites.any { it.type == type && it.id == id }
        if (!exists) {
            favorites.add(FavoriteItem(type, id, name))
            saveFavorites()
            Toast.makeText(this, "⭐ تمت الإضافة للمفضلة", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "موجود مسبقاً في المفضلة", Toast.LENGTH_SHORT).show()
        }
    }

    private fun removeFavorite(item: FavoriteItem) {
        favorites.removeAll { it.type == item.type && it.id == item.id }
        saveFavorites()
        Toast.makeText(this, "تم الحذف من المفضلة", Toast.LENGTH_SHORT).show()
    }

    private fun saveFavorites() {
        val json = JSONArray()
        favorites.forEach {
            val obj = JSONObject()
            obj.put("type", it.type)
            obj.put("id", it.id)
            obj.put("name", it.name)
            json.put(obj)
        }
        prefs.edit().putString("favorites", json.toString()).apply()
    }

    private fun loadFavorites() {
        val jsonStr = prefs.getString("favorites", "[]") ?: "[]"
        val json = JSONArray(jsonStr)
        for (i in 0 until json.length()) {
            val obj = json.getJSONObject(i)
            favorites.add(FavoriteItem(
                obj.getString("type"),
                obj.getInt("id"),
                obj.getString("name")
            ))
        }
    }

    // دوال السجل
    private fun addToHistory(type: String, id: Int, name: String) {
        watchHistory.removeAll { it.type == type && it.id == id }
        watchHistory.add(HistoryItem(type, id, name, System.currentTimeMillis()))
        if (watchHistory.size > 20) watchHistory.removeAt(0)
        saveHistory()
    }

    private fun saveHistory() {
        val json = JSONArray()
        watchHistory.forEach {
            val obj = JSONObject()
            obj.put("type", it.type)
            obj.put("id", it.id)
            obj.put("name", it.name)
            obj.put("timestamp", it.timestamp)
            json.put(obj)
        }
        prefs.edit().putString("history", json.toString()).apply()
    }

    private fun loadHistory() {
        val jsonStr = prefs.getString("history", "[]") ?: "[]"
        val json = JSONArray(jsonStr)
        for (i in 0 until json.length()) {
            val obj = json.getJSONObject(i)
            watchHistory.add(HistoryItem(
                obj.getString("type"),
                obj.getInt("id"),
                obj.getString("name"),
                obj.getLong("timestamp")
            ))
        }
    }

    private fun playFavoriteItem(fav: FavoriteItem) {
        when (fav.type) {
            "live" -> {
                val url = XtreamAPI.getStreamUrl(server!!, fav.id)
                playStream(url, fav.name)
                addToHistory("live", fav.id, fav.name)
            }
            "movie" -> {
                val url = XtreamAPI.getMovieUrl(server!!, fav.id)
                playStream(url, fav.name)
                addToHistory("movie", fav.id, fav.name)
            }
        }
    }

    private fun playHistoryItem(item: HistoryItem) {
        when (item.type) {
            "live" -> {
                val url = XtreamAPI.getStreamUrl(server!!, item.id)
                playStream(url, item.name)
            }
            "movie" -> {
                val url = XtreamAPI.getMovieUrl(server!!, item.id)
                playStream(url, item.name)
            }
        }
    }

    private fun loadLiveCategories() {
        server?.let { s ->
            showLoading()
            XtreamAPI.getLiveCategories(s) { categories ->
                liveCategories.clear()
                liveCategories.addAll(categories)
                hideLoading()
                if (categories.isEmpty()) loadLiveStreams(null) else showLiveCategories()
            }
        } ?: showLoginDialog()
    }

    private fun loadVodCategories() {
        server?.let { s ->
            showLoading()
            XtreamAPI.getVodCategories(s) { categories ->
                vodCategories.clear()
                vodCategories.addAll(categories)
                hideLoading()
                if (categories.isEmpty()) loadMovies(null) else showVodCategories()
            }
        } ?: showLoginDialog()
    }

    private fun loadSeriesCategories() {
        server?.let { s ->
            showLoading()
            XtreamAPI.getLiveCategories(s) { categories ->
                seriesCategories.clear()
                seriesCategories.addAll(categories)
                hideLoading()
                if (categories.isEmpty()) loadSeriesList(null) else showSeriesCategories()
            }
        } ?: showLoginDialog()
    }

    private fun showLiveCategories() {
        isShowingCategories = true
        tvTitle.text = "📺 مجموعات البث (${liveCategories.size})"
        rv.adapter = createCategoryAdapter(liveCategories, "📁", greenLive) { cat ->
            selectedCategoryId = cat.categoryId
            isShowingCategories = false
            btnBack.visibility = View.VISIBLE
            tvTitle.text = "📺 ${cat.categoryName}"
            loadLiveStreams(cat.categoryId)
        }
    }

    private fun showVodCategories() {
        isShowingCategories = true
        tvTitle.text = "🎬 مجموعات الأفلام (${vodCategories.size})"
        rv.adapter = createCategoryAdapter(vodCategories, "🎬", accentGold) { cat ->
            selectedCategoryId = cat.categoryId
            isShowingCategories = false
            btnBack.visibility = View.VISIBLE
            tvTitle.text = "🎬 ${cat.categoryName}"
            loadMovies(cat.categoryId)
        }
    }

    private fun showSeriesCategories() {
        isShowingCategories = true
        tvTitle.text = "🎭 مجموعات المسلسلات (${seriesCategories.size})"
        rv.adapter = createCategoryAdapter(seriesCategories, "📺", accentColor) { cat ->
            selectedCategoryId = cat.categoryId
            isShowingCategories = false
            btnBack.visibility = View.VISIBLE
            tvTitle.text = "🎭 ${cat.categoryName}"
            loadSeriesList(cat.categoryId)
        }
    }

    private fun createCategoryAdapter(categories: List<XtreamCategory>, icon: String, color: Int, onClick: (XtreamCategory) -> Unit): RecyclerView.Adapter<RecyclerView.ViewHolder> {
        return object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val card = CardView(parent.context).apply {
                    layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(15, 8, 15, 8) }
                    radius = 12f
                    setCardBackgroundColor(darkCard)
                    cardElevation = 4f
                }
                val layout = LinearLayout(parent.context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(30, 25, 30, 25)
                    gravity = Gravity.CENTER_VERTICAL
                }
                val iconView = TextView(parent.context).apply { text = icon; textSize = 24f }
                layout.addView(iconView)
                val tv = TextView(parent.context).apply {
                    setPadding(20, 0, 0, 0)
                    textSize = 16f
                    setTextColor(textWhite)
                    setTypeface(null, Typeface.BOLD)
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }
                layout.addView(tv)
                val arrow = TextView(parent.context).apply { text = "→"; textSize = 20f; setTextColor(color) }
                layout.addView(arrow)
                card.addView(layout)
                return object : RecyclerView.ViewHolder(card) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
                val card = holder.itemView as CardView
                val layout = card.getChildAt(0) as LinearLayout
                val tv = layout.getChildAt(1) as TextView
                tv.text = categories[pos].categoryName
                card.setOnClickListener { onClick(categories[pos]) }
            }

            override fun getItemCount() = categories.size
        }
    }

    private fun loadLiveStreams(categoryId: String?) {
        server?.let { s ->
            showLoading()
            XtreamAPI.getLiveStreams(s, categoryId) { channels ->
                hideLoading()
                liveChannels.clear()
                liveChannels.addAll(channels)
                updateLiveList()
            }
        }
    }

    private fun loadMovies(categoryId: String?) {
        server?.let { s ->
            showLoading()
            XtreamAPI.getVodStreams(s, categoryId) { movies ->
                hideLoading()
                vodMovies.clear()
                vodMovies.addAll(movies)
                updateMoviesList()
            }
        }
    }

    private fun loadSeriesList(categoryId: String?) {
        server?.let { s ->
            showLoading()
            XtreamAPI.getSeries(s, categoryId) { series ->
                hideLoading()
                seriesList.clear()
                seriesList.addAll(series)
                updateSeriesList()
            }
        }
    }

    private fun showLoading() { progressBar.visibility = View.VISIBLE }
    private fun hideLoading() { progressBar.visibility = View.GONE }

    private fun updateLiveList() {
        rv.adapter = createContentAdapter(liveChannels.map { Pair(it.name, "live") }, "📺") { name, _ ->
            val channel = liveChannels.find { it.name == name }!!
            val url = XtreamAPI.getStreamUrl(server!!, channel.streamId, channel.containerExtension)
            addToHistory("live", channel.streamId, channel.name)
            playStream(url, channel.name)
        }
    }

    private fun updateMoviesList() {
        rv.adapter = createContentAdapter(vodMovies.map { Pair(it.name, "movie") }, "🎬") { name, _ ->
            val movie = vodMovies.find { it.name == name }!!
            val url = XtreamAPI.getMovieUrl(server!!, movie.streamId, movie.containerExtension)
            addToHistory("movie", movie.streamId, movie.name)
            playStream(url, movie.name)
        }
    }

    private fun updateSeriesList() {
        rv.adapter = createContentAdapter(seriesList.map { Pair(it.name, "series") }, "📺") { name, _ ->
            val series = seriesList.find { it.name == name }!!
            server?.let { s ->
                XtreamAPI.getSeriesInfo(s, series.seriesId) { episodes ->
                    showEpisodesDialog(series.name, episodes)
                }
            }
        }
    }

    private fun createContentAdapter(items: List<Pair<String, String>>, icon: String, onClick: (String, String) -> Unit): RecyclerView.Adapter<RecyclerView.ViewHolder> {
        return object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val card = CardView(parent.context).apply {
                    layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(15, 5, 15, 5) }
                    radius = 10f
                    setCardBackgroundColor(darkCard)
                    cardElevation = 2f
                }
                val layout = LinearLayout(parent.context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(25, 20, 25, 20)
                    gravity = Gravity.CENTER_VERTICAL
                }
                val tv = TextView(parent.context).apply {
                    textSize = 15f
                    setTextColor(textWhite)
                    setTypeface(null, Typeface.BOLD)
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }
                layout.addView(tv)
                val btnFav = Button(parent.context).apply {
                    text = "⭐"
                    setBackgroundColor(Color.TRANSPARENT)
                }
                layout.addView(btnFav)
                card.addView(layout)
                return object : RecyclerView.ViewHolder(card) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
                val card = holder.itemView as CardView
                val layout = card.getChildAt(0) as LinearLayout
                val tv = layout.getChildAt(0) as TextView
                val btnFav = layout.getChildAt(1) as Button
                val (name, type) = items[pos]
                tv.text = "$icon  $name"
                card.setOnClickListener { onClick(name, type) }
                btnFav.setOnClickListener {
                    val id = when (type) {
                        "live" -> liveChannels.find { it.name == name }?.streamId ?: 0
                        "movie" -> vodMovies.find { it.name == name }?.streamId ?: 0
                        else -> 0
                    }
                    addToFavorites(type, id, name)
                }
            }

            override fun getItemCount() = items.size
        }
    }

    private fun showEpisodesDialog(seriesName: String, episodes: List<XtreamEpisode>) {
        val episodesArray = episodes.map { "🎭 حلقة ${it.episodeNum}: ${it.title}" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(seriesName)
            .setItems(episodesArray) { _, which ->
                val episode = episodes[which]
                val url = XtreamAPI.getSeriesEpisodeUrl(server!!, episode.id, episode.containerExtension)
                playStream(url, "${seriesName} - حلقة ${episode.episodeNum}")
            }
            .setNegativeButton("إغلاق", null)
            .show()
    }

    private fun playStream(url: String, name: String) {
        try {
            val mediaItem = MediaItem.fromUri(url)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
            Toast.makeText(this, "▶️ $name", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "❌ خطأ: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}