package com.aditya1875.pokeverse.feature.game.battle.data

import com.aditya1875.pokeverse.feature.game.battle.domain.BattleMath
import com.aditya1875.pokeverse.feature.game.battle.domain.Fighter
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.repository.PokemonDetailRepo

/**
 * Builds a battle-ready [Fighter] from PokeAPI's base stats, plus its base-stat total (used to match
 * strength against bots). Deterministic from the id alone: in an online battle both phones build the same
 * fighter from the same id, which is why only ids ever travel between them.
 */
suspend fun PokemonDetailRepo.loadFighter(id: Int): Pair<Fighter, Int> {
    val p = getPokemonByName(id.toString())
    fun base(name: String) = p.stats.firstOrNull { it.stat.name == name }?.base_stat ?: 50
    val stats = BattleMath.statsFromBase(
        hp = base("hp"), atk = base("attack"), def = base("defense"),
        spa = base("special-attack"), spd = base("special-defense"), spe = base("speed"),
    )
    val sprite = p.sprites.other?.officialArtwork?.frontDefault ?: p.sprites.front_default.orEmpty()
    val fighter = Fighter(
        id = p.id,
        name = p.name.replace('-', ' ').replaceFirstChar { it.uppercase() },
        types = p.types.map { it.type.name },
        stats = stats,
        spriteUrl = sprite,
    )
    return fighter to p.stats.sumOf { it.base_stat }
}
