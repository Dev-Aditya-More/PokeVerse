package com.aditya1875.pokeverse.feature.team

import com.aditya1875.pokeverse.feature.team.data.sync.CollectionMerge
import com.aditya1875.pokeverse.feature.team.data.sync.CollectionState
import com.aditya1875.pokeverse.feature.team.data.sync.SyncBattleTeam
import com.aditya1875.pokeverse.feature.team.data.sync.SyncFav
import com.aditya1875.pokeverse.feature.team.data.sync.SyncMember
import com.aditya1875.pokeverse.feature.team.data.sync.SyncTeam
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectionMergeTest {

    private val now = 1_800_000_000_000L
    private fun m(vararg names: String) = names.map { SyncMember(it, "", 1L) }
    private fun team(id: String, at: Long, vararg members: String, default: Boolean = false, name: String = id) =
        SyncTeam(id, name, 1L, default, m(*members), at)

    @Test fun `a fresh install's empty default team never wipes the account's`() {
        val phone = CollectionMerge.track(null, listOf(team("local", 0, default = true)), emptyList(), now)
        val account = CollectionState(teams = listOf(team("cloud", 500, "pikachu", "eevee", default = true)))
        val merged = CollectionMerge.merge(phone, account, now)
        assertEquals(1, merged.teams.count { it.isDefault })
        assertEquals(listOf("pikachu", "eevee"), merged.teams.single { it.isDefault }.members.map { it.name })
    }

    @Test fun `teams and favourites from both sides are kept`() {
        val a = CollectionState(listOf(team("d", 0, default = true), team("rain", 10, "kyogre")), listOf(SyncFav("mew", "", 5)))
        val b = CollectionState(listOf(team("d2", 0, default = true), team("sun", 20, "groudon")), listOf(SyncFav("lugia", "", 6)))
        val merged = CollectionMerge.merge(a, b, now)
        assertEquals(setOf("rain", "sun"), merged.teams.filter { !it.isDefault }.map { it.id }.toSet())
        assertEquals(setOf("mew", "lugia"), merged.favourites.map { it.name }.toSet())
    }

    @Test fun `the newer edit of a team wins`() {
        val old = CollectionState(listOf(team("t", 10, "a")))
        val new = CollectionState(listOf(team("t", 20, "a", "b")))
        assertEquals(listOf("a", "b"), CollectionMerge.merge(old, new, now).teams.single { it.id == "t" }.members.map { it.name })
        assertEquals(listOf("a", "b"), CollectionMerge.merge(new, old, now).teams.single { it.id == "t" }.members.map { it.name })
    }

    @Test fun `a deletion is not undone by a phone that still has the old copy`() {
        val before = CollectionState(listOf(team("d", 0, default = true), team("t", 10, "a")), listOf(SyncFav("mew", "", 5)))
        val afterDelete = CollectionMerge.track(before, listOf(team("d", 0, default = true)), emptyList(), now)
        val merged = CollectionMerge.merge(afterDelete, before, now)
        assertTrue(merged.teams.none { it.id == "t" })
        assertTrue(merged.favourites.isEmpty())
    }

    @Test fun `adding something again after deleting it sticks`() {
        val deleted = CollectionState(deleted = mapOf(CollectionMerge.favKey("mew") to now - 100))
        val readded = CollectionState(favourites = listOf(SyncFav("mew", "", now)))
        assertEquals(listOf("mew"), CollectionMerge.merge(deleted, readded, now).favourites.map { it.name })
    }

    @Test fun `tracking stamps edited and new teams, keeps untouched ones`() {
        val prev = CollectionState(listOf(team("a", 10, "x"), team("b", 10, "y")))
        val next = CollectionMerge.track(prev, listOf(team("a", 0, "x"), team("b", 0, "y", "z"), team("c", 0)), emptyList(), now)
        assertEquals(10L, next.teams.single { it.id == "a" }.updatedAt)
        assertEquals(now, next.teams.single { it.id == "b" }.updatedAt)
        assertEquals(now, next.teams.single { it.id == "c" }.updatedAt)
    }

    @Test fun `old tombstones are forgotten`() {
        val s = CollectionState(deleted = mapOf("fav:x" to now - CollectionMerge.TOMBSTONE_TTL_MS - 1))
        assertTrue(CollectionMerge.merge(s, CollectionState(), now).deleted.isEmpty())
    }

    private fun preset(name: String, at: Long, vararg ids: Int, format: String = "QUICK_3") =
        SyncBattleTeam(format, name, ids.toList(), at)

    @Test fun `battle presets combine per format and the newer edit wins`() {
        val a = CollectionState(battleTeams = listOf(preset("Rain", 10, 1, 2, 3), preset("Sun", 5, 4, 5, 6)))
        val b = CollectionState(battleTeams = listOf(preset("rain", 20, 7, 8, 9), preset("Sun", 5, 4, 5, 6, format = "QUICK_6")))
        val merged = CollectionMerge.merge(a, b, now).battleTeams
        assertEquals(listOf(7, 8, 9), merged.single { it.key == "QUICK_3:rain" }.ids)
        assertEquals(3, merged.size)
    }

    @Test fun `a deleted battle preset stays deleted`() {
        val before = CollectionState(battleTeams = listOf(preset("Rain", 10, 1, 2, 3)))
        val after = CollectionMerge.track(before, emptyList(), emptyList(), now, emptyList())
        assertTrue(CollectionMerge.merge(after, before, now).battleTeams.isEmpty())
    }

    @Test fun `presets on a phone that was never tracked lose to the account's version`() {
        val phone = CollectionMerge.track(null, emptyList(), emptyList(), now, listOf(preset("Rain", 0, 1, 2, 3)))
        val account = CollectionState(battleTeams = listOf(preset("Rain", 50, 4, 5, 6)))
        assertEquals(listOf(4, 5, 6), CollectionMerge.merge(phone, account, now).battleTeams.single().ids)
    }
}
