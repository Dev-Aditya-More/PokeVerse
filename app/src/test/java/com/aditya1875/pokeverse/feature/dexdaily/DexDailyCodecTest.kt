package com.aditya1875.pokeverse.feature.dexdaily

import com.aditya1875.pokeverse.feature.dexdaily.domain.DexDailyCodec
import com.aditya1875.pokeverse.feature.dexdaily.domain.DexDailyRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DexDailyCodecTest {

    private val day = "2026-11-09"

    @Test
    fun `a solve on either side stays solved`() {
        val phone = mapOf(day to DexDailyRecord(solved = false, wrongIds = listOf(7)))
        val cloud = mapOf(day to DexDailyRecord(solved = true))
        assertTrue(DexDailyCodec.merge(phone, cloud).getValue(day).solved)
        assertTrue(DexDailyCodec.merge(cloud, phone).getValue(day).solved)
    }

    @Test
    fun `a day only one side knows about is kept`() {
        val merged = DexDailyCodec.merge(
            mapOf(day to DexDailyRecord(solved = true)),
            mapOf("2026-11-08" to DexDailyRecord(solved = true)),
        )
        assertEquals(setOf(day, "2026-11-08"), merged.keys)
    }

    @Test
    fun `a live play on either side means it was not an archive play`() {
        val live = mapOf(day to DexDailyRecord(archive = false, solved = true))
        val archive = mapOf(day to DexDailyRecord(archive = true))
        assertFalse(DexDailyCodec.merge(live, archive).getValue(day).archive)
        assertTrue(DexDailyCodec.merge(archive, archive).getValue(day).archive)
    }

    @Test
    fun `the longer list of wrong guesses wins and hints cannot be shed`() {
        val a = mapOf(day to DexDailyRecord(wrongIds = listOf(1), hintsUsed = true))
        val b = mapOf(day to DexDailyRecord(wrongIds = listOf(1, 2)))
        val merged = DexDailyCodec.merge(a, b).getValue(day)
        assertEquals(listOf(1, 2), merged.wrongIds)
        assertTrue(merged.hintsUsed)
    }

    @Test
    fun `merging is stable through the stored format`() {
        val merged = DexDailyCodec.merge(
            mapOf(day to DexDailyRecord(wrongIds = listOf(3), solved = true)),
            mapOf("2026-11-01" to DexDailyRecord(archive = true)),
        )
        assertEquals(merged, DexDailyCodec.decode(DexDailyCodec.encode(merged)))
    }
}
