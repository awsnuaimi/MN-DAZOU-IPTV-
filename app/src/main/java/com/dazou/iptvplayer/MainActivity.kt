package com.dazou.iptvplayer

import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Rational
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dazou.iptvplayer.adapter.CategoryAdapter
import com.dazou.iptvplayer.adapter.ChannelAdapter
import com.dazou.iptvplayer.adapter.SearchResultAdapter
import com.dazou.iptvplayer.api.XtreamAPI
import com.dazou.iptvplayer.databinding.ActivityMainBinding
import com.dazou.iptvplayer.fragments.*
import com.dazou.iptvplayer.model.HistoryItem
import com.dazou.iptvplayer.model.SearchResultItem
import com.dazou.iptvplayer.model.XtreamCategory
import com.dazou.iptvplayer.model.XtreamChannel
import com.dazou.iptvplayer.model.XtreamEpisode
import com.dazou.iptvplayer.model.XtreamMovie
import com.dazou.iptvplayer.model.XtreamSeries
import com.dazou.iptvplayer.player.PlayerCallback
import com.dazou.iptvplayer.player.PlayerControlsController
import com.dazou.iptvplayer.player.PlayerManager
import com.dazou.iptvplayer.utils.CategoryGrouper
import com.dazou.iptvplayer.utils.NetworkMonitor
import com.dazou.iptvplayer.utils.ThemeManager
import com.dazou.iptvplayer.utils.UpdateManager
import com.dazou.iptvplayer.utils.UsageTracker
import com.dazou.iptvplayer.model.FavoriteItem
import com.bumptech.glide.Glide
import android.widget.LinearLayout
import android.widget.ImageView
import android.view.Gravity
import android.text.TextUtils
import android.widget.TextView
import com.dazou.iptvplayer.viewmodel.LiveViewModel
import com.dazou.iptvplayer.viewmodel.ViewModelFactory

/** ✅ الحل الجذري لمشكلة "الفوكس بيهرب للقائمة العلوية" — معروفة ومؤكدة بمصادر
 * أندرويد TV متعددة: لما RecyclerView يفشل يلاقي العنصر التالي للفوكس داخليًا
 * (onFocusSearchFailed)، الافتراضي بيخلي أندرويد يدور "برّا" القائمة، وغالبًا
 * بينتهي عند أقرب عنصر تنقّل (زي القائمة العلوية). هالكلاس بيمنع هالهروب:
 * لو فشل البحث الداخلي، منضل واقفين على نفس العنصر الحالي بدل ما نسيب النظام
 * يدور براني ويوصل لمكان غير متوقع.
 */
class SafeLinearLayoutManager(context: android.content.Context) :
    LinearLayoutManager(context) {
    override fun onFocusSearchFailed(
        focused: View,
        focusDirection: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ): View? {
        val result = super.onFocusSearchFailed(focused, focusDirection, recycler, state)
        if (result != null) return result
        return focused
    }
}

class MainActivity : AppCompatActivity(), PlayerCallback {

    private lateinit var binding: ActivityMainBinding
    lateinit var playerManager: PlayerManager
    private lateinit var liveViewModel: LiveViewModel
    private lateinit var controlsController: PlayerControlsController
    private lateinit var networkMonitor: NetworkMonitor

    private var fullscreen = false
    private var currentChannelName = ""
    private var wasPlayingBeforeBackground = false
    private var lastCategories: List<XtreamCategory> = emptyList()
    private var currentChannelList: List<XtreamChannel> = emptyList()
    private var currentChannelIndex: Int = -1
    private var wasChannelsPanelOpenBeforeFullscreen = false
    private var currentCategoryId: String? = null
    // ✅ آخر موضع كان عليه الفوكس بقائمة الفئات الرئيسية — نستخدمه لنرجّع الفوكس
    // بدقة لنفس المكان لما المستخدم يرجع من قائمة القنوات أو المجلدات الفرعية
    private var lastFocusedCategoryPosition = 0
    // ✅ "الذكاء البسيط" — نافذة تصفح شريط القنوات الأكتر مشاهدة (تحت EPG)
    private var mostWatchedBrowseWindowStart = 0
    private enum class MostWatchedFocusRequest { NONE, FIRST, LAST }
    private var currentCategoryName: String = ""
    private var pendingAutoPlayChannelId: Int? = null
    private var pendingGroupCode: String? = null
    private val categoryGroupMap = mutableMapOf<String, List<String>>()
    private val categoryGroupDetailsMap = mutableMapOf<String, List<XtreamCategory>>()

    private var currentPlayingType: String = ""
    private var currentPlayingId: Int = -1

    private var nextEpisodeProvider: (() -> Unit)? = null
    private var autoNextTimer: CountDownTimer? = null
    private var autoNextDialog: AlertDialog? = null

    // ✅ جديد: مؤقت النوم
    private var sleepTimer: CountDownTimer? = null
    private var sleepMinutesActive: Int = 0

    private val clockHandler = Handler(Looper.getMainLooper())
    private var clockRunnable: Runnable? = null

    private val progressHandler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null

    private var topBarLogoAnimator: ObjectAnimator? = null

    // ========== ✅ البحث الشامل (قنوات + أفلام + مسلسلات) ==========
    private var searchDataLoaded = false
    private var searchDataLoading = false
    private var cachedSearchChannels: List<XtreamChannel> = emptyList()
    private var cachedSearchMovies: List<XtreamMovie> = emptyList()
    private var cachedSearchSeries: List<XtreamSeries> = emptyList()
    private var searchLoadedParts = 0
    private var selectedSearchSeries: XtreamSeries? = null
    private var searchEpisodeList: List<XtreamEpisode> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startTopBarLogoAnimation()

