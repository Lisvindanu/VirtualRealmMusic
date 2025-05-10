// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/ui/main/MusicAppNavHost.kt
package com.virtualrealm.virtualrealmmusicplayer.ui.main

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.exyte.animatednavbar.AnimatedNavigationBar
import com.exyte.animatednavbar.animation.balltrajectory.Parabolic
import com.exyte.animatednavbar.animation.balltrajectory.Straight
import com.exyte.animatednavbar.animation.indendshape.Height
import com.exyte.animatednavbar.animation.indendshape.shapeCornerRadius
import com.virtualrealm.virtualrealmmusicplayer.R
import com.virtualrealm.virtualrealmmusicplayer.domain.model.AuthState
import com.virtualrealm.virtualrealmmusicplayer.ui.auth.LoginScreen
import com.virtualrealm.virtualrealmmusicplayer.ui.home.HomeScreen
import com.virtualrealm.virtualrealmmusicplayer.ui.player.PlayerScreen
import com.virtualrealm.virtualrealmmusicplayer.ui.playlist.PlaylistScreen
import com.virtualrealm.virtualrealmmusicplayer.ui.search.SearchScreen

@Composable
fun MusicAppNavHost(
    authState: AuthState?,
    startDestination: String,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem(
            label = stringResource(R.string.home),
            icon = Icons.Default.Home,
            route = Screen.Home.route
        ),
        BottomNavItem(
            label = stringResource(R.string.search),
            icon = Icons.Default.Search,
            route = Screen.Search.route
        )
    )

    val showBottomNav = currentRoute in listOf(
        Screen.Home.route,
        Screen.Search.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                AnimatedNavigationBar(
                    modifier = Modifier.height(64.dp),
                    selectedIndex = bottomNavItems.indexOfFirst { it.route == currentRoute },
                    cornerRadius = shapeCornerRadius(0.dp),
                    ballAnimation = Parabolic(tween(300)),
                    indentAnimation = Height(tween(300)),
                    barColor = MaterialTheme.colorScheme.primary,
                    ballColor = MaterialTheme.colorScheme.primary
                ) {
                    bottomNavItems.forEachIndexed { index, item ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .noRippleClickable {
                                    if (currentRoute != item.route) {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                modifier = Modifier.size(26.dp),
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = if (currentRoute == item.route)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.inversePrimary
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = modifier.padding(innerPadding)
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onSkipLogin = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToPlayer = { musicId, musicType ->
                        navController.navigate(Screen.Player.createRoute(musicId, musicType))
                    },
                    onNavigateToSearch = {
                        navController.navigate(Screen.Search.route)
                    },
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Search.route) {
                SearchScreen(
                    onNavigateToPlayer = { musicId, musicType ->
                        navController.navigate(Screen.Player.createRoute(musicId, musicType))
                    }
                )
            }

            composable(
                route = Screen.Player.route,
                arguments = listOf(
                    navArgument("musicId") { type = NavType.StringType },
                    navArgument("musicType") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val musicId = backStackEntry.arguments?.getString("musicId") ?: ""
                val musicType = backStackEntry.arguments?.getString("musicType") ?: ""

                PlayerScreen(
                    musicId = musicId,
                    musicType = musicType,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToPlaylist = {
                        navController.navigate(Screen.Playlist.route)
                    }
                )
            }

            composable(Screen.Playlist.route) {
                PlaylistScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToPlayer = { musicId, musicType ->
                        navController.navigate(Screen.Player.createRoute(musicId, musicType))
                    }
                )
            }
        }
    }
}

fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier = composed {
    clickable(
        indication = null,
        interactionSource = remember { MutableInteractionSource() }
    ) {
        onClick()
    }
}
