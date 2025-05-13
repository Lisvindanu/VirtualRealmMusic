package com.virtualrealm.virtualrealmmusicplayer.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.virtualrealm.virtualrealmmusicplayer.domain.model.AuthState
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Music
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Resource
import com.virtualrealm.virtualrealmmusicplayer.domain.usecase.auth.GetAuthStateUseCase
import com.virtualrealm.virtualrealmmusicplayer.domain.usecase.music.SearchMusicUseCase
import com.virtualrealm.virtualrealmmusicplayer.domain.usecase.music.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchMusicUseCase: SearchMusicUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val getAuthStateUseCase: GetAuthStateUseCase
) : ViewModel() {

    private val _searchResults = MutableStateFlow<Resource<List<Music>>>(Resource.Success(emptyList()))
    val searchResults: StateFlow<Resource<List<Music>>> = _searchResults.asStateFlow()

    private val _authState = MutableStateFlow<AuthState?>(null)
    val authState: StateFlow<AuthState?> = _authState.asStateFlow()

    private val _selectedSource = MutableStateFlow(SearchMusicUseCase.MusicSource.YOUTUBE)
    val selectedSource: StateFlow<SearchMusicUseCase.MusicSource> = _selectedSource.asStateFlow()

    private val _currentQuery = MutableStateFlow("")
    val currentQuery: StateFlow<String> = _currentQuery.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()



    init {
        viewModelScope.launch {
            getAuthStateUseCase().collect {
                _authState.value = it
            }
        }
    }

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
                if (result is Resource.Error) {
                    _uiEvent.emit(UiEvent.ShowSnackbar(result.message ?: "Something went wrong"))
                }
            }
        }
    }

    fun toggleFavorite(music: Music) {
        viewModelScope.launch {
            toggleFavoriteUseCase(music)
            _uiEvent.emit(UiEvent.ShowSnackbar("Added to favorites successfully!"))
        }
    }

}
