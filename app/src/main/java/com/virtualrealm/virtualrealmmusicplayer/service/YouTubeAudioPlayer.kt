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

    var onPrepared: (() -> Unit)? = null
    var onCompletion: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null

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
                        visibility = View.GONE
                        layoutParams = ViewGroup.LayoutParams(1, 1)

                        settings.javaScriptEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.domStorageEnabled = true
                        settings.cacheMode = WebSettings.LOAD_NO_CACHE // Non-aktifkan cache

                        webChromeClient = WebChromeClient()
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                if (url?.contains("youtube.com/embed") == true) {
                                    injectControlJS()
                                }
                            }
                        }
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
        _duration.value = 0

        mainHandler.post {
            try {
                // Gunakan URL embed yang benar dengan parameter untuk autoplay dan kontrol minimal
                val embedUrl = "https://www.youtube.com/embed/$videoId?autoplay=1&controls=0&fs=0&iv_load_policy=3&showinfo=0&rel=0&cc_load_policy=1&start=0&modestbranding=1"
                Log.d("YouTubeAudioPlayer", "Loading URL: $embedUrl")
                webView?.loadUrl(embedUrl)
            } catch (e: Exception) {
                Log.e("YouTubeAudioPlayer", "Error loading YouTube video: ${e.message}")
                _isLoading.value = false
                onError?.invoke("Failed to load audio: ${e.message}")
            }
        }
    }

    private fun injectControlJS() {
        Log.d("YouTubeAudioPlayer", "Injecting control JavaScript")
        val js = """
            var video = document.getElementsByTagName('video')[0];
            if (video) {
                video.style.display = 'none'; // Sembunyikan video
                
                video.addEventListener('ended', function() { AndroidAudioPlayer.onCompletion(); });
                video.addEventListener('play', function() { AndroidAudioPlayer.onPrepared(); });
                
                // Laporkan durasi saat tersedia
                if (video.duration) {
                    AndroidAudioPlayer.onDurationChanged(Math.round(video.duration * 1000));
                } else {
                    video.addEventListener('durationchange', function() {
                        AndroidAudioPlayer.onDurationChanged(Math.round(video.duration * 1000));
                    });
                }
                
                // Laporkan posisi secara berkala
                setInterval(function() {
                    if (!video.paused) {
                         AndroidAudioPlayer.onPositionChanged(Math.round(video.currentTime * 1000));
                    }
                }, 1000);
            }
        """
        webView?.evaluateJavascript(js, null)
    }

    fun play() {
        mainHandler.post {
            webView?.evaluateJavascript("document.getElementsByTagName('video')[0].play();", null)
            _isPlaying.value = true
        }
    }

    fun pause() {
        mainHandler.post {
            webView?.evaluateJavascript("document.getElementsByTagName('video')[0].pause();", null)
            _isPlaying.value = false
        }
    }

    fun stop() {
        mainHandler.post {
            webView?.loadUrl("about:blank")
            _currentPosition.value = 0
            _duration.value = 0
            _isPlaying.value = false
        }
    }

    fun seekTo(position: Long) {
        mainHandler.post {
            webView?.evaluateJavascript("document.getElementsByTagName('video')[0].currentTime = ${position / 1000.0};", null)
        }
    }

    fun release() {
        mainHandler.post {
            webView?.destroy()
            webView = null
        }
    }
}