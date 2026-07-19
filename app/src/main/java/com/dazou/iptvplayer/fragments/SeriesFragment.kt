package com.dazou.iptvplayer.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.dazou.iptvplayer.App
import com.dazou.iptvplayer.MainActivity
import com.dazou.iptvplayer.R
import com.dazou.iptvplayer.adapter.CategoryAdapter
import com.dazou.iptvplayer.adapter.SeriesAdapter
import com.dazou.iptvplayer.api.XtreamAPI
import com.dazou.iptvplayer.databinding.FragmentSeriesBinding
import com.dazou.iptvplayer.model.HistoryItem
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
    private var allSeriesInCategory: List<XtreamSeries> = emptyList()

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
            allSeriesInCategory = list
            binding.etSearchSeries.text?.clear()
            displaySeries(list)
        }

        binding.etSearchSeries.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim().orEmpty()
                val filtered = if (query.isEmpty()) {
                    allSeriesInCategory
                } else {
                    allSeriesInCategory.filter { it.name.contains(query, ignoreCase = true) }
                }
                displaySeries(filtered, isSearchResult = query.isNotEmpty())
            }
        })

        seriesViewModel.episodes.observe(viewLifecycleOwner) { episodes ->
            val series = selectedSeries ?: return@observe
            showEpisodesDialog(series, episodes)
        }

        seriesViewModel.loadCategories()
    }

    private fun displaySeries(list: List<XtreamSeries>, isSearchResult: Boolean = false) {
        if (_binding == null) return
        binding.tvSeriesStatus.text = when {
            list.isEmpty() && isSearchResult -> "لا نتائج مطابقة للبحث"
            list.isEmpty() -> "لا توجد مسلسلات بهذه المجموعة"
            else -> "${list.size} مسلسل"
        }
        binding.rvSeries.adapter = SeriesAdapter(list, requireApp().container.favoritesManager) { series ->
            showSeriesDetails(series)
        }
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

    private fun showSeriesDetails(series: XtreamSeries) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_media_details, null)

        val poster = dialogView.findViewById<ImageView>(R.id.ivDetailsPoster)
        val meta = dialogView.findViewById<TextView>(R.id.tvDetailsMeta)
        val plot = dialogView.findViewById<TextView>(R.id.tvDetailsPlot)
        val cast = dialogView.findViewById<TextView>(R.id.tvDetailsCast)

        Glide.with(this).load(series.cover).into(poster)

        val metaParts = mutableListOf<String>()
        if (series.year.isNotBlank()) metaParts.add(series.year)
        if (series.genre.isNotBlank()) metaParts.add(series.genre)
        if (series.rating.isNotBlank()) metaParts.add("⭐ ${series.rating}")
        meta.text = metaParts.joinToString("  •  ")

        plot.text = series.plot.ifBlank { "لا يوجد وصف متاح." }

        val castParts = mutableListOf<String>()
        if (series.cast.isNotBlank()) castParts.add("🎭 ${series.cast}")
        if (series.director.isNotBlank()) castParts.add("🎬 إخراج: ${series.director}")
        cast.text = castParts.joinToString("\n")
        cast.visibility = if (castParts.isEmpty()) View.GONE else View.VISIBLE

        AlertDialog.Builder(requireContext())
            .setTitle(series.name)
            .setView(dialogView)
            .setPositiveButton("📺 عرض الحلقات") { _, _ -> onSeriesClicked(series) }
            .setNegativeButton("إغلاق", null)
            .show()
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

        AlertDialog.Builder(requireContext())
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

        val series = selectedSeries
        if (series != null) {
            requireApp().container.historyManager.addOrUpdateHistory(
                HistoryItem(
                    type = "series",
                    id = series.seriesId,
                    name = series.name,
                    timestamp = System.currentTimeMillis(),
                    icon = series.cover,
                    containerExtension = episode.containerExtension
                )
            )
        }

        (activity as? MainActivity)?.playExternalMedia(url, episode.title, "series", series?.seriesId ?: -1)
    }

    private fun requireApp(): App = requireActivity().application as App

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}