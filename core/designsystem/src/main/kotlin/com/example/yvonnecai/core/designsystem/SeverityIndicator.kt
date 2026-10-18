package com.example.yvonnecai.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.yvonnecai.core.designsystem.theme.YvonneCaiTheme
import com.example.yvonnecai.core.model.Severity
import kotlin.math.roundToInt

/**
 * What the [SeverityIndicator] draws. Computed by the caller (the ViewModel pipeline), never in
 * a composable.
 *
 * @property shares share of each severity in `[0, 1]`, indexed by [Severity.ordinal].
 * @property errorDensity `(ERROR + FATAL) / total` in `[0, 1]`.
 * @property errorCount number of ERROR + FATAL entries (used for the accessibility description).
 * @property total number of entries.
 */
@Immutable
data class SeverityRingData(
    val shares: List<Float>,
    val errorDensity: Float,
    val errorCount: Int,
    val total: Int,
) {
    companion object {
        /** Ring for an empty result: no arcs, 0%. */
        val EMPTY = SeverityRingData(List(Severity.entries.size) { 0f }, 0f, 0, 0)
    }
}

/**
 * Custom severity indicator drawn with Compose [Canvas] (no chart library): a segmented ring,
 * one arc per [Severity] sized by its share, clockwise from 12 o'clock in severity order, with
 * the error density in the centre.
 *
 * Animations: the ring sweeps in once on first composition, and each segment's share and the
 * centre percentage animate with `animateFloatAsState` whenever [data] changes (e.g. while
 * filtering). Exposes a `contentDescription` for accessibility.
 *
 * @param data what to draw.
 * @param size outer diameter.
 * @param strokeWidth ring thickness.
 */
@Composable
fun SeverityIndicator(
    data: SeverityRingData,
    modifier: Modifier = Modifier,
    size: Dp = 132.dp,
    strokeWidth: Dp = 16.dp,
) {
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        reveal.animateTo(1f, tween(durationMillis = INTRO_MILLIS, easing = FastOutSlowInEasing))
    }

    // Fixed-size list (enum entries), so the number of animateFloatAsState calls never changes.
    val animatedShares = Severity.entries.map { severity ->
        animateFloatAsState(
            targetValue = data.shares[severity.ordinal],
            animationSpec = tween(durationMillis = UPDATE_MILLIS),
            label = "share-${severity.name}",
        ).value
    }
    val animatedDensity = animateFloatAsState(
        targetValue = data.errorDensity,
        animationSpec = tween(durationMillis = UPDATE_MILLIS),
        label = "error-density",
    ).value

    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val description = stringResource(
        R.string.severity_indicator_description,
        (data.errorDensity * 100).roundToInt(),
        data.errorCount,
        data.total,
    )

    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val inset = strokePx / 2f // keep the stroke inside the component bounds
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val topLeft = Offset(inset, inset)

            // Track: always visible, so an empty result still shows a ring.
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx),
            )
            computeRingArcs(shares = animatedShares, gapDegrees = GAP_DEGREES, reveal = reveal.value)
                .forEach { arc ->
                    drawArc(
                        color = Severity.entries[arc.index].color,
                        startAngle = arc.startAngle,
                        sweepAngle = arc.sweepAngle,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx),
                    )
                }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(animatedDensity * 100).roundToInt()}%",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.severity_indicator_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private const val INTRO_MILLIS = 900
private const val UPDATE_MILLIS = 450
private const val GAP_DEGREES = 3f

@Preview(showBackground = true)
@Composable
private fun SeverityIndicatorPreview() {
    YvonneCaiTheme(dynamicColor = false) {
        SeverityIndicator(
            data = SeverityRingData(
                shares = listOf(0.19f, 0.2f, 0.2f, 0.21f, 0.2f),
                errorDensity = 0.41f,
                errorCount = 2050,
                total = 5000,
            ),
        )
    }
}
