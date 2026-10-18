package com.example.yvonnecai.feature.logs

import com.example.yvonnecai.core.data.LogGroup
import com.example.yvonnecai.core.data.SeverityStats
import com.example.yvonnecai.core.designsystem.SeverityRingData
import com.example.yvonnecai.core.model.LogEntry
import com.example.yvonnecai.core.model.Severity
import com.example.yvonnecai.feature.logs.format.LogFormatters
import java.time.Instant
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Turns domain and data-layer results into the preformatted models the UI renders. All string
 * formatting happens here (off the main thread, at load or per search), never in composables.
 */
class LogUiMapper @Inject constructor(private val formatters: LogFormatters) {

    /**
     * One [LogRowUi] per entry, in the same order as [entries]. Built once per load so a search
     * only picks rows by index and reuses the instances.
     */
    fun rows(entries: List<LogEntry>): Array<LogRowUi> =
        Array(entries.size) { i ->
            val entry = entries[i]
            LogRowUi(
                id = entry.id,
                timeText = formatters.timeOfDay(entry.timestamp),
                message = entry.message,
                severity = entry.severity,
                tag = entry.tag,
            )
        }

    /** Builds the sticky-header groups, picking each group's rows from [rows] by index. */
    fun groups(groups: List<LogGroup>, rows: Array<LogRowUi>): List<LogGroupUi> =
        groups.map { group ->
            LogGroupUi(
                key = group.startMillis,
                title = formatters.minuteBucket(Instant.ofEpochMilli(group.startMillis)),
                rowCount = group.size,
                errorPercent = (group.errorCount * 100f / group.size).roundToInt(),
                rows = List(group.size) { rows[group.indices[it]] },
            )
        }

    /** Converts severity stats into the data the indicator draws. */
    fun ring(stats: SeverityStats): SeverityRingData =
        SeverityRingData(
            shares = Severity.entries.map { stats.share(it) },
            errorDensity = stats.errorDensity,
            errorCount = stats.errorCount,
            total = stats.total,
        )

    /** Preformats everything the details sheet needs for [entry]. */
    fun details(entry: LogEntry): LogDetailsUi =
        LogDetailsUi(
            id = entry.id,
            message = entry.message,
            severity = entry.severity,
            tag = entry.tag,
            localTime = formatters.fullLocal(entry.timestamp),
            utcTime = entry.timestamp.toString(),
            latencyMs = entry.latencyMs,
            isAiGenerated = entry.isAiGenerated,
            metadataJson = "{\n  \"latency_ms\": ${entry.latencyMs},\n  \"is_ai_generated\": ${entry.isAiGenerated}\n}",
        )
}
