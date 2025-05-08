// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/MainActivity.kt
package com.virtualrealm.virtualrealmmusicplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.virtualrealm.virtualrealmmusicplayer.ui.main.MainViewModel
import com.virtualrealm.virtualrealmmusicplayer.ui.main.MusicAppNavHost
import com.virtualrealm.virtualrealmmusicplayer.ui.main.Screen
import com.virtualrealm.virtualrealmmusicplayer.ui.theme.VirtualRealmMusicPlayerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VirtualRealmMusicPlayerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val authState by viewModel.authState.collectAsState(initial = null)

                    MusicAppNavHost(
                        authState = authState,
                        startDestination = if (authState?.isAuthenticated == true) {
                            Screen.Home.route
                        } else {
                            Screen.Login.route
                        }
                    )
                }
            }
        }
    }
}