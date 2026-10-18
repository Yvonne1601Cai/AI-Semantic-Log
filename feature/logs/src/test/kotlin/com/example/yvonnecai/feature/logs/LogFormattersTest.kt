package com.example.yvonnecai.feature.logs

import com.example.yvonnecai.feature.logs.format.LogFormatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

class LogFormattersTest {

    private val instant = Instant.parse("2026-01-22T15:48:17.892472Z")

    /** Newer JDKs put a narrow no-break space before AM/PM; compare with a plain space. */
    private fun String.plainSpaces() = replace('\u202f', ' ').replace('\u00a0', ' ')

    @Test
    fun `time of day follows a 24-hour locale`() {
        assertEquals("15:48:17", LogFormatters(ZoneOffset.UTC, Locale.GERMANY).timeOfDay(instant))
    }

    @Test
    fun `time of day follows a 12-hour locale`() {
        assertEquals("3:48:17 PM", LogFormatters(ZoneOffset.UTC, Locale.US).timeOfDay(instant).plainSpaces())
    }

    @Test
    fun `time of day uses the configured zone`() {
        assertEquals("20:48:17", LogFormatters(ZoneOffset.ofHours(5), Locale.GERMANY).timeOfDay(instant))
    }

    @Test
    fun `minute bucket is localized date plus short time`() {
        assertEquals("22.01.2026, 15:48", LogFormatters(ZoneOffset.UTC, Locale.GERMANY).minuteBucket(instant))
        assertEquals(
            "Jan 22, 2026, 3:48 PM",
            LogFormatters(ZoneOffset.UTC, Locale.US).minuteBucket(instant).plainSpaces(),
        )
    }

    @Test
    fun `full local time keeps millisecond precision in a fixed layout`() {
        val text = LogFormatters(ZoneOffset.UTC, Locale.US).fullLocal(instant)
        assertTrue(text.startsWith("2026-01-22 15:48:17.892"))
    }
}
