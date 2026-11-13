package com.aditya1875.pokeverse.feature.game.core.domain

import kotlin.random.Random

/**
 * Names for the stand-in rival a quick match falls back to when nobody else is searching. They read like the
 * trainer names real players pick (a name, a handle, sometimes a number), so the match feels like a person.
 */
object RivalNames {
    private val first = listOf(
        "Ash", "Misty", "Kai", "Leo", "Mira", "Ren", "Zoe", "Nico", "Aria", "Jay", "Luna", "Theo", "Ivy", "Max",
        "Sora", "Riya", "Arjun", "Dev", "Yuki", "Hana", "Lucas", "Ana", "Gabi", "Pedro", "Chloe", "Noah", "Emma",
        "Ishaan", "Tara", "Felix", "Rohan", "Maya", "Kenji", "Lia", "Omar", "Sam", "Alex", "Nina", "Ravi", "Elio",
    )
    private val handles = listOf(
        "Trainer", "Ace", "Master", "Dex", "Gym", "Rival", "Rocket", "Shiny", "Champ", "Poke", "Ranger", "Hunter",
    )

    fun pick(random: Random = Random.Default): String {
        val name = first.random(random)
        return when (random.nextInt(6)) {
            0 -> name
            1 -> "$name${random.nextInt(1, 100)}"
            2 -> "${name.lowercase()}_${random.nextInt(10, 1000)}"
            3 -> "${handles.random(random)}$name"
            4 -> "$name${handles.random(random)}"
            else -> "${name}_${handles.random(random).lowercase()}"
        }
    }
}
