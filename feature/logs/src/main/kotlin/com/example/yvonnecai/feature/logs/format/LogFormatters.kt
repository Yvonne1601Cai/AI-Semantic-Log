package com.example.yvonnecai.feature.logs.format

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import javax.inject.Inject

/**
 * The app's `DateTimeFormatter`s, created once per instance (building one per row would be
 * wasteful) and used by the UI mapper at load time, never during composition.
 *
 * Row times and group headers use the locale's own conventions (`FormatStyle`), so 12-hour
 * locales such as en-US show `3:48:17 PM`. This follows the locale, not the device's 12/24-hour
 * toggle. The details-sheet timestamp keeps a fixed ISO-like layout on purpose: it is the precise,
 * unambiguous one (milliseconds and zone name).
 *
 * @param zone zone used to render instants; the device zone in production, fixed in tests.
 * @param locale locale used for the localized styles; the device locale in production, fixed in tests.
 */
class LogFormatters(zone: ZoneId, locale: Locale = Locale.getDefault()) {

    @Inject
    constructor() : this(ZoneId.systemDefault())

    private val timeOfDay = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)
        .withLocale(locale).withZone(zone)
    private val minuteBucket = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withLocale(locale).withZone(zone)
    private val fullLocal = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS z", locale).withZone(zone)

    /** `15:48:17` or `3:48:17 PM` depending on the locale, shown on every row. */
    fun timeOfDay(instant: Instant): String = timeOfDay.format(instant)

    /** `Jan 22, 2026, 3:48 PM` (en-US) or `22.01.2026, 15:48` (de-DE), shown on group headers. */
    fun minuteBucket(instant: Instant): String = minuteBucket.format(instant)

    /** `2026-01-22 10:48:17.892 EST`, shown on the details sheet. */
    fun fullLocal(instant: Instant): String = fullLocal.format(instant)
}
