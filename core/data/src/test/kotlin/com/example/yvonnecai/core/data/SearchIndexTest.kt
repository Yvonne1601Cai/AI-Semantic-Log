package com.example.yvonnecai.core.data

import com.example.yvonnecai.core.model.Severity
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchIndexTest {

    private val entries = listOf(
        entry(id = "a1", message = "Connection timed out", tag = "network", severity = Severity.ERROR),
        entry(id = "b2", message = "Cache miss", tag = "cache", severity = Severity.WARN),
        entry(id = "c3", message = "Inference complete", tag = "neural_engine", severity = Severity.INFO),
    )
    private val index = SearchIndex.build("session-666", entries)

    private fun search(query: String) = index.search(query).toList()

    @Test
    fun `blank query matches every entry in order`() {
        assertEquals(listOf(0, 1, 2), search(""))
        assertEquals(listOf(0, 1, 2), search("   "))
    }

    @Test
    fun `matching is case-insensitive`() {
        assertEquals(listOf(1), search("CACHE MISS"))
    }

    @Test
    fun `partial words match`() {
        assertEquals(listOf(0), search("timed"))
        assertEquals(listOf(2), search("infer"))
    }

    @Test
    fun `tag severity id and session are searchable`() {
        assertEquals(listOf(2), search("neural"))
        assertEquals(listOf(0), search("error"))
        assertEquals(listOf(1), search("b2"))
        assertEquals(listOf(0, 1, 2), search("session-666"))
    }

    @Test
    fun `every term must match`() {
        assertEquals(listOf(0), search("network timed"))
        assertEquals(emptyList<Int>(), search("network cache"))
    }

    @Test
    fun `extra whitespace between terms is ignored`() {
        assertEquals(listOf(0), search("  network    timed  "))
    }

    @Test
    fun `no match returns an empty result`() {
        assertEquals(emptyList<Int>(), search("zzz"))
    }

    @Test
    fun `results are ascending positions in the master list`() {
        val many = (0 until 10).map { entry(id = "id-$it", message = "disk ${if (it % 2 == 0) "full" else "slow"}") }
        val result = SearchIndex.build("s", many).search("full")
        assertArrayEquals(intArrayOf(0, 2, 4, 6, 8), result)
    }

    @Test
    fun `size equals the number of indexed entries`() {
        assertEquals(3, index.size)
    }

    @Test
    fun `searches 5000 entries correctly`() {
        val big = (0 until 5_000).map { i ->
            entry(id = "id-$i", message = if (i % 5 == 0) "Invalid token" else "User logged in")
        }
        assertEquals(1_000, SearchIndex.build("s", big).search("invalid token").size)
    }
}
