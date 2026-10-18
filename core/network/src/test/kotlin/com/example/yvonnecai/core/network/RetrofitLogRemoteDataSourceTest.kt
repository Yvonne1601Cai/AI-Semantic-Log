package com.example.yvonnecai.core.network

import com.example.yvonnecai.core.model.LogLoadException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class RetrofitLogRemoteDataSourceTest {

    /** Hand-written fake of the Retrofit interface; records the URL it was called with. */
    private class FakeApi(private val behavior: () -> ResponseBody) : LogApi {
        var requestedUrl: String? = null
        override suspend fun getLogs(url: String): ResponseBody {
            requestedUrl = url
            return behavior()
        }
    }

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private fun TestScope.dataSource(api: LogApi) =
        RetrofitLogRemoteDataSource(api, json, UnconfinedTestDispatcher(testScheduler))

    private fun body(text: String) = text.toResponseBody()

    @Test
    fun `parses the payload and ignores unknown keys`() = runTest {
        val api = FakeApi {
            body(
                """
                {
                  "total_count": 1, "session_id": "session-666", "something_new": true,
                  "data": [{
                    "id": "33dbf6fc", "timestamp": "2026-01-22T15:48:17.892472Z",
                    "severity": "FATAL", "tag": "sync", "message": "Invalid token",
                    "metadata": { "latency_ms": 12, "is_ai_generated": false }
                  }]
                }
                """.trimIndent(),
            )
        }

        val dto = dataSource(api).fetchLogs()

        assertEquals("session-666", dto.sessionId)
        assertEquals(1, dto.data.size)
        assertEquals("FATAL", dto.data.single().severity)
        assertEquals(12, dto.data.single().metadata.latencyMs)
    }

    @Test
    fun `requests the single endpoint constant`() = runTest {
        val api = FakeApi { body("""{"data": []}""") }
        dataSource(api).fetchLogs()
        assertEquals(LogEndpoint.URL, api.requestedUrl)
    }

    @Test
    fun `endpoint keeps the encoded slash in the object name`() {
        assertTrue(LogEndpoint.URL.contains("data%2Flogs_5k.json"))
    }

    private suspend fun assertFailure(reason: LogLoadException.Reason, source: RetrofitLogRemoteDataSource) {
        try {
            source.fetchLogs()
            fail("expected LogLoadException")
        } catch (e: LogLoadException) {
            assertEquals(reason, e.reason)
        }
    }

    @Test
    fun `malformed json becomes INVALID_DATA`() = runTest {
        assertFailure(LogLoadException.Reason.INVALID_DATA, dataSource(FakeApi { body("{ not json") }))
    }

    @Test
    fun `io failure becomes NETWORK`() = runTest {
        assertFailure(LogLoadException.Reason.NETWORK, dataSource(FakeApi { throw IOException("offline") }))
    }

    @Test
    fun `http failure becomes SERVER`() = runTest {
        val api = FakeApi { throw HttpException(Response.error<Any>(500, "".toResponseBody())) }
        assertFailure(LogLoadException.Reason.SERVER, dataSource(api))
    }
}
