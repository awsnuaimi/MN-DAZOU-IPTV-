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
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {
    private var player: ExoPlayer? = null
    private var baseUrl = ""; private var username = ""; private var password = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences("Prefs", Context.MODE_PRIVATE)
        baseUrl = prefs.getString("url", "") ?: ""
        username = prefs.getString("user", "") ?: ""
        password = prefs.getString("pass", "") ?: ""

        val rv = findViewById<RecyclerView>(R.id.rvChannels)
        rv.layoutManager = LinearLayoutManager(this)
        
        // ربط الأزرار
        findViewById<Button>(R.id.btnXtream).setOnClickListener { showLoginDialog() }
        findViewById<Button>(R.id.btnLiveTv).setOnClickListener { loadApiData("get_live_streams", rv) }
        findViewById<Button>(R.id.btnMovies).setOnClickListener { loadApiData("get_vod_streams", rv) }
        findViewById<Button>(R.id.btnSeries)?.setOnClickListener { loadApiData("get_series", rv) }
    }

    private fun loadApiData(action: String, rv: RecyclerView) {
        val api = Retrofit.Builder().baseUrl(baseUrl).addConverterFactory(GsonConverterFactory.create()).build().create(XtreamApi::class.java)
        
        val call = when(action) {
            "get_live_streams" -> api.getLiveStreams(username, password)
            "get_vod_streams" -> api.getVodStreams(username, password)
            else -> api.getSeriesStreams(username, password)
        }

        call.enqueue(object : Callback<List<StreamModel>> {
            override fun onResponse(c: Call<List<StreamModel>>, r: Response<List<StreamModel>>) {
                r.body()?.let { list ->
                    rv.adapter = RecyclerViewAdapter(list) { s -> 
                        val id = s.stream_id ?: s.series_id ?: ""
                        play(id) 
                    }
                }
            }
            override fun onFailure(c: Call<List<StreamModel>>, t: Throwable) { Toast.makeText(this@MainActivity, "خطأ بالاتصال", Toast.LENGTH_SHORT).show() }
        })
    }

    private fun showLoginDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_login, null)
        val etUrl = dialogView.findViewById<EditText>(R.id.etUrl); val etUser = dialogView.findViewById<EditText>(R.id.etUser); val etPass = dialogView.findViewById<EditText>(R.id.etPass)
        etUrl.setText(baseUrl); etUser.setText(username); etPass.setText(password)
        AlertDialog.Builder(this).setTitle("إعدادات").setView(dialogView).setPositiveButton("حفظ") { _, _ ->
            baseUrl = etUrl.text.toString(); username = etUser.text.toString(); password = etPass.text.toString()
            getSharedPreferences("Prefs", Context.MODE_PRIVATE).edit().putString("url", baseUrl).putString("user", username).putString("pass", password).apply()
        }.show()
    }

    private fun play(id: String) {
        val pv = findViewById<PlayerView>(R.id.playerView)
        pv.visibility = View.VISIBLE
        player?.stop(); player?.release()
        player = ExoPlayer.Builder(this).build()
        pv.player = player
        player?.setMediaItem(MediaItem.fromUri("${baseUrl}live/${username}/${password}/${id}.m3u8"))
        player?.prepare(); player?.playWhenReady = true
    }
}
