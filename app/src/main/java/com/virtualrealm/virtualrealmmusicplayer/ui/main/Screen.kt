// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/ui/main/Screen.kt
package com.virtualrealm.virtualrealmmusicplayer.ui.main

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object Search : Screen("search")
    object Player : Screen("player/{musicId}/{musicType}") {
        fun createRoute(musicId: String, musicType: String): String {
            return "player/$musicId/$musicType"
        }
    }
}