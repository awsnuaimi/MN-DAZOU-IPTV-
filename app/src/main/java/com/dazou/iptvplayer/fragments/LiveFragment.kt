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
import com.dazou.iptvplayer.api.XtreamAPI
import com.dazou.iptvplayer.databinding.FragmentLiveBinding
import com.dazou.iptvplayer.player.PlayerCallback
import com.dazou.iptvplayer.viewmodel.LiveViewModel

class LiveFragment : Fragment() {

    private var _binding: FragmentLiveBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: LiveViewModel
    private var playerCallback: PlayerCallback? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())
            .get(LiveViewModel::class.java)
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {


        _binding = FragmentLiveBinding.inflate(
            inflater,
            container,
            false
        )


        playerCallback = requireActivity() as? PlayerCallback


        binding.rvLive.layoutManager =
            LinearLayoutManager(requireContext())


        viewModel.channels.observe(viewLifecycleOwner) { channels ->


            if (channels.isEmpty()) {

                Toast.makeText(
                    requireContext(),
                    "لم يتم العثور على قنوات",
                    Toast.LENGTH_LONG
                ).show()

                return@observe
            }


            binding.rvLive.adapter =
                ChannelAdapter(channels) { channel ->


                    val server = viewModel.getServer()


                    val url = XtreamAPI.getStreamUrl(
                        server,
                        channel.streamId,
                        channel.containerExtension,
                        "live"
                    )


                    playerCallback?.playStream(
                        url,
                        channel.name,
                        "live"
                    )

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