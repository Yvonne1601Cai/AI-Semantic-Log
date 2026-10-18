package com.example.yvonnecai.feature.logs.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.yvonnecai.core.designsystem.SeverityIndicator
import com.example.yvonnecai.core.designsystem.SeverityRingData
import com.example.yvonnecai.core.designsystem.color
import com.example.yvonnecai.core.designsystem.labelRes
import com.example.yvonnecai.core.model.Severity

/**
 * Header card: the [SeverityIndicator] ring next to a per-severity legend.
 *
 * @param ring data for the ring, computed from the logs currently matching the search.
 * @param severityCounts per-severity counts of those logs, indexed by [Severity.ordinal].
 */
@Composable
fun SeveritySummary(
    ring: SeverityRingData,
    severityCounts: List<Int>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            SeverityIndicator(data = ring)
            Spacer(Modifier.width(20.dp))
            // Most severe first: that is what the reader cares about most.
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Severity.entries.asReversed().forEach { severity ->
                    LegendRow(severity = severity, count = severityCounts[severity.ordinal])
                }
            }
        }
    }
}

@Composable
private fun LegendRow(severity: Severity, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(
            Modifier
                .size(10.dp)
                .background(severity.color, CircleShape),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(severity.labelRes),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(52.dp),
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
