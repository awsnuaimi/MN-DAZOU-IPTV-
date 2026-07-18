package com.dazou.iptvplayer.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.dazou.iptvplayer.App
import com.dazou.iptvplayer.MainActivity
import com.dazou.iptvplayer.adapter.CategoryAdapter
import com.dazou.iptvplayer.adapter.MovieAdapter
import com.dazou.iptvplayer.api.XtreamAPI
import com.dazou.iptvplayer.databinding.FragmentMoviesBinding
import com.dazou.iptvplayer.model.XtreamCategory
import com.dazou.iptvplayer.model.XtreamMovie
import com.dazou.iptvplayer.viewmodel.MoviesViewModel
import com.dazou.iptvplayer.viewmodel.ViewModelFactory

class MoviesFragment : Fragment() {

    private var _binding: FragmentMoviesBinding? = null
    private val binding get() = _binding!!

    private lateinit var moviesViewModel: MoviesViewModel

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
                Toast.makeText(requireContext(), "لا توجد مجموعات أفلام – تأكد من الحساب", Toast.LENGTH_LONG).show()
            }
            binding.rvMovieCategories.adapter = CategoryAdapter(categories) { category ->
                openCategory(category)
            }
        }

        moviesViewModel.movies.observe(viewLifecycleOwner) { movies ->
            binding.tvMoviesStatus.text = if (movies.isEmpty())
                "لا توجد أفلام بهذه المجموعة"
            else
                "${movies.size} فيلم"
            binding.rvMovies.adapter = MovieAdapter(movies) { movie -> playMovie(movie) }
        }

        moviesViewModel.loadCategories()
    }

    private fun openCategory(category: XtreamCategory) {
        binding.tvMoviesStatus.text = "جاري التحميل..."
        moviesViewModel.loadMovies(category.categoryId)
    }

    private fun playMovie(movie: XtreamMovie) {
        val server = moviesViewModel.getServer() ?: return
        val url = XtreamAPI.getMovieUrl(server, movie.streamId, movie.containerExtension)
        (activity as? MainActivity)?.playExternalMedia(url, movie.name, "movie")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}