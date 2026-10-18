package com.example.yvonnecai.core.data

import com.example.yvonnecai.core.model.LogEntry
import com.example.yvonnecai.core.model.Severity

/**
 * Per-severity counts for a set of logs; the numbers behind the severity indicator.
 *
 * @property counts number of entries per severity, indexed by [Severity.ordinal].
 */
class SeverityStats(val counts: List<Int>) {

    init {
        require(counts.size == Severity.entries.size) { "expected one count per severity" }
    }

    /** Total number of entries. */
    val total: Int = counts.sum()

    /** Number of entries whose severity [Severity.isError] (ERROR + FATAL). */
    val errorCount: Int = Severity.entries.filter { it.isError }.sumOf { counts[it.ordinal] }

    /** Share of error entries in `[0, 1]`: `(ERROR + FATAL) / total`, or `0` when empty. */
    val errorDensity: Float = if (total == 0) 0f else errorCount.toFloat() / total

    /** Number of entries with the given [severity]. */
    fun count(severity: Severity): Int = counts[severity.ordinal]

    /** Share of [severity] in `[0, 1]`, or `0` when there are no entries. */
    fun share(severity: Severity): Float = if (total == 0) 0f else count(severity).toFloat() / total

    companion object {
        /** Stats of an empty set. */
        val EMPTY = SeverityStats(List(Severity.entries.size) { 0 })

        /** Counts the entries of [entries] at the given [indices] in a single pass. */
        fun of(entries: List<LogEntry>, indices: IntArray): SeverityStats {
            val counts = IntArray(Severity.entries.size)
            for (position in indices) counts[entries[position].severity.ordinal]++
            return SeverityStats(counts.toList())
        }
    }
}
