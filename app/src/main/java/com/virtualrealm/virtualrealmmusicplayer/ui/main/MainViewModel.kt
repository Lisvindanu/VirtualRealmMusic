// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/ui/main/MainViewModel.kt
package com.virtualrealm.virtualrealmmusicplayer.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.virtualrealm.virtualrealmmusicplayer.domain.model.AuthState
import com.virtualrealm.virtualrealmmusicplayer.domain.usecase.auth.GetAuthStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getAuthStateUseCase: GetAuthStateUseCase
) : ViewModel() {

    // Initialize StateFlow in the proper way
    val authState: StateFlow<AuthState?>

    init {
        // Initialize StateFlow in a coroutine-safe way
        authState = viewModelScope.run {
            getAuthStateUseCase()
                .stateIn(
                    scope = this,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = null
                )
        }
    }
}