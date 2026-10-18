package com.example.yvonnecai.feature.logs.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.yvonnecai.core.model.LogLoadException
import com.example.yvonnecai.feature.logs.LogsContent
import com.example.yvonnecai.feature.logs.LogsUiState
import com.example.yvonnecai.feature.logs.LogsViewModel
import com.example.yvonnecai.feature.logs.R
import kotlinx.coroutines.flow.filter

/** Entry point of the logs screen: connects [LogsViewModel] to the stateless [LogsScreen]. */
@Composable
fun LogsRoute(viewModel: LogsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Remembered so every list row receives the very same lambda instance across recompositions.
    val onLogClick = remember(viewModel) { { id: String -> viewModel.onLogClick(id) } }
    LogsScreen(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onClearQuery = viewModel::onClearQuery,
        onLogClick = onLogClick,
        onDetailsDismiss = viewModel::onDetailsDismiss,
        onRetry = viewModel::onRetry,
    )
}

/**
 * Stateless logs screen: pinned title and search field on top, then loading, error or content.
 * Everything arrives through [state] and callbacks, which keeps it previewable.
 */
@Composable
fun LogsScreen(
    state: LogsUiState,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onLogClick: (String) -> Unit,
    onDetailsDismiss: () -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LogsTopBar(
                state = state,
                onQueryChange = onQueryChange,
                onClearQuery = onClearQuery,
            )
        },
    ) { innerPadding ->
        when (val content = state.content) {
            LogsContent.Loading -> LoadingState(Modifier.padding(innerPadding))
            is LogsContent.Error -> ErrorState(
                reason = content.reason,
                onRetry = onRetry,
                modifier = Modifier.padding(innerPadding),
            )
            is LogsContent.Loaded -> LogsList(
                content = content,
                query = state.query,
                onLogClick = onLogClick,
                contentPadding = innerPadding,
            )
        }
    }

    (state.content as? LogsContent.Loaded)?.details?.let {
        LogDetailsSheet(details = it, onDismiss = onDetailsDismiss)
    }
}

/** Title and the single-line search field with the result count and a text Clear button. */
@Composable
private fun LogsTopBar(
    state: LogsUiState,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
) {
    val loaded = state.content as? LogsContent.Loaded
    val focusManager = LocalFocusManager.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = stringResource(R.string.logs_title), style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            // Nothing to search while loading or after a failed load.
            enabled = loaded != null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            shape = RoundedCornerShape(28.dp),
            placeholder = {
                Text(
                    text = stringResource(R.string.search_placeholder),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            trailingIcon = {
                // The Clear button brings its own horizontal padding; without it the count would sit
                // flush against the field's rounded border.
                val endPadding = if (state.query.isNotEmpty()) 4.dp else 16.dp
                Row(
                    modifier = Modifier.padding(end = endPadding),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (loaded != null) {
                        Text(
                            text = stringResource(R.string.search_result_count, loaded.matchCount, loaded.totalCount),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                    if (state.query.isNotEmpty()) {
                        TextButton(onClick = onClearQuery) {
                            Text(text = stringResource(R.string.search_clear), maxLines = 1, softWrap = false)
                        }
                    }
                }
            },
        )
    }
}

/**
 * The scrolling content: severity header card, then one sticky header plus rows per minute.
 *
 * Items use stable keys (bucket start / entry id) and content types so Compose can reuse and skip
 * them. The keyboard is dismissed as soon as the user scrolls.
 */
@Composable
private fun LogsList(
    content: LogsContent.Loaded,
    query: String,
    onLogClick: (String) -> Unit,
    contentPadding: PaddingValues,
) {
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    // Collected as a flow so the list itself does not recompose when scrolling starts or stops.
    LaunchedEffect(listState, focusManager) {
        snapshotFlow { listState.isScrollInProgress }
            .filter { it }
            .collect { focusManager.clearFocus() }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        item(key = KEY_SUMMARY, contentType = TYPE_SUMMARY) {
            SeveritySummary(
                ring = content.ring,
                severityCounts = content.severityCounts,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        if (content.groups.isEmpty()) {
            item(key = KEY_EMPTY, contentType = TYPE_EMPTY) { EmptyState(query = query) }
        }

        content.groups.forEach { group ->
            stickyHeader(key = "group-${group.key}", contentType = TYPE_HEADER) {
                LogGroupHeader(group)
            }
            items(items = group.rows, key = { it.id }, contentType = { TYPE_ROW }) { row ->
                LogRow(row = row, onClick = onLogClick)
            }
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator()
            Text(stringResource(R.string.state_loading), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ErrorState(
    reason: LogLoadException.Reason,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val message = stringResource(
        when (reason) {
            LogLoadException.Reason.NETWORK -> R.string.error_network
            LogLoadException.Reason.SERVER -> R.string.error_server
            LogLoadException.Reason.INVALID_DATA -> R.string.error_invalid_data
            LogLoadException.Reason.UNKNOWN -> R.string.error_unknown
        },
    )
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            Button(onClick = onRetry) { Text(stringResource(R.string.state_retry)) }
        }
    }
}

@Composable
private fun EmptyState(query: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.empty_title), style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(R.string.empty_body, query),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private const val KEY_SUMMARY = "summary"
private const val KEY_EMPTY = "empty"
private const val TYPE_SUMMARY = "summary"
private const val TYPE_EMPTY = "empty"
private const val TYPE_HEADER = "header"
private const val TYPE_ROW = "row"
