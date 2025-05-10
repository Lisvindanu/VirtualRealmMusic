// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/service/SpotifyWebPlayerHelper.kt
package com.virtualrealm.virtualrealmmusicplayer.service

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.virtualrealm.virtualrealmmusicplayer.data.local.preferences.AuthPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpotifyWebPlayerHelper @Inject constructor(
    private val context: Context,
    private val authPreferences: AuthPreferences // Inject AuthPreferences
) {
    private var webView: WebView? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    // Create coroutine scope for this helper
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // StateFlow untuk status player
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    // Callback
    var onPrepared: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onCompletion: (() -> Unit)? = null

    @SuppressLint("SetJavaScriptEnabled")
    fun initialize() {
        if (webView == null) {
            mainHandler.post {
                try {
                    webView = WebView(context).apply {
                        // Konfigurasi WebView
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.110 Safari/537.36"

                        // JavaScript interface untuk komunikasi WebView ke Kotlin
                        addJavascriptInterface(WebAppInterface(), "AndroidPlayer")

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                Log.d("SpotifyWeb", "Page loaded: $url")
                                injectControlScripts()
                            }

                            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                Log.e("SpotifyWeb", "WebView error: ${error?.description}")
                                onError?.invoke("Failed to load page: ${error?.description}")
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onConsoleMessage(message: ConsoleMessage?): Boolean {
                                Log.d("WebConsole", "${message?.message()} -- From line ${message?.lineNumber()} of ${message?.sourceId()}")
                                return true
                            }
                        }

                        // Set visibility (bisa diubah jika ingin menampilkan WebView)
                        visibility = View.VISIBLE
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }

                    Log.d("SpotifyWeb", "WebView initialized successfully")
                } catch (e: Exception) {
                    Log.e("SpotifyWeb", "Error initializing WebView: ${e.message}", e)
                    onError?.invoke("Failed to initialize WebView: ${e.message}")
                }
            }
        }
    }

    // JavaScript interface
    inner class WebAppInterface {
        @JavascriptInterface
        fun onPlaybackStateChanged(playing: Boolean) {
            mainHandler.post {
                _isPlaying.value = playing
                Log.d("SpotifyWeb", "Playback state changed: playing=$playing")
            }
        }

        @JavascriptInterface
        fun onPositionChanged(positionMs: Long) {
            _currentPosition.value = positionMs
        }

        @JavascriptInterface
        fun onDurationChanged(durationMs: Long) {
            _duration.value = durationMs
            Log.d("SpotifyWeb", "Duration changed: $durationMs ms")
        }

        @JavascriptInterface
        fun onTrackEnded() {
            mainHandler.post {
                onCompletion?.invoke()
            }
        }

        @JavascriptInterface
        fun onError(message: String) {
            mainHandler.post {
                Log.e("SpotifyWeb", "Player error: $message")
                onError?.invoke(message)
            }
        }
    }

    private fun injectControlScripts() {
        val script = """
        let spotifyInitialized = false;
        let spotifyPlayer = null;
        
        // Setup polling for player elements
        function setupSpotifyObserver() {
            console.log("Setting up Spotify observer");
            
            // Function to check if login wall is present and handle it
            function checkLoginWall() {
                // Look for login button or wall
                const loginButton = document.querySelector('[data-testid="login-button"]');
                if (loginButton) {
                    console.log("Login button found, attempting to bypass");
                    // We could try to bypass, but Spotify requires login
                    // For now, notify the app
                    AndroidPlayer.onError("Spotify requires login");
                    return true;
                }
                return false;
            }
            
            // Check for login wall immediately
            if (checkLoginWall()) return;
            
            // Find the play button
            const findAndClickPlayButton = () => {
                const playButton = document.querySelector('[data-testid="play-button"]');
                if (playButton) {
                    console.log("Play button found, clicking");
                    playButton.click();
                    return true;
                }
                return false;
            };
            
            // Setup a timer to repeatedly check for player
            const checkInterval = setInterval(() => {
                // Check if we're redirected to a login page
                if (checkLoginWall()) {
                    clearInterval(checkInterval);
                    return;
                }
                
                // Try to find and click play
                if (findAndClickPlayButton()) {
                    console.log("Play initiated");
                }
                
                // Check if media element exists
                const mediaElements = document.querySelectorAll('audio, video');
                if (mediaElements.length > 0) {
                    console.log("Media element found");
                    
                    // Use the first media element found
                    const mediaElement = mediaElements[0];
                    
                    // Setup event listeners
                    mediaElement.addEventListener('play', () => {
                        AndroidPlayer.onPlaybackStateChanged(true);
                    });
                    
                    mediaElement.addEventListener('pause', () => {
                        AndroidPlayer.onPlaybackStateChanged(false);
                    });
                    
                    mediaElement.addEventListener('ended', () => {
                        AndroidPlayer.onTrackEnded();
                    });
                    
                    mediaElement.addEventListener('timeupdate', () => {
                        const position = Math.round(mediaElement.currentTime * 1000);
                        AndroidPlayer.onPositionChanged(position);
                    });
                    
                    mediaElement.addEventListener('durationchange', () => {
                        const duration = Math.round(mediaElement.duration * 1000);
                        AndroidPlayer.onDurationChanged(duration);
                    });
                    
                    // Store reference to the media element
                    window.spotifyMediaElement = mediaElement;
                    
                    // If we have a media element, we're good to go
                    clearInterval(checkInterval);
                    
                    // Notify initialization complete
                    spotifyInitialized = true;
                    AndroidPlayer.onPlaybackStateChanged(false);
                    console.log("Spotify observer setup complete");
                }
            }, 1000);
        }
        
        // Run this when page loads
        setupSpotifyObserver();
        """

        webView?.evaluateJavascript(script, null)
    }

    fun loadAndPlayTrack(trackId: String) {
        _isLoading.value = true

        coroutineScope.launch {
            try {
                val authState = authPreferences.authStateFlow.first()
                val token = authState.accessToken

                if (token != null) {
                    Log.d("SpotifyWeb", "Using token for track: $trackId")

                    // Create a cookie to pre-authenticate the WebView
                    val cookieManager = android.webkit.CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setCookie("open.spotify.com", "sp_t=$token")
                    cookieManager.flush()

                    // Make sure we're loading a secure URL for DRM
                    val spotifyWebUrl = "https://open.spotify.com/track/$trackId"

                    // Configure WebView for better media support
                    webView?.settings?.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        // These settings might help with DRM support
                        allowContentAccess = true
                        allowFileAccess = true
                    }

                    // Load the actual Spotify web page
                    webView?.loadUrl(spotifyWebUrl)

                    // Inject control script after delay
                    mainHandler.postDelayed({
                        injectControlScripts()
                    }, 3000)
                } else {
                    onError?.invoke("Spotify login required")
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("SpotifyWeb", "Error loading track: ${e.message}", e)
                onError?.invoke("Error loading track: ${e.message}")
                _isLoading.value = false
            }
        }
    }

    // New function to get a direct stream URL
    private suspend fun getSpotifyDirectStreamUrl(trackId: String, token: String): String? {
        // This approach would need a custom implementation potentially using:
        // 1. A proxy service that converts Spotify URLs to direct MP3s
        // 2. A service like Deezer's API which might have the same song
        // 3. A custom server implementation that handles Spotify's DRM

        // For now, return null to indicate we couldn't get a direct URL
        return null
    }

    fun play() {
        mainHandler.post {
            val playScript = """
                if (window.spotifyPlayer) {
                    spotifyPlayer.resume().then(() => {
                        console.log('Resumed playback');
                    });
                } else if (window.spotifyMediaElement) {
                    window.spotifyMediaElement.play();
                } else {
                    // Coba klik tombol play
                    const playButton = document.querySelector('[data-testid="play-button"]');
                    if (playButton) playButton.click();
                }
            """
            webView?.evaluateJavascript(playScript, null)
        }
    }

    fun pause() {
        mainHandler.post {
            val pauseScript = """
                if (window.spotifyPlayer) {
                    spotifyPlayer.pause().then(() => {
                        console.log('Paused playback');
                    });
                } else if (window.spotifyMediaElement) {
                    window.spotifyMediaElement.pause();
                } else {
                    // Coba klik tombol pause
                    const pauseButton = document.querySelector('[data-testid="pause-button"]');
                    if (pauseButton) pauseButton.click();
                }
            """
            webView?.evaluateJavascript(pauseScript, null)
        }
    }

    fun seekTo(positionMs: Long) {
        mainHandler.post {
            val seekScript = """
                if (window.spotifyPlayer) {
                    spotifyPlayer.seek(${positionMs}).then(() => {
                        console.log('Seeked to position: ${positionMs}ms');
                    });
                } else if (window.spotifyMediaElement) {
                    window.spotifyMediaElement.currentTime = ${positionMs / 1000.0};
                }
            """
            webView?.evaluateJavascript(seekScript, null)
        }
    }

    fun release() {
        mainHandler.post {
            val disconnectScript = """
                if (window.spotifyPlayer) {
                    spotifyPlayer.disconnect();
                }
            """
            webView?.evaluateJavascript(disconnectScript, null)

            webView?.clearHistory()
            webView?.clearCache(true)
            webView?.loadUrl("about:blank")
            webView?.onPause()
            webView = null
        }
    }

    // Menampilkan interface login jika diperlukan
    fun showLoginInterface() {
        mainHandler.post {
            // Pastikan WebView terlihat
            webView?.visibility = View.VISIBLE

            // Tambahkan WebView ke Activity/Fragment jika belum
            val activity = context.findActivity()
            activity?.findViewById<ViewGroup>(android.R.id.content)?.addView(webView)
        }
    }

    fun hideLoginInterface() {
        mainHandler.post {
            // Sembunyikan WebView setelah login
            webView?.visibility = View.GONE

            // Atau hapus dari parent
            val parent = webView?.parent as? ViewGroup
            parent?.removeView(webView)
        }
    }

    // Utility untuk mendapatkan Activity dari Context
    private fun Context.findActivity(): android.app.Activity? {
        var context = this
        while (context is android.content.ContextWrapper) {
            if (context is android.app.Activity) {
                return context
            }
            context = context.baseContext
        }
        return null
    }
}