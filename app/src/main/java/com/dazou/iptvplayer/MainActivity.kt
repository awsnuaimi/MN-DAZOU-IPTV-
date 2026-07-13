package com.dazou.iptvplayer

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

    private val baseUrl = "http://YOUR_SERVER_URL/"
    private val username = "YOUR_USERNAME"
    private val password = "YOUR_PASSWORD"
    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val rv = findViewById<RecyclerView>(R.id.rvChannels)
        rv.layoutManager = LinearLayoutManager(this)
        
        val api = Retrofit.Builder().baseUrl(baseUrl).addConverterFactory(GsonConverterFactory.create()).build().create(XtreamApi::class.java)

        findViewById<Button>(R.id.btnLiveTv).setOnClickListener {
            api.getLiveStreams(username, password).enqueue(object : Callback<List<StreamModel>> {
                override fun onResponse(c: Call<List<StreamModel>>, r: Response<List<StreamModel>>) {
                    r.body()?.let { rv.adapter = RecyclerViewAdapter(it) { s -> play(s.stream_id) } }
                }
                override fun onFailure(c: Call<List<StreamModel>>, t: Throwable) {}
            })
        }

        findViewById<Button>(R.id.btnMovies).setOnClickListener {
            api.getVodStreams(username, password).enqueue(object : Callback<List<StreamModel>> {
                override fun onResponse(c: Call<List<StreamModel>>, r: Response<List<StreamModel>>) {
                    r.body()?.let { rv.adapter = RecyclerViewAdapter(it) { s -> play(s.stream_id) } }
                }
                override fun onFailure(c: Call<List<StreamModel>>, t: Throwable) {}
            })
        }
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
