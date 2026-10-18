package com.example.yvonnecai.feature.logs

import com.example.yvonnecai.feature.logs.format.LogFormatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class LogFormattersTest {

    private val instant = Instant.parse("2026-01-22T15:48:17.892472Z")

    @Test
    fun `time of day uses the configured zone`() {
        assertEquals("15:48:17", LogFormatters(ZoneOffset.UTC).timeOfDay(instant))
        assertEquals("20:48:17", LogFormatters(ZoneOffset.ofHours(5)).timeOfDay(instant))
    }

    @Test
    fun `minute bucket drops seconds`() {
        // The month name depends on the locale, the rest does not.
        assertTrue(LogFormatters(ZoneOffset.UTC).minuteBucket(instant).endsWith("22, 15:48"))
    }

    @Test
    fun `full local time keeps millisecond precision`() {
        assertTrue(LogFormatters(ZoneOffset.UTC).fullLocal(instant).startsWith("2026-01-22 15:48:17.892"))
    }
}
