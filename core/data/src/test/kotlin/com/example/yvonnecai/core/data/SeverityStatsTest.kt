package com.example.yvonnecai.core.data

import com.example.yvonnecai.core.model.Severity
import org.junit.Assert.assertEquals
import org.junit.Test

class SeverityStatsTest {

    @Test
    fun `empty stats have zero density and shares`() {
        val stats = SeverityStats.EMPTY
        assertEquals(0, stats.total)
        assertEquals(0f, stats.errorDensity, 0f)
        assertEquals(0f, stats.share(Severity.ERROR), 0f)
    }

    @Test
    fun `of counts the selected entries per severity`() {
        val entries = listOf(
            entry(id = "1", severity = Severity.DEBUG),
            entry(id = "2", severity = Severity.ERROR),
            entry(id = "3", severity = Severity.ERROR),
            entry(id = "4", severity = Severity.FATAL),
        )
        val stats = SeverityStats.of(entries, intArrayOf(0, 1, 2, 3))
        assertEquals(4, stats.total)
        assertEquals(1, stats.count(Severity.DEBUG))
        assertEquals(0, stats.count(Severity.INFO))
        assertEquals(2, stats.count(Severity.ERROR))
        assertEquals(1, stats.count(Severity.FATAL))
    }

    @Test
    fun `of only looks at the given indices`() {
        val entries = listOf(entry(id = "1", severity = Severity.ERROR), entry(id = "2", severity = Severity.INFO))
        val stats = SeverityStats.of(entries, intArrayOf(1))
        assertEquals(1, stats.total)
        assertEquals(0, stats.errorCount)
    }

    @Test
    fun `error density is ERROR plus FATAL over total`() {
        val entries = listOf(
            entry(id = "1", severity = Severity.INFO),
            entry(id = "2", severity = Severity.WARN),
            entry(id = "3", severity = Severity.ERROR),
            entry(id = "4", severity = Severity.FATAL),
        )
        val stats = SeverityStats.of(entries, intArrayOf(0, 1, 2, 3))
        assertEquals(2, stats.errorCount)
        assertEquals(0.5f, stats.errorDensity, 0.0001f)
    }

    @Test
    fun `shares sum to one`() {
        val entries = Severity.entries.mapIndexed { i, s -> entry(id = "$i", severity = s) } +
            entry(id = "extra", severity = Severity.WARN)
        val stats = SeverityStats.of(entries, IntArray(entries.size) { it })
        assertEquals(1.0, Severity.entries.sumOf { stats.share(it).toDouble() }, 0.0001)
    }
}
