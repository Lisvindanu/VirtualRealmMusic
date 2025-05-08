// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/ui/auth/SpotifyAuthActivity.kt
package com.virtualrealm.virtualrealmmusicplayer.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.virtualrealm.virtualrealmmusicplayer.util.showToast
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SpotifyAuthActivity : ComponentActivity() {

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle the auth callback
        val uri = intent.data
        if (uri != null) {
            val code = uri.getQueryParameter("code")
            if (code != null) {
                viewModel.exchangeCodeForToken(code)
            } else {
                val error = uri.getQueryParameter("error")
                showToast("Authentication failed: $error")
                navigateToMainActivity()
            }
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
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}

@Composable
fun SpotifyAuthScreen(
    onAuthComplete: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authResult by viewModel.authResult.collectAsState()

    LaunchedEffect(authResult) {
        when (authResult) {
            is Resource.Success -> {
                onAuthComplete()
            }
            is Resource.Error -> {
                onAuthComplete()
            }
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CircularProgressIndicator(
            modifier = Modifier.align(Alignment.Center)
        )

        if (authResult is Resource.Error) {
            Text(
                text = (authResult as Resource.Error).message,
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}