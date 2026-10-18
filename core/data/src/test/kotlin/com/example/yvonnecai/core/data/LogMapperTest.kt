package com.example.yvonnecai.core.data

import com.example.yvonnecai.core.model.Severity
import com.example.yvonnecai.core.network.dto.LogResponseDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class LogMapperTest {

    @Test
    fun `maps every field`() {
        val mapped = dto().toLogEntryOrNull()
        assertNotNull(mapped)
        mapped!!
        assertEquals("id-1", mapped.id)
        assertEquals(Instant.parse("2026-01-22T15:48:17.892472Z"), mapped.timestamp)
        assertEquals(Severity.ERROR, mapped.severity)
        assertEquals("network", mapped.tag)
        assertEquals("Connection timed out", mapped.message)
        assertEquals(2040, mapped.latencyMs)
        assertTrue(mapped.isAiGenerated)
    }

    @Test
    fun `severity parsing is case-insensitive`() {
        assertEquals(Severity.WARN, dto(severity = "warn").toLogEntryOrNull()?.severity)
    }

    @Test
    fun `unknown severity falls back to INFO`() {
        assertEquals(Severity.INFO, dto(severity = "CATASTROPHIC").toLogEntryOrNull()?.severity)
    }

    @Test
    fun `blank id is dropped`() {
        assertNull(dto(id = " ").toLogEntryOrNull())
    }

    @Test
    fun `unparsable timestamp is dropped`() {
        assertNull(dto(timestamp = "yesterday").toLogEntryOrNull())
    }

    @Test
    fun `dataset drops bad entries and duplicate ids`() {
        val dataset = LogResponseDto(
            totalCount = 4,
            sessionId = "session-1",
            data = listOf(
                dto(id = "a", timestamp = "2026-01-22T15:48:10Z"),
                dto(id = "a", timestamp = "2026-01-22T15:48:11Z"), // duplicate id would crash LazyColumn keys
                dto(id = "b", timestamp = "nope"),
                dto(id = "c", timestamp = "2026-01-22T15:48:12Z"),
            ),
        ).toLogDataset()

        assertEquals("session-1", dataset.sessionId)
        assertEquals(listOf("c", "a"), dataset.entries.map { it.id })
    }

    @Test
    fun `dataset is sorted newest first and keeps payload order for equal timestamps`() {
        val dataset = LogResponseDto(
            sessionId = "s",
            data = listOf(
                dto(id = "old", timestamp = "2026-01-22T15:48:00Z"),
                dto(id = "tie-1", timestamp = "2026-01-22T15:50:00Z"),
                dto(id = "tie-2", timestamp = "2026-01-22T15:50:00Z"),
                dto(id = "mid", timestamp = "2026-01-22T15:49:00Z"),
            ),
        ).toLogDataset()

        assertEquals(listOf("tie-1", "tie-2", "mid", "old"), dataset.entries.map { it.id })
    }

    @Test
    fun `dataset index is parallel to the sorted entries`() {
        val dataset = LogResponseDto(
            sessionId = "s",
            data = listOf(
                dto(id = "old", timestamp = "2026-01-22T15:48:00Z", message = "alpha"),
                dto(id = "new", timestamp = "2026-01-22T15:50:00Z", message = "beta"),
            ),
        ).toLogDataset()

        // "alpha" belongs to the older entry, which is position 1 after sorting newest first.
        assertEquals(listOf(1), dataset.index.search("alpha").toList())
        assertEquals(dataset.entries.size, dataset.index.size)
    }

    @Test
    fun `dropped entries are counted`() {
        val dataset = LogResponseDto(
            sessionId = "s",
            data = listOf(
                dto(id = "a", timestamp = "2026-01-22T15:48:10Z"),
                dto(id = "a", timestamp = "2026-01-22T15:48:11Z"), // duplicate id
                dto(id = "b", timestamp = "nope"), // unparsable timestamp
                dto(id = " ", timestamp = "2026-01-22T15:48:12Z"), // blank id
                dto(id = "c", timestamp = "2026-01-22T15:48:13Z"),
            ),
        ).toLogDataset()

        assertEquals(2, dataset.entries.size)
        assertEquals(3, dataset.droppedCount)
    }

    @Test
    fun `a clean payload drops nothing`() {
        val dataset = LogResponseDto(sessionId = "s", data = listOf(dto(id = "a"), dto(id = "b"))).toLogDataset()
        assertEquals(0, dataset.droppedCount)
    }
}
