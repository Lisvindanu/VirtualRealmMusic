// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/ui/main/MusicAppNavHost.kt
package com.virtualrealm.virtualrealmmusicplayer.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.virtualrealm.virtualrealmmusicplayer.R
import com.virtualrealm.virtualrealmmusicplayer.domain.model.AuthState
import com.virtualrealm.virtualrealmmusicplayer.ui.auth.LoginScreen
import com.virtualrealm.virtualrealmmusicplayer.ui.home.HomeScreen
import com.virtualrealm.virtualrealmmusicplayer.ui.player.PlayerScreen
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
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentRoute == item.route,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
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
                        navController.navigate(
                            Screen.Player.createRoute(musicId, musicType)
                        )
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
                        navController.navigate(
                            Screen.Player.createRoute(musicId, musicType)
                        )
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
                    }
                )
            }
        }
    }
}