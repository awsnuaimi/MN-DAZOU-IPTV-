package com.dazou.iptvplayer.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.dazou.iptvplayer.App
import com.dazou.iptvplayer.MainActivity
import com.dazou.iptvplayer.adapter.CategoryAdapter
import com.dazou.iptvplayer.adapter.SeriesAdapter
import com.dazou.iptvplayer.api.XtreamAPI
import com.dazou.iptvplayer.databinding.FragmentSeriesBinding
import com.dazou.iptvplayer.model.XtreamCategory
import com.dazou.iptvplayer.model.XtreamEpisode
import com.dazou.iptvplayer.model.XtreamSeries
import com.dazou.iptvplayer.viewmodel.SeriesViewModel
import com.dazou.iptvplayer.viewmodel.ViewModelFactory

class SeriesFragment : Fragment() {

    private var _binding: FragmentSeriesBinding? = null
    private val binding get() = _binding!!

    private lateinit var seriesViewModel: SeriesViewModel
    private var selectedSeries: XtreamSeries? = null
    private var hasRequestedInitialFocus = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSeriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as App
        seriesViewModel = ViewModelProvider(this, ViewModelFactory(app.container.currentRepository))
            .get(SeriesViewModel::class.java)

        binding.rvSeriesCategories.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSeries.layoutManager = GridLayoutManager(requireContext(), 4)

        seriesViewModel.categories.observe(viewLifecycleOwner) { categories ->
            if (categories.isEmpty()) {
                Toast.makeText(requireContext(), "لا توجد مجموعات مسلسلات – تأكد من الحساب", Toast.LENGTH_LONG).show()
            }
            binding.rvSeriesCategories.adapter = CategoryAdapter(categories) { category ->
                openCategory(category)
            }

            if (!hasRequestedInitialFocus && categories.isNotEmpty()) {
                hasRequestedInitialFocus = true
                requestFocusWhenReady(binding.rvSeriesCategories)
            }
        }

        seriesViewModel.seriesList.observe(viewLifecycleOwner) { list ->
            binding.tvSeriesStatus.text = if (list.isEmpty())
                "لا توجد مسلسلات بهذه المجموعة"
            else
                "${list.size} مسلسل"
            binding.rvSeries.adapter = SeriesAdapter(list, app.container.favoritesManager) { series ->
                onSeriesClicked(series)
            }
        }

        seriesViewModel.episodes.observe(viewLifecycleOwner) { episodes ->
            val series = selectedSeries ?: return@observe
            showEpisodesDialog(series, episodes)
        }

        seriesViewModel.loadCategories()
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

    private fun openCategory(category: XtreamCategory) {
        binding.tvSeriesStatus.text = "جاري التحميل..."
        seriesViewModel.loadSeries(category.categoryId)
    }

    private fun onSeriesClicked(series: XtreamSeries) {
        selectedSeries = series
        Toast.makeText(requireContext(), "⏳ جاري تحميل الحلقات...", Toast.LENGTH_SHORT).show()
        seriesViewModel.loadEpisodes(series.seriesId)
    }

    private fun showEpisodesDialog(series: XtreamSeries, episodes: List<XtreamEpisode>) {
        if (!isAdded) return
        if (episodes.isEmpty()) {
            Toast.makeText(requireContext(), "لا توجد حلقات متاحة لهذا المسلسل", Toast.LENGTH_SHORT).show()
            return
        }

        val sorted = episodes.sortedWith(compareBy({ it.seasonNum }, { it.episodeNum }))
        val labels = sorted.map { "الموسم ${it.seasonNum} • الحلقة ${it.episodeNum}: ${it.title}" }.toTypedArray()

        android.app.AlertDialog.Builder(requireContext())
            .setTitle(series.name)
            .setItems(labels) { _, which ->
                playEpisode(sorted[which])
            }
            .setNegativeButton("إغلاق", null)
            .show()
    }

    private fun playEpisode(episode: XtreamEpisode) {
        val server = seriesViewModel.getServer() ?: return
        val url = XtreamAPI.getSeriesEpisodeUrl(server, episode.id, episode.containerExtension)
        (activity as? MainActivity)?.playExternalMedia(url, episode.title, "series")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}