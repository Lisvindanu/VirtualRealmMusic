
// domain/model/Music.kt
package com.virtualrealm.virtualrealmmusicplayer.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class Music : Parcelable {
    abstract val id: String
    abstract val title: String
    abstract val artists: String
    abstract val thumbnailUrl: String

    @Parcelize
    data class SpotifyTrack(
        override val id: String,
        override val title: String,
        override val artists: String,
        override val thumbnailUrl: String,
        val albumName: String,
        val uri: String,
        val durationMs: Long
    ) : Music()

    @Parcelize
    data class YoutubeVideo(
        override val id: String,
        override val title: String,
        override val artists: String,
        override val thumbnailUrl: String,
        val channelTitle: String
    ) : Music()
}
