// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/data/remote/service/NetworkBoundResource.kt
package com.virtualrealm.virtualrealmmusicplayer.data.remote.service

import com.virtualrealm.virtualrealmmusicplayer.domain.model.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * A generic class that follows the Repository pattern to handle data operations with network and local caching.
 */
abstract class NetworkBoundResource<ResultType, RequestType> {
    fun asFlow(): Flow<Resource<ResultType>> = flow {
        // 1. Emit loading state
        emit(Resource.Loading)

        // 2. Try to load from cache
        val cachedData = loadFromCache()
        if (cachedData != null) {
            emit(Resource.Success(cachedData))
        }

        // 3. Try to fetch from network
        try {
            val apiResponse = fetchFromNetwork()

            // 4. Save the response to the database
            saveNetworkResult(apiResponse)

            // 5. Re-fetch from cache to get the freshest data
            val freshData = loadFromCache()
            if (freshData != null) {
                emit(Resource.Success(freshData))
            } else {
                emit(Resource.Error("Could not retrieve data from local cache after network fetch."))
            }
        } catch (e: Exception) {
            // If there's no cached data and network fetch fails, emit an error
            if (cachedData == null) {
                emit(Resource.Error("Network error and no cached data available: ${e.message}", e))
            }
        }
    }

    /**
     * Load data from the local cache (database)
     */
    protected abstract suspend fun loadFromCache(): ResultType?

    /**
     * Fetch fresh data from the network
     */
    protected abstract suspend fun fetchFromNetwork(): RequestType

    /**
     * Save network result to the local cache
     */
    protected abstract suspend fun saveNetworkResult(data: RequestType)
}