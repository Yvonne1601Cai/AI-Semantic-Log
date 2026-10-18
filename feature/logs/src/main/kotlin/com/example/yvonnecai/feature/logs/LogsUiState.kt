package com.example.yvonnecai.feature.logs

import androidx.compose.runtime.Immutable
import com.example.yvonnecai.core.designsystem.SeverityRingData
import com.example.yvonnecai.core.model.LogLoadException
import com.example.yvonnecai.core.model.Severity

/**
 * One list row. Built once when the dataset loads (time already formatted) and reused by every
 * search, so the same instance flows into `LazyColumn` and the row can skip recomposition.
 */
@Immutable
data class LogRowUi(
    val id: String,
    val timeText: String,
    val message: String,
    val severity: Severity,
    val tag: String,
)

/**
 * A sticky-header bucket and its rows.
 *
 * @property key stable list key (start of the minute, epoch millis).
 * @property title formatted bucket label, e.g. `Jan 22, 2026, 3:48 PM`.
 * @property errorPercent share of ERROR + FATAL entries in this bucket, `0..100`.
 */
@Immutable
data class LogGroupUi(
    val key: Long,
    val title: String,
    val rowCount: Int,
    val errorPercent: Int,
    val rows: List<LogRowUi>,
)

/** Everything the details sheet shows for one entry, preformatted. */
@Immutable
data class LogDetailsUi(
    val id: String,
    val message: String,
    val severity: Severity,
    val tag: String,
    val localTime: String,
    val utcTime: String,
    val latencyMs: Int,
    val isAiGenerated: Boolean,
    /** Pretty-printed JSON of the metadata fields the app models (not the verbatim payload). */
    val metadataJson: String,
)

/**
 * What the logs screen renders, as one immutable snapshot.
 *
 * @property query text currently in the search field (updates instantly; results lag by the debounce).
 *   It lives outside [content] because the field stays visible in every state.
 * @property content the loading, error or loaded body; exactly one at a time by construction.
 */
@Immutable
data class LogsUiState(
    val query: String = "",
    val content: LogsContent = LogsContent.Loading,
)

/** Body of the logs screen. */
sealed interface LogsContent {

    /** The first load, a retry, or the first search after a load is in flight. */
    @Immutable
    data object Loading : LogsContent

    /** The load failed; the screen offers Retry. */
    @Immutable
    data class Error(val reason: LogLoadException.Reason) : LogsContent

    /**
     * Search results. [groups] may be empty, meaning "no matches".
     *
     * @property sessionId session id reported by the backend.
     * @property totalCount entries in the whole dataset.
     * @property matchCount entries matching the query.
     * @property groups matching entries grouped by minute, newest first.
     * @property ring data for the severity indicator, computed from the matching entries.
     * @property severityCounts per-severity counts of the matching entries, indexed by [Severity.ordinal].
     * @property details entry whose details sheet is open, or `null`.
     */
    @Immutable
    data class Loaded(
        val sessionId: String,
        val totalCount: Int,
        val matchCount: Int,
        val groups: List<LogGroupUi>,
        val ring: SeverityRingData,
        val severityCounts: List<Int>,
        val details: LogDetailsUi?,
    ) : LogsContent
}
