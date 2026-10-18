package com.example.yvonnecai.core.model

/**
 * Severity of a [LogEntry], declared from least to most severe.
 *
 * Declaration order is significant: it is the clockwise order of the arcs in the severity ring
 * and the legend order in the UI, and [ordinal] indexes per-severity arrays everywhere.
 *
 * @property argb colour as a raw `0xAARRGGBB` value. The model must stay free of Compose, so
 * the design system converts it to a `Color` once, up front, instead of at render time.
 */
enum class Severity(val argb: Long) {
    DEBUG(0xFF90A4AE),
    INFO(0xFF42A5F5),
    WARN(0xFFFFB300),
    ERROR(0xFFEF5350),
    FATAL(0xFFAD1457);

    /** `true` for the severities counted as "errors" by the severity indicator. */
    val isError: Boolean get() = this == ERROR || this == FATAL

    companion object {
        /**
         * Parses the severity string coming from the API (case-insensitive), or `null` if unknown
         * so the caller decides how to react.
         */
        fun fromRaw(raw: String): Severity? =
            entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) }
    }
}
