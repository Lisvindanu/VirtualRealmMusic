// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/util/ResourceState.kt
package com.virtualrealm.virtualrealmmusicplayer.util

import com.virtualrealm.virtualrealmmusicplayer.domain.model.Resource

sealed class ResourceState {
    object Loading : ResourceState()
    data class Error(val errorMessage: String) : ResourceState()
    object Success : ResourceState()
}

fun <T> Resource<T>.toResourceState(): ResourceState {
    return when (this) {
        is Resource.Loading -> ResourceState.Loading
        is Resource.Error -> ResourceState.Error(this.message)
        is Resource.Success -> ResourceState.Success
    }
}