package com.example.yvonnecai.feature.logs.format

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/**
 * The app's `DateTimeFormatter`s, created once per instance (building one per row would be
 * wasteful) and used by the UI mapper at load time, never during composition.
 *
 * @param zone zone used to render instants; the device zone in production, fixed in tests.
 */
class LogFormatters(zone: ZoneId) {

    @Inject
    constructor() : this(ZoneId.systemDefault())

    private val timeOfDay = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.getDefault()).withZone(zone)
    private val minuteBucket = DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.getDefault()).withZone(zone)
    private val fullLocal = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS z", Locale.getDefault()).withZone(zone)

    /** `15:48:17`, shown on every row. */
    fun timeOfDay(instant: Instant): String = timeOfDay.format(instant)

    /** `Jan 22, 15:48`, shown on group headers. */
    fun minuteBucket(instant: Instant): String = minuteBucket.format(instant)

    /** `2026-01-22 10:48:17.892 EST`, shown on the details sheet. */
    fun fullLocal(instant: Instant): String = fullLocal.format(instant)
}
