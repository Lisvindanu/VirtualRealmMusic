// data/local/entity/MusicEntity.kt
package com.virtualrealm.virtualrealmmusicplayer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Music

@Entity(tableName = "music")
data class MusicEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artists: String,
    val thumbnailUrl: String,
    val type: String, // "spotify" or "youtube"
    val additionalInfo: String, // JSON string with additional type-specific info
    val isFavorite: Boolean = false
) {
    companion object {
        const val TYPE_SPOTIFY = "spotify"
        const val TYPE_YOUTUBE = "youtube"

        fun fromSpotifyTrack(track: Music.SpotifyTrack, isFavorite: Boolean = false): MusicEntity {
            val spotifyAdditionalInfo = SpotifyAdditionalInfo(
                albumName = track.albumName,
                uri = track.uri,
                durationMs = track.durationMs
            )

            return MusicEntity(
                id = track.id,
                title = track.title,
                artists = track.artists,
                thumbnailUrl = track.thumbnailUrl,
                type = TYPE_SPOTIFY,
                additionalInfo = Gson().toJson(spotifyAdditionalInfo),
                isFavorite = isFavorite
            )
        }

        fun fromYoutubeVideo(video: Music.YoutubeVideo, isFavorite: Boolean = false): MusicEntity {
            val youtubeAdditionalInfo = YoutubeAdditionalInfo(
                channelTitle = video.channelTitle
            )

            return MusicEntity(
                id = video.id,
                title = video.title,
                artists = video.artists,
                thumbnailUrl = video.thumbnailUrl,
                type = TYPE_YOUTUBE,
                additionalInfo = Gson().toJson(youtubeAdditionalInfo),
                isFavorite = isFavorite
            )
        }
    }

    fun toMusic(): Music {
        return when (type) {
            TYPE_SPOTIFY -> {
                val additionalInfo = Gson().fromJson(additionalInfo, SpotifyAdditionalInfo::class.java)
                Music.SpotifyTrack(
                    id = id,
                    title = title,
                    artists = artists,
                    thumbnailUrl = thumbnailUrl,
                    albumName = additionalInfo.albumName,
                    uri = additionalInfo.uri,
                    durationMs = additionalInfo.durationMs
                )
            }
            TYPE_YOUTUBE -> {
                val additionalInfo = Gson().fromJson(additionalInfo, YoutubeAdditionalInfo::class.java)
                Music.YoutubeVideo(
                    id = id,
                    title = title,
                    artists = artists,
                    thumbnailUrl = thumbnailUrl,
                    channelTitle = additionalInfo.channelTitle
                )
            }
            else -> throw IllegalArgumentException("Unknown music type: $type")
        }
    }
}

data class SpotifyAdditionalInfo(
    @SerializedName("albumName") val albumName: String,
    @SerializedName("uri") val uri: String,
    @SerializedName("durationMs") val durationMs: Long
)

data class YoutubeAdditionalInfo(
    @SerializedName("channelTitle") val channelTitle: String
)