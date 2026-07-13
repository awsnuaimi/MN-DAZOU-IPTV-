package com.dazou.iptvplayer

import android.app.*
import android.content.*
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.media.AudioManager
import android.net.Uri
import android.os.*
import android.util.Rational
import android.view.*
import android.view.animation.AlphaAnimation
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView
    private lateinit var rv: RecyclerView
    private lateinit var tvTitle: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnHome: Button
    private lateinit var btnLive: Button
    private lateinit var btnMovies: Button
    private lateinit var btnSeries: Button
    private lateinit var btnFavorites: Button
    private lateinit var btnAccounts: Button
    private lateinit var btnBack: Button
    private lateinit var searchLayout: LinearLayout
    private lateinit var etSearch: EditText
    private lateinit var playerControlsLayout: LinearLayout
    private lateinit var btnPrevChannel: Button
    private lateinit var btnPlayPause: Button
    private lateinit var btnNextChannel: Button
    private lateinit var btnRecord: Button
    private lateinit var btnShare: Button
    private lateinit var btnFullscreen: Button
    private lateinit var btnDownload: Button
    private lateinit var btnQuality: Button
    private lateinit var btnAspectRatio: Button
    private lateinit var seekBar: SeekBar
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvTotalTime: TextView
    private lateinit var tvChannelInfo: TextView
    private lateinit var tvRecording: TextView
    private lateinit var audioManager: AudioManager

    private var server: XtreamServer? = null
    private var liveChannels = mutableListOf<XtreamChannel>()
    private var vodMovies = mutableListOf<XtreamMovie>()
    private var seriesList = mutableListOf<XtreamSeries>()
    private var liveCategories = mutableListOf<XtreamCategory>()
    private var vodCategories = mutableListOf<XtreamCategory>()
    private var seriesCategories = mutableListOf<XtreamCategory>()
    private var favorites = mutableListOf<FavoriteItem>()
    private var watchHistory = mutableListOf<HistoryItem>()
    private var epgData = mutableListOf<EpgProgram>()
    private var imageCache = mutableMapOf<String, Bitmap>()
    private var currentStreamUrl: String? = null
    private var currentStreamName: String? = null
    private var currentStreamIndex = -1
    private var currentStreamType = "live"
    private var isPlayerPlaying = false
    private var currentAspectRatio = 0
    private var currentTheme = "dark"
    private var downloadId: Long = 0
    private var isRecording = false
    private var recordingFile: File? = null
    private var recordingThread: Thread? = null
    private var watchStartTime: Long = 0
    private var accounts = mutableListOf<XtreamServer>()
    private var currentAccountIndex = 0

    private var currentCategory = "home"
    private var selectedCategoryId: String? = null
    private var isShowingCategories = true

    private lateinit var prefs: SharedPreferences
    private lateinit var downloadManager: DownloadManager
    private lateinit var themes: Map<String, ThemeColors>

    data class FavoriteItem(val type: String, val id: Int, val name: String, val icon: String = "")
    data class HistoryItem(val type: String, val id: Int, val name: String, val timestamp: Long, val icon: String = "")
    data class EpgProgram(val channelId: String, val title: String, val startTime: String, val endTime: String, val description: String)
    data class ThemeColors(val name: String, val bg: Int, val card: Int, val accent: Int, val bottomBar: Int, val textWhite: Int, val textGray: Int, val activeTab: Int)

    private fun initThemes() {
        themes = mapOf(
            "dark" to ThemeColors("داكن", Color.parseColor("#0F0F1A"), Color.parseColor("#1A1A35"), Color.parseColor("#FF6B6B"), Color.parseColor("#12122A"), Color.parseColor("#FFFFFF"), Color.parseColor("#AAAAAA"), Color.parseColor("#2D2D5E")),
            "blue" to ThemeColors("أزرق", Color.parseColor("#0A1628"), Color.parseColor("#1B2D4A"), Color.parseColor("#4FC3F7"), Color.parseColor("#0D1F3C"), Color.parseColor("#FFFFFF"), Color.parseColor("#90CAF9"), Color.parseColor("#1565C0")),
            "green" to ThemeColors("أخضر", Color.parseColor("#0A1F0A"), Color.parseColor("#1A3A1A"), Color.parseColor("#66BB6A"), Color.parseColor("#0D2A0D"), Color.parseColor("#FFFFFF"), Color.parseColor("#A5D6A7"), Color.parseColor("#2E7D32")),
            "purple" to ThemeColors("بنفسجي", Color.parseColor("#1A0A2E"), Color.parseColor("#2D1B4E"), Color.parseColor("#CE93D8"), Color.parseColor("#1F0D3D"), Color.parseColor("#FFFFFF"), Color.parseColor("#E1BEE7"), Color.parseColor("#6A1B9A")),
            "red" to ThemeColors("أحمر", Color.parseColor("#1A0A0A"), Color.parseColor("#3A1A1A"), Color.parseColor("#EF5350"), Color.parseColor("#2A0D0D"), Color.parseColor("#FFFFFF"), Color.parseColor("#EF9A9A"), Color.parseColor("#C62828"))
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        prefs = getSharedPreferences("mndazou_prefs", Context.MODE_PRIVATE)
        downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        initThemes()
        currentTheme = prefs.getString("theme", "dark") ?: "dark"
        val theme = themes[currentTheme]!!

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(theme.bg)
        }

        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; setPadding(20, 45, 20, 20)
            setBackgroundColor(theme.bottomBar); gravity = Gravity.CENTER_VERTICAL
        }
        btnBack = Button(this).apply { text="⬅️"; textSize=18f; setBackgroundColor(Color.TRANSPARENT); setTextColor(theme.textWhite); visibility=View.GONE; setOnClickListener{goBackToCategories()} }
        headerLayout.addView(btnBack)
        tvTitle = TextView(this).apply { text="MN-DAZOU IPTV"; textSize=20f; setTextColor(theme.accent); setTypeface(null,Typeface.BOLD); gravity=Gravity.CENTER; layoutParams=LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f) }
        headerLayout.addView(tvTitle)
        headerLayout.addView(Button(this).apply{text="🎨"; textSize=16f; setBackgroundColor(Color.TRANSPARENT); setTextColor(theme.textGray); setOnClickListener{showThemeDialog()}})
        headerLayout.addView(Button(this).apply{text="⚙️"; textSize=16f; setBackgroundColor(Color.TRANSPARENT); setTextColor(theme.textGray); setOnClickListener{showSettingsDialog()}})
        root.addView(headerLayout)

        searchLayout = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setPadding(15,10,15,10); setBackgroundColor(theme.bottomBar); gravity=Gravity.CENTER_VERTICAL; visibility=View.GONE }
        etSearch = EditText(this).apply { hint="🔍 بحث..."; setHintTextColor(theme.textGray); setTextColor(theme.textWhite); setBackgroundColor(theme.card); setPadding(25,15,25,15); layoutParams=LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f) }
        searchLayout.addView(etSearch)
        searchLayout.addView(Button(this).apply{text="بحث"; setBackgroundColor(theme.accent); setTextColor(Color.BLACK); setOnClickListener{performSearch()}})
        root.addView(searchLayout)

        val playerCard = CardView(this).apply { layoutParams=LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,420); radius=0f; setCardBackgroundColor(Color.BLACK); cardElevation=8f }
        val playerContainer = FrameLayout(this).apply { layoutParams=FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT) }
        playerView = PlayerView(this).apply { layoutParams=FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT); setBackgroundColor(Color.BLACK) }
        tvChannelInfo = TextView(this).apply { text=""; textSize=13f; setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#CC000000")); setPadding(15,8,15,8); visibility=View.GONE; gravity=Gravity.CENTER; layoutParams=FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT,Gravity.TOP) }
        tvRecording = TextView(this).apply { text="🔴"; textSize=14f; setTextColor(Color.RED); setBackgroundColor(Color.parseColor("#CC000000")); setPadding(15,5,15,5); visibility=View.GONE; gravity=Gravity.END; layoutParams=FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT,Gravity.TOP or Gravity.END) }
        playerContainer.addView(playerView); playerContainer.addView(tvChannelInfo); playerContainer.addView(tvRecording); playerCard.addView(playerContainer); root.addView(playerCard)

        val navLayout = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setPadding(15,10,15,10); setBackgroundColor(Color.parseColor("#DD000000")); gravity=Gravity.CENTER; visibility=View.GONE }
        btnPrevChannel = Button(this).apply { text="⏪"; setBackgroundColor(Color.TRANSPARENT); setTextColor(Color.WHITE); textSize=22f; setOnClickListener{playPreviousChannel()} }
        btnPlayPause = Button(this).apply { text="▶️"; setBackgroundColor(Color.TRANSPARENT); setTextColor(Color.WHITE); textSize=22f; setOnClickListener{togglePlayPause()} }
        btnNextChannel = Button(this).apply { text="⏩"; setBackgroundColor(Color.TRANSPARENT); setTextColor(Color.WHITE); textSize=22f; setOnClickListener{playNextChannel()} }
        btnRecord = Button(this).apply { text="🔴"; setBackgroundColor(Color.TRANSPARENT); setTextColor(Color.WHITE); textSize=18f; setOnClickListener{toggleRecording()} }
        navLayout.addView(btnPrevChannel); navLayout.addView(btnPlayPause); navLayout.addView(btnNextChannel); navLayout.addView(btnRecord); root.addView(navLayout)

        playerControlsLayout = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setPadding(10,8,10,8); setBackgroundColor(Color.parseColor("#DD000000")); gravity=Gravity.CENTER_VERTICAL; visibility=View.GONE }
        btnShare = Button(this).apply { text="📤"; setBackgroundColor(Color.TRANSPARENT); setTextColor(Color.WHITE); textSize=16f; setOnClickListener{shareCurrentStream()} }
        btnQuality = Button(this).apply { text="HD"; setBackgroundColor(Color.TRANSPARENT); setTextColor(Color.WHITE); textSize=14f; setOnClickListener{showQualityDialog()} }
        btnAspectRatio = Button(this).apply { text="🔲"; setBackgroundColor(Color.TRANSPARENT); setTextColor(Color.WHITE); textSize=16f; setOnClickListener{changeAspectRatio()} }
        btnDownload = Button(this).apply { text="⬇️"; setBackgroundColor(Color.TRANSPARENT); setTextColor(Color.WHITE); textSize=16f; setOnClickListener{downloadCurrentStream()} }
        btnFullscreen = Button(this).apply { text="⛶"; setBackgroundColor(Color.TRANSPARENT); setTextColor(Color.WHITE); textSize=18f; setOnClickListener{toggleFullscreen()} }
        playerControlsLayout.addView(btnShare); playerControlsLayout.addView(btnQuality); playerControlsLayout.addView(btnAspectRatio); playerControlsLayout.addView(btnDownload); playerControlsLayout.addView(btnFullscreen); root.addView(playerControlsLayout)

        val seekLayout = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setPadding(10,5,10,5); setBackgroundColor(Color.parseColor("#DD000000")); gravity=Gravity.CENTER_VERTICAL; visibility=View.GONE }
        tvCurrentTime = TextView(this).apply { text="00:00"; setTextColor(Color.WHITE); textSize=12f }
        seekBar = SeekBar(this).apply { layoutParams=LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f); setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(s:SeekBar?,p:Int,f:Boolean){if(f)player.seekTo(p.toLong())} override fun onStartTrackingTouch(s:SeekBar?){} override fun onStopTrackingTouch(s:SeekBar?){}}) }
        tvTotalTime = TextView(this).apply { text="00:00"; setTextColor(Color.WHITE); textSize=12f }
        seekLayout.addView(tvCurrentTime); seekLayout.addView(seekBar); seekLayout.addView(tvTotalTime); root.addView(seekLayout)

        progressBar = ProgressBar(this).apply { visibility=View.GONE; layoutParams=LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,6) }; root.addView(progressBar)

        rv = RecyclerView(this).apply { layoutParams=LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f); layoutManager=LinearLayoutManager(this@MainActivity); setBackgroundColor(theme.bg) }; root.addView(rv)

        val bottomBar = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setPadding(3,10,3,18); setBackgroundColor(theme.bottomBar); gravity=Gravity.CENTER }
        btnHome = createBottomButton("🏠","رئيسية",theme){switchTab("home")}
        btnLive = createBottomButton("📺","مباشر",theme){switchTab("live")}
        btnMovies = createBottomButton("🎬","أفلام",theme){switchTab("movies")}
        btnSeries = createBottomButton("🎭","مسلسلات",theme){switchTab("series")}
        btnFavorites = createBottomButton("⭐","مفضلة",theme){switchTab("favorites")}
        btnAccounts = createBottomButton("👤","حسابات",theme){showAccountsDialog()}
        bottomBar.addView(btnHome); bottomBar.addView(btnLive); bottomBar.addView(btnMovies)
        bottomBar.addView(btnSeries); bottomBar.addView(btnFavorites); bottomBar.addView(btnAccounts)
        root.addView(bottomBar)

        setContentView(root)
        initializePlayer()
        loadFavorites(); loadHistory(); loadAccounts(); registerDownloadReceiver()

        if(accounts.isNotEmpty()) {
            currentAccountIndex = prefs.getInt("current_account", 0)
            if(currentAccountIndex < accounts.size) {
                server = accounts[currentAccountIndex]
                switchTab("home")
            }
        } else {
            showLoginDialog()
        }
    }

    private fun createBottomButton(icon: String, label: String, theme: ThemeColors, onClick: () -> Unit) = Button(this).apply {
        text="$icon\n$label"; textSize=9f; setTextColor(theme.textGray); setBackgroundColor(Color.TRANSPARENT)
        setTypeface(null,Typeface.BOLD); layoutParams=LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f); gravity=Gravity.CENTER; setOnClickListener{onClick()}
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(this).build()
        playerView.player = player; playerView.useController = false
        player.addListener(object:Player.Listener{
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                isPlayerPlaying=isPlaying; btnPlayPause.text=if(isPlaying)"⏸️" else "▶️"
                showPlayerControls()
                if(isPlaying) watchStartTime = System.currentTimeMillis()
                else if(watchStartTime > 0) { val watchDuration = System.currentTimeMillis() - watchStartTime; saveWatchTime(watchDuration); watchStartTime = 0 }
            }
            override fun onPlaybackStateChanged(state: Int) {
                if(state==Player.STATE_READY){ seekBar.max=player.duration.toInt(); tvTotalTime.text=formatTime(player.duration) }
                else if(state==Player.STATE_ENDED) playNextChannel()
            }
        })
        thread { while(true){ if(player.isPlaying) runOnUiThread{seekBar.progress=player.currentPosition.toInt(); tvCurrentTime.text=formatTime(player.currentPosition)}; Thread.sleep(500) } }
    }

    private fun formatTime(millis: Long): String {
        val s=(millis/1000)%60; val m=(millis/(1000*60))%60; val h=millis/(1000*60*60)
        return if(h>0) String.format("%d:%02d:%02d",h,m,s) else String.format("%02d:%02d",m,s)
    }

    private fun toggleRecording() { if(isRecording) stopRecording() else startRecording() }

    private fun startRecording() {
        if(currentStreamUrl == null) { Toast.makeText(this,"لا يوجد بث للتسجيل",Toast.LENGTH_SHORT).show(); return }
        try {
            val dir = File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "Recordings"); dir.mkdirs()
            recordingFile = File(dir, "MN-DAZOU_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.ts")
            isRecording = true; btnRecord.text = "⏹️"; btnRecord.setTextColor(Color.RED); tvRecording.visibility = View.VISIBLE
            Toast.makeText(this,"🔴 بدأ التسجيل",Toast.LENGTH_SHORT).show()
            recordingThread = thread {
                try {
                    val input = URL(currentStreamUrl).openStream()
                    val output = FileOutputStream(recordingFile)
                    val buffer = ByteArray(4096)
                    var bytesRead = input.read(buffer)
                    while(isRecording && bytesRead != -1) { output.write(buffer, 0, bytesRead); bytesRead = input.read(buffer) }
                    output.close(); input.close()
                } catch(e: Exception) { runOnUiThread { Toast.makeText(this@MainActivity,"❌ خطأ في التسجيل",Toast.LENGTH_SHORT).show(); stopRecording() } }
            }
        } catch(e: Exception) { Toast.makeText(this,"❌ فشل بدء التسجيل",Toast.LENGTH_SHORT).show() }
    }

    private fun stopRecording() {
        isRecording = false; btnRecord.text = "🔴"; btnRecord.setTextColor(Color.WHITE); tvRecording.visibility = View.GONE
        recordingThread?.interrupt()
        recordingFile?.let { val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE); intent.data = Uri.fromFile(it); sendBroadcast(intent); Toast.makeText(this,"✅ تم حفظ التسجيل: ${it.name}",Toast.LENGTH_LONG).show() }
    }

    private fun shareCurrentStream() {
        currentStreamName?.let { name ->
            val shareText = "شاهد معي على MN-DAZOU IPTV:\n📺 $name\n🔗 ${currentStreamUrl ?: ""}"
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, shareText); putExtra(Intent.EXTRA_SUBJECT, name) }, "مشاركة عبر"))
        } ?: Toast.makeText(this,"لا يوجد محتوى للمشاركة",Toast.LENGTH_SHORT).show()
    }

    private fun saveAccounts() { val json = JSONArray(); accounts.forEach { val obj = JSONObject(); obj.put("url", it.url); obj.put("username", it.username); obj.put("password", it.password); json.put(obj) }; prefs.edit().putString("accounts", json.toString()).apply() }
    private fun loadAccounts() { val s = prefs.getString("accounts", "[]") ?: "[]"; val json = JSONArray(s); for(i in 0 until json.length()) { val obj = json.getJSONObject(i); accounts.add(XtreamServer(obj.getString("url"), obj.getString("username"), obj.getString("password"))) } }

    private fun showAccountsDialog() {
        if(accounts.isEmpty()) { showLoginDialog(); return }
        val names = accounts.mapIndexed { i, a -> "👤 حساب ${i+1}: ${a.username}@${a.url.removePrefix("http://").removePrefix("https://").substringBefore(":")}" }.toTypedArray()
        AlertDialog.Builder(this).setTitle("👤 إدارة الحسابات (${accounts.size})").setItems(names + arrayOf("➕ إضافة حساب", "🗑️ حذف كل الحسابات", "📊 إحصائيات المشاهدة")) { _, which ->
            when {
                which < accounts.size -> { currentAccountIndex = which; server = accounts[which]; prefs.edit().putInt("current_account", which).apply(); switchTab("home"); Toast.makeText(this,"✅ تم التبديل",Toast.LENGTH_SHORT).show() }
                which == accounts.size -> showLoginDialog()
                which == accounts.size + 1 -> { accounts.clear(); saveAccounts(); server = null; Toast.makeText(this,"🗑️ تم الحذف",Toast.LENGTH_SHORT).show(); showLoginDialog() }
                which == accounts.size + 2 -> showWatchStats()
            }
        }.show()
    }

    private fun saveWatchTime(duration: Long) {
        val today = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        prefs.edit().putLong("watch_$today", prefs.getLong("watch_$today", 0) + duration).apply()
        currentStreamName?.let { name -> prefs.edit().putInt("channel_count_$name", prefs.getInt("channel_count_$name", 0) + 1).apply() }
    }

    private fun showWatchStats() {
        val today = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val todayWatch = prefs.getLong("watch_$today", 0)
        val hours = todayWatch / (1000 * 60 * 60); val minutes = (todayWatch % (1000 * 60 * 60)) / (1000 * 60)
        val channelCounts = mutableMapOf<String, Int>()
        prefs.all.forEach { (key, value) -> if(key.startsWith("channel_count_") && value is Int) channelCounts[key.removePrefix("channel_count_")] = value }
        val topChannels = channelCounts.entries.sortedByDescending { it.value }.take(5)
        val message = StringBuilder()
        message.appendLine("📊 إحصائيات اليوم:"); message.appendLine("⏱️ وقت المشاهدة: ${hours}h ${minutes}m"); message.appendLine()
        if(topChannels.isNotEmpty()) { message.appendLine("📺 الأكثر مشاهدة:"); topChannels.forEachIndexed { i, (chname, count) -> message.appendLine("${i+1}. $chname ($count مرة)") } }
        AlertDialog.Builder(this).setTitle("📊 إحصائيات المشاهدة").setMessage(message.toString()).setPositiveButton("حسناً", null).show()
    }

    private fun showPlayerControls() {
        val anim = AlphaAnimation(0f,1f); anim.duration=300
        (playerControlsLayout.parent as? ViewGroup)?.let{vg->for(i in 0 until vg.childCount){val c=vg.getChildAt(i); if(c is LinearLayout){c.visibility=View.VISIBLE; c.startAnimation(anim)}}}
        tvChannelInfo.visibility=View.VISIBLE; tvChannelInfo.startAnimation(anim)
        tvChannelInfo.postDelayed({ hidePlayerControls() },5000)
    }

    private fun hidePlayerControls() {
        (playerControlsLayout.parent as? ViewGroup)?.let{vg->for(i in 0 until vg.childCount){val c=vg.getChildAt(i); if(c is LinearLayout)c.visibility=View.GONE}}
        tvChannelInfo.visibility=View.GONE
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?) = when(keyCode){
        KeyEvent.KEYCODE_DPAD_RIGHT,KeyEvent.KEYCODE_MEDIA_NEXT->{playNextChannel();true}
        KeyEvent.KEYCODE_DPAD_LEFT,KeyEvent.KEYCODE_MEDIA_PREVIOUS->{playPreviousChannel();true}
        KeyEvent.KEYCODE_DPAD_CENTER,KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE->{togglePlayPause();true}
        KeyEvent.KEYCODE_VOLUME_UP->{audioManager.adjustVolume(AudioManager.ADJUST_RAISE,AudioManager.FLAG_SHOW_UI);true}
        KeyEvent.KEYCODE_VOLUME_DOWN->{audioManager.adjustVolume(AudioManager.ADJUST_LOWER,AudioManager.FLAG_SHOW_UI);true}
        KeyEvent.KEYCODE_MEDIA_RECORD->{toggleRecording();true}
        else->super.onKeyDown(keyCode,event)
    }

    private fun togglePlayPause() { if(isPlayerPlaying) player.pause() else player.play() }
    private fun changeAspectRatio() { currentAspectRatio=(currentAspectRatio+1)%3; playerView.resizeMode=when(currentAspectRatio){0->androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT;1->androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL;else->androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM}; Toast.makeText(this,"📐 ${arrayOf("ملائم","ملء","تكبير")[currentAspectRatio]}",Toast.LENGTH_SHORT).show() }
    private fun toggleFullscreen() { if(playerView.layoutParams.height==420){playerView.layoutParams.height=ViewGroup.LayoutParams.MATCH_PARENT;Toast.makeText(this,"⛶ ملء الشاشة",Toast.LENGTH_SHORT).show()}else{playerView.layoutParams.height=420;Toast.makeText(this,"📱 وضع عادي",Toast.LENGTH_SHORT).show()};playerView.requestLayout() }
    private fun showQualityDialog() { AlertDialog.Builder(this).setTitle("🎯 جودة الفيديو").setItems(arrayOf("Auto","4K","1080p","720p","480p")){_,_->Toast.makeText(this,"⚙️ تلقائي",Toast.LENGTH_SHORT).show()}.show() }
    private fun downloadCurrentStream() { currentStreamUrl?.let{u->try{downloadId=downloadManager.enqueue(DownloadManager.Request(Uri.parse(u)).setTitle("MN-DAZOU IPTV").setDescription(currentStreamName?:"فيديو").setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED).setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES,"MN-DAZOU_${System.currentTimeMillis()}.mp4"));Toast.makeText(this,"⬇️ جاري التحميل",Toast.LENGTH_SHORT).show()}catch(e:Exception){Toast.makeText(this,"❌ فشل",Toast.LENGTH_SHORT).show()}}?:Toast.makeText(this,"لا يوجد فيديو",Toast.LENGTH_SHORT).show() }
    private fun registerDownloadReceiver() { registerReceiver(object:BroadcastReceiver(){override fun onReceive(c:Context?,i:Intent?){if(i?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID,-1)==downloadId)Toast.makeText(this@MainActivity,"✅ تم التحميل",Toast.LENGTH_SHORT).show()}},IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) }

    private fun playNextChannel() { val channels=when(currentStreamType){"live"->liveChannels;"movie"->vodMovies;else->emptyList()}; if(channels.isEmpty()||currentStreamIndex<0)return; playChannelAtIndex((currentStreamIndex+1)%channels.size) }
    private fun playPreviousChannel() { val channels=when(currentStreamType){"live"->liveChannels;"movie"->vodMovies;else->emptyList()}; if(channels.isEmpty()||currentStreamIndex<0)return; playChannelAtIndex(if(currentStreamIndex-1<0)channels.size-1 else currentStreamIndex-1) }

    private fun playChannelAtIndex(index: Int) {
        val channels=when(currentStreamType){"live"->liveChannels;"movie"->vodMovies;else->emptyList()}
        if(index<0||index>=channels.size)return
        currentStreamIndex=index; val ch=channels[index]
        val url=when(currentStreamType){"live"->XtreamAPI.getStreamUrl(server!!,(ch as XtreamChannel).streamId,ch.containerExtension);"movie"->XtreamAPI.getMovieUrl(server!!,(ch as XtreamMovie).streamId,ch.containerExtension);else->""}
        playStream(url,ch.name); tvChannelInfo.text="🎬 ${ch.name} (${index+1}/${channels.size})"
        prefs.edit().putInt("last_channel_index",index).putString("last_channel_type",currentStreamType).apply(); rv.smoothScrollToPosition(index)
    }

    private fun loadImage(url: String, callback: (Bitmap?) -> Unit) { imageCache[url]?.let{callback(it); return}; thread{try{val conn=URL(url).openConnection() as HttpURLConnection;conn.doInput=true;conn.connect();val bitmap=BitmapFactory.decodeStream(conn.inputStream);bitmap?.let{imageCache[url]=it};runOnUiThread{callback(bitmap)}}catch(e:Exception){runOnUiThread{callback(null)}}} }

    private fun showSettingsDialog() { AlertDialog.Builder(this).setTitle("⚙️ الإعدادات").setItems(arrayOf("🎨 تغيير الثيم","🔗 إعدادات Xtream","📋 إضافة M3U","📺 تحديث EPG","📊 إحصائيات","🗑️ مسح البيانات")){_,w->when(w){0->showThemeDialog();1->showLoginDialog();2->showM3uDialog();3->loadEpg();4->showWatchStats();5->{prefs.edit().clear().apply();accounts.clear();Toast.makeText(this,"تم المسح",Toast.LENGTH_SHORT).show();recreate()}}}.show() }
    private fun showThemeDialog() { AlertDialog.Builder(this).setTitle("🎨 اختر الثيم").setItems(themes.values.map{it.name}.toTypedArray()){_,w->prefs.edit().putString("theme",themes.keys.toList()[w]).apply();Toast.makeText(this,"🔄 أعد التشغيل",Toast.LENGTH_LONG).show()}.show() }
    private fun showM3uDialog() { val e=EditText(this).apply{hint="رابط M3U أو الصق المحتوى";minLines=3;setPadding(30,20,30,20)}; AlertDialog.Builder(this).setTitle("📋 إضافة M3U").setView(e).setPositiveButton("تحميل"){_,_->val i=e.text.toString(); if(i.startsWith("http"))loadM3uFromUrl(i)else parseM3uContent(i)}.setNegativeButton("إلغاء",null).show() }
    private fun loadM3uFromUrl(url: String) { showLoading(); thread{try{val c=java.net.URL(url).readText();runOnUiThread{hideLoading();parseM3uContent(c)}}catch(ex:Exception){runOnUiThread{hideLoading();Toast.makeText(this,"❌ فشل",Toast.LENGTH_SHORT).show()}}} }
    private fun parseM3uContent(content: String) { val ch=mutableListOf<XtreamChannel>(); var nm=""; for(l in content.split("\n")){if(l.startsWith("#EXTINF"))nm=Regex(",(.+)").find(l)?.groupValues?.get(1)?.trim()?:"قناة"; else if(l.startsWith("http"))ch.add(XtreamChannel(ch.size,nm,"live","","","","m3u","ts"))}; liveChannels.addAll(ch); Toast.makeText(this,"✅ ${ch.size} قناة",Toast.LENGTH_SHORT).show(); updateLiveList() }

    private fun loadEpg() { server?.let{s->showLoading(); thread{try{val j=java.net.URL("${s.url}/player_api.php?username=${s.username}&password=${s.password}&action=get_short_epg").readText();epgData.clear();val a=JSONObject(j).getJSONArray("epg_listings");for(i in 0 until a.length()){val o=a.getJSONObject(i);epgData.add(EpgProgram(o.optString("epg_id",""),o.optString("title",""),o.optString("start",""),o.optString("end",""),o.optString("description","")))};runOnUiThread{hideLoading();Toast.makeText(this,"📺 ${epgData.size} برنامج",Toast.LENGTH_SHORT).show();showEpg()}}catch(ex:Exception){runOnUiThread{hideLoading();Toast.makeText(this,"❌ فشل EPG",Toast.LENGTH_SHORT).show()}}}}?:Toast.makeText(this,"سجل دخول",Toast.LENGTH_SHORT).show() }

    private fun showEpg() { 
        val g=epgData.groupBy{it.channelId}
        val channelKeys=g.keys.toTypedArray()
        if(channelKeys.isEmpty()){Toast.makeText(this,"لا بيانات",Toast.LENGTH_SHORT).show();return}
        AlertDialog.Builder(this).setTitle("📺 دليل البرامج").setItems(channelKeys){_,i->
            val chName = channelKeys[i]
            val programs=g[chName]?:emptyList()
            val programTexts = programs.map{epg->"🕐 ${epg.startTime}-${epg.endTime}: ${epg.title}"}.toTypedArray()
            AlertDialog.Builder(this).setTitle(chName).setItems(programTexts,null).setPositiveButton("إغلاق",null).show()
        }.setPositiveButton("إغلاق",null).show() 
    }

    private fun switchTab(tab: String) {
        currentCategory=tab; selectedCategoryId=null; isShowingCategories=true; btnBack.visibility=View.GONE; searchLayout.visibility=View.GONE
        val t=themes[currentTheme]!!
        btnHome.setTextColor(t.textGray); btnLive.setTextColor(t.textGray); btnMovies.setTextColor(t.textGray); btnSeries.setTextColor(t.textGray); btnFavorites.setTextColor(t.textGray)
        when(tab){"home"->{btnHome.setTextColor(t.accent);tvTitle.text="🏠 MN-DAZOU IPTV";showHomeScreen()};"live"->{btnLive.setTextColor(t.accent);tvTitle.text="📺 البث المباشر";searchLayout.visibility=View.VISIBLE;currentStreamType="live";loadLiveCategories()};"movies"->{btnMovies.setTextColor(t.accent);tvTitle.text="🎬 الأفلام";searchLayout.visibility=View.VISIBLE;currentStreamType="movie";loadVodCategories()};"series"->{btnSeries.setTextColor(t.accent);tvTitle.text="🎭 المسلسلات";searchLayout.visibility=View.VISIBLE;loadSeriesCategories()};"favorites"->{btnFavorites.setTextColor(t.accent);tvTitle.text="⭐ المفضلة";showFavorites()}}
    }

    private fun showHomeScreen() {
        val t=themes[currentTheme]!!; val items=mutableListOf<Any>()
        if(watchHistory.isNotEmpty()){items.add("section_history");items.addAll(watchHistory.takeLast(5).reversed())}
        if(favorites.isNotEmpty()){items.add("section_favorites");items.addAll(favorites.take(5))}
        items.add("section_quick");items.add("quick_live");items.add("quick_movies");items.add("quick_series");items.add("quick_epg");items.add("quick_stats");items.add("quick_recordings")
        if(items.isEmpty())items.add("empty")
        rv.adapter=object:RecyclerView.Adapter<RecyclerView.ViewHolder>(){
            override fun getItemViewType(p:Int)=if(items[p] is String)0 else 1
            override fun onCreateViewHolder(p:ViewGroup,vt:Int):RecyclerView.ViewHolder{
                if(vt==0){val tv=TextView(p.context).apply{setPadding(20,15,20,10);textSize=16f;setTextColor(t.accent);setTypeface(null,Typeface.BOLD)};return object:RecyclerView.ViewHolder(tv){}}
                else{val c=CardView(p.context).apply{layoutParams=ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT).apply{setMargins(15,6,15,6)};radius=12f;setCardBackgroundColor(t.card);cardElevation=3f};val l=LinearLayout(p.context).apply{orientation=LinearLayout.HORIZONTAL;setPadding(20,15,20,15);gravity=Gravity.CENTER_VERTICAL};val iv=ImageView(p.context).apply{layoutParams=LinearLayout.LayoutParams(50,50);setBackgroundColor(t.activeTab)};l.addView(iv);l.addView(TextView(p.context).apply{setPadding(15,0,0,0);textSize=15f;setTextColor(t.textWhite);setTypeface(null,Typeface.BOLD);layoutParams=LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f)});c.addView(l);return object:RecyclerView.ViewHolder(c){}}
            }
            override fun onBindViewHolder(h:RecyclerView.ViewHolder,p:Int){
                val item=items[p]
                if(item is String){val tv=h.itemView as TextView
                    when(item){"section_history"->tv.text="🕐 آخر المشاهدات";"section_favorites"->tv.text="⭐ المفضلة";"section_quick"->tv.text="🚀 وصول سريع"
                        "quick_live"->{tv.text="📺 البث المباشر";tv.setOnClickListener{switchTab("live")}}
                        "quick_movies"->{tv.text="🎬 الأفلام";tv.setOnClickListener{switchTab("movies")}}
                        "quick_series"->{tv.text="🎭 المسلسلات";tv.setOnClickListener{switchTab("series")}}
                        "quick_epg"->{tv.text="📋 دليل البرامج";tv.setOnClickListener{loadEpg()}}
                        "quick_stats"->{tv.text="📊 إحصائيات المشاهدة";tv.setOnClickListener{showWatchStats()}}
                        "quick_recordings"->{tv.text="📁 التسجيلات";tv.setOnClickListener{showRecordings()}}
                        "empty"->tv.text="👋 أهلاً بك!"}}
                else if(item is HistoryItem){(h.itemView as CardView).let{c->val l=c.getChildAt(0) as LinearLayout;(l.getChildAt(1) as TextView).text="🕐 ${item.name}";c.setOnClickListener{playHistoryItem(item)};if(item.icon.isNotEmpty())loadImage(item.icon){b->(l.getChildAt(0) as ImageView).setImageBitmap(b?:Bitmap.createBitmap(50,50,Bitmap.Config.ARGB_8888))}}}
                else if(item is FavoriteItem){(h.itemView as CardView).let{c->val l=c.getChildAt(0) as LinearLayout;(l.getChildAt(1) as TextView).text="⭐ ${item.name}";c.setOnClickListener{playFavoriteItem(item)};if(item.icon.isNotEmpty())loadImage(item.icon){b->(l.getChildAt(0) as ImageView).setImageBitmap(b?:Bitmap.createBitmap(50,50,Bitmap.Config.ARGB_8888))}}}
            }
            override fun getItemCount()=items.size
        }
    }

    private fun showRecordings() {
        val dir = File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "Recordings"); dir.mkdirs()
        val files = dir.listFiles()?.filter{it.isFile && it.name.endsWith(".ts")}?.sortedByDescending{it.lastModified()} ?: emptyList()
        if(files.isEmpty()) { Toast.makeText(this,"لا توجد تسجيلات",Toast.LENGTH_SHORT).show(); return }
        AlertDialog.Builder(this).setTitle("📁 التسجيلات (${files.size})").setItems(files.map{it.name}.toTypedArray()){_, i ->
            val intent = Intent(Intent.ACTION_VIEW); intent.setDataAndType(Uri.fromFile(files[i]), "video/*"); intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            try { startActivity(intent) } catch(e:Exception) { Toast.makeText(this,"لا يوجد مشغل فيديو",Toast.LENGTH_SHORT).show() }
        }.setPositiveButton("إغلاق", null).show()
    }

    private fun showFavorites() { val t=themes[currentTheme]!!; rv.adapter=object:RecyclerView.Adapter<RecyclerView.ViewHolder>(){override fun onCreateViewHolder(p:ViewGroup,vt:Int):RecyclerView.ViewHolder{val c=CardView(p.context).apply{layoutParams=ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT).apply{setMargins(15,6,15,6)};radius=12f;setCardBackgroundColor(t.card);cardElevation=3f};val l=LinearLayout(p.context).apply{orientation=LinearLayout.HORIZONTAL;setPadding(20,15,20,15);gravity=Gravity.CENTER_VERTICAL};l.addView(ImageView(p.context).apply{layoutParams=LinearLayout.LayoutParams(45,45);setBackgroundColor(t.activeTab)});l.addView(TextView(p.context).apply{setPadding(15,0,0,0);textSize=15f;setTextColor(t.textWhite);setTypeface(null,Typeface.BOLD);layoutParams=LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f)});l.addView(Button(p.context).apply{text="❌";setBackgroundColor(Color.TRANSPARENT)});c.addView(l);return object:RecyclerView.ViewHolder(c){}};override fun onBindViewHolder(h:RecyclerView.ViewHolder,p:Int){val l=(h.itemView as CardView).getChildAt(0) as LinearLayout;val f=favorites[p];(l.getChildAt(1) as TextView).text="⭐ ${f.name}";h.itemView.setOnClickListener{playFavoriteItem(f)};l.getChildAt(2).setOnClickListener{removeFavorite(f);showFavorites()};if(f.icon.isNotEmpty())loadImage(f.icon){b->(l.getChildAt(0) as ImageView).setImageBitmap(b?:Bitmap.createBitmap(45,45,Bitmap.Config.ARGB_8888))}};override fun getItemCount()=favorites.size} }

    private fun goBackToCategories() { isShowingCategories=true;selectedCategoryId=null;btnBack.visibility=View.GONE;when(currentCategory){"live"->showLiveCategories();"movies"->showVodCategories();"series"->showSeriesCategories()} }

    private fun showLoginDialog() { val t=themes[currentTheme]!!;val d=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(50,50,50,50);setBackgroundColor(t.card)};d.addView(TextView(this).apply{text="⚙️ إضافة حساب Xtream";textSize=20f;setTextColor(t.accent);setTypeface(null,Typeface.BOLD);gravity=Gravity.CENTER;setPadding(0,0,0,30)});val es=EditText(this).apply{hint="رابط السيرفر";setHintTextColor(t.textGray);setTextColor(t.textWhite);setBackgroundColor(t.bg);setPadding(30,20,30,20);setText("http://")};val eu=EditText(this).apply{hint="اسم المستخدم";setHintTextColor(t.textGray);setTextColor(t.textWhite);setBackgroundColor(t.bg);setPadding(30,20,30,20)};val ep=EditText(this).apply{hint="كلمة المرور";setHintTextColor(t.textGray);setTextColor(t.textWhite);setBackgroundColor(t.bg);setPadding(30,20,30,20)};d.addView(es);d.addView(createSpacer(10));d.addView(eu);d.addView(createSpacer(10));d.addView(ep);AlertDialog.Builder(this).setView(d).setPositiveButton("حفظ"){_,_->val acc=XtreamServer(es.text.toString().trimEnd('/'),eu.text.toString(),ep.text.toString());accounts.add(acc);currentAccountIndex=accounts.size-1;server=acc;saveAccounts();prefs.edit().putInt("current_account",currentAccountIndex).apply();Toast.makeText(this,"✅ تم حفظ الحساب",Toast.LENGTH_SHORT).show();switchTab("home")}.setNegativeButton("إلغاء",null).show() }
    private fun createSpacer(h:Int)=View(this).apply{layoutParams=LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,h)}
    private fun performSearch() { val q=etSearch.text.toString().lowercase();if(q.isEmpty())return;when(currentCategory){"live"->{val f=liveChannels.filter{it.name.lowercase().contains(q)};if(f.isNotEmpty()){liveChannels.clear();liveChannels.addAll(f);updateLiveList();tvTitle.text="🔍 $q"}else Toast.makeText(this,"لا نتائج",Toast.LENGTH_SHORT).show()};"movies"->{val f=vodMovies.filter{it.name.lowercase().contains(q)};if(f.isNotEmpty()){vodMovies.clear();vodMovies.addAll(f);updateMoviesList();tvTitle.text="🔍 $q"}else Toast.makeText(this,"لا نتائج",Toast.LENGTH_SHORT).show()}} }

    private fun addToFavorites(type:String,id:Int,name:String,icon:String=""){if(favorites.none{it.type==type&&it.id==id}){favorites.add(FavoriteItem(type,id,name,icon));saveFavorites();Toast.makeText(this,"⭐ تم",Toast.LENGTH_SHORT).show()}else Toast.makeText(this,"موجود",Toast.LENGTH_SHORT).show()}
    private fun removeFavorite(item:FavoriteItem){favorites.removeAll{it.type==item.type&&it.id==item.id};saveFavorites()}
    private fun saveFavorites(){val j=JSONArray();favorites.forEach{val o=JSONObject();o.put("type",it.type);o.put("id",it.id);o.put("name",it.name);o.put("icon",it.icon);j.put(o)};prefs.edit().putString("favorites",j.toString()).apply()}
    private fun loadFavorites(){val s=prefs.getString("favorites","[]")?:return;val j=JSONArray(s);for(i in 0 until j.length()){val o=j.getJSONObject(i);favorites.add(FavoriteItem(o.getString("type"),o.getInt("id"),o.getString("name"),o.optString("icon","")))}}
    private fun addToHistory(type:String,id:Int,name:String,icon:String=""){watchHistory.removeAll{it.type==type&&it.id==id};watchHistory.add(HistoryItem(type,id,name,System.currentTimeMillis(),icon));if(watchHistory.size>20)watchHistory.removeAt(0);saveHistory()}
    private fun saveHistory(){val j=JSONArray();watchHistory.forEach{val o=JSONObject();o.put("type",it.type);o.put("id",it.id);o.put("name",it.name);o.put("timestamp",it.timestamp);o.put("icon",it.icon);j.put(o)};prefs.edit().putString("history",j.toString()).apply()}
    private fun loadHistory(){val s=prefs.getString("history","[]")?:return;val j=JSONArray(s);for(i in 0 until j.length()){val o=j.getJSONObject(i);watchHistory.add(HistoryItem(o.getString("type"),o.getInt("id"),o.getString("name"),o.getLong("timestamp"),o.optString("icon","")))}}
    private fun playFavoriteItem(fav:FavoriteItem){when(fav.type){"live"->{val u=XtreamAPI.getStreamUrl(server!!,fav.id);playStream(u,fav.name);addToHistory("live",fav.id,fav.name,fav.icon)};"movie"->{val u=XtreamAPI.getMovieUrl(server!!,fav.id);playStream(u,fav.name);addToHistory("movie",fav.id,fav.name,fav.icon)}}}
    private fun playHistoryItem(item:HistoryItem){when(item.type){"live"->playStream(XtreamAPI.getStreamUrl(server!!,item.id),item.name);"movie"->playStream(XtreamAPI.getMovieUrl(server!!,item.id),item.name)}}

    private fun loadLiveCategories(){server?.let{s->showLoading();XtreamAPI.getLiveCategories(s){liveCategories.clear();liveCategories.addAll(it);hideLoading();if(it.isEmpty())loadLiveStreams(null)else showLiveCategories()}}?:showLoginDialog()}
    private fun loadVodCategories(){server?.let{s->showLoading();XtreamAPI.getVodCategories(s){vodCategories.clear();vodCategories.addAll(it);hideLoading();if(it.isEmpty())loadMovies(null)else showVodCategories()}}?:showLoginDialog()}
    private fun loadSeriesCategories(){server?.let{s->showLoading();XtreamAPI.getLiveCategories(s){seriesCategories.clear();seriesCategories.addAll(it);hideLoading();if(it.isEmpty())loadSeriesList(null)else showSeriesCategories()}}?:showLoginDialog()}

    private fun showLiveCategories(){val t=themes[currentTheme]!!;tvTitle.text="📺 المجموعات (${liveCategories.size})";rv.adapter=createCategoryAdapter(liveCategories,"📁",t.accent){selectedCategoryId=it.categoryId;isShowingCategories=false;btnBack.visibility=View.VISIBLE;tvTitle.text="📺 ${it.categoryName}";loadLiveStreams(it.categoryId)}}
    private fun showVodCategories(){val t=themes[currentTheme]!!;tvTitle.text="🎬 المجموعات (${vodCategories.size})";rv.adapter=createCategoryAdapter(vodCategories,"🎬",t.accent){selectedCategoryId=it.categoryId;isShowingCategories=false;btnBack.visibility=View.VISIBLE;tvTitle.text="🎬 ${it.categoryName}";loadMovies(it.categoryId)}}
    private fun showSeriesCategories(){val t=themes[currentTheme]!!;tvTitle.text="🎭 المجموعات (${seriesCategories.size})";rv.adapter=createCategoryAdapter(seriesCategories,"📺",t.accent){selectedCategoryId=it.categoryId;isShowingCategories=false;btnBack.visibility=View.VISIBLE;tvTitle.text="🎭 ${it.categoryName}";loadSeriesList(it.categoryId)}}

    private fun createCategoryAdapter(cats:List<XtreamCategory>,icon:String,color:Int,onClick:(XtreamCategory)->Unit):RecyclerView.Adapter<RecyclerView.ViewHolder>{val t=themes[currentTheme]!!;return object:RecyclerView.Adapter<RecyclerView.ViewHolder>(){override fun onCreateViewHolder(p:ViewGroup,vt:Int):RecyclerView.ViewHolder{val c=CardView(p.context).apply{layoutParams=ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT).apply{setMargins(15,8,15,8)};radius=12f;setCardBackgroundColor(t.card);cardElevation=4f};val l=LinearLayout(p.context).apply{orientation=LinearLayout.HORIZONTAL;setPadding(30,25,30,25);gravity=Gravity.CENTER_VERTICAL};l.addView(TextView(p.context).apply{text=icon;textSize=24f});l.addView(TextView(p.context).apply{setPadding(20,0,0,0);textSize=16f;setTextColor(t.textWhite);setTypeface(null,Typeface.BOLD);layoutParams=LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f)});l.addView(TextView(p.context).apply{text="→";textSize=20f;setTextColor(color)});c.addView(l);return object:RecyclerView.ViewHolder(c){}}
        override fun onBindViewHolder(h:RecyclerView.ViewHolder,p:Int){val l=(h.itemView as CardView).getChildAt(0) as LinearLayout;(l.getChildAt(1) as TextView).text=cats[p].categoryName;h.itemView.setOnClickListener{onClick(cats[p])}}
        override fun getItemCount()=cats.size}}

    private fun loadLiveStreams(catId:String?){server?.let{s->showLoading();XtreamAPI.getLiveStreams(s,catId){hideLoading();liveChannels.clear();liveChannels.addAll(it);currentStreamIndex=-1;updateLiveList()}}}
    private fun loadMovies(catId:String?){server?.let{s->showLoading();XtreamAPI.getVodStreams(s,catId){hideLoading();vodMovies.clear();vodMovies.addAll(it);currentStreamIndex=-1;updateMoviesList()}}}
    private fun loadSeriesList(catId:String?){server?.let{s->showLoading();XtreamAPI.getSeries(s,catId){hideLoading();seriesList.clear();seriesList.addAll(it);updateSeriesList()}}}

    private fun showLoading(){progressBar.visibility=View.VISIBLE}
    private fun hideLoading(){progressBar.visibility=View.GONE}

    private fun updateLiveList(){val t=themes[currentTheme]!!;rv.adapter=createContentAdapter(liveChannels.map{Pair(it.name,it.streamIcon)},"📺",t){itemName,imgUrl->val idx=liveChannels.indexOfFirst{it.name==itemName};currentStreamIndex=idx;currentStreamType="live";val ch=liveChannels[idx];val u=XtreamAPI.getStreamUrl(server!!,ch.streamId,ch.containerExtension);addToHistory("live",ch.streamId,ch.name,imgUrl);playStream(u,ch.name)}}
    private fun updateMoviesList(){val t=themes[currentTheme]!!;rv.adapter=createContentAdapter(vodMovies.map{Pair(it.name,it.streamIcon)},"🎬",t){itemName,imgUrl->val idx=vodMovies.indexOfFirst{it.name==itemName};currentStreamIndex=idx;currentStreamType="movie";val m=vodMovies[idx];val u=XtreamAPI.getMovieUrl(server!!,m.streamId,m.containerExtension);addToHistory("movie",m.streamId,m.name,imgUrl);playStream(u,m.name)}}
    private fun updateSeriesList(){val t=themes[currentTheme]!!;rv.adapter=createContentAdapter(seriesList.map{Pair(it.name,it.cover)},"📺",t){itemName,_->val s=seriesList.find{it.name==itemName}!!;XtreamAPI.getSeriesInfo(server!!,s.seriesId){showEpisodesDialog(s.name,it)}}}

    private fun createContentAdapter(items:List<Pair<String,String>>,icon:String,theme:ThemeColors,onClick:(String,String)->Unit):RecyclerView.Adapter<RecyclerView.ViewHolder>{
        return object:RecyclerView.Adapter<RecyclerView.ViewHolder>(){
            override fun onCreateViewHolder(p:ViewGroup,vt:Int):RecyclerView.ViewHolder{
                val c=CardView(p.context).apply{layoutParams=ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT).apply{setMargins(15,5,15,5)};radius=10f;setCardBackgroundColor(theme.card);cardElevation=2f}
                val l=LinearLayout(p.context).apply{orientation=LinearLayout.HORIZONTAL;setPadding(15,12,15,12);gravity=Gravity.CENTER_VERTICAL}
                val iv=ImageView(p.context).apply{layoutParams=LinearLayout.LayoutParams(55,55);setBackgroundColor(theme.activeTab)}
                l.addView(iv)
                l.addView(TextView(p.context).apply{setPadding(12,0,0,0);textSize=15f;setTextColor(theme.textWhite);setTypeface(null,Typeface.BOLD);layoutParams=LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f)})
                l.addView(Button(p.context).apply{text="⭐";setBackgroundColor(Color.TRANSPARENT)})
                c.addView(l)
                return object:RecyclerView.ViewHolder(c){}
            }
            override fun onBindViewHolder(h:RecyclerView.ViewHolder,p:Int){
                val l=(h.itemView as CardView).getChildAt(0) as LinearLayout
                val iv=l.getChildAt(0) as ImageView
                val tv=l.getChildAt(1) as TextView
                val itemName = items[p].first
                val imgUrl = items[p].second
                tv.text="$icon $itemName"
                h.itemView.setOnClickListener{onClick(itemName,imgUrl)}
                l.getChildAt(2).setOnClickListener{
                    val type=if(currentStreamType=="live")"live" else "movie"
                    val id=if(type=="live")liveChannels.getOrNull(p)?.streamId ?: 0 else vodMovies.getOrNull(p)?.streamId ?: 0
                    addToFavorites(type,id,itemName,imgUrl)
                }
                if(imgUrl.isNotEmpty())loadImage(imgUrl){b->iv.setImageBitmap(b?:Bitmap.createBitmap(55,55,Bitmap.Config.ARGB_8888))}
            }
            override fun getItemCount()=items.size
        }
    }

    private fun showEpisodesDialog(name:String,episodes:List<XtreamEpisode>){AlertDialog.Builder(this).setTitle(name).setItems(episodes.map{ep->"🎭 حلقة ${ep.episodeNum}: ${ep.title}"}.toTypedArray()){_,i->val e=episodes[i];playStream(XtreamAPI.getSeriesEpisodeUrl(server!!,e.id,e.containerExtension),"$name - حلقة ${e.episodeNum}")}.setNegativeButton("إغلاق",null).show()}

    private fun playStream(url:String,name:String){try{currentStreamUrl=url;currentStreamName=name;player.setMediaItem(MediaItem.fromUri(url));player.prepare();player.play();tvChannelInfo.text="🎬 $name";watchStartTime=System.currentTimeMillis();Toast.makeText(this,"▶️ $name",Toast.LENGTH_SHORT).show()}catch(e:Exception){Toast.makeText(this,"❌ ${e.message}",Toast.LENGTH_LONG).show()}}

    override fun onDestroy(){ if(isRecording) stopRecording(); super.onDestroy(); player.release() }
}