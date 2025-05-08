// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/service/YoutubeAudioHelper.kt

package com.virtualrealm.virtualrealmmusicplayer.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YoutubeAudioHelper @Inject constructor() {

    /**
     * Try multiple methods to get audio URL from a YouTube video
     */
    suspend fun getAudioUrlFromYoutube(videoId: String): String? = withContext(Dispatchers.IO) {
        try {
            // Try method 1: Invidious API
            val audioUrl = getAudioUrlFromInvidious(videoId)
            if (!audioUrl.isNullOrEmpty()) {
                return@withContext audioUrl
            }

            // Try method 2: Direct media URL (only works for some videos)
            val directUrl = "https://rr2---sn-5goeen7d.googlevideo.com/videoplayback?id=$videoId"
            if (isUrlValid(directUrl)) {
                return@withContext directUrl
            }

            // All methods failed
            null
        } catch (e: Exception) {
            Log.e("YoutubeAudioHelper", "Error extracting audio URL: ${e.message}", e)
            null
        }
    }

    private suspend fun getAudioUrlFromInvidious(videoId: String): String? = withContext(Dispatchers.IO) {
        try {
            // Try different Invidious instances
            val instances = listOf(
                "invidious.sethforprivacy.com",
                "vid.puffyan.us",
                "yewtu.be"
            )

            for (instance in instances) {
                try {
                    val apiUrl = "https://$instance/api/v1/videos/$videoId"

                    val url = URL(apiUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000

                    val responseCode = connection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val reader = BufferedReader(InputStreamReader(connection.inputStream))
                        val response = StringBuilder()
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            response.append(line)
                        }
                        reader.close()

                        val jsonResponse = JSONObject(response.toString())
                        val audioFormats = jsonResponse.getJSONArray("adaptiveFormats")

                        // Look for audio/mp4 format (usually the best audio quality)
                        for (i in 0 until audioFormats.length()) {
                            val format = audioFormats.getJSONObject(i)
                            val mimeType = format.getString("type")
                            if (mimeType.startsWith("audio/")) {
                                val audioUrl = format.getString("url")
                                Log.d("YoutubeAudioHelper", "Found audio URL: $audioUrl")
                                return@withContext audioUrl
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("YoutubeAudioHelper", "Error with instance $instance: ${e.message}")
                    // Continue to the next instance
                }
            }

            // If API approach failed, try direct format
            val url = "https://yewtu.be/latest_version?id=$videoId&itag=140&local=true"
            if (isUrlValid(url)) {
                return@withContext url
            }

            null
        } catch (e: Exception) {
            Log.e("YoutubeAudioHelper", "Error extracting audio URL: ${e.message}", e)
            null
        }
    }

    private fun isUrlValid(urlString: String): Boolean {
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            val responseCode = connection.responseCode
            responseCode == HttpURLConnection.HTTP_OK
        } catch (e: Exception) {
            false
        }
    }
}