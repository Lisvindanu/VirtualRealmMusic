// data/remote/dto/YouTubeDto.kt
package com.virtualrealm.virtualrealmmusicplayer.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Music

data class YouTubeSearchResponse(
    @SerializedName("items") val items: List<YouTubeVideoDto>
)

data class YouTubeVideoDto(
    @SerializedName("id") val id: YouTubeVideoIdDto,
    @SerializedName("snippet") val snippet: YouTubeSnippetDto
) {
    fun toYouTubeVideo(): Music.YoutubeVideo? {
        val videoId = id.videoId ?: return null
        return Music.YoutubeVideo(
            id = videoId,
            title = snippet.title,
            artists = snippet.channelTitle,
            thumbnailUrl = snippet.thumbnails.high?.url ?: snippet.thumbnails.default?.url ?: "",
            channelTitle = snippet.channelTitle
        )
    }
}

data class YouTubeVideoIdDto(
    @SerializedName("videoId") val videoId: String?
)

data class YouTubeSnippetDto(
    @SerializedName("title") val title: String,
    @SerializedName("channelTitle") val channelTitle: String,
    @SerializedName("thumbnails") val thumbnails: YouTubeThumbnailsDto
)

data class YouTubeThumbnailsDto(
    @SerializedName("default") val default: YouTubeThumbnailDto?,
    @SerializedName("medium") val medium: YouTubeThumbnailDto?,
    @SerializedName("high") val high: YouTubeThumbnailDto?
)

data class YouTubeThumbnailDto(
    @SerializedName("url") val url: String,
    @SerializedName("width") val width: Int,
    @SerializedName("height") val height: Int
)