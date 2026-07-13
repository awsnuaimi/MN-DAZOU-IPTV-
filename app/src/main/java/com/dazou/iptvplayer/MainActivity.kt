package com.dazou.iptvplayer

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
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

    private var server: XtreamServer? = null
    private var liveChannels = mutableListOf<XtreamChannel>()
    private var currentStreamIndex = -1

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        prefs = getSharedPreferences("mndazou_prefs", Context.MODE_PRIVATE)

        val bgColor = Color.parseColor("#0F0F1A")
        val cardColor = Color.parseColor("#1A1A35")
        val accentColor = Color.parseColor("#FF6B6B")
        val textWhite = Color.parseColor("#FFFFFF")

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgColor)
        }

        // Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20, 45, 20, 20)
            setBackgroundColor(cardColor)
            gravity = Gravity.CENTER_VERTICAL
        }
        tvTitle = TextView(this).apply {
            text = "MN-DAZOU IPTV"
            textSize = 20f
            setTextColor(accentColor)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        header.addView(tvTitle)
        header.addView(Button(this).apply {
            text = "⚙️"
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(Color.parseColor("#AAAAAA"))
            setOnClickListener { showLoginDialog() }
        })
        root.addView(header)

        // Player
        playerView = PlayerView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 400
            )
            setBackgroundColor(Color.BLACK)
            useController = true
        }
        root.addView(playerView)

        // Progress
        progressBar = ProgressBar(this).apply {
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 6)
        }
        root.addView(progressBar)

        // Channel List
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
            server = XtreamServer(
                savedUrl!!,
                prefs.getString("server_username", "")!!,
                prefs.getString("server_password", "")!!
            )
            loadLiveStreams()
        } else {
            showLoginDialog()
        }
    }

    private fun showLoginDialog() {
        val d = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            setBackgroundColor(Color.parseColor("#1A1A35"))
        }
        d.addView(TextView(this).apply {
            text = "⚙️ إضافة حساب Xtream"
            textSize = 20f
            setTextColor(Color.parseColor("#FF6B6B"))
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 30)
        })
        val es = EditText(this).apply {
            hint = "رابط السيرفر"
            setHintTextColor(Color.parseColor("#AAAAAA"))
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#0F0F1A"))
            setPadding(30, 20, 30, 20)
            setText("http://")
            textSize = 14f
        }
        val eu = EditText(this).apply {
            hint = "اسم المستخدم"
            setHintTextColor(Color.parseColor("#AAAAAA"))
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#0F0F1A"))
            setPadding(30, 20, 30, 20)
            textSize = 14f
        }
        val ep = EditText(this).apply {
            hint = "كلمة المرور"
            setHintTextColor(Color.parseColor("#AAAAAA"))
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#0F0F1A"))
            setPadding(30, 20, 30, 20)
            textSize = 14f
        }
        d.addView(es)
        d.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 10) })
        d.addView(eu)
        d.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 10) })
        d.addView(ep)

        AlertDialog.Builder(this)
            .setView(d)
            .setPositiveButton("حفظ") { _, _ ->
                server = XtreamServer(es.text.toString().trimEnd('/'), eu.text.toString(), ep.text.toString())
                prefs.edit()
                    .putString("server_url", server!!.url)
                    .putString("server_username", server!!.username)
                    .putString("server_password", server!!.password)
                    .apply()
                Toast.makeText(this, "✅ تم حفظ الحساب", Toast.LENGTH_SHORT).show()
                loadLiveStreams()
            }
            .setNegativeButton("إلغاء", null)
            .setCancelable(false)
            .show()
    }

    private fun loadLiveStreams() {
        server?.let { srv ->
            showLoading()
            tvTitle.text = "📺 جاري تحميل القنوات..."
            XtreamAPI.getLiveStreams(srv) { channels ->
                hideLoading()
                liveChannels.clear()
                liveChannels.addAll(channels)
                currentStreamIndex = -1
                tvTitle.text = "📺 البث المباشر (${channels.size})"
                updateChannelList()
            }
        } ?: showLoginDialog()
    }

    private fun updateChannelList() {
        rv.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val tv = TextView(parent.context).apply {
                    setPadding(30, 25, 30, 25)
                    textSize = 16f
                    setTextColor(Color.WHITE)
                    setTypeface(null, Typeface.BOLD)
                    setBackgroundColor(Color.parseColor("#1A1A35"))
                }
                return object : RecyclerView.ViewHolder(tv) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
                val tv = holder.itemView as TextView
                val channel = liveChannels[pos]
                tv.text = "📺  ${channel.name}"
                tv.setOnClickListener {
                    currentStreamIndex = pos
                    val url = XtreamAPI.getStreamUrl(server!!, channel.streamId, channel.containerExtension)
                    playStream(url, channel.name)
                }
            }

            override fun getItemCount() = liveChannels.size
        }
    }

    private fun playStream(url: String, name: String) {
        try {
            val mediaItem = MediaItem.fromUri(Uri.parse(url))
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
            Toast.makeText(this, "▶️ $name", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "❌ ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showLoading() { progressBar.visibility = View.VISIBLE }
    private fun hideLoading() { progressBar.visibility = View.GONE }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}