package com.example.yvonnecai.core.data

import com.example.yvonnecai.core.model.LogEntry
import com.example.yvonnecai.core.model.Severity
import com.example.yvonnecai.core.network.dto.LogEntryDto
import com.example.yvonnecai.core.network.dto.LogMetadataDto
import java.time.Instant

/** Concise [LogEntry] builder; `timestamp` is an ISO-8601 instant string. */
internal fun entry(
    id: String = "id-1",
    timestamp: String = "2026-01-22T15:48:17Z",
    severity: Severity = Severity.INFO,
    tag: String = "network",
    message: String = "Connection timed out",
): LogEntry = LogEntry(
    id = id,
    timestamp = Instant.parse(timestamp),
    severity = severity,
    tag = tag,
    message = message,
    latencyMs = 100,
    isAiGenerated = false,
)

/** Concise [LogEntryDto] builder. */
internal fun dto(
    id: String = "id-1",
    timestamp: String = "2026-01-22T15:48:17.892472Z",
    severity: String = "ERROR",
    tag: String = "network",
    message: String = "Connection timed out",
) = LogEntryDto(
    id = id,
    timestamp = timestamp,
    severity = severity,
    tag = tag,
    message = message,
    metadata = LogMetadataDto(latencyMs = 2040, isAiGenerated = true),
)
