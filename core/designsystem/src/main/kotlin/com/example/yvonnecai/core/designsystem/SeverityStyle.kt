package com.example.yvonnecai.core.designsystem

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.example.yvonnecai.core.model.Severity

/**
 * One [Color] per severity, built once at class load from [Severity.argb] and indexed by
 * ordinal, so rendering is an array lookup with no allocation and no string comparison.
 *
 * Severity colours are deliberately independent of the (dynamic) Material colour scheme: they
 * carry meaning and must look the same on every wallpaper and in light and dark mode.
 */
private val severityColors: Array<Color> = Array(Severity.entries.size) { Color(Severity.entries[it].argb) }

/** Colour of this severity. */
val Severity.color: Color get() = severityColors[ordinal]

/** Localised display name of this severity. */
@get:StringRes
val Severity.labelRes: Int
    get() = when (this) {
        Severity.DEBUG -> R.string.severity_debug
        Severity.INFO -> R.string.severity_info
        Severity.WARN -> R.string.severity_warn
        Severity.ERROR -> R.string.severity_error
        Severity.FATAL -> R.string.severity_fatal
    }
