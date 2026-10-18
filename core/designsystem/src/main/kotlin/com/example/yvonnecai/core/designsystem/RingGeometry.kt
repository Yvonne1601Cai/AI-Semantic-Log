package com.example.yvonnecai.core.designsystem

import kotlin.math.max
import kotlin.math.min

/**
 * One arc of the severity ring, in the degrees `DrawScope.drawArc` expects
 * (0 = 3 o'clock, positive = clockwise).
 *
 * @property index index of the segment in the `shares` list it was computed from.
 */
internal data class RingArc(val index: Int, val startAngle: Float, val sweepAngle: Float)

/** Angle where the ring starts: 12 o'clock. */
internal const val RING_START_ANGLE = -90f

/**
 * Pure geometry of the severity ring, separate from the composable so it can be unit-tested.
 *
 * Segments are laid out clockwise from [RING_START_ANGLE]; each owns `share * 360` degrees.
 *
 * @param shares share of each segment in `[0, 1]`, in drawing order; expected to sum to ~1.
 * @param gapDegrees visual gap carved out of each segment. Only applied when more than one
 * segment is visible, so a single-colour ring is a closed circle.
 * @param reveal fraction `[0, 1]` of the circle currently revealed; drives the intro sweep by
 * clipping arcs at `reveal * 360` degrees from the start.
 * @return only the arcs with a positive visible sweep.
 */
internal fun computeRingArcs(
    shares: List<Float>,
    gapDegrees: Float,
    reveal: Float = 1f,
): List<RingArc> {
    val visibleSegments = shares.count { it > 0f }
    val gap = if (visibleSegments > 1) gapDegrees else 0f
    val revealLimit = reveal.coerceIn(0f, 1f) * 360f

    val arcs = ArrayList<RingArc>(shares.size)
    var cursor = 0f // degrees consumed so far, relative to the start angle
    shares.forEachIndexed { index, share ->
        val span = share.coerceAtLeast(0f) * 360f
        // Centre the gap on the segment boundary so arcs do not drift from their true share.
        val arcStart = cursor + gap / 2f
        val arcEnd = cursor + span - gap / 2f
        val sweep = max(min(arcEnd, revealLimit) - arcStart, 0f)
        if (span > 0f && sweep > 0f) {
            arcs += RingArc(index = index, startAngle = RING_START_ANGLE + arcStart, sweepAngle = sweep)
        }
        cursor += span
    }
    return arcs
}
