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
 * @property title formatted bucket label, e.g. `Jan 22, 15:48`.
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
    val rawMetadata: String,
    /** Plain-text version of the whole entry, put on the clipboard by the Copy button. */
    val copyText: String,
)

/**
 * Everything the logs screen renders, as one immutable snapshot.
 *
 * Exactly one holds at a time: [isLoading] -> spinner; [errorReason] != null -> error with retry;
 * otherwise content ([groups] may be empty = "no matches").
 *
 * @property query text currently in the search field (updates instantly; results lag by the debounce).
 * @property sessionId session id reported by the backend.
 * @property totalCount entries in the whole dataset.
 * @property matchCount entries matching [query].
 * @property groups matching entries grouped by minute, newest first.
 * @property ring data for the severity indicator, computed from the matching entries.
 * @property severityCounts per-severity counts of the matching entries, indexed by [Severity.ordinal].
 * @property details entry whose details sheet is open, or `null`.
 */
@Immutable
data class LogsUiState(
    val isLoading: Boolean = true,
    val errorReason: LogLoadException.Reason? = null,
    val query: String = "",
    val sessionId: String = "",
    val totalCount: Int = 0,
    val matchCount: Int = 0,
    val groups: List<LogGroupUi> = emptyList(),
    val ring: SeverityRingData = SeverityRingData.EMPTY,
    val severityCounts: List<Int> = List(Severity.entries.size) { 0 },
    val details: LogDetailsUi? = null,
)
