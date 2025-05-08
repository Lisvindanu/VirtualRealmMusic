// ui/home/HomeFragment.kt
package com.virtualrealm.virtualrealmmusicplayer.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.virtualrealm.virtualrealmmusicplayer.R
import com.virtualrealm.virtualrealmmusicplayer.databinding.FragmentHomeBinding
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Music
import com.virtualrealm.virtualrealmmusicplayer.ui.common.MusicAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var favoriteAdapter: MusicAdapter

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

        setupRecyclerView()
        setupListeners()
        observeFavorites()
        observeAuthState()
    }

    private fun setupRecyclerView() {
        favoriteAdapter = MusicAdapter(
            onItemClick = { music ->
                navigateToPlayer(music)
            },
            onFavoriteClick = { music ->
                viewModel.toggleFavorite(music)
            }
        )

        binding.recyclerViewFavorites.apply {
            adapter = favoriteAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun setupListeners() {
        binding.btnSearch.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_searchFragment)
        }

        binding.btnLogout.setOnClickListener {
            viewModel.logout()
        }
    }

    private fun observeFavorites() {
        viewModel.favorites.observe(viewLifecycleOwner) { favorites ->
            favoriteAdapter.submitList(favorites)

            if (favorites.isEmpty()) {
                binding.emptyStateView.visibility = View.VISIBLE
                binding.recyclerViewFavorites.visibility = View.GONE
            } else {
                binding.emptyStateView.visibility = View.GONE
                binding.recyclerViewFavorites.visibility = View.VISIBLE
            }
        }
    }

    private fun observeAuthState() {
        viewModel.authState.observe(viewLifecycleOwner) { state ->
            binding.tvAuthStatus.text = if (state.isAuthenticated) {
                getString(R.string.spotify_connected)
            } else {
                getString(R.string.spotify_not_connected)
            }

            binding.btnConnectSpotify.visibility = if (state.isAuthenticated) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }

        binding.btnConnectSpotify.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_loginFragment)
        }
    }

    private fun navigateToPlayer(music: Music) {
        val action = when (music) {
            is Music.SpotifyTrack ->
                HomeFragmentDirections.actionHomeFragmentToPlayerFragment(music)
            is Music.YoutubeVideo ->
                HomeFragmentDirections.actionHomeFragmentToPlayerFragment(music)
        }
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

