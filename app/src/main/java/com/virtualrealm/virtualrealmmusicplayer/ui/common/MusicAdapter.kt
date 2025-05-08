// ui/common/MusicAdapter.kt
package com.virtualrealm.virtualrealmmusicplayer.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.virtualrealm.virtualrealmmusicplayer.R
import com.virtualrealm.virtualrealmmusicplayer.databinding.ItemMusicBinding
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Music

class MusicAdapter(
    private val onItemClick: (Music) -> Unit,
    private val onFavoriteClick: (Music) -> Unit
) : ListAdapter<Music, MusicAdapter.MusicViewHolder>(MusicDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicViewHolder {
        val binding = ItemMusicBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MusicViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MusicViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MusicViewHolder(
        private val binding: ItemMusicBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }

            binding.btnFavorite.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onFavoriteClick(getItem(position))
                }
            }
        }

        fun bind(music: Music) {
            binding.tvTitle.text = music.title
            binding.tvArtist.text = music.artists

            when (music) {
                is Music.SpotifyTrack -> {
                    binding.tvSource.text = binding.root.context.getString(R.string.spotify)
                    binding.tvSource.setBackgroundResource(R.drawable.bg_spotify_tag)
                }
                is Music.YoutubeVideo -> {
                    binding.tvSource.text = binding.root.context.getString(R.string.youtube)
                    binding.tvSource.setBackgroundResource(R.drawable.bg_youtube_tag)
                }
            }

            Glide.with(binding.ivThumbnail)
                .load(music.thumbnailUrl)
                .placeholder(R.drawable.placeholder_album)
                .error(R.drawable.placeholder_album)
                .into(binding.ivThumbnail)
        }
    }

    class MusicDiffCallback : DiffUtil.ItemCallback<Music>() {
        override fun areItemsTheSame(oldItem: Music, newItem: Music): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Music, newItem: Music): Boolean {
            return oldItem == newItem
        }
    }
}