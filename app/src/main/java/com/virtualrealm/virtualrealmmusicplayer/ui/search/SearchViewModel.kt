// ui/search/SearchViewModel.kt
package com.virtualrealm.virtualrealmmusicplayer.ui.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.virtualrealm.virtualrealmmusicplayer.domain.model.AuthState
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Music
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Resource
import com.virtualrealm.virtualrealmmusicplayer.domain.usecase.auth.GetAuthStateUseCase
import com.virtualrealm.virtualrealmmusicplayer.domain.usecase.music.SearchMusicUseCase
import com.virtualrealm.virtualrealmmusicplayer.domain.usecase.music.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchMusicUseCase: SearchMusicUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val getAuthStateUseCase: GetAuthStateUseCase
) : ViewModel() {

    private val _searchResults = MutableLiveData<Resource<List<Music>>>()
    val searchResults: LiveData<Resource<List<Music>>> = _searchResults

    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    private var selectedSource = SearchMusicUseCase.MusicSource.YOUTUBE
    private var currentQuery = ""

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

    fun setMusicSource(source: SearchMusicUseCase.MusicSource) {
        selectedSource = source
        if (currentQuery.isNotBlank()) {
            searchMusic(currentQuery)
        }
    }

    fun searchMusic(query: String) {
        if (query.isBlank()) return

        currentQuery = query

        viewModelScope.launch {
            searchMusicUseCase(query, selectedSource).collect { result ->
                _searchResults.value = result
            }
        }
    }

    fun toggleFavorite(music: Music) {
        viewModelScope.launch {
            toggleFavoriteUseCase(music)
        }
    }
}