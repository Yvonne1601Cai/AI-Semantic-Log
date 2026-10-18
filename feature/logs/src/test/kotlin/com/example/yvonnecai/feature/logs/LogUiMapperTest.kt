package com.example.yvonnecai.feature.logs

import com.example.yvonnecai.core.data.LogGroup
import com.example.yvonnecai.core.data.SeverityStats
import com.example.yvonnecai.core.model.LogEntry
import com.example.yvonnecai.core.model.Severity
import com.example.yvonnecai.feature.logs.format.LogFormatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class LogUiMapperTest {

    private val mapper = LogUiMapper(LogFormatters(ZoneOffset.UTC))

    private fun entry(id: String, severity: Severity = Severity.INFO, timestamp: String = "2026-01-22T15:48:17.892Z") =
        LogEntry(id, Instant.parse(timestamp), severity, "network", "Connection timed out", 2040, true)

    @Test
    fun `rows keep entry order and preformat the time`() {
        val rows = mapper.rows(listOf(entry("a"), entry("b", timestamp = "2026-01-22T09:05:03Z")))
        assertEquals(listOf("a", "b"), rows.map { it.id })
        assertEquals("15:48:17", rows[0].timeText)
        assertEquals("09:05:03", rows[1].timeText)
    }

    @Test
    fun `groups pick their rows by index and reuse the same instances`() {
        val rows = mapper.rows(listOf(entry("a"), entry("b"), entry("c")))
        val start = Instant.parse("2026-01-22T15:48:00Z").toEpochMilli()

        val groups = mapper.groups(listOf(LogGroup(start, intArrayOf(0, 2), errorCount = 1)), rows)

        val group = groups.single()
        assertEquals(start, group.key)
        assertTrue(group.title.endsWith("22, 15:48")) // month name depends on the locale
        assertEquals(2, group.rowCount)
        assertEquals(50, group.errorPercent)
        assertSame(rows[0], group.rows[0])
        assertSame(rows[2], group.rows[1])
    }

    @Test
    fun `ring data carries shares density and counts`() {
        val entries = listOf(
            entry("1", Severity.INFO),
            entry("2", Severity.ERROR),
            entry("3", Severity.FATAL),
            entry("4", Severity.WARN),
        )
        val ring = mapper.ring(SeverityStats.of(entries, intArrayOf(0, 1, 2, 3)))

        assertEquals(0.25f, ring.shares[Severity.INFO.ordinal], 0.0001f)
        assertEquals(0f, ring.shares[Severity.DEBUG.ordinal], 0f)
        assertEquals(0.5f, ring.errorDensity, 0.0001f)
        assertEquals(2, ring.errorCount)
        assertEquals(4, ring.total)
    }

    @Test
    fun `details include formatted times raw metadata and copy text`() {
        val details = mapper.details(entry("a", Severity.ERROR))

        assertEquals("a", details.id)
        assertEquals("2026-01-22T15:48:17.892Z", details.utcTime)
        assertTrue(details.localTime.startsWith("2026-01-22 15:48:17.892"))
        assertTrue(details.rawMetadata.contains("\"latency_ms\": 2040"))
        assertTrue(details.rawMetadata.contains("\"is_ai_generated\": true"))
        assertTrue(details.copyText.startsWith("Connection timed out"))
        assertTrue(details.copyText.contains("ID: a"))
        assertTrue(details.copyText.contains("Severity: ERROR"))
    }
}
