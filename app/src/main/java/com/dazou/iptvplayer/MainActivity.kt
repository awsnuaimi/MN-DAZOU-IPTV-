package com.dazou.iptvplayer

import android.graphics.Color
import android.net.Uri
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
    private var channels = mutableListOf<Channel>()

    data class Channel(val name: String, val url: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // إنشاء الواجهة
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL

        playerView = PlayerView(this)
        playerView.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 600
        )
        root.addView(playerView)

        // أزرار التصنيف
        val btnLayout = LinearLayout(this)
        btnLayout.orientation = LinearLayout.HORIZONTAL

        val btnLive = Button(this).apply {
            text = "مباشر"
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { loadCategory("live") }
        }
        val btnMovies = Button(this).apply {
            text = "أفلام"
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { loadCategory("movies") }
        }
        val btnSeries = Button(this).apply {
            text = "مسلسلات"
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { loadCategory("series") }
        }

        btnLayout.addView(btnLive)
        btnLayout.addView(btnMovies)
        btnLayout.addView(btnSeries)
        root.addView(btnLayout)

        // إضافة زر تحميل M3U
        val btnLoad = Button(this).apply {
            text = "تحميل قائمة M3U"
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener { loadDefaultPlaylist() }
        }
        root.addView(btnLoad)

        rv = RecyclerView(this)
        rv.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        )
        rv.layoutManager = LinearLayoutManager(this)
        root.addView(rv)

        setContentView(root)

        // إعداد ExoPlayer
        initializePlayer()

        // تحميل قناة افتراضية للاختبار
        loadTestChannels()
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(this).build()
        playerView.player = player
    }

    private fun loadTestChannels() {
        channels.clear()
        channels.add(Channel("قناة الجزيرة الإخبارية", "https://bit.ly/3f7D4pY"))
        channels.add(Channel("قناة العربية", "https://bit.ly/3u8q9xV"))
        channels.add(Channel("MBC 1", "https://bit.ly/3z5vL2M"))
        // ضع روابط M3U8 حقيقية هنا
        updateChannelList()
    }

    private fun loadDefaultPlaylist() {
        // هنا تحط رابط ملف M3U الخاص بك
        val m3uUrl = "https://example.com/playlist.m3u"
        Toast.makeText(this, "جاري تحميل القائمة...", Toast.LENGTH_SHORT).show()
        // سنضيف كود تحميل M3U لاحقاً
    }

    private fun loadCategory(category: String) {
        Toast.makeText(this, "تصنيف: $category", Toast.LENGTH_SHORT).show()
        // سنضيف تصفية القنوات حسب التصنيف لاحقاً
    }

    private fun updateChannelList() {
        rv.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val tv = TextView(parent.context).apply {
                    setPadding(40, 40, 40, 40)
                    textSize = 18f
                    setTextColor(Color.BLACK)
                }
                return object : RecyclerView.ViewHolder(tv) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
                val tv = holder.itemView as TextView
                tv.text = channels[pos].name
                tv.setOnClickListener {
                    playChannel(channels[pos])
                }
            }

            override fun getItemCount() = channels.size
        }
    }

    private fun playChannel(channel: Channel) {
        val mediaItem = MediaItem.fromUri(Uri.parse(channel.url))
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
        Toast.makeText(this, "جاري تشغيل: ${channel.name}", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}