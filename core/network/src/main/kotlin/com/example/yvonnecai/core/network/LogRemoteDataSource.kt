package com.example.yvonnecai.core.network

import com.example.yvonnecai.core.model.LogLoadException
import com.example.yvonnecai.core.network.di.IoDispatcher
import com.example.yvonnecai.core.network.dto.LogResponseDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

/** Fetches the raw payload. The only type of this module the data layer talks to. */
interface LogRemoteDataSource {

    /**
     * Downloads and parses the payload.
     *
     * @throws LogLoadException for every failure, so callers never see Retrofit, OkHttp or
     * kotlinx.serialization exception types.
     */
    suspend fun fetchLogs(): LogResponseDto
}

/**
 * Decodes the response directly from the byte stream (`decodeFromStream`), so the 1.4 MB body is
 * never materialised as a `String`.
 *
 * Note the payload root is an object (`{ ..., "data": [...] }`), so entries cannot be streamed
 * one at a time with `decodeToSequence`: the full DTO list exists until the data layer has
 * mapped it, and is garbage once that finishes.
 */
class RetrofitLogRemoteDataSource @Inject constructor(
    private val api: LogApi,
    private val json: Json,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : LogRemoteDataSource {

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun fetchLogs(): LogResponseDto = withContext(ioDispatcher) {
        try {
            api.getLogs(LogEndpoint.URL).use { body ->
                json.decodeFromStream<LogResponseDto>(body.byteStream())
            }
        } catch (e: HttpException) {
            throw LogLoadException(LogLoadException.Reason.SERVER, e)
        } catch (e: IOException) {
            throw LogLoadException(LogLoadException.Reason.NETWORK, e)
        } catch (e: SerializationException) {
            throw LogLoadException(LogLoadException.Reason.INVALID_DATA, e)
        }
    }
}
