// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/ui/auth/SpotifyAuthScreen.kt

package com.virtualrealm.virtualrealmmusicplayer.ui.auth

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.virtualrealm.virtualrealmmusicplayer.R
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Resource
import com.virtualrealm.virtualrealmmusicplayer.ui.theme.SpotifyGreen
import kotlinx.coroutines.delay

@Composable
fun SpotifyAuthScreen(
    onAuthComplete: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authResult by viewModel.authResult.collectAsState()
    var pulsating by remember { mutableStateOf(true) }
    val scale by animateFloatAsState(
        targetValue = if (pulsating) 1.05f else 0.95f,
        label = "pulse animation"
    )

    // Start pulsating animation
    LaunchedEffect(key1 = true) {
        while (true) {
            pulsating = !pulsating
            delay(1000)
        }
    }

    // Handle auth result
    LaunchedEffect(authResult) {
        when (authResult) {
            is Resource.Success -> {
                onAuthComplete()
            }
            is Resource.Error -> {
                // Keep showing error state
            }
            else -> {
                // Loading or null state, continue showing loading UI
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Spotify Logo with pulsating animation
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(SpotifyGreen)
                    .scale(scale),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_spotify),
                    contentDescription = "Spotify Logo",
                    modifier = Modifier
                        .size(64.dp)
                        .padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Status text
            Text(
                text = when (authResult) {
                    is Resource.Loading -> "Connecting to Spotify..."
                    is Resource.Error -> "Authentication Error: ${(authResult as Resource.Error).message}"
                    is Resource.Success -> "Authentication Successful!"
                    null -> "Preparing authentication..."
                },
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Loading indicator or error message
            when (authResult) {
                is Resource.Loading -> {
                    CircularProgressIndicator(
                        color = SpotifyGreen,
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "This may take a few moments...",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
                is Resource.Error -> {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_error),
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Please try again later or contact support.",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
                else -> {
                    // For success state, nothing extra to display
                    // For null state, show a placeholder
                    if (authResult == null) {
                        CircularProgressIndicator(
                            color = SpotifyGreen,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
        }
    }

    // Bottom attribution
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Text(
            text = "Powered by Spotify",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            fontSize = 12.sp
        )
    }
}