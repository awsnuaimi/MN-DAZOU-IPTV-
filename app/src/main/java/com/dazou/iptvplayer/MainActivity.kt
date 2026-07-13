package com.dazou.iptvplayer

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView

class MainActivity : AppCompatActivity() {

    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView
    private lateinit var rv: RecyclerView
    private var server: XtreamServer? = null
    private var liveChannels = mutableListOf<XtreamChannel>()
    private var vodMovies = mutableListOf<XtreamMovie>()
    private var seriesList = mutableListOf<XtreamSeries>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // إنشاء الواجهة
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL

        playerView = PlayerView(this)
        playerView.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 500
        )
        root.addView(playerView)

        // زر إعدادات Xtream
        val btnSettings = Button(this).apply {
            text = "⚙️ إعدادات Xtream"
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener { showLoginDialog() }
        }
        root.addView(btnSettings)

        // أزرار التصنيف
        val btnLayout = LinearLayout(this)
        btnLayout.orientation = LinearLayout.HORIZONTAL

        val btnLive = Button(this).apply {
            text = "📺 مباشر"
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { loadLiveStreams() }
        }
        val btnMovies = Button(this).apply {
            text = "🎬 أفلام"
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { loadMovies() }
        }
        val btnSeries = Button(this).apply {
            text = "📺 مسلسلات"
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { loadSeries() }
        }

        btnLayout.addView(btnLive)
        btnLayout.addView(btnMovies)
        btnLayout.addView(btnSeries)
        root.addView(btnLayout)

        rv = RecyclerView(this)
        rv.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        )
        rv.layoutManager = LinearLayoutManager(this)
        root.addView(rv)

        setContentView(root)

        // إعداد ExoPlayer
        initializePlayer()
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(this).build()
        playerView.player = player
    }

    private fun showLoginDialog() {
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        val etServer = EditText(this).apply {
            hint = "رابط السيرفر (http://example.com:8080)"
            setText("http://")
        }
        val etUsername = EditText(this).apply {
            hint = "اسم المستخدم"
        }
        val etPassword = EditText(this).apply {
            hint = "كلمة المرور"
        }

        dialogView.addView(TextView(this).apply { text = "إعدادات Xtream:"; textSize = 18f })
        dialogView.addView(etServer)
        dialogView.addView(etUsername)
        dialogView.addView(etPassword)

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("حفظ") { _, _ ->
                server = XtreamServer(
                    url = etServer.text.toString().trimEnd('/'),
                    username = etUsername.text.toString(),
                    password = etPassword.text.toString()
                )
                Toast.makeText(this, "تم الحفظ ✓", Toast.LENGTH_SHORT).show()
                loadLiveStreams()
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun loadLiveStreams() {
        server?.let { s ->
            Toast.makeText(this, "جاري تحميل القنوات...", Toast.LENGTH_SHORT).show()
            XtreamAPI.getLiveStreams(s) { channels ->
                liveChannels.clear()
                liveChannels.addAll(channels)
                updateLiveList()
            }
        } ?: showLoginDialog()
    }

    private fun loadMovies() {
        server?.let { s ->
            Toast.makeText(this, "جاري تحميل الأفلام...", Toast.LENGTH_SHORT).show()
            XtreamAPI.getVodStreams(s) { movies ->
                vodMovies.clear()
                vodMovies.addAll(movies)
                updateMoviesList()
            }
        } ?: showLoginDialog()
    }

    private fun loadSeries() {
        server?.let { s ->
            Toast.makeText(this, "جاري تحميل المسلسلات...", Toast.LENGTH_SHORT).show()
            XtreamAPI.getSeries(s) { series ->
                seriesList.clear()
                seriesList.addAll(series)
                updateSeriesList()
            }
        } ?: showLoginDialog()
    }

    private fun updateLiveList() {
        rv.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val tv = TextView(parent.context).apply {
                    setPadding(40, 30, 40, 30)
                    textSize = 18f
                    setTextColor(Color.BLACK)
                }
                return object : RecyclerView.ViewHolder(tv) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
                val tv = holder.itemView as TextView
                val channel = liveChannels[pos]
                tv.text = "📺 ${channel.name}"
                tv.setOnClickListener {
                    val url = XtreamAPI.getStreamUrl(server!!, channel.streamId, channel.containerExtension)
                    playStream(url, channel.name)
                }
            }

            override fun getItemCount() = liveChannels.size
        }
    }

    private fun updateMoviesList() {
        rv.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val tv = TextView(parent.context).apply {
                    setPadding(40, 30, 40, 30)
                    textSize = 18f
                    setTextColor(Color.BLACK)
                }
                return object : RecyclerView.ViewHolder(tv) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
                val tv = holder.itemView as TextView
                val movie = vodMovies[pos]
                tv.text = "🎬 ${movie.name}"
                tv.setOnClickListener {
                    val url = XtreamAPI.getMovieUrl(server!!, movie.streamId, movie.containerExtension)
                    playStream(url, movie.name)
                }
            }

            override fun getItemCount() = vodMovies.size
        }
    }

    private fun updateSeriesList() {
        rv.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val tv = TextView(parent.context).apply {
                    setPadding(40, 30, 40, 30)
                    textSize = 18f
                    setTextColor(Color.BLACK)
                }
                return object : RecyclerView.ViewHolder(tv) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
                val tv = holder.itemView as TextView
                val series = seriesList[pos]
                tv.text = "📺 ${series.name}"
                tv.setOnClickListener {
                    // لما يضغط المستخدم على مسلسل، نحمل الحلقات
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
        val episodesArray = episodes.map { "حلقة ${it.episodeNum} - ${it.title}" }.toTypedArray()

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
            Toast.makeText(this, "▶️ جاري تشغيل: $name", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "❌ خطأ في التشغيل: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}