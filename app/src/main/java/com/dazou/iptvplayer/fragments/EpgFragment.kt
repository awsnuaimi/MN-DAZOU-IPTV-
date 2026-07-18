package com.dazou.iptvplayer.fragments

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.dazou.iptvplayer.App
import com.dazou.iptvplayer.MainActivity
import com.dazou.iptvplayer.R
import com.dazou.iptvplayer.api.XtreamAPI
import com.dazou.iptvplayer.databinding.FragmentEpgBinding
import com.dazou.iptvplayer.model.XtreamChannel
import com.dazou.iptvplayer.model.XtreamEpgProgram
import com.dazou.iptvplayer.viewmodel.LiveViewModel
import com.dazou.iptvplayer.viewmodel.ViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EpgFragment : Fragment() {

    private var _binding: FragmentEpgBinding? = null
    private val binding get() = _binding!!

    private lateinit var liveViewModel: LiveViewModel

    private val windowMinutes = 240
    private val dpPerMinute = 4
    private var windowStart = 0L

    private var allChannels: List<XtreamChannel> = emptyList()
    private var loadQueue: MutableList<Triple<XtreamChannel, LinearLayout, View>> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEpgBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        windowStart = System.currentTimeMillis() / 1000

        val app = requireActivity().application as App
        liveViewModel = ViewModelProvider(this, ViewModelFactory(app.container.currentRepository))
            .get(LiveViewModel::class.java)

        liveViewModel.channels.observe(viewLifecycleOwner) { channels ->
            if (channels.isEmpty()) {
                binding.epgGrid.removeAllViews()
                binding.epgStatus.text = "لا توجد قنوات لعرضها"
                return@observe
            }
            allChannels = channels
            val limited = channels.take(25)
            binding.epgStatus.text = "عرض ${limited.size} من ${channels.size} قناة (أول 25 قناة حاليًا)"

            binding.epgGrid.removeAllViews()
            addTimeRuler()

            loadQueue.clear()
            limited.forEach { channel ->
                val (row, placeholder) = buildChannelRow(channel)
                binding.epgGrid.addView(row)
                loadQueue.add(Triple(channel, row, placeholder))
            }
            processNextInQueue()
        }

        liveViewModel.loadChannels(null)
    }

    private fun processNextInQueue() {
        if (!isAdded || loadQueue.isEmpty()) return
        val (channel, row, placeholder) = loadQueue.removeAt(0)
        val server = liveViewModel.getServer()
        if (server == null) {
            processNextInQueue()
            return
        }
        XtreamAPI.getShortEpg(server, channel.streamId) { programs ->
            if (!isAdded) return@getShortEpg
            if (placeholder.parent != null) {
                row.removeView(placeholder)
            }
            renderPrograms(row, programs, channel)
            processNextInQueue()
        }
    }

    private fun dp(value: Int): Int {
        val density = resources.displayMetrics.density
        return (value * density).toInt()
    }

    private fun addTimeRuler() {
        val row = LinearLayout(requireContext())
        row.orientation = LinearLayout.HORIZONTAL

        val spacer = TextView(requireContext())
        spacer.layoutParams = LinearLayout.LayoutParams(dp(140), dp(36))
        row.addView(spacer)

        var minutesFromStart = 0
        while (minutesFromStart <= windowMinutes) {
            val markTime = windowStart + (minutesFromStart * 60)
            val label = TextView(requireContext())
            label.text = formatTime(markTime)
            label.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_gray))
            label.textSize = 12f
            label.gravity = Gravity.CENTER
            label.layoutParams = LinearLayout.LayoutParams(dp(30 * dpPerMinute), dp(36))
            row.addView(label)
            minutesFromStart += 30
        }

        binding.epgGrid.addView(row)
    }

    private fun buildChannelRow(channel: XtreamChannel): Pair<LinearLayout, View> {
        val row = LinearLayout(requireContext())
        row.orientation = LinearLayout.HORIZONTAL
        val rowParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        rowParams.topMargin = dp(2)
        row.layoutParams = rowParams

        val nameLabel = TextView(requireContext())
        nameLabel.text = channel.name
        nameLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_white))
        nameLabel.textSize = 13f
        nameLabel.maxLines = 2
        nameLabel.setPadding(dp(6), dp(4), dp(6), dp(4))
        nameLabel.gravity = Gravity.CENTER_VERTICAL
        nameLabel.layoutParams = LinearLayout.LayoutParams(dp(140), dp(46))
        nameLabel.setBackgroundResource(R.drawable.tv_button_selector)
        nameLabel.isFocusable = true
        nameLabel.isFocusableInTouchMode = true
        nameLabel.isClickable = true
        nameLabel.setOnClickListener { playChannel(channel) }
        row.addView(nameLabel)

        val placeholder = TextView(requireContext())
        placeholder.text = "..."
        placeholder.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_gray))
        placeholder.textSize = 12f
        placeholder.gravity = Gravity.CENTER
        placeholder.layoutParams = LinearLayout.LayoutParams(dp(windowMinutes * dpPerMinute), dp(46))
        row.addView(placeholder)

        return row to placeholder
    }

    private fun renderPrograms(row: LinearLayout, programs: List<XtreamEpgProgram>, channel: XtreamChannel) {
        val windowEnd = windowStart + (windowMinutes * 60)
        var cursor = windowStart

        val visiblePrograms = programs
            .filter { it.stopTimestamp > windowStart && it.startTimestamp < windowEnd }
            .sortedBy { it.startTimestamp }

        if (visiblePrograms.isEmpty()) {
            val empty = TextView(requireContext())
            empty.text = "لا توجد بيانات برنامج"
            empty.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_gray))
            empty.textSize = 12f
            empty.gravity = Gravity.CENTER
            empty.layoutParams = LinearLayout.LayoutParams(dp(windowMinutes * dpPerMinute), dp(46))
            row.addView(empty)
            return
        }

        for (program in visiblePrograms) {
            val start = program.startTimestamp.coerceAtLeast(cursor)
            val end = program.stopTimestamp.coerceAtMost(windowEnd)

            if (start > cursor) {
                val gapMinutes = ((start - cursor) / 60).toInt()
                if (gapMinutes > 0) {
                    val gap = View(requireContext())
                    gap.setBackgroundColor(Color.parseColor("#151515"))
                    gap.layoutParams = LinearLayout.LayoutParams(dp(gapMinutes * dpPerMinute), dp(46))
                    row.addView(gap)
                }
            }

            val durationMinutes = ((end - start) / 60).toInt().coerceAtLeast(1)

            val block = TextView(requireContext())
            block.text = program.title
            block.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_white))
            block.textSize = 12f
            block.maxLines = 2
            block.ellipsize = TextUtils.TruncateAt.END
            block.setPadding(dp(6), dp(4), dp(6), dp(4))
            block.gravity = Gravity.CENTER_VERTICAL
            block.isFocusable = true
            block.isFocusableInTouchMode = true
            block.isClickable = true
            block.setOnClickListener { playChannel(channel) }

            val bg = GradientDrawable()
            bg.cornerRadius = dp(4).toFloat()
            bg.setColor(
                if (program.nowPlaying)
                    ContextCompat.getColor(requireContext(), R.color.accent)
                else
                    ContextCompat.getColor(requireContext(), R.color.panel_darker)
            )
            block.background = bg

            val lp = LinearLayout.LayoutParams(dp(durationMinutes * dpPerMinute), dp(46))
            lp.marginEnd = dp(1)
            block.layoutParams = lp

            row.addView(block)
            cursor = end
        }
    }

    private fun playChannel(channel: XtreamChannel) {
        (activity as? MainActivity)?.playChannelFromExternal(channel, allChannels)
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp * 1000))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        loadQueue.clear()
        _binding = null
    }
}