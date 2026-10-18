package com.example.yvonnecai.core.data

import app.cash.turbine.test
import com.example.yvonnecai.core.model.LogLoadException
import com.example.yvonnecai.core.network.LogRemoteDataSource
import com.example.yvonnecai.core.network.dto.LogResponseDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class LogRepositoryTest {

    /** Hand-written fake: the network is never mocked. */
    private class FakeRemote(var behavior: suspend () -> LogResponseDto) : LogRemoteDataSource {
        var calls = 0
        override suspend fun fetchLogs(): LogResponseDto {
            calls++
            return behavior()
        }
    }

    private val payload = LogResponseDto(sessionId = "session-1", data = listOf(dto(id = "a")))

    @Test
    fun `emits the mapped dataset once and completes`() = runTest {
        val repository = LogRepositoryImpl(FakeRemote { payload }, UnconfinedTestDispatcher(testScheduler))

        repository.fetchLogs().test {
            val dataset = awaitItem()
            assertEquals("session-1", dataset.sessionId)
            assertEquals(listOf("a"), dataset.entries.map { it.id })
            awaitComplete()
        }
    }

    @Test
    fun `second collection reuses the cached dataset without fetching again`() = runTest {
        val remote = FakeRemote { payload }
        val repository = LogRepositoryImpl(remote, UnconfinedTestDispatcher(testScheduler))

        var first: LogDataset? = null
        repository.fetchLogs().test { first = awaitItem(); awaitComplete() }
        repository.fetchLogs().test {
            assertSame(first, awaitItem())
            awaitComplete()
        }
        assertEquals(1, remote.calls)
    }

    @Test
    fun `concurrent first collections fetch only once`() = runTest {
        val remote = FakeRemote { delay(100); payload }
        val repository = LogRepositoryImpl(remote, UnconfinedTestDispatcher(testScheduler))

        val a = async { repository.fetchLogs().test { awaitItem(); awaitComplete() } }
        val b = async { repository.fetchLogs().test { awaitItem(); awaitComplete() } }
        a.await()
        b.await()

        assertEquals(1, remote.calls)
    }

    @Test
    fun `a failed load is not cached and retry fetches again`() = runTest {
        val remote = FakeRemote { throw LogLoadException(LogLoadException.Reason.NETWORK, IOException()) }
        val repository = LogRepositoryImpl(remote, UnconfinedTestDispatcher(testScheduler))

        repository.fetchLogs().test {
            val failure = awaitError()
            assertEquals(LogLoadException.Reason.NETWORK, (failure as LogLoadException).reason)
        }

        remote.behavior = { payload }
        repository.fetchLogs().test {
            assertEquals("session-1", awaitItem().sessionId)
            awaitComplete()
        }
        assertEquals(2, remote.calls)
    }

    @Test
    fun `unexpected exceptions are wrapped as UNKNOWN`() = runTest {
        val repository = LogRepositoryImpl(
            FakeRemote { throw IllegalStateException("boom") },
            UnconfinedTestDispatcher(testScheduler),
        )

        repository.fetchLogs().test {
            val failure = awaitError()
            assertTrue(failure is LogLoadException)
            assertEquals(LogLoadException.Reason.UNKNOWN, (failure as LogLoadException).reason)
        }
    }
}
