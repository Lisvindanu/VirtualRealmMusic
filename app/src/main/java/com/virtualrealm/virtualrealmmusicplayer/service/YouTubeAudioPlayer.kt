// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/service/YouTubeAudioPlayer.kt

package com.virtualrealm.virtualrealmmusicplayer.service

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YouTubeAudioPlayer @Inject constructor(
    private val context: Context
) {
    private var webView: WebView? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition

    // Untuk callback saat audio dimulai/berakhir
    var onPrepared: (() -> Unit)? = null
    var onCompletion: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    // JavaScript interface untuk komunikasi dari WebView ke Kotlin
    inner class AudioPlayerJSInterface {
        @JavascriptInterface
        fun onDurationChanged(durationMs: Long) {
            Log.d("YouTubeAudio", "Duration changed: $durationMs ms")
            _duration.value = durationMs
        }

        @JavascriptInterface
        fun onPositionChanged(positionMs: Long) {
            _currentPosition.value = positionMs
        }

        @JavascriptInterface
        fun onCompletion() {
            mainHandler.post {
                onCompletion?.invoke()
            }
        }

        @JavascriptInterface
        fun onError(message: String) {
            mainHandler.post {
                onError?.invoke(message)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun initialize() {
        if (webView == null) {
            mainHandler.post {
                try {
                    webView = WebView(context).apply {
                        // Buat WebView tidak terlihat
                        visibility = View.GONE
                        // Gunakan ViewGroup.LayoutParams sebagai gantinya
                        layoutParams = ViewGroup.LayoutParams(1, 1)

                        // Konfigurasi WebView untuk audio
                        settings.javaScriptEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.domStorageEnabled = true
                        settings.cacheMode = WebSettings.LOAD_DEFAULT

                        webChromeClient = WebChromeClient()
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)

                                if (url?.contains("youtube.com") == true ||
                                    url?.contains("youtu.be") == true) {
                                    // Inject JS untuk menyembunyikan video dan hanya mendengarkan audio
                                    injectAudioOnlyJS()
                                }
                            }
                        }

                        // Tambahkan JavaScript interface
                        addJavascriptInterface(AudioPlayerJSInterface(), "AndroidAudioPlayer")
                    }
                    Log.d("YouTubeAudioPlayer", "WebView initialized")
                } catch (e: Exception) {
                    Log.e("YouTubeAudioPlayer", "Error initializing WebView: ${e.message}")
                    onError?.invoke("Failed to initialize audio player: ${e.message}")
                }
            }
        }
    }

    fun loadAndPlayYouTube(videoId: String) {
        if (webView == null) {
            initialize()
        }

        _isLoading.value = true
        _duration.value = 0 // Reset duration

        mainHandler.post {
            try {
                // Direct YouTube embed dengan autoplay dan kontrol API
                val embedUrl = "https://www.youtube.com/embed/$videoId?autoplay=1&enablejsapi=1&controls=0&disablekb=1"
                Log.d("YouTubeAudioPlayer", "Loading URL: $embedUrl")
                webView?.loadUrl(embedUrl)

                // Delay untuk memastikan video telah dimuat
                mainHandler.postDelayed({
                    _isPlaying.value = true
                    _isLoading.value = false
                    onPrepared?.invoke()

                    // Setup update posisi berkala
                    startPositionUpdates()

                    // Requests video duration after a delay
                    getDuration()
                }, 3000) // Wait longer for loading
            } catch (e: Exception) {
                Log.e("YouTubeAudioPlayer", "Error loading YouTube video: ${e.message}")
                _isLoading.value = false
                onError?.invoke("Failed to load audio: ${e.message}")
            }
        }
    }

    private fun injectAudioOnlyJS() {
        Log.d("YouTubeAudioPlayer", "Injecting audio-only JavaScript")

        val js = """
            // Sembunyikan video, hanya putar audio
            var videos = document.getElementsByTagName('video');
            for(var i=0; i<videos.length; i++) {
                videos[i].style.display = 'none';
                
                // Set callback for video end
                videos[i].addEventListener('ended', function() {
                    AndroidAudioPlayer.onCompletion();
                });
                
                // Get and set duration
                if (videos[i].duration) {
                    let durationMs = Math.round(videos[i].duration * 1000);
                    AndroidAudioPlayer.onDurationChanged(durationMs);
                    console.log("Duration set to: " + durationMs + "ms");
                }
            }
            
            // Sembunyikan elemen UI
            var elements = document.querySelectorAll('.ytp-chrome-bottom, .ytp-chrome-top');
            for(var i=0; i<elements.length; i++) {
                elements[i].style.display = 'none';
            }
            
            // Hide everything except the video player
            document.body.style.backgroundColor = 'black';
            var nonPlayerElements = document.querySelectorAll('body > *:not(#player)');
            for(var i=0; i<nonPlayerElements.length; i++) {
                nonPlayerElements[i].style.display = 'none';
            }
            
            console.log("YouTube audio mode activated");
        """

        webView?.evaluateJavascript(js, null)
    }

    private fun getDuration() {
        val js = """
            var videos = document.getElementsByTagName('video');
            if(videos.length > 0 && videos[0].duration) {
                let durationMs = Math.round(videos[0].duration * 1000);
                AndroidAudioPlayer.onDurationChanged(durationMs);
                durationMs;
            } else {
                0;
            }
        """

        webView?.evaluateJavascript(js) { result ->
            try {
                val duration = result.toLongOrNull() ?: 0
                if (duration > 0) {
                    _duration.value = duration
                    Log.d("YouTubeAudioPlayer", "Got duration: $duration ms")
                } else {
                    // Try again after delay if duration is not available
                    mainHandler.postDelayed({ getDuration() }, 1000)
                }
            } catch (e: Exception) {
                Log.e("YouTubeAudioPlayer", "Error getting duration: ${e.message}")
            }
        }
    }

    private fun startPositionUpdates() {
        // Update posisi setiap 1 detik
        val updateRunnable = object : Runnable {
            override fun run() {
                if (_isPlaying.value) {
                    updateCurrentPosition()
                    mainHandler.postDelayed(this, 1000)
                }
            }
        }

        mainHandler.post(updateRunnable)
    }

    private fun updateCurrentPosition() {
        val js = """
            var videos = document.getElementsByTagName('video');
            if(videos.length > 0) {
                let positionMs = Math.round(videos[0].currentTime * 1000);
                AndroidAudioPlayer.onPositionChanged(positionMs);
                positionMs;
            } else {
                0;
            }
        """

        webView?.evaluateJavascript(js) { result ->
            try {
                val position = result.toLongOrNull() ?: 0
                _currentPosition.value = position

                // Also check duration if not set yet
                if (_duration.value <= 0) {
                    getDuration()
                }
            } catch (e: Exception) {
                Log.e("YouTubeAudioPlayer", "Error updating position: ${e.message}")
            }
        }
    }

    fun play() {
        mainHandler.post {
            val js = """
                var videos = document.getElementsByTagName('video');
                for(var i=0; i<videos.length; i++) {
                    videos[i].play();
                }
                true;
            """
            webView?.evaluateJavascript(js, null)
            _isPlaying.value = true
        }
    }

    fun pause() {
        mainHandler.post {
            val js = """
                var videos = document.getElementsByTagName('video');
                for(var i=0; i<videos.length; i++) {
                    videos[i].pause();
                }
                true;
            """
            webView?.evaluateJavascript(js, null)
            _isPlaying.value = false
        }
    }

    fun stop() {
        pause()
        mainHandler.post {
            webView?.loadUrl("about:blank")
            _currentPosition.value = 0
            _duration.value = 0
        }
    }

    fun seekTo(position: Long) {
        mainHandler.post {
            val js = """
                var videos = document.getElementsByTagName('video');
                for(var i=0; i<videos.length; i++) {
                    videos[i].currentTime = ${position / 1000.0};
                }
                true;
            """
            Log.d("YouTubeAudioPlayer", "Seeking to $position ms (${position / 1000.0} seconds)")
            webView?.evaluateJavascript(js, null)
        }
    }

    fun release() {
        mainHandler.post {
            webView?.destroy()
            webView = null
        }
    }
}