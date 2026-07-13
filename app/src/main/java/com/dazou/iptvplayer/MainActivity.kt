package com.dazou.iptvplayer

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.util.MimeTypes
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private var baseUrl = ""
    private var username = ""
    private var password = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // تحميل البيانات المحفوظة سابقاً
        val prefs = getSharedPreferences("Prefs", Context.MODE_PRIVATE)
        baseUrl = prefs.getString("url", "") ?: ""
        username = prefs.getString("user", "") ?: ""
        password = prefs.getString("pass", "") ?: ""

        val rv = findViewById<RecyclerView>(R.id.rvChannels)
        rv.layoutManager = LinearLayoutManager(this)

        findViewById<Button>(R.id.btnXtream).setOnClickListener { showLoginDialog() }

        findViewById<Button>(R.id.btnLiveTv).setOnClickListener {
            if (baseUrl.isNotEmpty()) loadData("get_live_streams")
            else Toast.makeText(this, "أدخل بيانات Xtream أولاً", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLoginDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_login, null)
        val etUrl = dialogView.findViewById<EditText>(R.id.etUrl)
        val etUser = dialogView.findViewById<EditText>(R.id.etUser)
        val etPass = dialogView.findViewById<EditText>(R.id.etPass)
        
        etUrl.setText(baseUrl)
        etUser.setText(username)
        etPass.setText(password)

        AlertDialog.Builder(this).setTitle("إعدادات Xtream").setView(dialogView).setPositiveButton("حفظ") { _, _ ->
            baseUrl = etUrl.text.toString(); username = etUser.text.toString(); password = etPass.text.toString()
            getSharedPreferences("Prefs", Context.MODE_PRIVATE).edit().putString("url", baseUrl).putString("user", username).putString("pass", password).apply()
        }.show()
    }

    private fun loadData(action: String) {
        val api = Retrofit.Builder().baseUrl(baseUrl).addConverterFactory(GsonConverterFactory.create()).build().create(XtreamApi::class.java)
        api.getLiveStreams(username, password).enqueue(object : Callback<List<StreamModel>> {
            override fun onResponse(c: Call<List<StreamModel>>, r: Response<List<StreamModel>>) {
                r.body()?.let { findViewById<RecyclerView>(R.id.rvChannels).adapter = RecyclerViewAdapter(it) { s -> play(s.stream_id) } }
            }
            override fun onFailure(c: Call<List<StreamModel>>, t: Throwable) { Toast.makeText(this@MainActivity, "خطأ بالاتصال", Toast.LENGTH_SHORT).show() }
        })
    }

    private fun play(id: String) {
        val pv = findViewById<PlayerView>(R.id.playerView)
        pv.visibility = View.VISIBLE
        player?.release()
        player = ExoPlayer.Builder(this).build()
        pv.player = player
        val url = "${baseUrl}live/${username}/${password}/${id}.m3u8"
        player?.setMediaItem(MediaItem.Builder().setUri(url).setMimeType(MimeTypes.APPLICATION_M3U8).build())
        player?.prepare()
        player?.playWhenReady = true
    }
}
