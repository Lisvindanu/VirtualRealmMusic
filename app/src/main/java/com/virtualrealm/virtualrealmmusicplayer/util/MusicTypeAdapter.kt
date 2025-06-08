// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/util/MusicTypeAdapter.kt

package com.virtualrealm.virtualrealmmusicplayer.util

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Music
import java.lang.reflect.Type

class MusicTypeAdapter : JsonDeserializer<Music>, JsonSerializer<Music> {

    companion object {
        private const val TYPE_FIELD = "musicType"
        private const val TYPE_SPOTIFY = "spotify"
        private const val TYPE_YOUTUBE = "youtube"
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Music {
        val jsonObject = json.asJsonObject

        // First, try to get the musicType field
        val typeElement = jsonObject.get(TYPE_FIELD)
        val musicType = typeElement?.asString

        // If musicType is not present, try to infer from other fields
        val inferredType = musicType ?: inferTypeFromObject(jsonObject)

        return when (inferredType) {
            TYPE_SPOTIFY -> {
                // Deserialize as Spotify track
                Music.SpotifyTrack(
                    id = jsonObject.get("id").asString,
                    title = jsonObject.get("title").asString,
                    artists = jsonObject.get("artists").asString,
                    thumbnailUrl = jsonObject.get("thumbnailUrl")?.asString ?: "",
                    albumName = jsonObject.get("albumName")?.asString ?: "",
                    uri = jsonObject.get("uri")?.asString ?: "spotify:track:${jsonObject.get("id").asString}",
                    durationMs = jsonObject.get("durationMs")?.asLong ?: 0L
                )
            }
            TYPE_YOUTUBE -> {
                // Deserialize as YouTube video
                Music.YoutubeVideo(
                    id = jsonObject.get("id").asString,
                    title = jsonObject.get("title").asString,
                    artists = jsonObject.get("artists").asString,
                    thumbnailUrl = jsonObject.get("thumbnailUrl")?.asString ?: "",
                    channelTitle = jsonObject.get("channelTitle")?.asString ?: jsonObject.get("artists")?.asString ?: ""
                )
            }
            else -> {
                // Default fallback - try to determine by presence of specific fields
                if (jsonObject.has("albumName") || jsonObject.has("uri") || jsonObject.has("durationMs")) {
                    // Looks like Spotify
                    Music.SpotifyTrack(
                        id = jsonObject.get("id").asString,
                        title = jsonObject.get("title").asString,
                        artists = jsonObject.get("artists").asString,
                        thumbnailUrl = jsonObject.get("thumbnailUrl")?.asString ?: "",
                        albumName = jsonObject.get("albumName")?.asString ?: "",
                        uri = jsonObject.get("uri")?.asString ?: "spotify:track:${jsonObject.get("id").asString}",
                        durationMs = jsonObject.get("durationMs")?.asLong ?: 0L
                    )
                } else {
                    // Default to YouTube
                    Music.YoutubeVideo(
                        id = jsonObject.get("id").asString,
                        title = jsonObject.get("title").asString,
                        artists = jsonObject.get("artists").asString,
                        thumbnailUrl = jsonObject.get("thumbnailUrl")?.asString ?: "",
                        channelTitle = jsonObject.get("channelTitle")?.asString ?: jsonObject.get("artists")?.asString ?: ""
                    )
                }
            }
        }
    }

    override fun serialize(src: Music, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()

        // Add common fields
        jsonObject.addProperty("id", src.id)
        jsonObject.addProperty("title", src.title)
        jsonObject.addProperty("artists", src.artists)
        jsonObject.addProperty("thumbnailUrl", src.thumbnailUrl)

        // Add type-specific fields and type identifier
        when (src) {
            is Music.SpotifyTrack -> {
                jsonObject.addProperty(TYPE_FIELD, TYPE_SPOTIFY)
                jsonObject.addProperty("albumName", src.albumName)
                jsonObject.addProperty("uri", src.uri)
                jsonObject.addProperty("durationMs", src.durationMs)
            }
            is Music.YoutubeVideo -> {
                jsonObject.addProperty(TYPE_FIELD, TYPE_YOUTUBE)
                jsonObject.addProperty("channelTitle", src.channelTitle)
            }
        }

        return jsonObject
    }

    private fun inferTypeFromObject(jsonObject: JsonObject): String {
        return when {
            // Check for Spotify-specific fields
            jsonObject.has("albumName") ||
                    jsonObject.has("uri") ||
                    jsonObject.has("durationMs") -> TYPE_SPOTIFY

            // Check for YouTube-specific fields
            jsonObject.has("channelTitle") -> TYPE_YOUTUBE

            // Check URI patterns
            jsonObject.get("uri")?.asString?.startsWith("spotify:") == true -> TYPE_SPOTIFY

            // Default to YouTube if we can't determine
            else -> TYPE_YOUTUBE
        }
    }
}