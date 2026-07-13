package com.dazou.iptvplayer

import android.app.AlertDialog
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
    // متغيرات لحفظ بيانات السيرفر
    private var baseUrl = ""
    private var username = ""
    private var password = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val rv = findViewById<RecyclerView>(R.id.rvChannels)
        rv.layoutManager = LinearLayoutManager(this)

        // برمجة زر Xtream
        findViewById<Button>(R.id.btnXtream).setOnClickListener { showLoginDialog() }

        // برمجة زر المباشر
        findViewById<Button>(R.id.btnLiveTv).setOnClickListener {
            if (baseUrl.isNotEmpty()) loadData("get_live_streams")
            else Toast.makeText(this, "يرجى إدخال بيانات Xtream أولاً", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLoginDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_login, null)
        val etUrl = dialogView.findViewById<EditText>(R.id.etUrl)
        val etUser = dialogView.findViewById<EditText>(R.id.etUser)
        val etPass = dialogView.findViewById<EditText>(R.id.etPass)

        AlertDialog.Builder(this)
            .setTitle("إعدادات Xtream")
            .setView(dialogView)
            .setPositiveButton("حفظ") { _, _ ->
                baseUrl = etUrl.text.toString()
                username = etUser.text.toString()
                password = etPass.text.toString()
                Toast.makeText(this, "تم حفظ البيانات!", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun loadData(action: String) {
        val api = Retrofit.Builder().baseUrl(baseUrl).addConverterFactory(GsonConverterFactory.create()).build().create(XtreamApi::class.java)
        api.getLiveStreams(username, password).enqueue(object : Callback<List<StreamModel>> {
            override fun onResponse(c: Call<List<StreamModel>>, r: Response<List<StreamModel>>) {
                r.body()?.let { findViewById<RecyclerView>(R.id.rvChannels).adapter = RecyclerViewAdapter(it) { s -> play(s.stream_id) } }
            }
            override fun onFailure(c: Call<List<StreamModel>>, t: Throwable) {}
        })
    }

    private fun play(id: String) {
        val pv = findViewById<PlayerView>(R.id.playerView)
        pv.visibility = View.VISIBLE
        player = ExoPlayer.Builder(this).build()
        pv.player = player
        player?.setMediaItem(MediaItem.fromUri("${baseUrl}live/${username}/${password}/${id}.m3u8"))
        player?.prepare()
        player?.playWhenReady = true
    }
}
