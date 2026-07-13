package com.dazou.iptvplayer

import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ui.PlayerView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // إنشاء الواجهة برمجياً (بدون الحاجة لملفات XML التالفة)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL

        val playerView = PlayerView(this)
        playerView.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 600)
        root.addView(playerView)

        val btnLayout = LinearLayout(this)
        btnLayout.orientation = LinearLayout.HORIZONTAL
        
        val btnLive = Button(this); btnLive.text = "مباشر"; btnLive.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        val btnMovies = Button(this); btnMovies.text = "أفلام"; btnMovies.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        val btnSeries = Button(this); btnSeries.text = "مسلسلات"; btnSeries.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        
        btnLayout.addView(btnLive)
        btnLayout.addView(btnMovies)
        btnLayout.addView(btnSeries)
        root.addView(btnLayout)

        val rv = RecyclerView(this)
        rv.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        rv.layoutManager = LinearLayoutManager(this)
        root.addView(rv)

        setContentView(root)

        // تلوين النص بالأسود ليظهر بوضوح
        rv.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val tv = TextView(parent.context)
                tv.setPadding(40, 40, 40, 40)
                tv.textSize = 18f
                tv.setTextColor(Color.BLACK)
                return object : RecyclerView.ViewHolder(tv) {}
            }
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
                (holder.itemView as TextView).text = "قناة ${pos + 1}"
            }
            override fun getItemCount() = 20
        }
    }
}
