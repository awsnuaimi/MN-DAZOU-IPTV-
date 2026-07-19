package com.dazou.iptvplayer.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.dazou.iptvplayer.App
import com.dazou.iptvplayer.MainActivity
import com.dazou.iptvplayer.adapter.FavoriteAdapter
import com.dazou.iptvplayer.api.XtreamAPI
import com.dazou.iptvplayer.databinding.FragmentHomeBinding
import com.dazou.iptvplayer.model.FavoriteItem
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
        binding.rvHome.layoutManager = GridLayoutManager(requireContext(), 4)
        loadFavorites()
    }

    override fun onResume() {
        super.onResume()
        // نعيد التحميل كل مرة تُفتح فيها الشاشة، لأن المفضلة ممكن تتغيّر من شاشات ثانية
        loadFavorites()
    }

    private fun loadFavorites() {
        if (_binding == null) return
        val app = requireActivity().application as App
        val favorites = app.container.favoritesManager.getFavorites()

        binding.tvWelcome.text = if (favorites.isEmpty())
            "⭐ لا توجد عناصر بالمفضلة بعد — اضغط مطولًا على أي فيلم أو مسلسل لإضافته"
        else
            "⭐ المفضلة (${favorites.size})"

        binding.rvHome.adapter = FavoriteAdapter(
            favorites,
            onClick = { item -> playFavorite(item) },
            onLongClick = { item -> removeFavorite(item) }
        )
    }

    private fun playFavorite(item: FavoriteItem) {
        val app = requireActivity().application as App
        val server = app.container.currentRepository?.server
        if (server == null) {
            Toast.makeText(requireContext(), "اختر حساب IPTV أولاً", Toast.LENGTH_SHORT).show()
            return
        }

        when (item.type) {
            "live" -> {
                val url = XtreamAPI.getStreamUrl(server, item.id, item.containerExtension, "live")
                (activity as? MainActivity)?.playExternalMedia(url, item.name, "live")
            }
            "movie" -> {
                val url = XtreamAPI.getMovieUrl(server, item.id, item.containerExtension)
                (activity as? MainActivity)?.playExternalMedia(url, item.name, "movie")
            }
            "series" -> {
                Toast.makeText(requireContext(), "⏳ جاري تحميل الحلقات...", Toast.LENGTH_SHORT).show()
                XtreamAPI.getSeriesInfo(server, item.id) { episodes ->
                    showEpisodesDialog(item, episodes)
                }
            }
        }
    }

    private fun showEpisodesDialog(item: FavoriteItem, episodes: List<XtreamEpisode>) {
        if (!isAdded) return
        if (episodes.isEmpty()) {
            Toast.makeText(requireContext(), "لا توجد حلقات متاحة لهذا المسلسل", Toast.LENGTH_SHORT).show()
            return
        }

        val sorted = episodes.sortedWith(compareBy({ it.seasonNum }, { it.episodeNum }))
        val labels = sorted.map { "الموسم ${it.seasonNum} • الحلقة ${it.episodeNum}: ${it.title}" }.toTypedArray()

        android.app.AlertDialog.Builder(requireContext())
            .setTitle(item.name)
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
        loadFavorites()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}