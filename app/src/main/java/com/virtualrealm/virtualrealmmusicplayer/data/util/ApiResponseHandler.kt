
// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/data/util/ApiResponseHandler.kt
package com.virtualrealm.virtualrealmmusicplayer.data.util

import com.virtualrealm.virtualrealmmusicplayer.domain.model.Resource
import retrofit2.HttpException
import java.io.IOException

suspend fun <T> safeApiCall(apiCall: suspend () -> T): Resource<T> {
    return try {
        Resource.Success(apiCall())
    } catch (throwable: Throwable) {
        when (throwable) {
            is IOException -> Resource.Error("Network Error: Check your internet connection.", throwable)
            is HttpException -> {
                val errorMessage = when (throwable.code()) {
                    401 -> "Unauthorized: Authentication is required."
                    403 -> "Forbidden: You don't have permission to access this resource."
                    404 -> "Not Found: The requested resource could not be found."
                    429 -> "Too Many Requests: Rate limit exceeded."
                    500, 501, 502, 503 -> "Server Error: Please try again later."
                    else -> "HTTP Error: ${throwable.code()} ${throwable.message()}"
                }
                Resource.Error(errorMessage, throwable)
            }
            else -> Resource.Error("Unknown Error: ${throwable.message}", throwable)
        }
    }
}