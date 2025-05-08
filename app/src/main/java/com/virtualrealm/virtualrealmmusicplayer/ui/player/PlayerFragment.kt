// ui/player/PlayerFragment.kt
package com.virtualrealm.virtualrealmmusicplayer.ui.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.virtualrealm.virtualrealmmusicplayer.R
import com.virtualrealm.virtualrealmmusicplayer.databinding.FragmentPlayerBinding
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Music
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PlayerFragment : Fragment() {

    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PlayerViewModel by viewModels()
    private val args: PlayerFragmentArgs by navArgs()

    private var youTubePlayer: YouTubePlayer? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMusicDetails()
        setupPlayerView()
        setupListeners()
        observeFavoriteStatus()
    }

    private fun setupMusicDetails() {
        val music = args.music

        binding.tvTitle.text = music.title
        binding.tvArtist.text = music.artists

        Glide.with(this)
            .load(music.thumbnailUrl)
            .placeholder(R.drawable.placeholder_album)
            .error(R.drawable.placeholder_album)
            .into(binding.ivThumbnail)

        when (music) {
            is Music.SpotifyTrack -> {
                binding.tvAlbum.text = music.albumName
                binding.tvAlbum.visibility = View.VISIBLE

                binding.youtubePlayerView.visibility = View.GONE
                binding.spotifyPlayerView.visibility = View.VISIBLE
            }
            is Music.YoutubeVideo -> {
                binding.tvAlbum.visibility = View.GONE

                binding.youtubePlayerView.visibility = View.VISIBLE
                binding.spotifyPlayerView.visibility = View.GONE
            }
        }
    }

    private fun setupPlayerView() {
        val music = args.music

        when (music) {
            is Music.SpotifyTrack -> {
                // We're using a WebView to display the Spotify embed player
                val embedUrl = "https://open.spotify.com/embed/track/${music.id}"
                binding.spotifyPlayerView.settings.javaScriptEnabled = true
                binding.spotifyPlayerView.loadUrl(embedUrl)
            }
            is Music.YoutubeVideo -> {
                // Configure YouTube player
                lifecycle.addObserver(binding.youtubePlayerView)

                binding.youtubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                    override fun onReady(player: YouTubePlayer) {
                        youTubePlayer = player
                        player.loadVideo(music.id, 0f)
                    }
                })
            }
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnToggleFavorite.setOnClickListener {
            viewModel.toggleFavorite(args.music)
        }
    }

    private fun observeFavoriteStatus() {
        viewModel.checkFavoriteStatus(args.music.id)

        viewModel.isFavorite.observe(viewLifecycleOwner) { isFavorite ->
            binding.btnToggleFavorite.setImageResource(
                if (isFavorite) R.drawable.ic_favorite
                else R.drawable.ic_favorite_border
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        youTubePlayer = null
        _binding = null
    }
}
