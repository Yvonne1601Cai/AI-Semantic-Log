package com.example.yvonnecai.feature.logs

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.yvonnecai.core.data.LogDataset
import com.example.yvonnecai.core.data.LogRepository
import com.example.yvonnecai.core.data.SearchIndex
import com.example.yvonnecai.core.model.LogEntry
import com.example.yvonnecai.core.model.LogLoadException
import com.example.yvonnecai.core.model.Severity
import com.example.yvonnecai.feature.logs.format.LogFormatters
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class LogsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** Repository fake whose behaviour each test swaps; the network is never involved. */
    private class FakeLogRepository(var behavior: () -> Flow<LogDataset>) : LogRepository {
        var fetchCount = 0
        override fun fetchLogs(): Flow<LogDataset> {
            fetchCount++
            return behavior()
        }
    }

    private fun entry(id: String, timestamp: String, severity: Severity, tag: String, message: String) =
        LogEntry(id, Instant.parse(timestamp), severity, tag, message, latencyMs = 10, isAiGenerated = false)

    // Newest first, as the data layer guarantees.
    private val entries = listOf(
        entry("3", "2026-01-22T15:49:05Z", Severity.FATAL, "auth", "Invalid token"),
        entry("2", "2026-01-22T15:48:40Z", Severity.INFO, "auth", "User logged in"),
        entry("1", "2026-01-22T15:48:10Z", Severity.ERROR, "network", "Connection timed out"),
    )
    private val dataset = LogDataset("session-666", entries, SearchIndex.build("session-666", entries))
    private val repository = FakeLogRepository { flow { emit(dataset) } }

    private val LogsUiState.loaded: LogsContent.Loaded get() = content as LogsContent.Loaded
    private val LogsUiState.isLoading: Boolean get() = content == LogsContent.Loading
    private val LogsUiState.errorReason: LogLoadException.Reason? get() = (content as? LogsContent.Error)?.reason

    private fun viewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()) = LogsViewModel(
        repository = repository,
        mapper = LogUiMapper(LogFormatters(ZoneOffset.UTC)),
        savedStateHandle = savedStateHandle,
        defaultDispatcher = mainDispatcherRule.dispatcher,
    )

    @Test
    fun `starts in loading state`() = runTest {
        viewModel().uiState.test {
            val initial = awaitItem()
            assertTrue(initial.isLoading)
            assertNull(initial.errorReason)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `exposes grouped content after load`() = runTest {
        viewModel().uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()

            assertFalse(state.isLoading)
            assertEquals("session-666", state.loaded.sessionId)
            assertEquals(3, state.loaded.totalCount)
            assertEquals(3, state.loaded.matchCount)
            assertEquals(2, state.loaded.groups.size) // 15:49 and 15:48 buckets
            assertEquals(listOf("3"), state.loaded.groups[0].rows.map { it.id })
            assertEquals(listOf("2", "1"), state.loaded.groups[1].rows.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ring data and counts describe the matching entries`() = runTest {
        viewModel().uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()

            assertEquals(2f / 3f, state.loaded.ring.errorDensity, 0.0001f)
            assertEquals(2, state.loaded.ring.errorCount)
            assertEquals(3, state.loaded.ring.total)
            assertEquals(1, state.loaded.severityCounts[Severity.FATAL.ordinal])
            assertEquals(1, state.loaded.severityCounts[Severity.INFO.ordinal])
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `rows are preformatted once at load`() = runTest {
        viewModel().uiState.test {
            advanceUntilIdle()
            val row = expectMostRecentItem().loaded.groups[0].rows.single()
            assertEquals("15:49:05", row.timeText)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `query text updates immediately but results wait for the debounce`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            advanceUntilIdle()
            expectMostRecentItem()

            vm.onQueryChange("auth")
            advanceTimeBy(149)
            runCurrent()
            val beforeDebounce = expectMostRecentItem()
            assertEquals("auth", beforeDebounce.query)
            assertEquals(3, beforeDebounce.loaded.matchCount)

            advanceTimeBy(1)
            runCurrent()
            val afterDebounce = expectMostRecentItem()
            assertEquals(2, afterDebounce.loaded.matchCount)
            assertEquals(3, afterDebounce.loaded.totalCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `only the latest query is applied when typing quickly`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            advanceUntilIdle()
            expectMostRecentItem()

            vm.onQueryChange("t")
            advanceTimeBy(50)
            vm.onQueryChange("to")
            advanceTimeBy(50)
            vm.onQueryChange("token")
            advanceUntilIdle()

            val state = expectMostRecentItem()
            assertEquals("token", state.query)
            assertEquals(1, state.loaded.matchCount)
            assertEquals("3", state.loaded.groups.single().rows.single().id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ring follows the filtered set`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            advanceUntilIdle()
            expectMostRecentItem()

            vm.onQueryChange("logged in") // a single INFO entry
            advanceUntilIdle()

            val state = expectMostRecentItem()
            assertEquals(1, state.loaded.ring.total)
            assertEquals(0f, state.loaded.ring.errorDensity, 0f)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `no match gives empty groups and zero counts without the loading flag`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            advanceUntilIdle()
            expectMostRecentItem()

            vm.onQueryChange("zzz")
            advanceUntilIdle()

            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertTrue(state.loaded.groups.isEmpty())
            assertEquals(0, state.loaded.matchCount)
            assertEquals(0, state.loaded.ring.total)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearing the query restores every log immediately`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            advanceUntilIdle()
            expectMostRecentItem()
            vm.onQueryChange("auth")
            advanceUntilIdle()
            assertEquals(2, expectMostRecentItem().loaded.matchCount)

            vm.onClearQuery()
            runCurrent() // no time advance: a blank query skips the debounce

            val state = expectMostRecentItem()
            assertEquals("", state.query)
            assertEquals(3, state.loaded.matchCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `load failure exposes its reason and retry recovers`() = runTest {
        repository.behavior = { flow { throw LogLoadException(LogLoadException.Reason.NETWORK, IOException()) } }
        val vm = viewModel()
        vm.uiState.test {
            advanceUntilIdle()
            val failed = expectMostRecentItem()
            assertFalse(failed.isLoading)
            assertEquals(LogLoadException.Reason.NETWORK, failed.errorReason)

            repository.behavior = { flow { emit(dataset) } }
            vm.onRetry()
            advanceUntilIdle()

            val recovered = expectMostRecentItem()
            assertNull(recovered.errorReason)
            assertEquals(3, recovered.loaded.matchCount)
            assertEquals(2, repository.fetchCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `unexpected exceptions map to UNKNOWN`() = runTest {
        repository.behavior = { flow { throw IllegalStateException("boom") } }
        viewModel().uiState.test {
            advanceUntilIdle()
            assertEquals(LogLoadException.Reason.UNKNOWN, expectMostRecentItem().errorReason)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `retry shows loading again while the new request is in flight`() = runTest {
        repository.behavior = { flow { throw LogLoadException(LogLoadException.Reason.SERVER) } }
        val vm = viewModel()
        vm.uiState.test {
            advanceUntilIdle()
            assertEquals(LogLoadException.Reason.SERVER, expectMostRecentItem().errorReason)

            repository.behavior = { flow { delay(1_000); emit(dataset) } }
            vm.onRetry()
            runCurrent()
            assertTrue(expectMostRecentItem().isLoading)

            advanceUntilIdle()
            assertFalse(expectMostRecentItem().isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clicking a row opens details and dismiss closes them`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            advanceUntilIdle()
            expectMostRecentItem()

            vm.onLogClick("2")
            advanceUntilIdle()
            val open = expectMostRecentItem().loaded.details
            assertEquals("2", open?.id)
            assertEquals("User logged in", open?.message)

            vm.onDetailsDismiss()
            advanceUntilIdle()
            assertNull(expectMostRecentItem().loaded.details)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clicking an unknown id leaves details closed`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            advanceUntilIdle()
            expectMostRecentItem()

            vm.onLogClick("does-not-exist")
            advanceUntilIdle()
            expectNoEvents() // state is unchanged, so nothing is emitted
            assertNull(vm.uiState.value.loaded.details)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `details stay open across a new search`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            advanceUntilIdle()
            expectMostRecentItem()
            vm.onLogClick("1")
            vm.onQueryChange("auth")
            advanceUntilIdle()

            val state = expectMostRecentItem()
            assertEquals("1", state.loaded.details?.id)
            assertEquals(2, state.loaded.matchCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a saved query is restored and applied after recreation`() = runTest {
        val handle = SavedStateHandle(mapOf("query" to "auth"))
        viewModel(handle).uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals("auth", state.query)
            assertEquals(2, state.loaded.matchCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `typing is written to the saved state`() = runTest {
        val handle = SavedStateHandle()
        val vm = viewModel(handle)
        vm.uiState.test {
            advanceUntilIdle()
            vm.onQueryChange("token")
            advanceUntilIdle()
            assertEquals("token", handle.get<String>("query"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `retry closes a stale details sheet`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            advanceUntilIdle()
            expectMostRecentItem()
            vm.onLogClick("1")
            advanceUntilIdle()
            assertEquals("1", expectMostRecentItem().loaded.details?.id)

            vm.onRetry()
            advanceUntilIdle()

            assertNull(expectMostRecentItem().loaded.details)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
