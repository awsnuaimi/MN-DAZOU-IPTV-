package com.dazou.iptvplayer.player

import android.app.AlertDialog
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.ui.AspectRatioFrameLayout
import com.bumptech.glide.Glide
import com.dazou.iptvplayer.R
import com.dazou.iptvplayer.databinding.ActivityMainBinding
import com.dazou.iptvplayer.model.XtreamChannel

/**
 * مسؤول عن كل شي متعلق بلوحة تحكم المشغل نفسها: الأزرار، شريط التقدم،
 * شريط القنوات المجاورة، الإخفاء التلقائي، والصوت — منفصل عن MainActivity.
 */
class PlayerControlsController(
    private val activity: AppCompatActivity,
    private val binding: ActivityMainBinding,
    private val playerManager: PlayerManager,
    private val onChannelPicked: (Int) -> Unit
) {
    private var currentChannelList: List<XtreamChannel> = emptyList()
    private var currentChannelIndex: Int = -1
    private var currentType: String = "live"
    private var isMuted = false
    private var userSeeking = false
    private var currentVolume = 1f
    private var aspectModeIndex = 0
    private val aspectModes = intArrayOf(
        AspectRatioFrameLayout.RESIZE_MODE_FIT,
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
        AspectRatioFrameLayout.RESIZE_MODE_FILL
    )

    private val zapHandler = Handler(Looper.getMainLooper())
    private var zapRunnable: Runnable? = null

    // ✅ شريط القنوات المصغّر لازم يشتغل بس بوضع ملء الشاشة، وبس بضغطتين متتاليتين على السهم السفلي
    private var lastDpadDownPressTime = 0L
    private val doublePressWindowMs = 700L

    var isFullscreen: Boolean = false
        set(value) {
            field = value
            if (!value) {
                hideChannelStrip()
                lastDpadDownPressTime = 0L
            }
        }

    private val controlsHandler = Handler(Looper.getMainLooper())
    private var controlsRunnable: Runnable? = null

    private val seekHandler = Handler(Looper.getMainLooper())
    private var seekRunnable: Runnable? = null

    fun setup(
        onPrev: () -> Unit,
        onNext: () -> Unit,
        onFullscreenToggle: () -> Unit,
        onPip: () -> Unit,
        onManualRetry: () -> Unit
    ) {
        val focusShowListener = View.OnFocusChangeListener { _, hasFocus -> if (hasFocus) showControls() }

        binding.btnPlayPause.setOnClickListener {
            showControls()
            if (playerManager.isPlaying) {
                playerManager.pause()
                binding.btnPlayPause.setImageResource(R.drawable.ic_play_small)
            } else {
                playerManager.resume()
                binding.btnPlayPause.setImageResource(R.drawable.ic_pause)
            }
        }
        binding.btnPlayPause.onFocusChangeListener = focusShowListener

        binding.btnFullscreen.setOnClickListener {
            showControls()
            onFullscreenToggle()
        }
        binding.btnFullscreen.onFocusChangeListener = focusShowListener

        binding.btnPip.setOnClickListener { onPip() }
        binding.btnPip.onFocusChangeListener = focusShowListener

        binding.btnPrev.setOnClickListener {
            showControls()
            onPrev()
        }
        binding.btnPrev.onFocusChangeListener = focusShowListener

        binding.btnNext.setOnClickListener {
            showControls()
            onNext()
        }
        binding.btnNext.onFocusChangeListener = focusShowListener

        binding.btnVolume.setOnClickListener {
            showControls()
            toggleMute()
        }
        binding.btnVolume.onFocusChangeListener = focusShowListener

        // ✅ تقديم/إرجاع 10 ثواني — أسهل بكتير بالريموت من سحب شريط التقدم
        binding.btnRewind10.setOnClickListener {
            showControls()
            playerManager.seekBackward()
        }
        binding.btnRewind10.onFocusChangeListener = focusShowListener

        binding.btnForward10.setOnClickListener {
            showControls()
            playerManager.seekForward()
        }
        binding.btnForward10.onFocusChangeListener = focusShowListener

        // ✅ صوت متدرّج (+/-) بدل الكتم/التشغيل بس
        binding.btnVolumeDown.setOnClickListener {
            showControls()
            adjustVolume(-0.1f)
        }
        binding.btnVolumeDown.onFocusChangeListener = focusShowListener

        binding.btnVolumeUp.setOnClickListener {
            showControls()
            adjustVolume(0.1f)
        }
        binding.btnVolumeUp.onFocusChangeListener = focusShowListener

        // ✅ نسبة العرض: يبدّل بالتناوب بين احتواء/تكبير/تمديد
        binding.btnAspectRatio.setOnClickListener {
            showControls()
            cycleAspectRatio()
        }
        binding.btnAspectRatio.onFocusChangeListener = focusShowListener

        // ✅ اختيار مسار الصوت (لو المحتوى عنده أكتر من لغة)
        binding.btnAudioTrack.setOnClickListener {
            showControls()
            showAudioTrackPicker()
        }
        binding.btnAudioTrack.onFocusChangeListener = focusShowListener

        // ✅ زر إعادة المحاولة اليدوي — يظهر بس لما المحاولات التلقائية تفشل كلها
        binding.btnManualRetry.setOnClickListener {
            hideManualRetry()
            onManualRetry()
        }

        binding.videoPlayer.setOnClickListener { showControls() }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                showControls()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) { userSeeking = true }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                userSeeking = false
                val dur = playerManager.player.duration
                if (dur > 0 && seekBar != null) {
                    val target = (dur * (seekBar.progress.toFloat() / 1000f)).toLong()
                    playerManager.player.seekTo(target)
                }
            }
        })

        startSeekUpdater()
    }

    fun onChannelListChanged(list: List<XtreamChannel>, index: Int) {
        currentChannelList = list
        currentChannelIndex = index
        buildChannelStrip()
    }

    fun onMediaStarted(type: String) {
        currentType = type
        updateMediaTypeUi()
        binding.btnPlayPause.setImageResource(R.drawable.ic_pause)
        hideManualRetry()
        binding.tvQualityBadge.visibility = View.GONE
        showControls()
    }

    /** ✅ يحدّث شارة الجودة (1080p/720p/...) — يُستدعى لما الفيديو يجهز أو المسارات تتغيّر */
    fun updateQualityBadge() {
        val label = playerManager.getCurrentQualityLabel()
        if (label != null) {
            binding.tvQualityBadge.text = label
            binding.tvQualityBadge.visibility = View.VISIBLE
        } else {
            binding.tvQualityBadge.visibility = View.GONE
        }
    }

    /** ✅ يظهّر زر إعادة المحاولة اليدوي — يُستدعى لما كل المحاولات التلقائية تفشل */
    fun showManualRetry() {
        binding.btnManualRetry.visibility = View.VISIBLE
        binding.btnManualRetry.requestFocus()
    }

    fun hideManualRetry() {
        binding.btnManualRetry.visibility = View.GONE
    }

    /** ✅ يظهّر شعار القناة كبير بالنص لثانيتين ونص لما المستخدم يبدّل قناة بسرعة (Zapping) */
    fun showZapOverlay(logoUrl: String, name: String) {
        zapRunnable?.let { zapHandler.removeCallbacks(it) }

        Glide.with(activity)
            .load(logoUrl)
            .placeholder(R.drawable.ic_live_tv)
            .error(R.drawable.ic_live_tv)
            .into(binding.ivZapLogo)
        binding.tvZapName.text = name
        binding.zapOverlay.visibility = View.VISIBLE

        val runnable = Runnable { binding.zapOverlay.visibility = View.GONE }
        zapRunnable = runnable
        zapHandler.postDelayed(runnable, 1500L)
    }

    /** ✅ يبدّل نسبة عرض الفيديو بالتناوب: احتواء ← تكبير ← تمديد */
    private fun cycleAspectRatio() {
        aspectModeIndex = (aspectModeIndex + 1) % aspectModes.size
        binding.videoPlayer.resizeMode = aspectModes[aspectModeIndex]
        val labelRes = when (aspectModes[aspectModeIndex]) {
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> R.string.player_aspect_zoom
            AspectRatioFrameLayout.RESIZE_MODE_FILL -> R.string.player_aspect_stretch
            else -> R.string.player_aspect_fit
        }
        Toast.makeText(activity, activity.getString(labelRes), Toast.LENGTH_SHORT).show()
    }

    /** ✅ يعرض قائمة مسارات الصوت المتاحة بالمحتوى الحالي عشان المستخدم يختار لغة */
    private fun showAudioTrackPicker() {
        val options = playerManager.getAvailableAudioTracks()
        if (options.isEmpty()) {
            Toast.makeText(activity, activity.getString(R.string.player_no_audio_tracks), Toast.LENGTH_SHORT).show()
            return
        }
        val labels = options.map { it.label }.toTypedArray()
        AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.player_audio_track_title))
            .setItems(labels) { _, which ->
                playerManager.selectAudioTrack(options[which])
            }
            .setNegativeButton(activity.getString(R.string.common_close), null)
            .show()
    }

    /** ✅ يرفع/يخفّض الصوت الداخلي للمشغل بخطوات صغيرة، ويحدّث أيقونة الكتم تلقائيًا */
    private fun adjustVolume(delta: Float) {
        currentVolume = (currentVolume + delta).coerceIn(0f, 1f)
        isMuted = currentVolume <= 0f
        playerManager.player.volume = currentVolume
        binding.btnVolume.setImageResource(if (isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_on)
    }

    fun updateFullscreenIcon(isFullscreen: Boolean) {
        binding.btnFullscreen.setImageResource(
            if (isFullscreen) R.drawable.ic_fullscreen_exit else R.drawable.ic_fullscreen_enter
        )
    }

    fun showControls() {
        binding.playerControls.visibility = View.VISIBLE
        controlsRunnable?.let { controlsHandler.removeCallbacks(it) }
        val runnable = Runnable {
            binding.playerControls.visibility = View.INVISIBLE
            binding.channelStripScroll.visibility = View.GONE
        }
        controlsRunnable = runnable
        controlsHandler.postDelayed(runnable, 5000)
    }

    fun handleControlKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false
        return when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (!isFullscreen || binding.searchOverlay.visibility == View.VISIBLE) {
                    showControls()
                    false
                } else if (binding.channelStripScroll.visibility == View.VISIBLE) {
                    showControls()
                    false
                } else {
                    // ✅ ما بيظهر شريط القنوات إلا بضغطتين متتاليتين على السهم السفلي
                    // خلال أقل من 700 ميلي ثانية — ضغطة وحدة بس بتفتح/تحدّث لوحة التحكم عادي
                    val now = System.currentTimeMillis()
                    val isDoublePress = (now - lastDpadDownPressTime) in 1..doublePressWindowMs
                    lastDpadDownPressTime = now
                    if (isDoublePress) {
                        showChannelStrip()
                        lastDpadDownPressTime = 0L
                        true
                    } else {
                        showControls()
                        false
                    }
                }
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (binding.channelStripScroll.visibility == View.VISIBLE) {
                    hideChannelStrip()
                    true
                } else {
                    showControls()
                    false
                }
            }
            KeyEvent.KEYCODE_BACK -> {
                if (binding.channelStripScroll.visibility == View.VISIBLE) {
                    hideChannelStrip()
                    true
                } else {
                    showControls()
                    false
                }
            }
            else -> {
                showControls()
                false
            }
        }
    }

    private fun showChannelStrip() {
        binding.channelStripScroll.visibility = View.VISIBLE
        val cell = binding.channelStripTrack.findViewById<View>(
            binding.channelStripTrack.getChildAt(
                currentChannelIndex.coerceIn(0, (binding.channelStripTrack.childCount - 1).coerceAtLeast(0))
            )?.id ?: View.NO_ID
        )
        cell?.requestFocus()
        showControls()
    }

    private fun hideChannelStrip() {
        binding.channelStripScroll.visibility = View.GONE
        binding.btnPlayPause.requestFocus()
    }

    private fun startSeekUpdater() {
        val runnable = object : Runnable {
            override fun run() {
                if (currentType != "live" && !userSeeking && playerManager.player.duration > 0) {
                    val pos = playerManager.player.currentPosition
                    val dur = playerManager.player.duration
                    val progress = ((pos.toFloat() / dur.toFloat()) * 1000).toInt()
                    binding.seekBar.progress = progress
                    binding.tvElapsed.text = formatDuration(pos)
                    binding.tvDuration.text = formatDuration(dur)
                }
                seekHandler.postDelayed(this, 500)
            }
        }
        seekRunnable = runnable
        seekHandler.post(runnable)
    }

    private fun updateMediaTypeUi() {
        val isLive = currentType == "live"
        binding.liveBadge.visibility = if (isLive) View.VISIBLE else View.GONE
        binding.seekBar.visibility = if (isLive) View.GONE else View.VISIBLE
        binding.tvElapsed.visibility = if (isLive) View.GONE else View.VISIBLE
        binding.tvDuration.visibility = if (isLive) View.GONE else View.VISIBLE
        if (!isLive) {
            binding.channelStripScroll.visibility = View.GONE
        }
    }

    private fun buildChannelStrip() {
        binding.channelStripTrack.removeAllViews()
        if (currentChannelList.isEmpty()) return

        val density = activity.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        // ✅ نعرض بس 6 قنوات كحد أقصى (نافذة حول القناة الحالية)، مش القائمة كاملة —
        // أسهل بكتير للتصفح بالريموت وما بتصير الشاشة مزدحمة بمربعات كتير
        val maxVisible = 6
        val total = currentChannelList.size
        val windowStart = if (total <= maxVisible) {
            0
        } else {
            (currentChannelIndex - maxVisible / 2)
                .coerceAtLeast(0)
                .coerceAtMost(total - maxVisible)
        }
        val windowEnd = (windowStart + maxVisible).coerceAtMost(total)
        val displayList = currentChannelList.subList(windowStart, windowEnd)
        val displayCurrentIndex = currentChannelIndex - windowStart

        val cells = mutableListOf<LinearLayout>()

        displayList.forEachIndexed { localIndex, channel ->
            val globalIndex = windowStart + localIndex
            val cell = LinearLayout(activity)
            cell.id = View.generateViewId()
            // ✅ شكل مستطيل أفقي (الشعار جنب الاسم) بدل مربع رأسي — وعرض متساوي (weight)
            // يخلي الست قنوات ياخدوا عرض الشاشة بالكامل بدل عمود ضيق بالنص
            cell.orientation = LinearLayout.HORIZONTAL
            cell.gravity = Gravity.CENTER_VERTICAL
            val lp = LinearLayout.LayoutParams(0, dp(52), 1f)
            lp.marginStart = dp(3)
            lp.marginEnd = dp(3)
            cell.layoutParams = lp
            cell.setPadding(dp(8), dp(4), dp(8), dp(4))
            cell.setBackgroundResource(R.drawable.tv_button_selector)
            cell.isFocusable = true
            cell.isFocusableInTouchMode = true
            cell.isClickable = true
            cell.setOnClickListener { onChannelPicked(globalIndex) }

            val logo = ImageView(activity)
            logo.layoutParams = LinearLayout.LayoutParams(dp(32), dp(32)).apply { marginEnd = dp(8) }
            logo.scaleType = ImageView.ScaleType.CENTER_INSIDE
            Glide.with(activity)
                .load(channel.streamIcon)
                .placeholder(R.drawable.ic_live_tv)
                .error(R.drawable.ic_live_tv)
                .into(logo)

            val nameText = TextView(activity)
            nameText.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            nameText.text = "${globalIndex + 1}. ${channel.name}"
            nameText.textSize = 11f
            nameText.maxLines = 1
            nameText.ellipsize = TextUtils.TruncateAt.END
            nameText.setTextColor(ContextCompat.getColor(activity, R.color.text_white))

            cell.addView(logo)
            cell.addView(nameText)

            if (localIndex == displayCurrentIndex) {
                cell.background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = dp(6).toFloat()
                    setColor(ContextCompat.getColor(activity, R.color.accent))
                }
            }

            binding.channelStripTrack.addView(cell)
            cells.add(cell)
        }

        for (i in cells.indices) {
            if (i > 0) cells[i].nextFocusRightId = cells[i - 1].id
            if (i < cells.size - 1) cells[i].nextFocusLeftId = cells[i + 1].id
            cells[i].nextFocusUpId = R.id.btn_play_pause
        }
        if (displayCurrentIndex in cells.indices) {
            val currentCellId = cells[displayCurrentIndex].id
            binding.btnPrev.nextFocusDownId = currentCellId
            binding.btnRewind10.nextFocusDownId = currentCellId
            binding.btnPlayPause.nextFocusDownId = currentCellId
            binding.btnForward10.nextFocusDownId = currentCellId
            binding.btnNext.nextFocusDownId = currentCellId
            binding.btnVolumeDown.nextFocusDownId = currentCellId
            binding.btnVolume.nextFocusDownId = currentCellId
            binding.btnVolumeUp.nextFocusDownId = currentCellId
            binding.btnAspectRatio.nextFocusDownId = currentCellId
            binding.btnAudioTrack.nextFocusDownId = currentCellId
            binding.btnFullscreen.nextFocusDownId = currentCellId
        }
    }

    private var volumeBeforeMute = 1f

    private fun toggleMute() {
        isMuted = !isMuted
        if (isMuted) {
            volumeBeforeMute = currentVolume.takeIf { it > 0f } ?: 1f
            currentVolume = 0f
        } else {
            currentVolume = volumeBeforeMute
        }
        playerManager.player.volume = currentVolume
        binding.btnVolume.setImageResource(if (isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_on)
    }

    private fun formatDuration(ms: Long): String {
        if (ms < 0) return "00:00"
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
    }

    fun release() {
        controlsRunnable?.let { controlsHandler.removeCallbacks(it) }
        seekRunnable?.let { seekHandler.removeCallbacks(it) }
        zapRunnable?.let { zapHandler.removeCallbacks(it) }
    }
}