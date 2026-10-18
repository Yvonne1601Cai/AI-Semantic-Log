package com.example.yvonnecai.feature.logs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yvonnecai.core.data.LogDataset
import com.example.yvonnecai.core.data.LogRepository
import com.example.yvonnecai.core.data.di.DefaultDispatcher
import com.example.yvonnecai.core.data.search
import com.example.yvonnecai.core.designsystem.SeverityRingData
import com.example.yvonnecai.core.model.LogLoadException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * State holder for the logs screen. Exposes a single [StateFlow] of [LogsUiState]; the UI sends
 * events up through the `on...` functions.
 *
 * ```
 * repository.fetchLogs() -> loadState --+
 *                                       +-> debounce -> index.search -> group -> map  -> searchResult --+
 * onQueryChange()        -> query ------+                                                               +-> uiState
 * onLogClick()           -> details ---------------------------------------------------------------------+
 * ```
 * The raw query goes straight into [uiState] so the text field never lags. Only the filtering is
 * debounced, and it always runs on [defaultDispatcher], never on the main thread.
 */
@HiltViewModel
class LogsViewModel @Inject constructor(
    private val repository: LogRepository,
    private val mapper: LogUiMapper,
    savedStateHandle: SavedStateHandle,
    @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private sealed interface LoadState {
        data object Loading : LoadState
        data class Failed(val reason: LogLoadException.Reason) : LoadState

        /** [rows] is parallel to `dataset.entries` and built once per load. */
        class Loaded(val dataset: LogDataset, val rows: Array<LogRowUi>) : LoadState
    }

    /** UI-ready outcome of one search. */
    private class SearchResult(
        val groups: List<LogGroupUi>,
        val ring: SeverityRingData,
        val severityCounts: List<Int>,
        val matchCount: Int,
    )

    private val loadState = MutableStateFlow<LoadState>(LoadState.Loading)
    /** Survives process death through [SavedStateHandle]. */
    private val query = savedStateHandle.getMutableStateFlow(KEY_QUERY, "")
    private val details = MutableStateFlow<LogDetailsUi?>(null)
    private var loadJob: Job? = null

    /**
     * Latest search result, or `null` while there is nothing to search (loading, failed, or the
     * first search has not finished).
     *
     * - `debounce` waits for a pause in typing so we do not filter on every keystroke. 150 ms is
     *   below the point where a delay feels laggy and above the typical gap between keystrokes.
     *   A blank query bypasses it so the initial render and "clear" are instant.
     * - `distinctUntilChanged` skips identical queries; `mapLatest` cancels a stale search when a
     *   newer query arrives.
     */
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private val searchResult: Flow<SearchResult?> =
        combine(
            loadState,
            query.debounce { if (it.isBlank()) 0L else SEARCH_DEBOUNCE_MS }.distinctUntilChanged(),
        ) { state, debouncedQuery -> state to debouncedQuery }
            .mapLatest { (state, debouncedQuery) ->
                when (state) {
                    is LoadState.Loaded -> withContext(defaultDispatcher) { search(state, debouncedQuery) }
                    else -> null
                }
            }

    /** Single immutable snapshot consumed by the screen. */
    val uiState: StateFlow<LogsUiState> =
        combine(loadState, query, searchResult, details) { state, currentQuery, result, currentDetails ->
            val content = when (state) {
                is LoadState.Loading -> LogsContent.Loading
                is LoadState.Failed -> LogsContent.Error(state.reason)
                // Loaded but the first search has not finished: keep the spinner.
                is LoadState.Loaded -> if (result == null) {
                    LogsContent.Loading
                } else {
                    LogsContent.Loaded(
                        sessionId = state.dataset.sessionId,
                        totalCount = state.dataset.entries.size,
                        matchCount = result.matchCount,
                        groups = result.groups,
                        ring = result.ring,
                        severityCounts = result.severityCounts,
                        details = currentDetails,
                    )
                }
            }
            LogsUiState(query = currentQuery, content = content)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), LogsUiState(query = query.value))

    init {
        load()
    }

    /** Called on every keystroke of the search field. */
    fun onQueryChange(newQuery: String) {
        query.value = newQuery
    }

    /** Clears the search field. */
    fun onClearQuery() {
        query.value = ""
    }

    /**
     * Opens the details sheet for the entry with [id]. A linear scan is fine: it happens once per
     * tap, not per row per frame.
     */
    fun onLogClick(id: String) {
        val loaded = loadState.value as? LoadState.Loaded ?: return
        val entry = loaded.dataset.entries.firstOrNull { it.id == id } ?: return
        details.value = mapper.details(entry)
    }

    /** Closes the details sheet. */
    fun onDetailsDismiss() {
        details.value = null
    }

    /** Retries after a failed load. */
    fun onRetry() {
        load()
    }

    private fun search(state: LoadState.Loaded, query: String): SearchResult {
        val result = state.dataset.search(query)
        return SearchResult(
            groups = mapper.groups(result.groups, state.rows),
            ring = mapper.ring(result.stats),
            severityCounts = result.stats.counts,
            matchCount = result.matchCount,
        )
    }

    private fun load() {
        loadJob?.cancel()
        // A sheet opened for the previous dataset must not outlive a reload.
        details.value = null
        loadJob = viewModelScope.launch {
            repository.fetchLogs()
                .map<LogDataset, LoadState> { dataset -> LoadState.Loaded(dataset, mapper.rows(dataset.entries)) }
                .flowOn(defaultDispatcher)
                .onStart { loadState.value = LoadState.Loading }
                .catch { error ->
                    val reason = (error as? LogLoadException)?.reason ?: LogLoadException.Reason.UNKNOWN
                    loadState.value = LoadState.Failed(reason)
                }
                .collect { loadState.value = it }
        }
    }

    private companion object {
        const val KEY_QUERY = "query"
        const val SEARCH_DEBOUNCE_MS = 150L

        /** Keeps the upstream alive across configuration changes. */
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
