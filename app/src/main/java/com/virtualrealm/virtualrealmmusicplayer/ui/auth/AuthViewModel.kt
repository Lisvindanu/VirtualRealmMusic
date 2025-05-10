package com.virtualrealm.virtualrealmmusicplayer.ui.auth

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.virtualrealm.virtualrealmmusicplayer.domain.model.AuthState
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Resource
import com.virtualrealm.virtualrealmmusicplayer.domain.usecase.auth.ExchangeSpotifyCodeUseCase
import com.virtualrealm.virtualrealmmusicplayer.domain.usecase.auth.GetAuthStateUseCase
import com.virtualrealm.virtualrealmmusicplayer.domain.usecase.auth.LogoutUseCase
import com.virtualrealm.virtualrealmmusicplayer.domain.usecase.auth.RefreshSpotifyTokenUseCase
import com.virtualrealm.virtualrealmmusicplayer.domain.usecase.auth.StartSpotifyAuthFlowUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val getAuthStateUseCase: GetAuthStateUseCase,
    private val exchangeSpotifyCodeUseCase: ExchangeSpotifyCodeUseCase,
    private val refreshSpotifyTokenUseCase: RefreshSpotifyTokenUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val startSpotifyAuthFlowUseCase: StartSpotifyAuthFlowUseCase
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState?>(null)
    val authState: StateFlow<AuthState?> = _authState.asStateFlow()

    private val _authResult = MutableStateFlow<Resource<AuthState>?>(null)
    val authResult: StateFlow<Resource<AuthState>?> = _authResult.asStateFlow()

    init {
        getAuthState()
    }

    private fun getAuthState() {
        viewModelScope.launch {
            getAuthStateUseCase().collectLatest { state ->
                _authState.value = state
            }
        }
    }

    fun getSpotifyAuthUrl(): String {
        // We'll keep this method but it's not used anymore
        return "https://accounts.spotify.com/authorize"
    }

    // New method to start the auth flow
    fun startSpotifyAuth() {
        startSpotifyAuthFlowUseCase()
    }

    // Keep original method for backward compatibility
    fun exchangeCodeForToken(code: String) {
        viewModelScope.launch {
            exchangeSpotifyCodeUseCase(code).collectLatest { result ->
                _authResult.value = result
            }
        }
    }

    // Add new method for handling intents
    fun exchangeCodeForToken(intent: Intent) {
        viewModelScope.launch {
            exchangeSpotifyCodeUseCase(intent).collectLatest { result ->
                _authResult.value = result
            }
        }
    }

    fun refreshToken() {
        viewModelScope.launch {
            refreshSpotifyTokenUseCase().collectLatest { result ->
                _authResult.value = result
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            _authResult.value = null
        }
    }
}

