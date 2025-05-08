// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/data/remote/dto/YouTubeVideoDetailsDto.kt

package com.virtualrealm.virtualrealmmusicplayer.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Music

data class YouTubeVideoDetailsResponse(
    @SerializedName("items") val items: List<YouTubeVideoDetailsDto> = emptyList()
)

data class YouTubeVideoDetailsDto(
    @SerializedName("id") val id: String,
    @SerializedName("snippet") val snippet: YouTubeSnippetDto,
    @SerializedName("contentDetails") val contentDetails: YouTubeContentDetailsDto? = null
) {
    fun toYouTubeVideo(): Music.YoutubeVideo {
        return Music.YoutubeVideo(
            id = id,
            title = snippet.title,
            artists = snippet.channelTitle,
            thumbnailUrl = snippet.thumbnails.high?.url
                ?: snippet.thumbnails.medium?.url
                ?: snippet.thumbnails.default?.url
                ?: "",
            channelTitle = snippet.channelTitle
        )
    }
}

data class YouTubeContentDetailsDto(
    @SerializedName("duration") val duration: String,
    @SerializedName("dimension") val dimension: String,
    @SerializedName("definition") val definition: String,
    @SerializedName("caption") val caption: String,
    @SerializedName("licensedContent") val licensedContent: Boolean
)