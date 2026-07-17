package com.dazou.iptvplayer.fragments

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.dazou.iptvplayer.App
import com.dazou.iptvplayer.adapter.CategoryAdapter
import com.dazou.iptvplayer.adapter.ChannelAdapter
import com.dazou.iptvplayer.api.XtreamAPI
import com.dazou.iptvplayer.databinding.FragmentLiveBinding
import com.dazou.iptvplayer.model.XtreamCategory
import com.dazou.iptvplayer.player.PlayerCallback
import com.dazou.iptvplayer.viewmodel.LiveViewModel
import com.dazou.iptvplayer.viewmodel.ViewModelFactory

class LiveFragment : Fragment(), BackHandledFragment {

    private var _binding: FragmentLiveBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: LiveViewModel
    private var inChannelsMode = false
    private var lastCategories: List<XtreamCategory> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = requireActivity().application as App
        viewModel = ViewModelProvider(this, ViewModelFactory(app.container.currentRepository))
            .get(LiveViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLiveBinding.inflate(inflater, container, false)
        binding.rvLive.layoutManager = LinearLayoutManager(requireContext())

        // زر الرجوع من القنوات إلى المجموعات
        binding.btnBackToCategories.setOnClickListener { showCategories() }

        // دعم أسهم الريموت يمين/يسار للتنقل بين المجموعات والقنوات
        binding.rvLive.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        if (inChannelsMode) {
                            showCategories()
                            true
                        } else false
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (!inChannelsMode) {
                            binding.rvLive.focusedChild?.performClick()
                            true
                        } else false
                    }
                    else -> false
                }
            } else false
        }

        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            lastCategories = categories
            if (!inChannelsMode) {
                if (categories.isEmpty()) {
                    Toast.makeText(requireContext(), "لا توجد مجموعات – تأكد من الحساب", Toast.LENGTH_LONG).show()
                    return@observe
                }
                binding.rvLive.adapter = CategoryAdapter(categories) { category -> openCategory(category) }
            }
        }

        viewModel.channels.observe(viewLifecycleOwner) { channels ->
            if (inChannelsMode) {
                binding.rvLive.adapter = ChannelAdapter(channels) { channel ->
                    val server = viewModel.getServer()
                    if (server == null) {
                        Toast.makeText(requireContext(), "اختر حساب IPTV أولاً", Toast.LENGTH_SHORT).show()
                        return@ChannelAdapter
                    }
                    val url = XtreamAPI.getStreamUrl(server, channel.streamId, channel.containerExtension, "live")
                    val callback = requireActivity() as? PlayerCallback
                    callback?.playStream(url, channel.name, "live")
                }
            }
        }

        showCategories()
        return binding.root
    }

    private fun showCategories() {
        inChannelsMode = false
        binding.headerRow.visibility = View.GONE
        if (lastCategories.isNotEmpty()) {
            binding.rvLive.adapter = CategoryAdapter(lastCategories) { category -> openCategory(category) }
        } else {
            viewModel.loadCategories()
        }
    }

    private fun openCategory(category: XtreamCategory) {
        inChannelsMode = true
        binding.headerRow.visibility = View.VISIBLE
        binding.tvCategoryTitle.text = category.categoryName
        viewModel.loadChannels(category.categoryId)
    }

    // يستدعى من MainActivity عند الضغط على زر الرجوع بالريموت
    override fun onBackPressedInFragment(): Boolean {
        return if (inChannelsMode) {
            showCategories()
            true
        } else {
            false
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}