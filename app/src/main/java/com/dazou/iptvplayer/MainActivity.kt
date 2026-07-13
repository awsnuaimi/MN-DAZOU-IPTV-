package com.dazou.iptvplayer

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.media3.common.MediaItem
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
    
    private var server: XtreamServer? = null
    private var liveChannels = mutableListOf<XtreamChannel>()
    private var vodMovies = mutableListOf<XtreamMovie>()
    private var seriesList = mutableListOf<XtreamSeries>()
    private var currentCategory = "live"

    // ألوان الثيم الداكن
    private val darkBg = Color.parseColor("#1A1A2E")
    private val darkCard = Color.parseColor("#16213E")
    private val accentColor = Color.parseColor("#E94560")
    private val accentGold = Color.parseColor("#F5A623")
    private val textWhite = Color.parseColor("#FFFFFF")
    private val textGray = Color.parseColor("#B0B0B0")
    private val activeTab = Color.parseColor("#0F3460")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(darkBg)
        }

        // شريط العنوان
        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20, 40, 20, 20)
            setBackgroundColor(darkCard)
            gravity = Gravity.CENTER_VERTICAL
        }
        
        tvTitle = TextView(this).apply {
            text = "DAZOU IPTV"
            textSize = 24f
            setTextColor(accentColor)
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        headerLayout.addView(tvTitle)
        
        val btnSettings = Button(this).apply {
            text = "⚙️"
            textSize = 20f
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(textWhite)
            setOnClickListener { showLoginDialog() }
        }
        headerLayout.addView(btnSettings)
        root.addView(headerLayout)

        // مشغل الفيديو
        val playerCard = CardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 450
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

        // شريط التصنيفات
        val tabLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(10, 15, 10, 15)
            setBackgroundColor(darkCard)
            gravity = Gravity.CENTER
        }

        btnLive = createTabButton("📺 مباشر") { switchTab("live") }
        btnMovies = createTabButton("🎬 أفلام") { switchTab("movies") }
        btnSeries = createTabButton("📺 مسلسلات") { switchTab("series") }

        tabLayout.addView(btnLive)
        tabLayout.addView(btnMovies)
        tabLayout.addView(btnSeries)
        root.addView(tabLayout)

        // شريط التحميل
        progressBar = ProgressBar(this).apply {
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 8
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

        setContentView(root)
        initializePlayer()
        
        // تحميل تلقائي
        if (server != null) {
            loadLiveStreams()
        } else {
            showLoginDialog()
        }
    }

    private fun createTabButton(text: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            textSize = 14f
            setTextColor(textWhite)
            setBackgroundColor(Color.TRANSPARENT)
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { onClick() }
        }
    }

    private fun switchTab(tab: String) {
        currentCategory = tab
        
        // إعادة تعيين ألوان الأزرار
        btnLive.setBackgroundColor(Color.TRANSPARENT)
        btnMovies.setBackgroundColor(Color.TRANSPARENT)
        btnSeries.setBackgroundColor(Color.TRANSPARENT)

        // تفعيل الزر المختار
        when (tab) {
            "live" -> {
                btnLive.setBackgroundColor(activeTab)
                loadLiveStreams()
            }
            "movies" -> {
                btnMovies.setBackgroundColor(activeTab)
                loadMovies()
            }
            "series" -> {
                btnSeries.setBackgroundColor(activeTab)
                loadSeries()
            }
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
                Toast.makeText(this, "✅ تم الاتصال", Toast.LENGTH_SHORT).show()
                loadLiveStreams()
            }
            .setNegativeButton("إلغاء", null)
            .setCancelable(false)
            .show()
    }

    private fun createSpacer(height: Int): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
        }
    }

    private fun loadLiveStreams() {
        server?.let { s ->
            showLoading()
            XtreamAPI.getLiveStreams(s) { channels ->
                hideLoading()
                liveChannels.clear()
                liveChannels.addAll(channels)
                updateLiveList()
            }
        } ?: showLoginDialog()
    }

    private fun loadMovies() {
        server?.let { s ->
            showLoading()
            XtreamAPI.getVodStreams(s) { movies ->
                hideLoading()
                vodMovies.clear()
                vodMovies.addAll(movies)
                updateMoviesList()
            }
        } ?: showLoginDialog()
    }

    private fun loadSeries() {
        server?.let { s ->
            showLoading()
            XtreamAPI.getSeries(s) { series ->
                hideLoading()
                seriesList.clear()
                seriesList.addAll(series)
                updateSeriesList()
            }
        } ?: showLoginDialog()
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
    }

    private fun updateLiveList() {
        tvTitle.text = "📺 البث المباشر (${liveChannels.size})"
        rv.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val card = CardView(parent.context).apply {
                    layoutParams = ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(15, 8, 15, 8) }
                    radius = 12f
                    setCardBackgroundColor(darkCard)
                    cardElevation = 4f
                }
                val tv = TextView(parent.context).apply {
                    setPadding(30, 30, 30, 30)
                    textSize = 16f
                    setTextColor(textWhite)
                    setTypeface(null, Typeface.BOLD)
                }
                card.addView(tv)
                return object : RecyclerView.ViewHolder(card) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
                val card = holder.itemView as CardView
                val tv = card.getChildAt(0) as TextView
                val channel = liveChannels[pos]
                tv.text = "📺  ${channel.name}"
                card.setOnClickListener {
                    val url = XtreamAPI.getStreamUrl(server!!, channel.streamId, channel.containerExtension)
                    playStream(url, channel.name)
                }
            }

            override fun getItemCount() = liveChannels.size
        }
    }

    private fun updateMoviesList() {
        tvTitle.text = "🎬 الأفلام (${vodMovies.size})"
        rv.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val card = CardView(parent.context).apply {
                    layoutParams = ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(15, 8, 15, 8) }
                    radius = 12f
                    setCardBackgroundColor(darkCard)
                    cardElevation = 4f
                }
                val tv = TextView(parent.context).apply {
                    setPadding(30, 30, 30, 30)
                    textSize = 16f
                    setTextColor(textWhite)
                    setTypeface(null, Typeface.BOLD)
                }
                card.addView(tv)
                return object : RecyclerView.ViewHolder(card) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
                val card = holder.itemView as CardView
                val tv = card.getChildAt(0) as TextView
                val movie = vodMovies[pos]
                tv.text = "🎬  ${movie.name}"
                card.setOnClickListener {
                    val url = XtreamAPI.getMovieUrl(server!!, movie.streamId, movie.containerExtension)
                    playStream(url, movie.name)
                }
            }

            override fun getItemCount() = vodMovies.size
        }
    }

    private fun updateSeriesList() {
        tvTitle.text = "📺 المسلسلات (${seriesList.size})"
        rv.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val card = CardView(parent.context).apply {
                    layoutParams = ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(15, 8, 15, 8) }
                    radius = 12f
                    setCardBackgroundColor(darkCard)
                    cardElevation = 4f
                }
                val tv = TextView(parent.context).apply {
                    setPadding(30, 30, 30, 30)
                    textSize = 16f
                    setTextColor(textWhite)
                    setTypeface(null, Typeface.BOLD)
                }
                card.addView(tv)
                return object : RecyclerView.ViewHolder(card) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
                val card = holder.itemView as CardView
                val tv = card.getChildAt(0) as TextView
                val series = seriesList[pos]
                tv.text = "📺  ${series.name}"
                card.setOnClickListener {
                    server?.let { s ->
                        XtreamAPI.getSeriesInfo(s, series.seriesId) { episodes ->
                            showEpisodesDialog(series.name, episodes)
                        }
                    }
                }
            }

            override fun getItemCount() = seriesList.size
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