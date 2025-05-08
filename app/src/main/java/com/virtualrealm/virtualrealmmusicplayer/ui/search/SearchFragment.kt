// ui/search/SearchFragment.kt
package com.virtualrealm.virtualrealmmusicplayer.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.virtualrealm.virtualrealmmusicplayer.databinding.FragmentSearchBinding
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Music
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Resource
import com.virtualrealm.virtualrealmmusicplayer.ui.common.MusicAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SearchViewModel by viewModels()
    private lateinit var musicAdapter: MusicAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchView()
        setupSourceSelection()
        observeSearchResults()
        observeAuthState()
    }

    private fun setupRecyclerView() {
        musicAdapter = MusicAdapter(
            onItemClick = { music ->
                navigateToPlayer(music)
            },
            onFavoriteClick = { music ->
                viewModel.toggleFavorite(music)
            }
        )

        binding.recyclerViewResults.apply {
            adapter = musicAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    viewModel.searchMusic(query)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }
        })
    }

    private fun setupSourceSelection() {
        binding.radioGroupSource.setOnCheckedChangeListener { _, _ ->
            val query = binding.searchView.query.toString()
            if (query.isNotBlank()) {
                viewModel.searchMusic(query)
            }
        }

        binding.radioButtonYoutube.isChecked = true

        // Update source selection based on auth state
        viewModel.authState.observe(viewLifecycleOwner) { state ->
            binding.radioButtonSpotify.isEnabled = state.isAuthenticated

            if (!state.isAuthenticated) {
                binding.radioButtonYoutube.isChecked = true
            }
        }
    }

    private fun observeSearchResults() {
        viewModel.searchResults.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.emptyStateView.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    musicAdapter.submitList(result.data)

                    if (result.data.isEmpty()) {
                        binding.emptyStateView.visibility = View.VISIBLE
                        binding.recyclerViewResults.visibility = View.GONE
                    } else {
                        binding.emptyStateView.visibility = View.GONE
                        binding.recyclerViewResults.visibility = View.VISIBLE
                    }
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.emptyStateView.visibility = View.VISIBLE
                    binding.recyclerViewResults.visibility = View.GONE

                    Toast.makeText(
                        requireContext(),
                        "Error: ${result.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun observeAuthState() {
        viewModel.authState.observe(viewLifecycleOwner) { state ->
            binding.spotifyStatusView.visibility = if (state.isAuthenticated) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }
    }

    private fun navigateToPlayer(music: Music) {
        val action = SearchFragmentDirections.actionSearchFragmentToPlayerFragment(music)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}