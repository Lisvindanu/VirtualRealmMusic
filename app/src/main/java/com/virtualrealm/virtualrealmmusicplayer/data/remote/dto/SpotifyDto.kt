// data/remote/dto/SpotifyDto.kt
package com.virtualrealm.virtualrealmmusicplayer.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Music

data class SpotifyTokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("expires_in") val expiresIn: Int,
    @SerializedName("refresh_token") val refreshToken: String? = null,
    @SerializedName("scope") val scope: String
)

data class SpotifySearchResponse(
    @SerializedName("tracks") val tracks: SpotifyTrackResponse
)

data class SpotifyTrackResponse(
    @SerializedName("items") val items: List<SpotifyTrackDto>
)

data class SpotifyTrackDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("artists") val artists: List<SpotifyArtistDto>,
    @SerializedName("album") val album: SpotifyAlbumDto,
    @SerializedName("duration_ms") val durationMs: Long
) {
    fun toSpotifyTrack(): Music.SpotifyTrack {
        return Music.SpotifyTrack(
            id = id,
            title = name,
            artists = artists.joinToString(", ") { it.name },
            thumbnailUrl = album.images.firstOrNull()?.url ?: "",
            albumName = album.name,
            uri = "spotify:track:$id",
            durationMs = durationMs
        )
    }
}

data class SpotifyArtistDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String
)

data class SpotifyAlbumDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("images") val images: List<SpotifyImageDto>
)

data class SpotifyImageDto(
    @SerializedName("url") val url: String,
    @SerializedName("height") val height: Int?,
    @SerializedName("width") val width: Int?
)
