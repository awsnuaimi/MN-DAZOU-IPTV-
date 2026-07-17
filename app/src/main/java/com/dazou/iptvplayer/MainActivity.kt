package com.dazou.iptvplayer

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.Player
import com.dazou.iptvplayer.adapter.ChannelAdapter
import com.dazou.iptvplayer.databinding.ActivityMainBinding
import com.dazou.iptvplayer.fragments.*
import com.dazou.iptvplayer.model.XtreamChannel
import com.dazou.iptvplayer.player.PlayerManager
import com.dazou.iptvplayer.viewmodel.MainViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var playerManager: PlayerManager
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. تهيئة ViewModel
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        // 2. تهيئة المشغل (Player)
        playerManager = PlayerManager(this)
        binding.videoPlayer.player = playerManager.player
        setupPlayerListener()

        // 3. مراقبة تغييرات الحالة من ViewModel
        observeViewModel()

        // 4. إعداد القائمة والـ Fragments
        setupNavigation()
        setupBackPress()

        // افتح القنوات افتراضياً عند البدء
        if (viewModel.channels.value.isNullOrEmpty()) {
            // محاكاة بيانات أولية (أو جلبها من الـ Repository)
            viewModel.channels.value = listOf(XtreamChannel(1, "beIN Sports 1", "live", "", "", "", "", "ts"))
        }
    }

    private fun setupPlayerListener() {
        playerManager.setListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                // تحديث زر التشغيل/الإيقاف بناءً على الحالة الحقيقية للمشغل
                binding.btnPlayPause.setImageResource(
                    if (isPlaying) android.R.drawable.ic_media_pause 
                    else android.R.drawable.ic_media_play
                )
            }
        })
        
        // تفعيل أزرار المشغل
        binding.btnPlayPause.setOnClickListener {
            if (playerManager.player.isPlaying) playerManager.pause() else playerManager.resume()
        }
        binding.btnFullscreen.setOnClickListener { viewModel.toggleFullscreen() }
        // التالي/السابق (سيتم تفعيله لاحقاً عبر القائمة)
    }

    private fun observeViewModel() {
        // مراقبة القناة النشطة وتشغيلها
        viewModel.activeChannel.observe(this) { channel ->
            if (channel != null) {
                // توليد رابط التشغيل
                val url = "http://example.com/live/${channel.streamId}.ts" // مثال (استخدم الـ API هنا)
                playerManager.play(url)
                binding.channelInfo.text = "📺 ${channel.name}"
            }
        }

        // مراقبة وضع ملء الشاشة وتنفيذه بشكل صحيح
        viewModel.isFullscreen.observe(this) { isFullscreen ->
            if (isFullscreen) {
                // إخفاء شريط الحالة والتحكمات
                WindowCompat.setDecorFitsSystemWindows(window, false)
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                WindowInsetsControllerCompat(window, binding.videoPlayer).hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            } else {
                // إعادتها
                WindowCompat.setDecorFitsSystemWindows(window, true)
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }
        }

        // مراقبة تغيير القنوات وتحديث القائمة الجانبية
        viewModel.channels.observe(this) { channels ->
            binding.channelList.adapter = ChannelAdapter(channels) { channel ->
                viewModel.playChannel(channel)
            }
        }
    }

    private fun setupNavigation() {
        val fragments = mapOf(
            binding.menuHome to HomeFragment(),
            binding.menuLive to LiveFragment(),
            binding.menuMovies to MoviesFragment(),
            binding.menuSeries to SeriesFragment(),
            binding.menuEpg to EpgFragment()
        )
        for ((view, fragment) in fragments) {
            view.setOnClickListener { loadFragment(fragment) }
        }
        binding.settings.setOnClickListener { loadFragment(SettingsFragment()) }
        binding.account.setOnClickListener { loadFragment(AccountsFragment()) }

        // تحميل الصفحة الرئيسية كبداية
        loadFragment(HomeFragment())
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment)
            .commit()
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val current = supportFragmentManager.findFragmentById(binding.fragmentContainer.id)
                if (current is BackHandledFragment && current.onBackPressedInFragment()) {
                    return
                }
                // إغلاق التطبيق إذا كان الـ Fullscreen مغلق
                if (viewModel.isFullscreen.value == false) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                } else {
                    viewModel.toggleFullscreen()
                }
            }
        })
    }

    // اعتراض متقدم للريموت
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    // تنفيذ المنطق الخاص بالقائمة الجانبية أو العودة
                    val current = supportFragmentManager.findFragmentById(binding.fragmentContainer.id)
                    if (current is BackHandledFragment && current.onBackPressedInFragment()) {
                        return true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {
        playerManager.release()
        super.onDestroy()
    }
}