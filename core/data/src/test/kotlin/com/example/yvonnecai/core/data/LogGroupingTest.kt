package com.example.yvonnecai.core.data

import com.example.yvonnecai.core.model.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class LogGroupingTest {

    // Master list is newest first, as the mapper guarantees.
    private val entries = listOf(
        entry(id = "5", timestamp = "2026-01-22T15:50:20Z", severity = Severity.FATAL),
        entry(id = "4", timestamp = "2026-01-22T15:50:10Z", severity = Severity.INFO),
        entry(id = "3", timestamp = "2026-01-22T15:49:59.999Z", severity = Severity.ERROR),
        entry(id = "2", timestamp = "2026-01-22T15:48:50Z", severity = Severity.ERROR),
        entry(id = "1", timestamp = "2026-01-22T15:48:10Z", severity = Severity.DEBUG),
    )
    private val all = IntArray(entries.size) { it }

    @Test
    fun `no indices gives no groups`() {
        assertTrue(groupByMinute(entries, IntArray(0)).isEmpty())
    }

    @Test
    fun `entries of the same minute share a group`() {
        val groups = groupByMinute(entries, all)
        assertEquals(3, groups.size)
        assertEquals(listOf(0, 1), groups[0].indices.toList())
        assertEquals(listOf(2), groups[1].indices.toList())
        assertEquals(listOf(3, 4), groups[2].indices.toList())
    }

    @Test
    fun `group key is the start of the minute`() {
        val groups = groupByMinute(entries, all)
        assertEquals(Instant.parse("2026-01-22T15:50:00Z").toEpochMilli(), groups[0].startMillis)
        assertEquals(Instant.parse("2026-01-22T15:49:00Z").toEpochMilli(), groups[1].startMillis)
    }

    @Test
    fun `groups come out newest first`() {
        val starts = groupByMinute(entries, all).map { it.startMillis }
        assertEquals(starts.sortedDescending(), starts)
    }

    @Test
    fun `error count only counts ERROR and FATAL`() {
        val groups = groupByMinute(entries, all)
        assertEquals(listOf(1, 1, 1), groups.map { it.errorCount })
    }

    @Test
    fun `a filtered subset only groups the selected indices`() {
        val groups = groupByMinute(entries, intArrayOf(0, 3))
        assertEquals(2, groups.size)
        assertEquals(listOf(0), groups[0].indices.toList())
        assertEquals(listOf(3), groups[1].indices.toList())
    }

    @Test
    fun `group size matches its indices`() {
        assertEquals(listOf(2, 1, 2), groupByMinute(entries, all).map { it.size })
    }
}
