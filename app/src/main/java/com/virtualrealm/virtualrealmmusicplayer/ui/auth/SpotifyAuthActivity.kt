// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/ui/auth/SpotifyAuthActivity.kt

package com.virtualrealm.virtualrealmmusicplayer.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.virtualrealm.virtualrealmmusicplayer.MainActivity
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Resource
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
        if (intent?.action == Intent.ACTION_VIEW) {
            viewModel.exchangeCodeForToken(intent)
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

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
