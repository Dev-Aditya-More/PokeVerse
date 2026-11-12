package com.aditya1875.pokeverse.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CountUpMemoryTest {

    @Test
    fun `a number counts up the first time it is asked and never again in the same visit`() {
        val visit = CountUpMemory()
        assertTrue(visit.firstTime("xp"))
        assertFalse("scrolled back into view", visit.firstTime("xp"))
        assertFalse(visit.firstTime("xp"))
    }

    @Test
    fun `each number has its own turn`() {
        val visit = CountUpMemory()
        assertTrue(visit.firstTime("xp"))
        assertTrue(visit.firstTime("games"))
        assertFalse(visit.firstTime("games"))
    }

    @Test
    fun `a new visit to the screen counts up again`() {
        val first = CountUpMemory()
        assertTrue(first.firstTime("xp"))
        // The screen was left and entered again: a fresh memory, so the numbers play once more.
        val second = CountUpMemory()
        assertTrue(second.firstTime("xp"))
    }
}
