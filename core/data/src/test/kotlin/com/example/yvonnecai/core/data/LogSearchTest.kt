package com.example.yvonnecai.core.data

import com.example.yvonnecai.core.model.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogSearchTest {

    private val entries = listOf(
        entry("3", "2026-01-22T15:49:05Z", Severity.FATAL, "auth", "Invalid token"),
        entry("2", "2026-01-22T15:48:40Z", Severity.INFO, "auth", "User logged in"),
        entry("1", "2026-01-22T15:48:10Z", Severity.ERROR, "network", "Connection timed out"),
    )
    private val dataset = LogDataset("s", entries, SearchIndex.build("s", entries))

    @Test
    fun `blank query returns everything grouped by minute`() {
        val result = dataset.search("")

        assertEquals(3, result.matchCount)
        assertEquals(2, result.groups.size)
        assertEquals(2, result.stats.errorCount)
    }

    @Test
    fun `stats and groups describe only the matches`() {
        val result = dataset.search("auth")

        assertEquals(2, result.matchCount)
        assertEquals(1, result.stats.count(Severity.FATAL))
        assertEquals(1, result.stats.count(Severity.INFO))
        assertEquals(0, result.stats.count(Severity.ERROR))
        assertEquals(2, result.groups.size)
    }

    @Test
    fun `no match gives an empty result`() {
        val result = dataset.search("zzz")

        assertEquals(0, result.matchCount)
        assertTrue(result.groups.isEmpty())
        assertEquals(0, result.stats.total)
    }
}
