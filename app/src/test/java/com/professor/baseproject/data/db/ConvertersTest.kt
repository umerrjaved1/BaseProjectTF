package com.professor.baseproject.data.db

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure-JVM round-trip test for the Room type converter. Worth having because a converter
 * that silently returns an empty list on malformed input is indistinguishable from a
 * column that was never written.
 */
class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `round trips a normal list`() {
        val input = listOf("alpha", "beta", "gamma")
        assertEquals(input, converters.toStringList(converters.fromStringList(input)))
    }

    @Test
    fun `round trips an empty list`() {
        assertEquals(emptyList<String>(), converters.toStringList(converters.fromStringList(emptyList())))
    }

    @Test
    fun `null in becomes empty out, in both directions`() {
        assertEquals(emptyList<String>(), converters.toStringList(null))
        // A null list must still serialise to valid JSON, not the string "null".
        assertEquals(emptyList<String>(), converters.toStringList(converters.fromStringList(null)))
    }

    @Test
    fun `blank column value yields an empty list`() {
        assertEquals(emptyList<String>(), converters.toStringList(""))
        assertEquals(emptyList<String>(), converters.toStringList("   "))
    }

    @Test
    fun `malformed json degrades to an empty list instead of throwing`() {
        assertEquals(emptyList<String>(), converters.toStringList("{not valid json"))
    }

    @Test
    fun `preserves values containing separators and unicode`() {
        val input = listOf("a,b", "c\"d", "e\\f", "地図", "")
        assertEquals(input, converters.toStringList(converters.fromStringList(input)))
    }
}
