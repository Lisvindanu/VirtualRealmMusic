// data/local/preferences/AuthPreferences.kt
package com.virtualrealm.virtualrealmmusicplayer.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.virtualrealm.virtualrealmmusicplayer.domain.model.AuthState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_preferences")

@Singleton
class AuthPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_IS_AUTHENTICATED = booleanPreferencesKey("is_authenticated")
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val KEY_EXPIRES_IN = intPreferencesKey("expires_in")
        private val KEY_TOKEN_TYPE = stringPreferencesKey("token_type")
    }

    val authStateFlow: Flow<AuthState> = context.dataStore.data.map { preferences ->
        AuthState(
            isAuthenticated = preferences[KEY_IS_AUTHENTICATED] ?: false,
            accessToken = preferences[KEY_ACCESS_TOKEN],
            refreshToken = preferences[KEY_REFRESH_TOKEN],
            expiresIn = preferences[KEY_EXPIRES_IN],
            tokenType = preferences[KEY_TOKEN_TYPE]
        )
    }

    suspend fun saveAuthState(authState: AuthState) {
        context.dataStore.edit { preferences ->
            preferences[KEY_IS_AUTHENTICATED] = authState.isAuthenticated
            if (authState.accessToken != null) {
                preferences[KEY_ACCESS_TOKEN] = authState.accessToken
            }
            if (authState.refreshToken != null) {
                preferences[KEY_REFRESH_TOKEN] = authState.refreshToken
            }
            if (authState.expiresIn != null) {
                preferences[KEY_EXPIRES_IN] = authState.expiresIn
            }
            if (authState.tokenType != null) {
                preferences[KEY_TOKEN_TYPE] = authState.tokenType
            }
        }
    }

    suspend fun clearAuthState() {
        context.dataStore.edit { preferences ->
            preferences[KEY_IS_AUTHENTICATED] = false
            preferences.remove(KEY_ACCESS_TOKEN)
            preferences.remove(KEY_REFRESH_TOKEN)
            preferences.remove(KEY_EXPIRES_IN)
            preferences.remove(KEY_TOKEN_TYPE)
        }
    }
}