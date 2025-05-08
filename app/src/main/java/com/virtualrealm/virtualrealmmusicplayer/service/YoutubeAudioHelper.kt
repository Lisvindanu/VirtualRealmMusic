// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/service/YoutubeAudioHelper.kt

package com.virtualrealm.virtualrealmmusicplayer.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONObject

@Singleton
class YoutubeAudioHelper @Inject constructor() {

    /**
     * Mendapatkan URL audio dari video YouTube
     * Metode ini menggunakan Invidious API yang menyediakan informasi audio saja
     */
    suspend fun getAudioUrlFromYoutube(videoId: String): String? = withContext(Dispatchers.IO) {
        try {
            // Opsi 1: Gunakan Invidious API (contoh proxy publik)
            val apiUrl = "https://invidious.sethforprivacy.com/api/v1/videos/$videoId"

            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

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

                // Cari format audio (biasanya MIME type audio/mp4 atau audio/webm)
                for (i in 0 until audioFormats.length()) {
                    val format = audioFormats.getJSONObject(i)
                    val mimeType = format.getString("type")
                    if (mimeType.startsWith("audio/")) {
                        // Dapatkan URL audio berkualitas tinggi
                        val audioUrl = format.getString("url")
                        Log.d("YoutubeAudioHelper", "Found audio URL: $audioUrl")
                        return@withContext audioUrl
                    }
                }
            }

            // Opsi 2: Jika API tidak berfungsi, gunakan URL direct format audio 140 (AAC)
            "https://invidious.sethforprivacy.com/latest_version?id=$videoId&itag=140&local=true"
        } catch (e: Exception) {
            Log.e("YoutubeAudioHelper", "Error extracting audio URL: ${e.message}", e)
            null
        }
    }
}