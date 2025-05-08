// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/ui/auth/LoginScreen.kt
package com.virtualrealm.virtualrealmmusicplayer.ui.auth

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.virtualrealm.virtualrealmmusicplayer.R
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Resource
import com.virtualrealm.virtualrealmmusicplayer.util.showToast

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onSkipLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsState()
    val authResult by viewModel.authResult.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Check if already authenticated
    LaunchedEffect(authState) {
        if (authState?.isAuthenticated == true) {
            onLoginSuccess()
        }
    }

    // Handle auth result
    LaunchedEffect(authResult) {
        when (authResult) {
            is Resource.Success -> {
                onLoginSuccess()
            }
            is Resource.Error -> {
                val errorMessage = (authResult as Resource.Error).message
                snackbarHostState.showSnackbar(errorMessage)
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // App logo
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.size(120.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // App title
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // App description
                Text(
                    text = stringResource(R.string.login_description),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Spotify login button
                Button(
                    onClick = {
                        val authUrl = viewModel.getSpotifyAuthUrl()
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = authResult !is Resource.Loading
                ) {
                    Text(text = stringResource(R.string.login_with_spotify))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Skip login button
                OutlinedButton(
                    onClick = onSkipLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = authResult !is Resource.Loading
                ) {
                    Text(text = stringResource(R.string.skip_login))
                }
            }

            // Loading indicator
            if (authResult is Resource.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}