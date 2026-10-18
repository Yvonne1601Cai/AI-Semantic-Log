package com.example.yvonnecai.core.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RingGeometryTest {

    @Test
    fun `no shares give no arcs`() {
        assertTrue(computeRingArcs(listOf(0f, 0f, 0f), gapDegrees = 3f).isEmpty())
    }

    @Test
    fun `a single segment is a closed circle without a gap`() {
        val arc = computeRingArcs(listOf(0f, 1f, 0f), gapDegrees = 3f).single()
        assertEquals(1, arc.index)
        assertEquals(RING_START_ANGLE, arc.startAngle, 0.001f)
        assertEquals(360f, arc.sweepAngle, 0.001f)
    }

    @Test
    fun `segments run clockwise from 12 o'clock with centred gaps`() {
        val arcs = computeRingArcs(listOf(0.5f, 0.25f, 0.25f), gapDegrees = 4f)
        assertEquals(3, arcs.size)
        assertEquals(RING_START_ANGLE + 2f, arcs[0].startAngle, 0.001f)
        assertEquals(176f, arcs[0].sweepAngle, 0.001f)
        assertEquals(RING_START_ANGLE + 182f, arcs[1].startAngle, 0.001f)
        assertEquals(86f, arcs[1].sweepAngle, 0.001f)
        assertEquals(RING_START_ANGLE + 272f, arcs[2].startAngle, 0.001f)
    }

    @Test
    fun `zero share segments are skipped but keep their original index`() {
        val arcs = computeRingArcs(listOf(0.5f, 0f, 0.5f), gapDegrees = 0f)
        assertEquals(listOf(0, 2), arcs.map { it.index })
    }

    @Test
    fun `reveal clips arcs at the revealed angle`() {
        val arcs = computeRingArcs(listOf(0.5f, 0.5f), gapDegrees = 0f, reveal = 0.75f)
        assertEquals(180f, arcs[0].sweepAngle, 0.001f)
        assertEquals(90f, arcs[1].sweepAngle, 0.001f) // 270 degrees revealed in total
    }

    @Test
    fun `reveal of zero draws nothing and reveal of one draws everything`() {
        val shares = listOf(0.4f, 0.6f)
        assertTrue(computeRingArcs(shares, gapDegrees = 2f, reveal = 0f).isEmpty())
        assertEquals(2, computeRingArcs(shares, gapDegrees = 2f, reveal = 1f).size)
    }

    @Test
    fun `five equal segments never exceed the full circle`() {
        val arcs = computeRingArcs(List(5) { 0.2f }, gapDegrees = 3f)
        assertEquals(5, arcs.size)
        assertTrue(arcs.sumOf { it.sweepAngle.toDouble() } < 360.0)
    }
}
