package com.example.yvonnecai.core.data

import com.example.yvonnecai.core.model.LogEntry
import com.example.yvonnecai.core.model.Severity
import com.example.yvonnecai.core.network.dto.LogEntryDto
import com.example.yvonnecai.core.network.dto.LogResponseDto
import java.time.Instant
import java.time.format.DateTimeParseException

/**
 * Maps the wire payload to a [LogDataset]: domain entries sorted newest first plus their search
 * index. The DTO list is unreachable once this returns.
 *
 * Entries that cannot be represented (blank id, unparsable timestamp) are dropped instead of
 * failing the whole load. Duplicate ids keep their first occurrence because the id is the
 * `LazyColumn` key and a duplicate would crash the list. The sort is stable, so entries with the
 * same timestamp keep payload order.
 */
internal fun LogResponseDto.toLogDataset(): LogDataset {
    val entries = data
        .mapNotNull { it.toLogEntryOrNull() }
        .distinctBy { it.id }
        .sortedByDescending { it.timestamp }
    return LogDataset(
        sessionId = sessionId,
        entries = entries,
        index = SearchIndex.build(sessionId, entries),
    )
}

/**
 * Maps one entry, or returns `null` when it is unusable. An unknown severity is treated as
 * [Severity.INFO] so the entry stays visible.
 */
internal fun LogEntryDto.toLogEntryOrNull(): LogEntry? {
    if (id.isBlank()) return null
    val parsedTimestamp = try {
        Instant.parse(timestamp)
    } catch (_: DateTimeParseException) {
        return null
    }
    return LogEntry(
        id = id,
        timestamp = parsedTimestamp,
        severity = Severity.fromRaw(severity) ?: Severity.INFO,
        tag = tag,
        message = message,
        latencyMs = metadata.latencyMs,
        isAiGenerated = metadata.isAiGenerated,
    )
}
