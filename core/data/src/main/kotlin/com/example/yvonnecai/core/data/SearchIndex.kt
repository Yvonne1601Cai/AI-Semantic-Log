package com.example.yvonnecai.core.data

import com.example.yvonnecai.core.model.LogEntry

/**
 * Search index built once when the dataset loads.
 *
 * It keeps one pre-lowercased haystack per entry (`message tag severity session id`) in an array
 * parallel to the master entry list, so a keystroke costs one `String.contains` per term per
 * entry and allocates nothing per entry. A result is an [IntArray] of positions in the master
 * list, never copies of entries (5,000 entries is small enough that no inverted index is needed;
 * that would only add build time and memory).
 *
 * The entry id is included on top of the descriptive fields so an id copied from the details sheet
 * can be pasted straight into search. The session id is the same for every entry today but is
 * part of the haystack so the index stays correct if the payload ever mixes sessions.
 */
class SearchIndex private constructor(private val haystacks: Array<String>) {

    /** Number of indexed entries. */
    val size: Int get() = haystacks.size

    /**
     * Returns the positions (ascending) of every entry matching [query].
     *
     * Matching is case-insensitive. The query is split on whitespace and **every** term must be
     * found, so `network timeout` narrows the result instead of widening it. A blank query matches
     * everything.
     */
    fun search(query: String): IntArray {
        val terms = query.trim().lowercase().split(WHITESPACE).filter { it.isNotEmpty() }
        if (terms.isEmpty()) return IntArray(size) { it }

        val matches = IntArray(size)
        var count = 0
        for (position in haystacks.indices) {
            val haystack = haystacks[position]
            if (terms.all { haystack.contains(it) }) matches[count++] = position
        }
        return matches.copyOf(count)
    }

    companion object {
        private val WHITESPACE = Regex("\\s+")

        /** Builds the index for [entries]; positions in search results refer to this list. */
        fun build(sessionId: String, entries: List<LogEntry>): SearchIndex =
            SearchIndex(
                Array(entries.size) { i ->
                    val entry = entries[i]
                    "${entry.message} ${entry.tag} ${entry.severity.name} $sessionId ${entry.id}".lowercase()
                },
            )
    }
}
