// ui/auth/AuthViewModel.kt
package com.virtualrealm.virtualrealmmusicplayer.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.virtualrealm.virtualrealmmusicplayer.domain.model.AuthState
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Resource
import com.virtualrealm.virtualrealmmusicplayer.domain.repository.AuthRepository
import com.virtualrealm.virtualrealmmusicplayer.domain.usecase.auth.ExchangeSpotifyCodeUseCase
import com.virtualrealm.virtualrealmmusicplayer.domain.usecase.auth.GetAuthStateUseCase
import com.virtualrealm.virtualrealmmusicplayer.domain.usecase.auth.LogoutUseCase
import com.virtualrealm.virtualrealmmusicplayer.domain.usecase.auth.RefreshSpotifyTokenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val getAuthStateUseCase: GetAuthStateUseCase,
    private val exchangeSpotifyCodeUseCase: ExchangeSpotifyCodeUseCase,
    private val refreshSpotifyTokenUseCase: RefreshSpotifyTokenUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    private val _authResult = MutableLiveData<Resource<AuthState>>()
    val authResult: LiveData<Resource<AuthState>> = _authResult

    init {
        getAuthState()
    }

    private fun getAuthState() {
        viewModelScope.launch {
            getAuthStateUseCase().collect { state ->
                _authState.value = state
            }
        }
    }

    fun getSpotifyAuthUrl(): String {
        return authRepository.getSpotifyAuthUrl()
    }

    fun exchangeCodeForToken(code: String) {
        viewModelScope.launch {
            exchangeSpotifyCodeUseCase(code).collect { result ->
                _authResult.value = result
            }
        }
    }

    fun refreshToken() {
        viewModelScope.launch {
            refreshSpotifyTokenUseCase().collect { result ->
                _authResult.value = result
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
        }
    }
}