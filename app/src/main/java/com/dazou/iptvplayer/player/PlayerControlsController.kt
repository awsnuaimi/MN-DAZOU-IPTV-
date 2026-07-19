package com.dazou.iptvplayer.player

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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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

    private val controlsHandler = Handler(Looper.getMainLooper())
    private var controlsRunnable: Runnable? = null

    private val seekHandler = Handler(Looper.getMainLooper())
    private var seekRunnable: Runnable? = null

    fun setup(
        onPrev: () -> Unit,
        onNext: () -> Unit,
        onFullscreenToggle: () -> Unit,
        onPip: () -> Unit
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
        showControls()
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
                if (isFocusInPlayerButtons() && binding.channelStripScroll.visibility != View.VISIBLE) {
                    showChannelStrip()
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

    private fun isFocusInPlayerButtons(): Boolean {
        val f = activity.currentFocus ?: return false
        return f === binding.btnPrev || f === binding.btnPlayPause || f === binding.btnNext ||
            f === binding.btnVolume || f === binding.btnFullscreen || f === binding.videoPlayer
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

        val cells = mutableListOf<LinearLayout>()

        currentChannelList.forEachIndexed { index, channel ->
            val cell = LinearLayout(activity)
            cell.id = View.generateViewId()
            cell.orientation = LinearLayout.VERTICAL
            cell.gravity = Gravity.CENTER
            val lp = LinearLayout.LayoutParams(dp(70), dp(76))
            lp.marginEnd = dp(6)
            cell.layoutParams = lp
            cell.setPadding(dp(4), dp(4), dp(4), dp(4))
            cell.setBackgroundResource(R.drawable.tv_button_selector)
            cell.isFocusable = true
            cell.isFocusableInTouchMode = true
            cell.isClickable = true
            cell.setOnClickListener { onChannelPicked(index) }

            val logo = ImageView(activity)
            logo.layoutParams = LinearLayout.LayoutParams(dp(30), dp(30))
            logo.scaleType = ImageView.ScaleType.CENTER_INSIDE
            Glide.with(activity)
                .load(channel.streamIcon)
                .placeholder(R.drawable.ic_live_tv)
                .error(R.drawable.ic_live_tv)
                .into(logo)

            val idText = TextView(activity)
            idText.text = "${index + 1}"
            idText.textSize = 9f
            idText.setTextColor(ContextCompat.getColor(activity, R.color.text_gray))
            idText.gravity = Gravity.CENTER

            val nameText = TextView(activity)
            nameText.text = channel.name
            nameText.textSize = 9.5f
            nameText.maxLines = 1
            nameText.ellipsize = TextUtils.TruncateAt.END
            nameText.gravity = Gravity.CENTER
            nameText.setTextColor(ContextCompat.getColor(activity, R.color.text_white))

            cell.addView(logo)
            cell.addView(idText)
            cell.addView(nameText)

            if (index == currentChannelIndex) {
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
        if (currentChannelIndex in cells.indices) {
            val currentCellId = cells[currentChannelIndex].id
            binding.btnPrev.nextFocusDownId = currentCellId
            binding.btnPlayPause.nextFocusDownId = currentCellId
            binding.btnNext.nextFocusDownId = currentCellId
            binding.btnVolume.nextFocusDownId = currentCellId
            binding.btnFullscreen.nextFocusDownId = currentCellId
        }

        binding.channelStripScroll.post {
            val cellWidth = dp(76)
            val scrollX = (currentChannelIndex * cellWidth) - (binding.channelStripScroll.width / 2) + (cellWidth / 2)
            binding.channelStripScroll.smoothScrollTo(scrollX.coerceAtLeast(0), 0)
        }
    }

    private fun toggleMute() {
        isMuted = !isMuted
        playerManager.player.volume = if (isMuted) 0f else 1f
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
    }
}