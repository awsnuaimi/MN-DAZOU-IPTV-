package com.dazou.iptvplayer.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.dazou.iptvplayer.App
import com.dazou.iptvplayer.MainActivity
import com.dazou.iptvplayer.adapter.FavoriteAdapter
import com.dazou.iptvplayer.adapter.HistoryAdapter
import com.dazou.iptvplayer.api.XtreamAPI
import com.dazou.iptvplayer.databinding.FragmentHomeBinding
import com.dazou.iptvplayer.model.FavoriteItem
import com.dazou.iptvplayer.model.HistoryItem
import com.dazou.iptvplayer.model.XtreamEpisode

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvHistory.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvFavorites.layoutManager = GridLayoutManager(requireContext(), 4)
        loadContent()
    }

    override fun onResume() {
        super.onResume()
        loadContent()
    }

    private fun loadContent() {
        if (_binding == null) return
        val app = requireActivity().application as App

        val history = app.container.historyManager.getHistory()
        if (history.isEmpty()) {
            binding.tvHistoryLabel.visibility = View.GONE
            binding.rvHistory.visibility = View.GONE
        } else {
            binding.tvHistoryLabel.visibility = View.VISIBLE
            binding.rvHistory.visibility = View.VISIBLE
            binding.rvHistory.adapter = HistoryAdapter(history) { item -> playHistoryItem(item) }
        }

        val favorites = app.container.favoritesManager.getFavorites()
        binding.tvWelcome.text = if (favorites.isEmpty() && history.isEmpty())
            "🎉 مرحبًا بك في DAZOU IPTV — شغّل أي محتوى أو اضغط مطولًا لإضافته للمفضلة"
        else
            "🎉 مرحبًا بك في DAZOU IPTV"

        binding.rvFavorites.adapter = FavoriteAdapter(
            favorites,
            onClick = { item -> playFavorite(item) },
            onLongClick = { item -> removeFavorite(item) }
        )
    }

    private fun playHistoryItem(item: HistoryItem) {
        playByTypeAndId(item.type, item.id, item.name, item.containerExtension)
    }

    private fun playFavorite(item: FavoriteItem) {
        playByTypeAndId(item.type, item.id, item.name, item.containerExtension)
    }

    private fun playByTypeAndId(type: String, id: Int, name: String, containerExtension: String) {
        val app = requireActivity().application as App
        val server = app.container.currentRepository?.server
        if (server == null) {
            Toast.makeText(requireContext(), "اختر حساب IPTV أولاً", Toast.LENGTH_SHORT).show()
            return
        }

        when (type) {
            "live" -> {
                val url = XtreamAPI.getStreamUrl(server, id, containerExtension, "live")
                (activity as? MainActivity)?.playExternalMedia(url, name, "live")
            }
            "movie" -> {
                val url = XtreamAPI.getMovieUrl(server, id, containerExtension)
                (activity as? MainActivity)?.playExternalMedia(url, name, "movie")
            }
            "series" -> {
                Toast.makeText(requireContext(), "⏳ جاري تحميل الحلقات...", Toast.LENGTH_SHORT).show()
                XtreamAPI.getSeriesInfo(server, id) { episodes ->
                    showEpisodesDialog(name, episodes)
                }
            }
        }
    }

    private fun showEpisodesDialog(seriesName: String, episodes: List<XtreamEpisode>) {
        if (!isAdded) return
        if (episodes.isEmpty()) {
            Toast.makeText(requireContext(), "لا توجد حلقات متاحة لهذا المسلسل", Toast.LENGTH_SHORT).show()
            return
        }

        val sorted = episodes.sortedWith(compareBy({ it.seasonNum }, { it.episodeNum }))
        val labels = sorted.map { "الموسم ${it.seasonNum} • الحلقة ${it.episodeNum}: ${it.title}" }.toTypedArray()

        android.app.AlertDialog.Builder(requireContext())
            .setTitle(seriesName)
            .setItems(labels) { _, which ->
                val server = (requireActivity().application as App).container.currentRepository?.server
                    ?: return@setItems
                val episode = sorted[which]
                val url = XtreamAPI.getSeriesEpisodeUrl(server, episode.id, episode.containerExtension)
                (activity as? MainActivity)?.playExternalMedia(url, episode.title, "series")
            }
            .setNegativeButton("إغلاق", null)
            .show()
    }

    private fun removeFavorite(item: FavoriteItem) {
        val app = requireActivity().application as App
        app.container.favoritesManager.removeFavorite(item.type, item.id)
        Toast.makeText(requireContext(), "🗑️ تم الحذف من المفضلة", Toast.LENGTH_SHORT).show()
        loadContent()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}