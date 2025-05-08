// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/util/Extensions.kt
package com.virtualrealm.virtualrealmmusicplayer.util

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.virtualrealm.virtualrealmmusicplayer.R
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Music
import com.virtualrealm.virtualrealmmusicplayer.domain.model.Resource

fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

@Composable
fun ResourceState.HandleResourceState(
    onLoading: @Composable () -> Unit,
    onError: @Composable (String) -> Unit,
    onSuccess: @Composable () -> Unit
) {
    when (this) {
        is ResourceState.Loading -> onLoading()
        is ResourceState.Error -> onError(errorMessage)
        is ResourceState.Success -> onSuccess()
    }
}

fun Music.getPlayerUrl(): String {
    return when (this) {
        is Music.SpotifyTrack -> "https://open.spotify.com/track/$id"
        is Music.YoutubeVideo -> "https://www.youtube.com/watch?v=$id"
    }
}

fun Music.getMusicType(): String {
    return when (this) {
        is Music.SpotifyTrack -> Constants.MUSIC_TYPE_SPOTIFY
        is Music.YoutubeVideo -> Constants.MUSIC_TYPE_YOUTUBE
    }
}

fun <T> LiveData<T>.observeOnce(lifecycleOwner: LifecycleOwner, observer: Observer<T>) {
    observe(lifecycleOwner, object : Observer<T> {
        override fun onChanged(value: T) {
            observer.onChanged(value)
            removeObserver(this)
        }
    })
}
