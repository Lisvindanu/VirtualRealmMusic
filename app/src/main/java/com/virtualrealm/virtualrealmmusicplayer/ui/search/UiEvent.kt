package com.virtualrealm.virtualrealmmusicplayer.ui.search

sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
}