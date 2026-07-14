package com.dazou.iptvplayer.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.dazou.iptvplayer.adapter.ChannelAdapter
import com.dazou.iptvplayer.databinding.FragmentLiveBinding
import com.dazou.iptvplayer.player.PlayerCallback
import com.dazou.iptvplayer.viewmodel.LiveViewModel
import com.dazou.iptvplayer.api.XtreamAPI

class LiveFragment : Fragment() {

    private var _binding: FragmentLiveBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: LiveViewModel
    private var callback: PlayerCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(LiveViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLiveBinding.inflate(inflater, container, false)
        callback = requireActivity() as? PlayerCallback

        binding.rvLive.layoutManager = LinearLayoutManager(requireContext())

        viewModel.channels.observe(viewLifecycleOwner) { channels ->
            binding.rvLive.adapter = ChannelAdapter(channels) { channel ->
                val url = XtreamAPI.getStreamUrl(
                    viewModel.getServer(), channel.streamId, channel.containerExtension
                )
                callback?.playStream(url, channel.name, "live")
            }
        }

        viewModel.loadAllChannels()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}