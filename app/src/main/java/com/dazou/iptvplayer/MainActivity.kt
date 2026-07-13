package com.example.iptvapp // تأكد أن هذا السطر يطابق اسم الباكيج عندك

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var playerView: PlayerView
    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // استخدام تنسيق بسيط جداً لضمان ظهور النص باللون الأسود
        val rootLayout = android.widget.LinearLayout(this)
        rootLayout.orientation = android.widget.LinearLayout.VERTICAL
        
        playerView = PlayerView(this)
        playerView.layoutParams = android.widget.LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 500
        )
        
        recyclerView = RecyclerView(this)
        recyclerView.layoutParams = android.widget.LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        )
        
        rootLayout.addView(playerView)
        rootLayout.addView(recyclerView)
        setContentView(rootLayout)

        recyclerView.layoutManager = LinearLayoutManager(this)
        
        // هنا نقوم بتعريف الـ Adapter مباشرة وبداخله قمنا بتلوين النص بالأسود
        recyclerView.adapter = object : RecyclerView.Adapter<ChannelViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
                val tv = TextView(parent.context)
                tv.textSize = 18f
                tv.setPadding(20, 20, 20, 20)
                tv.setTextColor(Color.BLACK) // هنا حل مشكلة اللون الأبيض
                return ChannelViewHolder(tv)
            }

            override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
                holder.textView.text = "قناة رقم $position" // استبدلها بقائمة قنواتك الفعلية
                holder.textView.setOnClickListener {
                    playVideo("رابط_القناة_هنا") // ضع الرابط هنا
                }
            }

            override fun getItemCount() = 20
        }
    }

    private fun playVideo(url: String) {
        player?.release()
        player = ExoPlayer.Builder(this).build()
        playerView.player = player
        player?.setMediaItem(MediaItem.fromUri(url))
        player?.prepare()
        player?.play()
    }

    class ChannelViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)
}
