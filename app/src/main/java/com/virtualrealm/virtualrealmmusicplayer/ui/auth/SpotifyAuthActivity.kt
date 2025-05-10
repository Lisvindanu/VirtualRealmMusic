// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/ui/auth/SpotifyAuthActivity.kt

package com.virtualrealm.virtualrealmmusicplayer.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.virtualrealm.virtualrealmmusicplayer.MainActivity
import com.virtualrealm.virtualrealmmusicplayer.ui.theme.VirtualRealmMusicPlayerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SpotifyAuthActivity : ComponentActivity() {
    private val TAG = "SpotifyAuthActivity"
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "SpotifyAuthActivity created with intent: ${intent?.data}")

        // Handle authorization response from Spotify
        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            processAuthCallback(intent)
        } else {
            Log.e(TAG, "No valid callback URI found")
            Toast.makeText(this, "Authentication failed: No valid callback received", Toast.LENGTH_LONG).show()
            navigateToMainActivity()
            return
        }

        setContent {
            VirtualRealmMusicPlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SpotifyAuthScreen(
                        onAuthComplete = { navigateToMainActivity() }
                    )
                }
            }
        }
    }

    private fun processAuthCallback(intent: Intent) {
        try {
            val uri = intent.data
            Log.d(TAG, "Processing callback URI: $uri")

            // Extract code from the URI
            val code = uri?.getQueryParameter("code")

            if (code != null) {
                Log.d(TAG, "Authorization code extracted: $code (length: ${code.length})")
                // Directly exchange code - don't use the intent overload
                viewModel.exchangeCodeForToken(code)
                Toast.makeText(this, "Connecting to Spotify...", Toast.LENGTH_SHORT).show()
            } else {
                // Check for error
                val error = uri?.getQueryParameter("error")
                Log.e(TAG, "Auth error from Spotify: $error")
                Toast.makeText(this, "Authentication failed: $error", Toast.LENGTH_LONG).show()
                navigateToMainActivity()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing callback: ${e.message}", e)
            Toast.makeText(this, "Authentication error: ${e.message}", Toast.LENGTH_LONG).show()
            navigateToMainActivity()
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}