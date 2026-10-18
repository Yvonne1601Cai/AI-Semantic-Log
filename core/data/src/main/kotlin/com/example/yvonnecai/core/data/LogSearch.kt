package com.example.yvonnecai.core.data

/**
 * Outcome of one search over a [LogDataset].
 *
 * @property groups matching entries bucketed by minute, newest first.
 * @property stats per-severity counts of the matching entries.
 * @property matchCount number of matching entries.
 */
class LogSearchResult(
    val groups: List<LogGroup>,
    val stats: SeverityStats,
    val matchCount: Int,
)

/**
 * Runs [query] against the dataset's index and derives everything the UI needs from the matches,
 * so callers never touch positional indices.
 *
 * CPU-bound: call it off the main thread.
 */
fun LogDataset.search(query: String): LogSearchResult {
    val indices = index.search(query)
    return LogSearchResult(
        groups = groupByMinute(entries, indices),
        stats = SeverityStats.of(entries, indices),
        matchCount = indices.size,
    )
}
