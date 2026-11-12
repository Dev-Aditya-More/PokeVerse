package com.aditya1875.pokeverse.feature.game

import com.aditya1875.pokeverse.feature.game.battle.domain.PickerFilter
import com.aditya1875.pokeverse.utils.PokemonIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PickerFilterTest {

    private val index = mapOf(
        6 to PokemonIndex.Entry("charizard", listOf("fire", "flying")),
        25 to PokemonIndex.Entry("pikachu", listOf("electric")),
        122 to PokemonIndex.Entry("mr-mime", listOf("psychic", "fairy")),
        150 to PokemonIndex.Entry("mewtwo", listOf("psychic")),
        4 to PokemonIndex.Entry("charmander", listOf("fire")),
    )
    private val roster = listOf(4, 6, 25, 122, 150)

    @Test
    fun `no query and no type shows everything in order`() {
        assertEquals(roster, PickerFilter.apply(roster, "", null, index))
        assertEquals(roster, PickerFilter.apply(roster, "   ", null, index))
    }

    @Test
    fun `a name matches any part of it, ignoring case`() {
        assertEquals(listOf(4, 6), PickerFilter.apply(roster, "CHAR", null, index))
        assertEquals(listOf(25), PickerFilter.apply(roster, "kachu", null, index))
    }

    @Test
    fun `spaces, dots and hyphens do not matter`() {
        assertEquals(listOf(122), PickerFilter.apply(roster, "mr mime", null, index))
        assertEquals(listOf(122), PickerFilter.apply(roster, "Mr. Mime", null, index))
        assertEquals(listOf(122), PickerFilter.apply(roster, "mrmime", null, index))
    }

    @Test
    fun `a number matches the dex number, with or without a hash or zeros`() {
        assertEquals(listOf(25), PickerFilter.apply(roster, "25", null, index))
        assertEquals(listOf(25), PickerFilter.apply(roster, "#25", null, index))
        assertEquals(listOf(25), PickerFilter.apply(roster, "025", null, index))
    }

    @Test
    fun `a type matches either of a Pokemon's types`() {
        assertEquals(listOf(4, 6), PickerFilter.apply(roster, "", "fire", index))
        assertEquals(listOf(6), PickerFilter.apply(roster, "", "flying", index))
        assertEquals(listOf(122, 150), PickerFilter.apply(roster, "", "psychic", index))
    }

    @Test
    fun `name and type must both match`() {
        assertEquals(listOf(6), PickerFilter.apply(roster, "chariz", "fire", index))
        assertEquals(emptyList<Int>(), PickerFilter.apply(roster, "pikachu", "fire", index))
    }

    @Test
    fun `a Pokemon missing from the index is kept when unfiltered and hidden by a type`() {
        val withUnknown = roster + 9999
        assertTrue(9999 in PickerFilter.apply(withUnknown, "", null, index))
        assertTrue(9999 !in PickerFilter.apply(withUnknown, "", "fire", index))
    }

    @Test
    fun `only types the roster has are offered, in the usual order`() {
        val order = listOf("normal", "fire", "water", "electric", "psychic", "flying", "fairy")
        assertEquals(listOf("fire", "electric", "psychic", "flying", "fairy"), PickerFilter.typesPresent(roster, index, order))
        assertEquals(emptyList<String>(), PickerFilter.typesPresent(emptyList(), index, order))
    }
}

class PokemonIndexTest {

    @Test
    fun `a damaged entry is skipped, the rest survive`() {
        val parsed = PokemonIndex.parse("""{"1":{"n":"bulbasaur","t":["grass","poison"]},"x":{"n":"bad"},"2":"oops","3":{"t":["fire"]}}""")
        assertEquals(setOf(1), parsed.keys)
        assertEquals(PokemonIndex.Entry("bulbasaur", listOf("grass", "poison")), parsed[1])
    }

    @Test
    fun `the bundled asset covers every Pokemon with one or two types`() {
        // Unit tests run from the module folder, so the real asset can be read straight from the source tree.
        val parsed = PokemonIndex.parse(File("src/main/assets/pokemon_index.json").readText())
        assertEquals(1025, parsed.size)
        for (id in 1..1025) {
            val entry = parsed[id]!!
            assertTrue("#$id has a name", entry.name.isNotBlank())
            assertTrue("#$id has one or two types", entry.types.size in 1..2)
        }
        assertEquals(listOf("fire", "flying"), parsed[6]!!.types)
        assertEquals("mr-mime", parsed[122]!!.name)
    }
}
