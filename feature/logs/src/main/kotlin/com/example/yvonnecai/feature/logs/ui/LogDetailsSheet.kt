package com.example.yvonnecai.feature.logs.ui

import android.content.ClipData
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.yvonnecai.core.designsystem.color
import com.example.yvonnecai.core.designsystem.labelRes
import com.example.yvonnecai.feature.logs.LogDetailsUi
import com.example.yvonnecai.feature.logs.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Modal bottom sheet showing every field of a log plus its raw metadata, with one Copy button
 * that puts the whole entry on the clipboard as text.
 *
 * @param details preformatted content (see `LogUiMapper.details`).
 * @param onDismiss called when the sheet is swiped away or the scrim is tapped.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogDetailsSheet(
    details: LogDetailsUi,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, modifier = modifier) {
        DetailsContent(details)
    }
}

@Composable
private fun DetailsContent(details: LogDetailsUi) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var copied by remember(details.id) { mutableStateOf(false) }
    val copyText = rememberCopyText(details)

    // "Copied" is a short confirmation, not a permanent state.
    LaunchedEffect(copied) {
        if (copied) {
            delay(COPIED_RESET_MILLIS)
            copied = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(50), color = details.severity.color) {
                Text(
                    text = stringResource(details.severity.labelRes).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            TextButton(
                onClick = {
                    scope.launch {
                        clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("log entry", copyText)))
                        copied = true
                    }
                },
            ) {
                Text(stringResource(if (copied) R.string.details_copied else R.string.details_copy))
            }
        }
        Text(text = details.message, style = MaterialTheme.typography.headlineSmall)

        HorizontalDivider()

        DetailRow(R.string.details_id, details.id, monospace = true)
        DetailRow(R.string.details_timestamp, details.localTime)
        DetailRow(R.string.details_timestamp_utc, details.utcTime)
        DetailRow(R.string.details_tag, details.tag)
        DetailRow(R.string.details_severity, stringResource(details.severity.labelRes))
        DetailRow(R.string.details_latency, stringResource(R.string.log_latency_ms, details.latencyMs))
        DetailRow(
            R.string.details_ai_generated,
            stringResource(if (details.isAiGenerated) R.string.details_yes else R.string.details_no),
        )

        HorizontalDivider()

        Text(text = stringResource(R.string.details_metadata), style = MaterialTheme.typography.titleSmall)
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = details.metadataJson,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(12.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** Plain-text version of the whole entry for the clipboard, with localized labels. */
@Composable
private fun rememberCopyText(details: LogDetailsUi): String {
    val severity = stringResource(details.severity.labelRes)
    val latency = stringResource(R.string.log_latency_ms, details.latencyMs)
    val ai = stringResource(if (details.isAiGenerated) R.string.details_yes else R.string.details_no)
    val labels = listOf(
        R.string.details_id to details.id,
        R.string.details_severity to severity,
        R.string.details_tag to details.tag,
        R.string.details_timestamp to details.localTime,
        R.string.details_timestamp_utc to details.utcTime,
        R.string.details_latency to latency,
        R.string.details_ai_generated to ai,
    ).map { (label, value) -> stringResource(label) to value }
    return buildString {
        append(details.message)
        labels.forEach { (label, value) -> append('\n').append(label).append(": ").append(value) }
    }
}

@Composable
private fun DetailRow(@StringRes label: Int, value: String, monospace: Boolean = false) {
    Row(verticalAlignment = Alignment.Top) {
        DetailLabel(label)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = if (monospace) FontFamily.Monospace else null,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun RowScope.DetailLabel(@StringRes label: Int) {
    Text(
        text = stringResource(label),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.width(112.dp),
    )
}

private const val COPIED_RESET_MILLIS = 2_000L
