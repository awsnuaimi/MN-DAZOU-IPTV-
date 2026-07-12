package com.dazou.iptvplayer

import android.os.Bundle
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

    private lateinit var rv: RecyclerView
    private var player: ExoPlayer? = null
    
    // ضع بياناتك هنا
    private val baseUrl = "http://your-server-url:port/"
    private val username = "your_username"
    private val password = "your_password"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rv = findViewById(R.id.rvChannels)
        rv.layoutManager = LinearLayoutManager(this)

        val btnLive = findViewById<Button>(R.id.btnLiveTv)
        val btnMovies = findViewById<Button>(R.id.btnMovies)

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(XtreamApi::class.java)

        btnLive.setOnClickListener {
            api.getLiveStreams(username, password).enqueue(object : Callback<List<StreamModel>> {
                override fun onResponse(call: Call<List<StreamModel>>, response: Response<List<StreamModel>>) {
                    response.body()?.let { updateList(it) }
                }
                override fun onFailure(call: Call<List<StreamModel>>, t: Throwable) {
                    Toast.makeText(this@MainActivity, "خطأ في الاتصال", Toast.LENGTH_SHORT).show()
                }
            })
        }

        btnMovies.setOnClickListener {
            api.getVodStreams(username, password).enqueue(object : Callback<List<StreamModel>> {
                override fun onResponse(call: Call<List<StreamModel>>, response: Response<List<StreamModel>>) {
                    response.body()?.let { updateList(it) }
                }
                override fun onFailure(call: Call<List<StreamModel>>, t: Throwable) {
                    Toast.makeText(this@MainActivity, "خطأ في الاتصال", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

        private fun updateList(list: List<StreamModel>) {
        // تحديث المحول باستخدام النوع الصريح
        val adapter = RecyclerViewAdapter(list) { stream -> 
            playStream(stream) 
        }
        rv.adapter = adapter
    }
{
        val names = list.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, names)
        // ملاحظة: لتبسيط الكود، استخدمنا ArrayAdapter، سأعلمك كيف تجعلها احترافية لاحقاً
        rv.adapter = RecyclerViewAdapter(list) { stream -> 
            playStream(stream) 
        }
    }

    private fun playStream(stream: StreamModel) {
        val playerView = findViewById<PlayerView>(R.id.playerView)
        playerView.visibility = android.view.View.VISIBLE
        player = ExoPlayer.Builder(this).build()
        playerView.player = player
        // ملاحظة: رابط التشغيل يختلف حسب النوع، هذا مبدئي
        val url = "${baseUrl}live/${username}/${password}/${stream.stream_id}.m3u8"
        player?.setMediaItem(MediaItem.fromUri(url))
        player?.prepare()
        player?.playWhenReady = true
    }
}
