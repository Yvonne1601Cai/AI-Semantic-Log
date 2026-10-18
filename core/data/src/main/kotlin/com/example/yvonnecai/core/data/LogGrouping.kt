package com.example.yvonnecai.core.data

import com.example.yvonnecai.core.model.LogEntry

/**
 * A run of logs that happened within the same minute.
 *
 * @property startMillis start of the minute, in epoch milliseconds; doubles as a stable list key.
 * @property indices positions in the master entry list, newest first.
 * @property errorCount how many of those entries are errors (ERROR or FATAL).
 */
class LogGroup(
    val startMillis: Long,
    val indices: IntArray,
    val errorCount: Int,
) {
    /** Number of entries in the group. */
    val size: Int get() = indices.size
}

private const val MILLIS_PER_MINUTE = 60_000L
private const val SECONDS_PER_MINUTE = 60L

/**
 * Groups the entries at [indices] into one-minute buckets (decision: the payload has a single
 * session id, so time is the only key that yields useful sticky headers).
 *
 * Preconditions: [entries] is sorted newest first and [indices] is ascending, which is what
 * [LogDataset] and [SearchIndex.search] guarantee. That makes this a single linear pass over
 * adjacent entries instead of a sort plus a `groupBy` on every keystroke.
 */
fun groupByMinute(entries: List<LogEntry>, indices: IntArray): List<LogGroup> {
    if (indices.isEmpty()) return emptyList()

    val groups = ArrayList<LogGroup>()
    var groupStart = 0
    var minute = minuteOf(entries[indices[0]])
    var errors = 0

    for (position in indices.indices) {
        val entry = entries[indices[position]]
        val entryMinute = minuteOf(entry)
        if (entryMinute != minute) {
            groups += LogGroup(minute * MILLIS_PER_MINUTE, indices.copyOfRange(groupStart, position), errors)
            groupStart = position
            minute = entryMinute
            errors = 0
        }
        if (entry.severity.isError) errors++
    }
    groups += LogGroup(minute * MILLIS_PER_MINUTE, indices.copyOfRange(groupStart, indices.size), errors)
    return groups
}

private fun minuteOf(entry: LogEntry): Long = entry.timestamp.epochSecond.floorDiv(SECONDS_PER_MINUTE)