        // ============================================================
        // 🔧 سجل تشخيص الفوكس — أداة مطوّرين، يشتغل بس لو مفعّل من الإعدادات
        // (Settings → تفعيل سجل تشخيص الفوكس). بيعرض بأعلى الشاشة سجل بآخر
        // 8 حركات فوكس بالترتيب الزمني، مفيد جدًا لو صارت مشكلة تنقل بالريموت
        // بالمستقبل وحبينا نشخصها بسرعة.
        // ============================================================
        val debugFocusEnabled = getSharedPreferences("dazou_prefs", MODE_PRIVATE)
            .getBoolean("debug_focus_log_enabled", false)
        if (debugFocusEnabled) {
            val debugFocusText = android.widget.TextView(this).apply {
                setBackgroundColor(0xDD000000.toInt())
                setTextColor(0xFF00FF00.toInt())
                textSize = 11f
                setPadding(14, 8, 14, 8)
                text = "Focus log:\n-"
                elevation = 9999f
            }
            val debugRootContent = findViewById<android.view.ViewGroup>(android.R.id.content)
            val debugLp = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.TOP or android.view.Gravity.START
                topMargin = 24
                leftMargin = 24
            }
            debugRootContent.addView(debugFocusText, debugLp)
            val debugStartTime = System.currentTimeMillis()
            val debugFocusLog = mutableListOf<String>()
            var debugSeq = 0
            window.decorView.viewTreeObserver.addOnGlobalFocusChangeListener { oldFocus, newFocus ->
                fun nameOf(v: View?): String = try {
                    when {
                        v == null -> "NULL"
                        v.id != View.NO_ID -> resources.getResourceEntryName(v.id)
                        else -> "no-id(${v.javaClass.simpleName})"
                    }
                } catch (e: Exception) { "؟" }
                debugSeq++
                val t = System.currentTimeMillis() - debugStartTime
                debugFocusLog.add("#$debugSeq [${t}ms] ${nameOf(oldFocus)} → ${nameOf(newFocus)}")
                while (debugFocusLog.size > 8) debugFocusLog.removeAt(0)
                debugFocusText.text = "Focus log:\n" + debugFocusLog.joinToString("\n")
            }
        }
        // ============================================================

        playerManager = PlayerManager(this)
        binding.videoPlayer.player = playerManager.player

        controlsController = PlayerControlsController(this, binding, playerManager) { index ->
            playChannelAt(index)
        }
        controlsController.setup(
            onPrev = { onPreviousChannel() },
            onNext = { onNextChannel() },
            onFullscreenToggle = { toggleFullscreen() },
            onPip = { enterPipMode() },
            onManualRetry = { playerManager.manualRetry() }
        )

        binding.btnSleepTimer.setOnClickListener {
            showSleepTimerPicker()
        }

        binding.btnFullscreenSmall.setOnClickListener {
            toggleFullscreen()
        }

        // ✅ ساعة المشغل: نطبّق الحالة المحفوظة (ظاهرة/مخفية) فور فتح
        // التطبيق، ونربط الزر يبدّلها ويحفظ الاختيار الجديد
        applyPlayerClockVisibility()
        binding.btnClockToggle.setOnClickListener {
            val prefs = getSharedPreferences("dazou_prefs", MODE_PRIVATE)
            val currentlyVisible = prefs.getBoolean("player_clock_visible", true)
            prefs.edit().putBoolean("player_clock_visible", !currentlyVisible).apply()
            applyPlayerClockVisibility()
        }

        val app = application as App
        liveViewModel = ViewModelProvider(this, ViewModelFactory(app.container.currentRepository))
            .get(LiveViewModel::class.java)

        binding.categoryList.layoutManager = SafeLinearLayoutManager(this)
        binding.channelList.layoutManager = SafeLinearLayoutManager(this)

        liveViewModel.categories.observe(this) { categories ->
            lastCategories = categories
            if (categories.isEmpty()) {
                Toast.makeText(this, getString(R.string.live_no_categories), Toast.LENGTH_LONG).show()
            }
            val displayCategories = CategoryGrouper.buildDisplayCategories(
                this@MainActivity, categories, categoryGroupMap, categoryGroupDetailsMap
            )
            binding.categoryList.adapter = CategoryAdapter(
                displayCategories,
                R.id.channel_list,
                onItemFocused = { pos -> lastFocusedCategoryPosition = pos }
            ) { category ->
                openCategory(category)
            }

            val code = pendingGroupCode
            if (code != null) {
                pendingGroupCode = null
                val ids = categoryGroupMap[code] ?: emptyList()
                loadMergedChannels(ids) { merged -> applyChannelResults(merged) }
            }
        }

        liveViewModel.channels.observe(this) { channels ->
            applyChannelResults(channels)
        }

        binding.channelsPanelBack.setOnClickListener {
            hideChannelsPanel()
        }

        startClock()
        startProgressTracking()
        setupWifiStatus()
        setupThemeToggle()
        setupPlayerErrorHandling()
        setupMenu()
        setupSearch()

        val hasAccount = app.container.accountManager.getActiveAccount() != null

        if (hasAccount) {
            showMainUi()
            // ✅ "الذكاء البسيط" — نفتح آخر قسم كان فيه المستخدم (رئيسية/مباشر/
            // أفلام/مسلسلات) بدل ما نفتح البث المباشر دايمًا بشكل ثابت
            when (getSharedPreferences("dazou_prefs", MODE_PRIVATE).getString("last_section", "live")) {
                "home" -> loadFragment(HomeFragment())
                "movies" -> loadFragment(MoviesFragment())
                "series" -> loadFragment(SeriesFragment())
                else -> restoreLastSessionOrShowCategories()
            }
            binding.menuHome.requestFocus()
        } else {
            showLoginUi()
            loadFragment(LoginFragment())
        }

        // ✅ فحص تحديث صامت عند فتح التطبيق (ما بيطلع شي لو ما في تحديث جديد)
        UpdateManager.checkForUpdate(this, silent = true)

        // ✅ لازم يكون آخر سطر بالدالة — حتى ما في أي كود تاني يقدر يكتب فوق ألوان التيم
        ThemeManager.applyToMainScreen(this, binding)
    }

    /** ✅ "الذكاء البسيط" — بعد ما المستخدم يشوف قناة معيّنة عدد كافي من
     * المرات (وهي مش مضافة للمفضلة أصلاً)، منقترح عليه يضيفها. مرة وحدة بس
     * لكل قناة (مش إزعاج متكرر) */
    private fun showFavoriteSuggestionDialog(channel: XtreamChannel) {
        AlertDialog.Builder(this)
            .setTitle("⭐ ${channel.name}")
            .setMessage("لاحظنا إنك بتتابع هالقناة كتير — بدك تضيفها للمفضلة عشان توصلها أسرع بالمرة الجاية؟")
            .setPositiveButton(getString(R.string.common_yes)) { dialog, _ ->
                val added = (application as App).container.favoritesManager.toggleFavorite(
                    FavoriteItem("live", channel.streamId, channel.name, channel.streamIcon, channel.containerExtension)
                )
                if (added) {
                    Toast.makeText(this, getString(R.string.favorite_added_channel), Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.common_cancel)) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    /** ✅ يفتح قائمة اختيار مدة مؤقت النوم، أو إيقافه لو مفعّل حاليًا */
    private fun showSleepTimerPicker() {
        val options = listOf(
            getString(R.string.sleep_timer_off) to 0,
            getString(R.string.sleep_timer_15) to 15,
            getString(R.string.sleep_timer_30) to 30,
            getString(R.string.sleep_timer_45) to 45,
            getString(R.string.sleep_timer_60) to 60
        )
        val labels = options.map { it.first }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.sleep_timer_title))
            .setItems(labels) { _, which ->
                val minutes = options[which].second
                if (minutes == 0) {
                    cancelSleepTimer()
                } else {
                    startSleepTimer(minutes)
                }
            }
            .show()
    }

    private fun startSleepTimer(minutes: Int) {
        sleepTimer?.cancel()
        sleepMinutesActive = minutes
        updateSleepTimerIcon()

        sleepTimer = object : CountDownTimer(minutes * 60_000L, 60_000L) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                playerManager.pause()
                sleepMinutesActive = 0
                updateSleepTimerIcon()
                Toast.makeText(this@MainActivity, getString(R.string.sleep_timer_finished), Toast.LENGTH_LONG).show()
            }
        }.start()

        Toast.makeText(this, getString(R.string.sleep_timer_set, minutes), Toast.LENGTH_SHORT).show()
    }

    private fun cancelSleepTimer() {
        sleepTimer?.cancel()
        sleepTimer = null
        sleepMinutesActive = 0
        updateSleepTimerIcon()
        Toast.makeText(this, getString(R.string.sleep_timer_cancelled), Toast.LENGTH_SHORT).show()
    }

    /** ✅ يلوّن أيقونة مؤقت النوم بلون التيم المختار لما يكون مفعّل، وشفاف لما يكون متوقف */
    private fun updateSleepTimerIcon() {
        if (sleepMinutesActive > 0) {
            val theme = ThemeManager.getSavedTheme(this)
            binding.btnSleepTimer.setColorFilter(theme.accent)
        } else {
            binding.btnSleepTimer.clearColorFilter()
        }
    }

    /** ✅ يظهّر أو يخفي ساعة المشغل حسب آخر اختيار محفوظ للمستخدم، وبيلوّن
     * أيقونة الزر عشان توضّح الحالة الحالية بلمحة (زي مؤقت النوم بالظبط) */
    private fun applyPlayerClockVisibility() {
        val prefs = getSharedPreferences("dazou_prefs", MODE_PRIVATE)
        val visible = prefs.getBoolean("player_clock_visible", true)
        binding.tvPlayerClock.visibility = if (visible) View.VISIBLE else View.GONE
        if (visible) {
            binding.btnClockToggle.setTextColor(getColor(R.color.text_white))
        } else {
            val theme = ThemeManager.getSavedTheme(this)
            binding.btnClockToggle.setTextColor(theme.accent)
        }
    }

    fun setNextEpisodeProvider(provider: (() -> Unit)?) {
        nextEpisodeProvider = provider
    }private fun triggerAutoNextEpisode() {
        val provider = nextEpisodeProvider ?: return
        autoNextTimer?.cancel()
        autoNextDialog?.dismiss()

        var secondsLeft = 5
        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.series_next_episode_title))
            .setMessage(getString(R.string.series_next_episode_countdown, secondsLeft))
            .setNegativeButton(getString(R.string.common_cancel)) { d, _ ->
                autoNextTimer?.cancel()
                d.dismiss()
            }
            .setCancelable(true)
            .create()
        dialog.show()
        autoNextDialog = dialog

        autoNextTimer = object : CountDownTimer(5000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                secondsLeft = ((millisUntilFinished + 999) / 1000).toInt()
                if (dialog.isShowing) {
                    dialog.setMessage(getString(R.string.series_next_episode_countdown, secondsLeft))
                }
            }

            override fun onFinish() {
                if (dialog.isShowing) dialog.dismiss()
                nextEpisodeProvider?.invoke()
            }
        }.start()
    }

    private fun startTopBarLogoAnimation() {
        topBarLogoAnimator = ObjectAnimator.ofFloat(binding.ivTopBarLogoRing, "rotation", 0f, 360f).apply {
            duration = 3000L
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }
    }

    private fun requestFocusWhenReady(view: View) {
        if (view.isLaidOut && view.width > 0 && view.height > 0) {
            view.requestFocus()
        } else {
            view.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    view.requestFocus()
                }
            })
        }
    }

    /** ✅ خاص بقوائم RecyclerView تحديدًا — لما نغيّر الـ adapter، الحاوية نفسها
     * ممكن يكون عندها حجم فورًا (كانت موجودة من قبل)، بس عناصرها الجديدة (القنوات)
     * لسا ما اتكوّنت بصريًا. طلب الفوكس فورًا بهاي اللحظة بيفشل بصمت والفوكس بيضيع
     * أو يضل معلّق بمكانه القديم. هالدالة بتستنى فعليًا لحد ما يصير في عنصر أول
     * ظاهر بالقائمة قبل ما تحاول تحط الفوكس عليه. */
    private fun focusFirstItemWhenReady(recyclerView: androidx.recyclerview.widget.RecyclerView, attemptsLeft: Int = 5) {
        recyclerView.post {
            val firstChild = recyclerView.getChildAt(0)
            if (firstChild != null) {
                firstChild.requestFocus()
            } else if (attemptsLeft > 0) {
                // ✅ لسا ما تكوّنت العناصر — نجرب تاني بعد الإطار الجاي، لحد 5 محاولات
                focusFirstItemWhenReady(recyclerView, attemptsLeft - 1)
            }
        }
    }

    private fun savePlaybackState(channel: XtreamChannel) {
        val prefs = getSharedPreferences("dazou_prefs", MODE_PRIVATE)
        prefs.edit()
            .putString("last_category_id", currentCategoryId)
            .putString("last_category_name", currentCategoryName)
            .putInt("last_channel_id", channel.streamId)
            .apply()
    }

    private fun restoreLastSessionOrShowCategories() {
        clearContentFragment()
        binding.sidebar.visibility = View.VISIBLE
        binding.videoPlayer.visibility = View.VISIBLE
        applyPlayerOverlayVisibility()
        binding.liveEpgPanel.visibility = View.VISIBLE
        updateMostWatchedStrip()
        if (lastCategories.isEmpty()) {
            liveViewModel.loadCategories()
        }

        val prefs = getSharedPreferences("dazou_prefs", MODE_PRIVATE)
        val savedCategoryId = prefs.getString("last_category_id", null)
        val savedCategoryName = prefs.getString("last_category_name", null)
        val savedChannelId = prefs.getInt("last_channel_id", -1)

        if (savedCategoryId != null && savedChannelId != -1) {
            currentCategoryId = savedCategoryId
            currentCategoryName = savedCategoryName ?: ""
            pendingAutoPlayChannelId = savedChannelId
            binding.channelsPanelTitle.text = currentCategoryName
            binding.channelsPanel.visibility = View.VISIBLE

            if (savedCategoryId.startsWith("GROUP:")) {
                pendingGroupCode = savedCategoryId.removePrefix("GROUP:")
            } else {
                liveViewModel.loadChannels(savedCategoryId)
            }
        }
    }

    fun playExternalMedia(url: String, name: String, type: String, itemId: Int = -1) {
        binding.videoPlayer.visibility = View.VISIBLE
        applyPlayerOverlayVisibility()
        playStreamWithResume(url, name, type, itemId)
    }

    private fun playStreamWithResume(url: String, name: String, type: String, itemId: Int) {
        nextEpisodeProvider = null
        autoNextTimer?.cancel()
        autoNextDialog?.dismiss()

        currentPlayingType = type
        currentPlayingId = itemId

        if (itemId != -1 && type != "live") {
            val saved = (application as App).container.historyManager.getSavedProgress(type, itemId)
            val hasResumablePosition = saved != null &&
                saved.durationMs > 0 &&
                saved.positionMs > 5_000L &&
                saved.positionMs < (saved.durationMs * 0.95).toLong()

            if (hasResumablePosition && saved != null) {
                android.app.AlertDialog.Builder(this)
                    .setTitle(name)
                    .setMessage(getString(R.string.resume_message, formatDurationShort(saved.positionMs)))
                    .setPositiveButton(getString(R.string.resume_continue)) { _, _ -> startPlayback(url, name, type, saved.positionMs) }
                    .setNegativeButton(getString(R.string.resume_from_start)) { _, _ -> startPlayback(url, name, type, 0L) }
                    .setCancelable(false)
                    .show()
                return
            }
        }

        startPlayback(url, name, type, 0L)
    }

    /** ✅ يحدّث نص لوحة معلومات القناة (أخطاء/تحميل) ويظهّرها بس لما يكون
     * فيه رسالة فعلية — بدل ما تضل خلفية فاضية ظاهرة بدون نص */
    private fun setChannelInfo(text: String) {
        binding.channelInfo.text = text
        binding.channelInfo.visibility = if (text.isBlank()) View.GONE else View.VISIBLE
    }

    private fun startPlayback(url: String, name: String, type: String, startPositionMs: Long) {
        currentChannelName = name
        playerManager.play(url, name, type, startPositionMs)
        setChannelInfo("")
        controlsController.onMediaStarted(type)
    }

    private fun formatDurationShort(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
    }

    private fun startProgressTracking() {
        val runnable = object : Runnable {
            override fun run() {
                if (currentPlayingType.isNotEmpty() && currentPlayingType != "live" &&
                    currentPlayingId != -1 && playerManager.isPlaying
                ) {
                    val pos = playerManager.player.currentPosition
                    val dur = playerManager.player.duration
                    if (dur > 0 && pos >= 0) {
                        (application as App).container.historyManager.updateProgress(
                            currentPlayingType, currentPlayingId, pos, dur
                        )
                    }
                }
                progressHandler.postDelayed(this, 5000)
            }
        }
        progressRunnable = runnable
        progressHandler.post(runnable)
    }

    fun playChannelFromExternal(channel: XtreamChannel, sourceList: List<XtreamChannel>) {
        nextEpisodeProvider = null
        autoNextTimer?.cancel()
        autoNextDialog?.dismiss()

        currentChannelList = sourceList
        currentChannelIndex = sourceList.indexOf(channel)
        currentPlayingType = "live"
        currentPlayingId = -1
        val server = liveViewModel.getServer() ?: return
        val url = XtreamAPI.getStreamUrl(server, channel.streamId, channel.containerExtension, "live")
        playStream(url, channel.name, "live")
        controlsController.showZapOverlay(channel.streamIcon, channel.name)
        updateNowPlayingPanel(channel)
        controlsController.onChannelListChanged(currentChannelList, currentChannelIndex)
    }

    private var mostWatchedBrowseStart = 0
    private enum class StripFocusTarget { NONE, FIRST, LAST }

    /** ✅ "الذكاء البسيط" — يبني شريط القنوات الأكتر مشاهدة (حسب تاريخ المشاهدة
     * الفعلي، مش القائمة الحالية بس)، ويخفيه تلقائيًا لو ما في قنوات كافية
     * إلها تاريخ مشاهدة حقيقي (أقل من مرتين) */
    private fun updateMostWatchedStrip() {
        val app = application as App
        val candidates = app.container.historyManager.getHistory()
            .filter { it.type == "live" }
            .let { UsageTracker.sortByMostWatched(this, it, "channel") { item -> item.id } }
            .filter { UsageTracker.getViewCount(this, "channel", it.id) >= 2 }

        if (candidates.size < 2) {
            binding.mostWatchedStripContainer.visibility = View.GONE
            return
        }
        mostWatchedBrowseStart = mostWatchedBrowseStart.coerceIn(0, (candidates.size - 3).coerceAtLeast(0))
        binding.mostWatchedStripContainer.visibility = View.VISIBLE
        buildMostWatchedStripViews(candidates, StripFocusTarget.NONE)
    }

    /** ✅ نافذة عرض 3 قنوات بالضبط بنفس الوقت — تزحف عند الحواف بدل ما توقف،
     * نفس أسلوب شريط القنوات المصغّر بملء الشاشة تمامًا */
    private fun buildMostWatchedStripViews(items: List<HistoryItem>, focusRequest: StripFocusTarget) {
        binding.mostWatchedStripTrack.removeAllViews()
        val density = resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        val maxVisible = 3
        val total = items.size
        val windowStart = mostWatchedBrowseStart.coerceIn(0, (total - maxVisible).coerceAtLeast(0))
        val windowEnd = (windowStart + maxVisible).coerceAtMost(total)
        val displayList = items.subList(windowStart, windowEnd)
        val cells = mutableListOf<LinearLayout>()

        displayList.forEach { item ->
            val cell = LinearLayout(this)
            cell.id = View.generateViewId()
            cell.orientation = LinearLayout.HORIZONTAL
            cell.gravity = Gravity.CENTER_VERTICAL
            val lp = LinearLayout.LayoutParams(0, dp(48), 1f)
            lp.marginStart = dp(4)
            lp.marginEnd = dp(4)
            cell.layoutParams = lp
            cell.setPadding(dp(8), dp(4), dp(8), dp(4))
            cell.setBackgroundResource(R.drawable.tv_button_selector)
            cell.isFocusable = true
            cell.isFocusableInTouchMode = true
            cell.isClickable = true
            cell.setOnClickListener { playMostWatchedItem(item) }

            val logo = ImageView(this)
            logo.layoutParams = LinearLayout.LayoutParams(dp(30), dp(30)).apply { marginEnd = dp(6) }
            logo.scaleType = ImageView.ScaleType.CENTER_INSIDE
            Glide.with(this).load(item.icon)
                .placeholder(R.drawable.ic_live_tv).error(R.drawable.ic_live_tv).into(logo)

            val nameText = TextView(this)
            nameText.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            nameText.text = item.name
            nameText.textSize = 11f
            nameText.maxLines = 1
            nameText.ellipsize = TextUtils.TruncateAt.END
            nameText.setTextColor(getColor(R.color.text_white))

            cell.addView(logo)
            cell.addView(nameText)
            binding.mostWatchedStripTrack.addView(cell)
            cells.add(cell)
        }

        // ✅ زحف النافذة عند الحواف بدل ما تتوقف عند حدود الـ3 قنوات
        if (cells.isNotEmpty()) {
            cells.first().setOnKeyListener { _, keyCode, event ->
                if (event.action == android.view.KeyEvent.ACTION_DOWN &&
                    keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT && windowStart > 0
                ) {
                    mostWatchedBrowseStart = windowStart - 1
                    buildMostWatchedStripViews(items, StripFocusTarget.FIRST)
                    true
                } else false
            }
            cells.last().setOnKeyListener { _, keyCode, event ->
                if (event.action == android.view.KeyEvent.ACTION_DOWN &&
                    keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT && windowStart + maxVisible < total
                ) {
                    mostWatchedBrowseStart = windowStart + 1
                    buildMostWatchedStripViews(items, StripFocusTarget.LAST)
                    true
                } else false
            }
        }

        when (focusRequest) {
            StripFocusTarget.FIRST -> cells.firstOrNull()?.requestFocus()
            StripFocusTarget.LAST -> cells.lastOrNull()?.requestFocus()
            StripFocusTarget.NONE -> {}
        }
    }

    private fun playMostWatchedItem(item: HistoryItem) {
        val server = liveViewModel.getServer() ?: return
        val url = XtreamAPI.getStreamUrl(server, item.id, item.containerExtension, "live")
        playExternalMedia(url, item.name, "live", item.id)
    }

    private fun updateNowPlayingPanel(channel: XtreamChannel) {
        val server = liveViewModel.getServer() ?: return
        binding.tvNowTitle.text = getString(R.string.live_loading_epg)
        binding.tvNowTime.text = ""
        binding.tvNowDescription.visibility = View.GONE
        binding.tvNextTitle.text = ""
        binding.pbNowProgress.progress = 0
        binding.tvControlsNow.text = getString(R.string.live_loading_epg)
        binding.pbControlsProgress.visibility = View.GONE

        XtreamAPI.getShortEpg(server, channel.streamId) { programs ->
            if (programs.isEmpty()) {
                binding.tvNowTitle.text = "📺 ${channel.name}"
                binding.tvNowTime.text = getString(R.string.live_no_epg_data)
                binding.tvNowDescription.visibility = View.GONE
                binding.tvNextTitle.text = ""
                binding.pbNowProgress.progress = 0
                binding.tvControlsNow.text = "📺 ${channel.name}"
                binding.pbControlsProgress.visibility = View.GONE
                return@getShortEpg
            }

            val now = programs.firstOrNull { it.nowPlaying } ?: programs.first()
            val nowIndex = programs.indexOf(now)
            val next = programs.getOrNull(nowIndex + 1)

            binding.tvNowTitle.text = getString(R.string.live_now_playing, now.title)
            binding.pbNowProgress.progress = now.progressPercent()
            binding.tvNowTime.text = "${formatEpgTime(now.startTimestamp)} - ${formatEpgTime(now.stopTimestamp)}"

            // ✅ نبذة عن البرنامج الحالي — لو موجودة فعليًا بالبيانات القادمة من السيرفر
            if (now.description.isNotBlank()) {
                binding.tvNowDescription.text = now.description
                binding.tvNowDescription.visibility = View.VISIBLE
            } else {
                binding.tvNowDescription.visibility = View.GONE
            }

            binding.tvNextTitle.text = if (next != null)
                getString(R.string.live_next_program, next.title, formatEpgTime(next.startTimestamp))
            else ""

            binding.tvControlsNow.text = "${channel.name}  •  ${now.title}"
            binding.pbControlsProgress.visibility = View.VISIBLE
            binding.pbControlsProgress.progress = now.progressPercent()
        }
    }

    private fun formatEpgTime(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp * 1000))
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && binding.videoPlayer.visibility == View.VISIBLE) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_CHANNEL_UP -> {
                    onNextChannel()
                    return true
                }
                KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                    onPreviousChannel()
                    return true
                }
                else -> {
                    if (controlsController.handleControlKeyEvent(event)) return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun loadMergedChannels(categoryIds: List<String>, onComplete: (List<XtreamChannel>) -> Unit) {
        val server = liveViewModel.getServer()
        if (server == null || categoryIds.isEmpty()) {
            onComplete(emptyList())
            return
        }
        val combined = mutableListOf<XtreamChannel>()
        var remaining = categoryIds.size
        categoryIds.forEach { catId ->
            XtreamAPI.getLiveStreams(server, catId) { channels ->
                combined.addAll(channels)
                remaining--
                if (remaining <= 0) onComplete(combined)
            }
        }
    }

    private fun applyChannelResults(channels: List<XtreamChannel>) {
        currentChannelList = channels
        binding.channelList.adapter = ChannelAdapter(
            channels,
            (application as App).container.favoritesManager,
            R.id.btnFullscreenSmall,
            onRequestFocusLeft = { restoreFocusToCategoryProtected() },
            onRequestFullscreen = { if (!fullscreen) toggleFullscreen() }
        ) { channel ->
            val index = channels.indexOf(channel)
            if (index == currentChannelIndex && playerManager.isPlaying) {
                toggleFullscreen()
            } else {
                playChannelAt(index)
            }
        }

        if (channels.isNotEmpty()) {
            focusFirstItemWhenReady(binding.channelList)
        }

        val pendingId = pendingAutoPlayChannelId
        if (pendingId != null) {
            pendingAutoPlayChannelId = null
            val index = channels.indexOfFirst { it.streamId == pendingId }
            if (index >= 0) playChannelAt(index)
        } else {
            controlsController.onChannelListChanged(currentChannelList, currentChannelIndex)
        }
    }

    private fun setupWifiStatus() {
        networkMonitor = NetworkMonitor(this)
        networkMonitor.start { connected ->
            runOnUiThread {
                binding.wifiIcon.setImageResource(if (connected) R.drawable.ic_wifi else R.drawable.ic_wifi_off)
                binding.wifiIcon.alpha = if (connected) 1f else 0.5f

                // ✅ لو الشبكة رجعت وكانت كل محاولات إعادة الاتصال التلقائية فشلت (المستخدم
                // عالق على "تعذر الاتصال")، نعيد التشغيل تلقائيًا بدل ما ننتظر تدخله يدويًا
                if (connected && playerManager.retriesExhausted && binding.videoPlayer.visibility == View.VISIBLE) {
                    Toast.makeText(this, getString(R.string.player_reconnected), Toast.LENGTH_SHORT).show()
                    controlsController.hideManualRetry()
                    playerManager.manualRetry()
                }
            }
        }
    }

    private fun setupThemeToggle() {
        val prefs = getSharedPreferences("dazou_prefs", MODE_PRIVATE)
        val isLight = prefs.getBoolean("is_light_theme", false)
        binding.themeToggle.setImageResource(if (isLight) R.drawable.ic_theme_light else R.drawable.ic_theme_dark)

        binding.themeToggle.setOnClickListener {
            val newIsLight = !prefs.getBoolean("is_light_theme", false)
            prefs.edit().putBoolean("is_light_theme", newIsLight).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (newIsLight) AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES
            )
            recreate()
        }
    }

    private fun startClock() {
        val runnable = object : Runnable {
            override fun run() {
                val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                val now = sdf.format(java.util.Date())
                binding.clockText.text = now
                binding.tvPlayerClock.text = now
                clockHandler.postDelayed(this, 30000)
            }
        }
        clockRunnable = runnable
        clockHandler.post(runnable)
    }

    private fun clearContentFragment() {
        supportFragmentManager.findFragmentById(binding.fragmentContainer.id)?.let {
            supportFragmentManager.beginTransaction().remove(it).commitNowAllowingStateLoss()
        }
        binding.fragmentContainer.visibility = View.GONE
    }

    private fun showCategories() {
        clearContentFragment()
        binding.sidebar.visibility = View.VISIBLE
        binding.videoPlayer.visibility = View.VISIBLE
        applyPlayerOverlayVisibility()
        binding.liveEpgPanel.visibility = View.VISIBLE
        updateMostWatchedStrip()
        if (lastCategories.isEmpty()) {
            liveViewModel.loadCategories()
        }
    }

    private fun openCategory(category: XtreamCategory) {
        if (category.categoryId.startsWith("GROUP:")) {
            val code = category.categoryId.removePrefix("GROUP:")
            openCountryGroup(code, category.categoryName)
        } else {
            openRealCategory(category)
        }
    }

    private fun openCountryGroup(code: String, countryLabel: String) {
        currentCategoryId = "GROUP:$code"
        currentCategoryName = countryLabel
        binding.channelsPanelTitle.text = countryLabel
        binding.channelsPanel.visibility = View.VISIBLE

        val subCategories = categoryGroupDetailsMap[code] ?: emptyList()
        val cleanedSubCategories = subCategories.map {
            it.copy(categoryName = CategoryGrouper.stripCountryPrefix(it.categoryName))
        }

        binding.channelList.adapter = CategoryAdapter(
            cleanedSubCategories,
            R.id.btnFullscreenSmall,
            onRequestFocusLeft = { restoreFocusToCategoryProtected() }
        ) { subCategory ->
            openRealCategory(subCategory)
        }

        if (cleanedSubCategories.isNotEmpty()) {
            focusFirstItemWhenReady(binding.channelList)
        }
    }

    private fun openRealCategory(category: XtreamCategory) {
        currentCategoryId = category.categoryId
        currentCategoryName = category.categoryName
        binding.channelsPanelTitle.text = category.categoryName
        binding.channelsPanel.visibility = View.VISIBLE
        liveViewModel.loadChannels(category.categoryId)
    }

    private fun hideChannelsPanel() {
        binding.channelsPanel.visibility = View.GONE
    }

    /** ✅ نسخة محمية من restoreFocusToCategory — بتحجب القائمة العلوية مؤقتًا
     * وقت البحث عن العنصر الصحيح، وإلا أندرويد بيهرب ويعطي فوكس افتراضي لأول
     * عنصر بالقائمة العلوية (زر الحساب) لو أخذت العملية وقت أطول من المتوقع.
     * نفس الحل المستخدم بالضبط لمشكلة الخروج من ملء الشاشة. */
    private fun restoreFocusToCategoryProtected() {
        binding.topBar.descendantFocusability = android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS
        restoreFocusToCategory {
            binding.topBar.postDelayed({
                binding.topBar.descendantFocusability = android.view.ViewGroup.FOCUS_BEFORE_DESCENDANTS
            }, 150)
        }
    }

    /** ✅ يرجّع الفوكس بدقة لنفس مكان الفئة اللي كان المستخدم واقف عليها قبل ما
     * يدخل عالقنوات أو المجلدات الفرعية — بدل ما نسيب أندرويد يخمّن ويغلط أحيانًا */
    private fun restoreFocusToCategory(onDone: () -> Unit = {}) {
        val layoutManager = binding.categoryList.layoutManager as? LinearLayoutManager ?: run { onDone(); return }
        val itemCount = binding.categoryList.adapter?.itemCount ?: 0
        if (itemCount == 0) { onDone(); return }
        val target = lastFocusedCategoryPosition.coerceIn(0, itemCount - 1)

        val existingView = layoutManager.findViewByPosition(target)
        if (existingView != null) {
            existingView.requestFocus()
            onDone()
            return
        }

        binding.categoryList.scrollToPosition(target)
        var finished = false
        val listener = object : RecyclerView.OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(view: View) {
                if (finished) return
                if (binding.categoryList.getChildAdapterPosition(view) == target) {
                    finished = true
                    binding.categoryList.removeOnChildAttachStateChangeListener(this)
                    view.requestFocus()
                    onDone()
                }
            }
            override fun onChildViewDetachedFromWindow(view: View) {}
        }
        binding.categoryList.addOnChildAttachStateChangeListener(listener)
        // ✅ حد أمان زمني (ثانيتين) — لو لأي سبب غير متوقع العنصر ما تكوّن أبدًا،
        // ما نضل عالقين للأبد وننفذ onDone على أي حال
        binding.categoryList.postDelayed({
            if (!finished) {
                finished = true
                binding.categoryList.removeOnChildAttachStateChangeListener(listener)
                layoutManager.findViewByPosition(target)?.requestFocus()
                onDone()
            }
        }, 2000)
    }

    /** ✅ يرجّع الفوكس بدقة لنفس القناة الشغالة حاليًا بقائمة القنوات — يُستخدم
     * لما نطلع من وضع ملء الشاشة، بدل ما يضل الفوكس ضايع أو بمكان عشوائي */
    /** ✅ يرجّع الفوكس بدقة لنفس القناة الشغالة حاليًا بقائمة القنوات — يُستخدم
     * لما نطلع من وضع ملء الشاشة. القائمة كانت مخفية بالكامل (GONE) وقت ملء
     * الشاشة، فعناصرها بتحتاج أكتر من إطار واحد لترجع تترسم — لهيك بنجرب كذا
     * مرة (post متكرر) بدل محاولة وحدة بس، وإلا فوكس أندرويد الافتراضي بياخد
     * المكان (زي زر الحساب بالقائمة العلوية) قبل ما نلحق نحط الفوكس الصح. */
    private fun restoreFocusToChannel(onDone: () -> Unit = {}) {
        val layoutManager = binding.channelList.layoutManager as? LinearLayoutManager ?: run { onDone(); return }
        val itemCount = binding.channelList.adapter?.itemCount ?: 0
        if (itemCount == 0 || currentChannelIndex !in 0 until itemCount) { onDone(); return }
        val target = currentChannelIndex

        val existingView = layoutManager.findViewByPosition(target)
        if (existingView != null) {
            existingView.requestFocus()
            onDone()
            return
        }

        binding.channelList.scrollToPosition(target)
        var finished = false
        val listener = object : RecyclerView.OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(view: View) {
                if (finished) return
                if (binding.channelList.getChildAdapterPosition(view) == target) {
                    finished = true
                    binding.channelList.removeOnChildAttachStateChangeListener(this)
                    view.requestFocus()
                    onDone()
                }
            }
            override fun onChildViewDetachedFromWindow(view: View) {}
        }
        binding.channelList.addOnChildAttachStateChangeListener(listener)
        binding.channelList.postDelayed({
            if (!finished) {
                finished = true
                binding.channelList.removeOnChildAttachStateChangeListener(listener)
                layoutManager.findViewByPosition(target)?.requestFocus()
                onDone()
            }
        }, 2000)
    }

    private fun playChannelAt(index: Int) {
        if (index < 0 || index >= currentChannelList.size) return
        val server = liveViewModel.getServer()
        if (server == null) {
            Toast.makeText(this, getString(R.string.common_choose_account_first), Toast.LENGTH_SHORT).show()
            return
        }
        nextEpisodeProvider = null
        autoNextTimer?.cancel()
        autoNextDialog?.dismiss()

        currentChannelIndex = index
        val channel = currentChannelList[index]
        currentPlayingType = "live"
        currentPlayingId = -1
        val url = XtreamAPI.getStreamUrl(server, channel.streamId, channel.containerExtension, "live")
        playStream(url, channel.name, "live")
        controlsController.showZapOverlay(channel.streamIcon, channel.name)
        updateNowPlayingPanel(channel)
        savePlaybackState(channel)
        controlsController.onChannelListChanged(currentChannelList, currentChannelIndex)

        // ✅ "الذكاء البسيط": نسجّل مشاهدة القناة، ولو وصلت لعدد مشاهدات معيّن
        // ومش مضافة للمفضلة أصلاً، نقترح على المستخدم يضيفها
        UsageTracker.recordView(this, "channel", channel.streamId)
        if (UsageTracker.shouldSuggestFavorite(this, "channel", channel.streamId)) {
            val alreadyFavorite = (application as App).container.favoritesManager
                .isFavorite("live", channel.streamId)
            if (!alreadyFavorite) {
                showFavoriteSuggestionDialog(channel)
            }
        }
        updateMostWatchedStrip()
    }

    fun goToHome() {
        showMainUi()
        showCategories()
        binding.menuHome.requestFocus()
    }

    private fun showMainUi() {
        binding.topBar.visibility = View.VISIBLE
        binding.sidebar.visibility = View.VISIBLE
        binding.videoPlayer.visibility = View.VISIBLE
        applyPlayerOverlayVisibility()
    }

    /** ✅ الشاشة المصغّرة (غير ملء الشاشة) لازم تكون فاضية إلا من زر التكبير —
     * كل معلومات القناة (الاسم/الوقت/البرنامج القادم) أصلاً معروضة باللوحة تحت
     * (live_epg_panel)، فما في داعي نكررها فوق الفيديو نفسه. بملء الشاشة، بيرجع
     * يظهر شريط التحكم الكامل عادي ويختفي زر التكبير المصغّر. */
    private fun applyPlayerOverlayVisibility() {
        if (fullscreen) {
            binding.channelInfo.visibility = View.VISIBLE
            binding.playerControls.visibility = View.VISIBLE
            binding.btnFullscreenSmall.visibility = View.GONE
        } else {
            binding.channelInfo.visibility = View.GONE
            binding.playerControls.visibility = View.GONE
            binding.btnFullscreenSmall.visibility = View.VISIBLE
            // ✅ لو أي زر جوا شريط التحكم كان حامل الفوكس قبل ما ينخفى، لازم نلغيه
            // صراحة — وإلا بيضل "فوكس شبح" معلّق بعنصر مخفي (GONE)، وهاد بيسبب
            // سلوك غير متوقع بالتنقل بعدها (فوكس بيضيع أو ما بيتحرك بشكل طبيعي)
            binding.playerControls.clearFocus()
        }
    }

    private fun showLoginUi() {
        binding.topBar.visibility = View.GONE
        binding.sidebar.visibility = View.GONE
        binding.channelsPanel.visibility = View.GONE
        binding.liveEpgPanel.visibility = View.GONE
        binding.videoPlayer.visibility = View.GONE
        binding.channelInfo.visibility = View.GONE
        binding.playerControls.visibility = View.GONE
        binding.btnFullscreenSmall.visibility = View.GONE
    }

    private fun setupPlayerErrorHandling() {
        playerManager.player.addListener(object : Player.Listener {

            override fun onPlayerError(error: PlaybackException) {
                binding.playerLoading.visibility = View.GONE
                val isNetworkError = error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                    error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
                    error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS

                if (isNetworkError) {setChannelInfo(getString(R.string.live_connection_lost))
                    playerManager.retryCurrent {
                        setChannelInfo(getString(R.string.live_connection_failed))
                        // ✅ لما كل المحاولات التلقائية تفشل، نظهّر زر يدوي بدل ما يضل المستخدم عالق
                        controlsController.showManualRetry()
                    }
                    return
                }

                val message = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
                        getString(R.string.live_url_unavailable)
                    PlaybackException.ERROR_CODE_DECODING_FAILED,
                    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ->
                        getString(R.string.live_format_unsupported)
                    else ->
                        getString(R.string.live_playback_failed, error.errorCodeName)
                }
                setChannelInfo("⚠️ $message")
            }

            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> {
                        setChannelInfo(getString(R.string.common_loading))
                        binding.playerLoading.visibility = View.VISIBLE
                    }
                    Player.STATE_READY -> {
                        playerManager.resetRetry()
                        controlsController.hideManualRetry()
                        controlsController.updateQualityBadge()
                        binding.playerLoading.visibility = View.GONE
                        setChannelInfo("")
                    }
                    Player.STATE_ENDED -> {
                        binding.playerLoading.visibility = View.GONE
                        if (currentPlayingType == "series" && nextEpisodeProvider != null) {
                            triggerAutoNextEpisode()
                        }
                    }
                }
            }

            // ✅ يلتقط أي تغيير بجودة الفيديو (لو ABR بدّل الدقة تلقائيًا وقت التشغيل)
            // ويحدّث شارة الجودة فورًا بدل ما تضل واقفة على القيمة القديمة
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                controlsController.updateQualityBadge()
            }

            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                controlsController.updateQualityBadge()
            }
        })
    }

    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Toast.makeText(this, getString(R.string.live_feature_unsupported), Toast.LENGTH_SHORT).show()
            return
        }
        if (binding.videoPlayer.visibility != View.VISIBLE) return
        val aspectRatio = Rational(16, 9)
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(aspectRatio)
            .build()
        try {
            enterPictureInPictureMode(params)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.live_pip_failed), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            binding.videoPlayer.visibility == View.VISIBLE &&
            playerManager.isPlaying
        ) {
            enterPipMode()
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            binding.topBar.visibility = View.GONE
            binding.sidebar.visibility = View.GONE
            binding.channelsPanel.visibility = View.GONE
            binding.liveEpgPanel.visibility = View.GONE
            binding.fragmentContainer.visibility = View.GONE
            binding.playerControls.visibility = View.GONE
            binding.channelInfo.visibility = View.GONE
            binding.btnFullscreenSmall.visibility = View.GONE
        } else {
            applyPlayerOverlayVisibility()
            if (supportFragmentManager.findFragmentById(binding.fragmentContainer.id) != null) {
                binding.fragmentContainer.visibility = View.VISIBLE
            } else {
                binding.topBar.visibility = View.VISIBLE
                binding.sidebar.visibility = View.VISIBLE
                binding.liveEpgPanel.visibility = View.VISIBLE
            }
        }
    }

    private fun toggleFullscreen() {
        fullscreen = !fullscreen

        if (fullscreen) {
            wasChannelsPanelOpenBeforeFullscreen = binding.channelsPanel.visibility == View.VISIBLE

            binding.topBar.visibility = View.GONE
            binding.sidebar.visibility = View.GONE
            binding.channelsPanel.visibility = View.GONE
            binding.liveEpgPanel.visibility = View.GONE
            binding.fragmentContainer.visibility = View.GONE

            // ✅ يشيل الحشو الداخلي (12dp) اللي كان يترك إطار رفيع حول الفيديو
            binding.contentArea.setPadding(0, 0, 0, 0)
            // ✅ يعيد ربط حدود منطقة المشغل مباشرة بحواف الشاشة الحقيقية (بدل ما تضل
            // معتمدة على مكان العناصر المخفية جنبها)، فيضمن يملأ الشاشة كاملة بدون أي إطار
            setContentAreaFullScreenConstraints(true)

            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            )
        } else {
            // ✅ الحل الحاسم: نمنع القائمة العلوية من قبول أي فوكس مؤقتًا وقت
            // ما ترجع تظهر — وإلا أندرويد بيعطيها فوكس افتراضي (زر الحساب)
            // فورًا قبل ما نلحق نرجّع الفوكس للقناة الصح. بمجرد ما نضمن رجوع
            // الفوكس بالمكان الصحيح، منرجع نسمحلها بالفوكس عادي.
            binding.topBar.descendantFocusability = android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS
            binding.topBar.visibility = View.VISIBLE
            binding.sidebar.visibility = View.VISIBLE
            val reallowTopBarFocus: () -> Unit = {
                // ✅ نأخّر رفع الحجب بإطار إضافي (بدل ما نرفعه فورًا) — عشان
                // نضمن إنه الفوكس استقر فعليًا عالقناة قبل ما نسمح لأي شي
                // تاني ياخد فوكس. لو رفعناه فورًا، ممكن الفوكس يكون "نجح"
                // بشكل ظاهري بس مش مستقر لسا، فيضيع لما تصير إعادة رسم تالية.
                binding.topBar.postDelayed({
                    binding.topBar.descendantFocusability = android.view.ViewGroup.FOCUS_BEFORE_DESCENDANTS
                }, 150)
                Unit
            }
            if (wasChannelsPanelOpenBeforeFullscreen) {
                binding.channelsPanel.visibility = View.VISIBLE
                // ✅ يرجّع الفوكس لنفس القناة اللي كانت شغالة/محددة قبل الدخول
                // بملء الشاشة، بدل ما يضل الفوكس ضايع
                restoreFocusToChannel(onDone = reallowTopBarFocus)
            } else {
                // ✅ لو لوحة القنوات ما كانت مفتوحة أصلاً (كان بس متصفح الفئات)،
                // نرجّع الفوكس لنفس الفئة بدل ما نسيبه بلا وجهة
                restoreFocusToCategory(onDone = reallowTopBarFocus)
            }
            if (supportFragmentManager.findFragmentById(binding.fragmentContainer.id) != null) {
                binding.fragmentContainer.visibility = View.VISIBLE
            } else {
                binding.liveEpgPanel.visibility = View.VISIBLE
            }

            val padDp = (12 * resources.displayMetrics.density).toInt()
            binding.contentArea.setPadding(padDp, padDp, padDp, padDp)
            setContentAreaFullScreenConstraints(false)

            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }

        controlsController.isFullscreen = fullscreen
        controlsController.updateFullscreenIcon(fullscreen)
        applyPlayerOverlayVisibility()
        if (fullscreen) {
            // ✅ يبلّش عداد الإخفاء التلقائي (5 ثواني) فورًا، بدل ما يضل شريط
            // التحكم ظاهر لحد ما المستخدم يتفاعل مع شي
            controlsController.showControls()
            // ✅ الأهم: نحط الفوكس صراحة على زر التشغيل فور الدخول لملء الشاشة
            requestFocusWhenReady(binding.btnPlayPause)
        }
    }

    /** ✅ يبدّل ربط حواف منطقة المشغل بين "بجانب العناصر الجانبية" (الوضع العادي)
     * و"حواف الشاشة مباشرة" (وضع ملء الشاشة) — يضمن تغطية كاملة بدون أي إطار متبقي */
    private fun setContentAreaFullScreenConstraints(full: Boolean) {
        val parent = binding.contentArea.parent as? androidx.constraintlayout.widget.ConstraintLayout ?: return
        val set = androidx.constraintlayout.widget.ConstraintSet()
        set.clone(parent)
        if (full) {
            set.connect(binding.contentArea.id, androidx.constraintlayout.widget.ConstraintSet.TOP,
                androidx.constraintlayout.widget.ConstraintSet.PARENT_ID, androidx.constraintlayout.widget.ConstraintSet.TOP, 0)
            set.connect(binding.contentArea.id, androidx.constraintlayout.widget.ConstraintSet.END,
                androidx.constraintlayout.widget.ConstraintSet.PARENT_ID, androidx.constraintlayout.widget.ConstraintSet.END, 0)
        } else {
            set.connect(binding.contentArea.id, androidx.constraintlayout.widget.ConstraintSet.TOP,
                R.id.top_bar, androidx.constraintlayout.widget.ConstraintSet.BOTTOM, 0)
            set.connect(binding.contentArea.id, androidx.constraintlayout.widget.ConstraintSet.END,
                R.id.channels_panel, androidx.constraintlayout.widget.ConstraintSet.START, 0)
        }
        set.applyTo(parent)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            binding.searchOverlay.visibility == View.VISIBLE -> closeSearchOverlay()
            fullscreen -> toggleFullscreen()
            binding.channelsPanel.visibility == View.VISIBLE -> hideChannelsPanel()
            binding.fragmentContainer.visibility == View.VISIBLE -> goToHome()
            else -> super.onBackPressed()
        }
    }

    private fun setupMenu(){
        binding.menuHome.setOnClickListener { saveLastSection("home"); loadFragment(HomeFragment()) }
        binding.menuLive.setOnClickListener { saveLastSection("live"); showCategories() }
        binding.menuMovies.setOnClickListener { saveLastSection("movies"); loadFragment(MoviesFragment()) }
        binding.menuSeries.setOnClickListener { saveLastSection("series"); loadFragment(SeriesFragment()) }
        binding.menuEpg.setOnClickListener { loadFragment(EpgFragment()) }
        binding.settings.setOnClickListener { loadFragment(SettingsFragment()) }
        binding.account.setOnClickListener { loadFragment(AccountsFragment()) }
        binding.sidebarLiveButton.setOnClickListener { saveLastSection("live"); showCategories() }
    }

    /** ✅ "الذكاء البسيط" — يحفظ آخر قسم رئيسي فتحه المستخدم (رئيسية/مباشر/
     * أفلام/مسلسلات) عشان نرجعله تلقائيًا لما يفتح التطبيق من جديد */
    private fun saveLastSection(section: String) {
        getSharedPreferences("dazou_prefs", MODE_PRIVATE)
            .edit().putString("last_section", section).apply()
    }

    // ========== ✅ منطق البحث الشامل (قنوات + أفلام + مسلسلات) ==========

    private fun setupSearch() {
        binding.rvSearchResults.layoutManager = LinearLayoutManager(this)
        binding.searchIcon.setOnClickListener { openSearchOverlay() }
        binding.btnCloseSearch.setOnClickListener { closeSearchOverlay() }

        binding.etGlobalSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                performSearch(s?.toString()?.trim().orEmpty())
            }
        })
    }

    private fun openSearchOverlay() {
        binding.searchOverlay.visibility = View.VISIBLE
        setBackgroundFocusBlocked(true)
        requestFocusWhenReady(binding.etGlobalSearch)
        loadSearchDataIfNeeded()
    }

    private fun closeSearchOverlay() {
        binding.searchOverlay.visibility = View.GONE
        binding.etGlobalSearch.text?.clear()
        setBackgroundFocusBlocked(false)
    }

    /** ✅ يمنع أي عنصر وراء نافذة البحث من ياخد الفوكس بالغلط وقت النافذة مفتوحة */
    private fun setBackgroundFocusBlocked(blocked: Boolean) {
        val mode = if (blocked) android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS
                   else android.view.ViewGroup.FOCUS_BEFORE_DESCENDANTS
        binding.topBar.descendantFocusability = mode
        binding.sidebar.descendantFocusability = mode
        binding.channelsPanel.descendantFocusability = mode
        binding.contentArea.descendantFocusability = mode
        binding.fragmentContainer.descendantFocusability = mode
    }

    private fun loadSearchDataIfNeeded() {
        if (searchDataLoaded || searchDataLoading) return
        val server = liveViewModel.getServer() ?: return

        searchDataLoading = true
        searchLoadedParts = 0
        binding.tvSearchStatus.text = getString(R.string.search_loading)

        fun onPartLoaded() {
            searchLoadedParts++
            if (searchLoadedParts >= 3) {
                searchDataLoading = false
                searchDataLoaded = true
                performSearch(binding.etGlobalSearch.text?.toString()?.trim().orEmpty())
            }
        }

        XtreamAPI.getLiveStreams(server, null) { channels ->
            cachedSearchChannels = channels
            onPartLoaded()
        }
        XtreamAPI.getVodStreams(server, null) { movies ->
            cachedSearchMovies = movies
            onPartLoaded()
        }
        XtreamAPI.getSeries(server, null) { series ->
            cachedSearchSeries = series
            onPartLoaded()
        }
    }

    private fun performSearch(query: String) {
        if (query.isEmpty()) {
            binding.rvSearchResults.adapter = null
            binding.tvSearchStatus.text = if (searchDataLoading)
                getString(R.string.search_loading) else getString(R.string.search_prompt)
            return
        }

        if (searchDataLoading) {
            binding.tvSearchStatus.text = getString(R.string.search_loading)
            return
        }

        val results = mutableListOf<SearchResultItem>()
        cachedSearchChannels.filter { it.name.contains(query, ignoreCase = true) }
            .forEach { results.add(SearchResultItem.Channel(it)) }
        cachedSearchMovies.filter { it.name.contains(query, ignoreCase = true) }
            .forEach { results.add(SearchResultItem.Movie(it)) }
        cachedSearchSeries.filter { it.name.contains(query, ignoreCase = true) }
            .forEach { results.add(SearchResultItem.Series(it)) }

        // ✅ نحد أقصى للنتائج المعروضة حتى ما نبطّئ الواجهة لو النتائج كتير
        val limited = results.take(150)

        binding.tvSearchStatus.text = if (limited.isEmpty())
            getString(R.string.search_no_results)
        else
            getString(R.string.search_results_count, results.size)

        binding.rvSearchResults.adapter = SearchResultAdapter(
            limited,
            getString(R.string.search_type_live),
            getString(R.string.search_type_movie),
            getString(R.string.search_type_series)
        ) { item -> onSearchResultClicked(item) }
    }

    private fun onSearchResultClicked(item: SearchResultItem) {
        when (item) {
            is SearchResultItem.Channel -> {
                closeSearchOverlay()
                playChannelFromExternal(item.channel, listOf(item.channel))
            }
            is SearchResultItem.Movie -> playSearchMovie(item.movie)
            is SearchResultItem.Series -> playSearchSeries(item.series)
        }
    }

    private fun playSearchMovie(movie: XtreamMovie) {
        val server = liveViewModel.getServer() ?: return
        val url = XtreamAPI.getMovieUrl(server, movie.streamId, movie.containerExtension)

        (application as App).container.historyManager.addOrUpdateHistory(
            HistoryItem(
                type = "movie",
                id = movie.streamId,
                name = movie.name,
                timestamp = System.currentTimeMillis(),
                icon = movie.streamIcon,
                containerExtension = movie.containerExtension
            )
        )

        closeSearchOverlay()
        playExternalMedia(url, movie.name, "movie", movie.streamId)
    }

    private fun playSearchSeries(series: XtreamSeries) {
        selectedSearchSeries = series
        Toast.makeText(this, getString(R.string.search_loading_episodes_short), Toast.LENGTH_SHORT).show()
        val server = liveViewModel.getServer() ?: return
        XtreamAPI.getSeriesInfo(server, series.seriesId) { episodes ->
            showSearchEpisodesDialog(series, episodes)
        }
    }

    private fun showSearchEpisodesDialog(series: XtreamSeries, episodes: List<XtreamEpisode>) {
        if (episodes.isEmpty()) {
            Toast.makeText(this, getString(R.string.series_no_episodes), Toast.LENGTH_SHORT).show()
            return
        }

        val sorted = episodes.sortedWith(compareBy({ it.seasonNum }, { it.episodeNum }))
        searchEpisodeList = sorted
        val labels = sorted.map {
            getString(R.string.series_episode_label, it.seasonNum, it.episodeNum, it.title)
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(series.name)
            .setItems(labels) { _, which -> playSearchEpisode(sorted[which]) }
            .setNegativeButton(getString(R.string.common_close), null)
            .show()
    }

    private fun playSearchEpisode(episode: XtreamEpisode) {
        val server = liveViewModel.getServer() ?: return
        val url = XtreamAPI.getSeriesEpisodeUrl(server, episode.id, episode.containerExtension)

        val series = selectedSearchSeries
        if (series != null) {
            (application as App).container.historyManager.addOrUpdateHistory(
                HistoryItem(
                    type = "series",
                    id = series.seriesId,
                    name = series.name,
                    timestamp = System.currentTimeMillis(),
                    icon = series.cover,
                    containerExtension = episode.containerExtension
                )
            )
        }

        closeSearchOverlay()
        playExternalMedia(url, episode.title, "series", series?.seriesId ?: -1)

        val currentIndex = searchEpisodeList.indexOf(episode)
        val nextEpisode = if (currentIndex >= 0) searchEpisodeList.getOrNull(currentIndex + 1) else null
        setNextEpisodeProvider(if (nextEpisode != null) { { playSearchEpisode(nextEpisode) } } else null)
    }

    private fun loadFragment(fragment: Fragment){
        binding.videoPlayer.visibility = View.GONE
        binding.channelInfo.visibility = View.GONE
        binding.playerControls.visibility = View.GONE
        binding.btnFullscreenSmall.visibility = View.GONE
        binding.sidebar.visibility = View.GONE
        binding.channelsPanel.visibility = View.GONE
        binding.liveEpgPanel.visibility = View.GONE
        binding.fragmentContainer.visibility = View.VISIBLE
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment)
            .commit()
    }

    override fun playStream(url:String, name:String, type:String){
        currentChannelName = name
        playerManager.play(url, name, type)
        setChannelInfo("")
        controlsController.onMediaStarted(type)
    }

    override fun onNextChannel(){
        if (currentChannelList.isEmpty()) return
        val next = (currentChannelIndex + 1).coerceAtMost(currentChannelList.size - 1)
        playChannelAt(next)
    }

    override fun onPreviousChannel(){
        if (currentChannelList.isEmpty()) return
        val prev = (currentChannelIndex - 1).coerceAtLeast(0)
        playChannelAt(prev)
    }

    override fun onStop() {
        super.onStop()
        wasPlayingBeforeBackground = playerManager.isPlaying
        if (playerManager.isPlaying) {
            playerManager.pause()
        }
    }

    override fun onStart() {
        super.onStart()
        if (wasPlayingBeforeBackground) {
            playerManager.resume()
            wasPlayingBeforeBackground = false
        }
    }

    override fun onDestroy(){
        clockRunnable?.let { clockHandler.removeCallbacks(it) }
        progressRunnable?.let { progressHandler.removeCallbacks(it) }
        topBarLogoAnimator?.cancel()
        autoNextTimer?.cancel()
        autoNextDialog?.dismiss()
        sleepTimer?.cancel()
        controlsController.release()
        networkMonitor.stop()
        playerManager.release()
        super.onDestroy()
    }
}