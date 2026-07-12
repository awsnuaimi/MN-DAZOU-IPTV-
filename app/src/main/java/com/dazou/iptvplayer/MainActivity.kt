package com.dazou.iptvplayer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView

class MainActivity : AppCompatActivity() {
    private var player: ExoPlayer? = null
    private var playerView: PlayerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // إنشاء واجهة المشغل برمجياً لضمان الخفة والسرعة
        playerView = PlayerView(this)
        setContentView(playerView)

        // تهيئة محرك تشغيل الفيديو ExoPlayer
        player = ExoPlayer.Builder(this).build()
        playerView?.player = player

        // رابط بث تجريبي (يمكنك استبداله برابط الـ IPTV الخاص بك لاحقاً)
        val videoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        val mediaItem = MediaItem.fromUri(videoUrl)
        
        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.playWhenReady = true
    }

    override fun onDestroy() {
        super.onDestroy()
        // تنظيف الذاكرة عند إغلاق التطبيق لضمان عدم استهلاك البطارية
        player?.release()
        player = null
    }
}
