package com.aditya1875.pokeverse.feature.game.battle.domain

import com.aditya1875.pokeverse.utils.PokemonIndex

/**
 * Which Pokémon the team picker shows. Pure, so the matching rules are tested without a screen.
 *
 *  - [query] matches a name ("char", "mr mime", "mr-mime") or a dex number ("25", "#25"); blank matches all.
 *  - [type] keeps only Pokémon of that type (either of their types); null means every type.
 * Both must hold. The order of [ids] is kept.
 */
object PickerFilter {

    /** Lower-case letters and digits only, so "Mr. Mime", "mr-mime" and "mrmime" all compare equal. */
    fun normalise(text: String): String = text.lowercase().filter { it.isLetterOrDigit() }

    fun apply(ids: List<Int>, query: String, type: String?, index: Map<Int, PokemonIndex.Entry>): List<Int> {
        val q = normalise(query)
        if (q.isEmpty() && type == null) return ids
        val numeric = q.isNotEmpty() && q.all { it.isDigit() }
        return ids.filter { id ->
            val entry = index[id]
            val typeOk = type == null || (entry != null && type in entry.types)
            val queryOk = when {
                q.isEmpty() -> true
                numeric -> id.toString().startsWith(q.trimStart('0').ifEmpty { "0" })
                else -> entry != null && normalise(entry.name).contains(q)
            }
            typeOk && queryOk
        }
    }

    /** The types present among [ids], in the usual type order, so the chip row never offers an empty filter. */
    fun typesPresent(ids: List<Int>, index: Map<Int, PokemonIndex.Entry>, order: List<String>): List<String> {
        val present = HashSet<String>()
        for (id in ids) index[id]?.types?.let(present::addAll)
        return order.filter { it in present }
    }
}
