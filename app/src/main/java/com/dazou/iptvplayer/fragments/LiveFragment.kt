package com.dazou.iptvplayer.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.dazou.iptvplayer.adapter.CategoryAdapter
import com.dazou.iptvplayer.adapter.ChannelAdapter
import com.dazou.iptvplayer.api.XtreamAPI
import com.dazou.iptvplayer.databinding.FragmentLiveBinding
import com.dazou.iptvplayer.model.XtreamCategory
import com.dazou.iptvplayer.player.PlayerCallback
import com.dazou.iptvplayer.viewmodel.LiveViewModel


class LiveFragment : Fragment() {


    private var _binding: FragmentLiveBinding? = null

    private val binding get() = _binding!!


    private lateinit var viewModel: LiveViewModel


    private var playerCallback: PlayerCallback? = null


    private var inChannelsMode = false





    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)


        viewModel =
            ViewModelProvider(requireActivity())
                .get(LiveViewModel::class.java)

    }







    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {


        _binding =
            FragmentLiveBinding.inflate(
                inflater,
                container,
                false
            )



        playerCallback =
            requireActivity() as? PlayerCallback




        binding.rvLive.layoutManager =
            LinearLayoutManager(
                requireContext()
            )



        binding.btnBackToCategories.setOnClickListener {

            showCategories()

        }



        observeCategories()

        observeChannels()



        showCategories()



        return binding.root

    }





    private fun observeCategories() {

        viewModel.categories.observe(
            viewLifecycleOwner
        ) { categories ->

            if (!inChannelsMode) {

                if (categories.isEmpty()) {

                    Toast.makeText(
                        requireContext(),
                        "لا توجد مجموعات أو لم يتم اختيار حساب",
                        Toast.LENGTH_LONG
                    ).show()

                    return@observe

                }

                binding.rvLive.adapter =
                    CategoryAdapter(categories) { category ->

                        openCategory(category)

                    }

            }

        }

    }





    private fun observeChannels() {

        viewModel.channels.observe(
            viewLifecycleOwner
        ) { channels ->

            if (inChannelsMode) {

                if (channels.isEmpty()) {

                    Toast.makeText(
                        requireContext(),
                        "لا توجد قنوات بهذه المجموعة",
                        Toast.LENGTH_SHORT
                    ).show()

                }

                binding.rvLive.adapter =
                    ChannelAdapter(channels) { channel ->

                        val server =
                            viewModel.getServer()

                        if (server == null) {

                            Toast.makeText(
                                requireContext(),
                                "اختر حساب IPTV أولاً",
                                Toast.LENGTH_SHORT
                            ).show()

                            return@ChannelAdapter

                        }

                        val url =
                            XtreamAPI.getStreamUrl(
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

        }

    }





    private fun showCategories() {

        inChannelsMode = false

        binding.headerRow.visibility = View.GONE

        viewModel.loadCategories()

    }





    private fun openCategory(
        category: XtreamCategory
    ) {

        inChannelsMode = true

        binding.headerRow.visibility = View.VISIBLE

        binding.tvCategoryTitle.text =
            category.categoryName

        viewModel.loadChannels(
            category.categoryId
        )

    }







    override fun onDestroyView(){

        super.onDestroyView()

        _binding = null

    }


}