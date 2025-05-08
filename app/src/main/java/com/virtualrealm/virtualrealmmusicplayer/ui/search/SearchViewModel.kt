// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/ui/search/SearchViewModel.kt
package com.virtualrealm.virtualrealmmusicplayer.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.virtualrealm.virtualrealmmusicplayer.domain.model.AuthState
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Music
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Resource
import com.virtualrealm.virtualrealmmusicplayer.domain.usecase.auth.GetAuthStateUseCase
import com.virtualrealm.virtualrealmmusicplayer.domain.usecase.music.SearchMusicUseCase
import com.virtualrealm.virtualrealmmusicplayer.domain.usecase.music.ToggleFavoriteUseCase
import com.virtualrealm.virtualrealmmusicplayer.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchMusicUseCase: SearchMusicUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    getAuthStateUseCase: GetAuthStateUseCase
) : ViewModel() {

    private val _searchResults = MutableStateFlow<Resource<List<Music>>>(Resource.Success(emptyList()))
    val searchResults: StateFlow<Resource<List<Music>>> = _searchResults.asStateFlow()

    val authState: StateFlow<AuthState?> = getAuthStateUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _selectedSource = MutableStateFlow(SearchMusicUseCase.MusicSource.YOUTUBE)
    val selectedSource: StateFlow<SearchMusicUseCase.MusicSource> = _selectedSource.asStateFlow()

    private val _currentQuery = MutableStateFlow("")
    val currentQuery: StateFlow<String> = _currentQuery.asStateFlow()

    fun setMusicSource(source: SearchMusicUseCase.MusicSource) {
        _selectedSource.value = source
        if (_currentQuery.value.isNotBlank()) {
            searchMusic(_currentQuery.value)
        }
    }

    fun searchMusic(query: String) {
        if (query.isBlank()) {
            _searchResults.value = Resource.Success(emptyList())
            return
        }

        _currentQuery.value = query
        viewModelScope.launch {
            _searchResults.value = Resource.Loading
            searchMusicUseCase(query, _selectedSource.value).collectLatest { result ->
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