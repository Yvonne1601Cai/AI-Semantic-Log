package com.example.yvonnecai.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire format of `logs_5k.json`: `{ total_count, session_id, data: [...] }`.
 *
 * Every field has a default so one sloppy entry cannot fail the whole 5,000-entry payload; the
 * data layer decides what to do with missing values.
 */
@Serializable
data class LogResponseDto(
    @SerialName("total_count") val totalCount: Int = 0,
    @SerialName("session_id") val sessionId: String = "",
    val data: List<LogEntryDto> = emptyList(),
)

/** One entry of [LogResponseDto.data]. */
@Serializable
data class LogEntryDto(
    val id: String = "",
    val timestamp: String = "",
    val severity: String = "",
    val tag: String = "",
    val message: String = "",
    val metadata: LogMetadataDto = LogMetadataDto(),
)

/** The nested `metadata` object of a [LogEntryDto]. */
@Serializable
data class LogMetadataDto(
    @SerialName("latency_ms") val latencyMs: Int = 0,
    @SerialName("is_ai_generated") val isAiGenerated: Boolean = false,
)
