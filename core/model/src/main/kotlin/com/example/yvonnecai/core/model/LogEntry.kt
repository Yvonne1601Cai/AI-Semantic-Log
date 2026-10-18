package com.example.yvonnecai.core.model

import java.time.Instant

/**
 * A single log event.
 *
 * @property id unique identifier; also the stable key of the row in the list.
 * @property timestamp moment the event was emitted.
 * @property severity how severe the event is.
 * @property tag subsystem that emitted the event (e.g. `network`, `neural_engine`).
 * @property message human readable description.
 * @property latencyMs latency attached to the event, in milliseconds.
 * @property isAiGenerated whether the event was produced by an AI component.
 */
data class LogEntry(
    val id: String,
    val timestamp: Instant,
    val severity: Severity,
    val tag: String,
    val message: String,
    val latencyMs: Int,
    val isAiGenerated: Boolean,
)
