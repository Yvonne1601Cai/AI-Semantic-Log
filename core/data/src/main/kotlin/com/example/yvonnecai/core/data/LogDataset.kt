package com.example.yvonnecai.core.data

import com.example.yvonnecai.core.model.LogEntry

/**
 * Everything one successful load produced, kept in memory by the repository.
 *
 * @property sessionId session the payload belongs to.
 * @property entries master list, sorted newest first; positions in [index] results refer to it.
 * @property index search index parallel to [entries].
 */
class LogDataset(
    val sessionId: String,
    val entries: List<LogEntry>,
    val index: SearchIndex,
)
