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
import com.dazou.iptvplayer.adapter.MovieAdapter
import com.dazou.iptvplayer.api.XtreamAPI
import com.dazou.iptvplayer.databinding.FragmentMoviesBinding
import com.dazou.iptvplayer.model.HistoryItem
import com.dazou.iptvplayer.model.XtreamCategory
import com.dazou.iptvplayer.model.XtreamMovie
import com.dazou.iptvplayer.viewmodel.MoviesViewModel
import com.dazou.iptvplayer.viewmodel.ViewModelFactory

class MoviesFragment : Fragment() {

    private var _binding: FragmentMoviesBinding? = null
    private val binding get() = _binding!!

    private lateinit var moviesViewModel: MoviesViewModel
    private var hasRequestedInitialFocus = false
    private var allMoviesInCategory: List<XtreamMovie> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMoviesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as App
        moviesViewModel = ViewModelProvider(this, ViewModelFactory(app.container.currentRepository))
            .get(MoviesViewModel::class.java)

        binding.rvMovieCategories.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMovies.layoutManager = GridLayoutManager(requireContext(), 4)

        moviesViewModel.categories.observe(viewLifecycleOwner) { categories ->
            if (categories.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.movies_no_categories), Toast.LENGTH_LONG).show()
            }
            binding.rvMovieCategories.adapter = CategoryAdapter(categories) { category ->
                openCategory(category)
            }

            if (!hasRequestedInitialFocus && categories.isNotEmpty()) {
                hasRequestedInitialFocus = true
                requestFocusWhenReady(binding.rvMovieCategories)
            }
        }

        moviesViewModel.movies.observe(viewLifecycleOwner) { movies ->
            allMoviesInCategory = movies
            binding.etSearchMovies.text?.clear()
            displayMovies(movies)
        }

        binding.etSearchMovies.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim().orEmpty()
                val filtered = if (query.isEmpty()) {
                    allMoviesInCategory
                } else {
                    allMoviesInCategory.filter { it.name.contains(query, ignoreCase = true) }
                }
                displayMovies(filtered, isSearchResult = query.isNotEmpty())
            }
        })

        moviesViewModel.loadCategories()
    }

    private fun displayMovies(movies: List<XtreamMovie>, isSearchResult: Boolean = false) {
        if (_binding == null) return
        binding.tvMoviesStatus.text = when {
            movies.isEmpty() && isSearchResult -> getString(R.string.movies_no_search_results)
            movies.isEmpty() -> getString(R.string.movies_no_results_in_category)
            else -> getString(R.string.movies_count, movies.size)
        }
        binding.rvMovies.adapter = MovieAdapter(movies, requireApp().container.favoritesManager) { movie ->
            showMovieDetails(movie)
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
        binding.tvMoviesStatus.text = getString(R.string.common_loading)
        moviesViewModel.loadMovies(category.categoryId)
    }

    private fun showMovieDetails(movie: XtreamMovie) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_media_details, null)

        val poster = dialogView.findViewById<ImageView>(R.id.ivDetailsPoster)
        val meta = dialogView.findViewById<TextView>(R.id.tvDetailsMeta)
        val plot = dialogView.findViewById<TextView>(R.id.tvDetailsPlot)
        val cast = dialogView.findViewById<TextView>(R.id.tvDetailsCast)

        Glide.with(this).load(movie.streamIcon).into(poster)

        val metaParts = mutableListOf<String>()
        if (movie.year.isNotBlank()) metaParts.add(movie.year)
        if (movie.genre.isNotBlank()) metaParts.add(movie.genre)
        if (movie.rating.isNotBlank()) metaParts.add("⭐ ${movie.rating}")
        meta.text = metaParts.joinToString("  •  ")

        plot.text = movie.plot.ifBlank { getString(R.string.movies_no_description) }

        val castParts = mutableListOf<String>()
        if (movie.cast.isNotBlank()) castParts.add("🎭 ${movie.cast}")
        if (movie.director.isNotBlank()) castParts.add("🎬 ${movie.director}")
        cast.text = castParts.joinToString("\n")
        cast.visibility = if (castParts.isEmpty()) View.GONE else View.VISIBLE

        AlertDialog.Builder(requireContext())
            .setTitle(movie.name)
            .setView(dialogView)
            .setPositiveButton(getString(R.string.movies_play)) { _, _ -> playMovie(movie) }
            .setNegativeButton(getString(R.string.common_close), null)
            .show()
    }

    private fun playMovie(movie: XtreamMovie) {
        val server = moviesViewModel.getServer() ?: return
        val url = XtreamAPI.getMovieUrl(server, movie.streamId, movie.containerExtension)

        requireApp().container.historyManager.addOrUpdateHistory(
            HistoryItem(
                type = "movie",
                id = movie.streamId,
                name = movie.name,
                timestamp = System.currentTimeMillis(),
                icon = movie.streamIcon,
                containerExtension = movie.containerExtension
            )
        )

        (activity as? MainActivity)?.playExternalMedia(url, movie.name, "movie", movie.streamId)
    }

    private fun requireApp(): App = requireActivity().application as App

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}