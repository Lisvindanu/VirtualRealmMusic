// domain/model/AuthState.kt
package com.virtualrealm.virtualrealmmusicplayer.domain.model

data class AuthState(
    val isAuthenticated: Boolean = false,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val expiresIn: Int? = null,
    val tokenType: String? = null
)