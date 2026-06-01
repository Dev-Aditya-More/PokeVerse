package com.aditya1875.pokeverse.feature.game.pokequiz.data

import com.aditya1875.pokeverse.feature.game.pokequiz.domain.model.QuizCategory
import com.aditya1875.pokeverse.feature.game.pokequiz.domain.model.QuizDifficulty
import com.aditya1875.pokeverse.feature.game.pokequiz.domain.model.QuizQuestion
import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.PokemonResponse
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.repository.PokemonDetailRepo
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class DynamicQuizRepository(private val repo: PokemonDetailRepo) {

    private val allTypes = listOf(
        "Normal", "Fire", "Water", "Electric", "Grass", "Ice",
        "Fighting", "Poison", "Ground", "Flying", "Psychic", "Bug",
        "Rock", "Ghost", "Dragon", "Dark", "Steel", "Fairy"
    )

    // Pool of ability names used only as distractors (wrong answers)
    private val abilityPool = listOf(
        "overgrow", "blaze", "torrent", "pressure", "static", "intimidate", "levitate",
        "swift-swim", "chlorophyll", "huge-power", "guts", "shed-skin", "soundproof",
        "adaptability", "wonder-guard", "battle-armor", "keen-eye", "pickup", "run-away",
        "synchronize", "natural-cure", "serene-grace", "water-absorb", "volt-absorb",
        "flash-fire", "shield-dust", "sturdy", "rock-head", "lightning-rod", "early-bird",
        "marvel-scale", "inner-focus", "magnet-pull", "compound-eyes", "tinted-lens",
        "magic-guard", "hyper-cutter", "drought", "drizzle", "sand-stream", "snow-warning",
        "speed-boost", "moxie", "multiscale", "unaware", "regenerator", "magic-bounce",
        "prankster", "protean", "gale-wings", "thick-fat", "pure-power", "shadow-tag",
        "arena-trap", "trace", "skill-link", "iron-fist", "tough-claws", "pixilate",
        "aerilate", "refrigerate", "galvanize", "sand-force", "analytic", "hustle",
        "no-guard", "steadfast", "gluttony", "anger-point", "unburden", "heatproof",
        "simple", "dry-skin", "download", "poison-heal", "anticipation", "forewarn",
        "solar-power", "bad-dreams", "solid-rock", "frisk", "reckless", "scrappy",
        "storm-drain", "ice-body", "honey-gather", "moody", "immunity", "own-tempo",
        "oblivious", "cloud-nine", "color-change", "stench", "effect-spore", "clear-body",
        "liquid-ooze", "rain-dish", "sand-veil", "sand-rush", "thick-fat", "flame-body"
    ).distinct()

    // Used for HARD weakness questions — single-type Pokémon only to guarantee accuracy
    private val typeWeaknessMap = mapOf(
        "normal"   to listOf("Fighting"),
        "fire"     to listOf("Water", "Ground", "Rock"),
        "water"    to listOf("Electric", "Grass"),
        "electric" to listOf("Ground"),
        "grass"    to listOf("Fire", "Ice", "Poison", "Flying", "Bug"),
        "ice"      to listOf("Fire", "Fighting", "Rock", "Steel"),
        "fighting" to listOf("Flying", "Psychic", "Fairy"),
        "poison"   to listOf("Ground", "Psychic"),
        "ground"   to listOf("Water", "Grass", "Ice"),
        "flying"   to listOf("Electric", "Ice", "Rock"),
        "psychic"  to listOf("Bug", "Ghost", "Dark"),
        "bug"      to listOf("Fire", "Flying", "Rock"),
        "rock"     to listOf("Water", "Grass", "Fighting", "Ground", "Steel"),
        "ghost"    to listOf("Ghost", "Dark"),
        "dragon"   to listOf("Ice", "Dragon", "Fairy"),
        "dark"     to listOf("Fighting", "Bug", "Fairy"),
        "steel"    to listOf("Fire", "Fighting", "Ground"),
        "fairy"    to listOf("Poison", "Steel")
    )

    suspend fun generateQuestions(difficulty: QuizDifficulty): List<QuizQuestion> =
        try {
            when (difficulty) {
                QuizDifficulty.EASY   -> easyQuestions(difficulty.questionCount)
                QuizDifficulty.MEDIUM -> mediumQuestions(difficulty.questionCount)
                QuizDifficulty.HARD   -> hardQuestions(difficulty.questionCount)
            }
        } catch (_: Exception) {
            emptyList()
        }

    // ── EASY: primary type OR ability questions for Gen-1 Pokémon ─────────────
    private suspend fun easyQuestions(count: Int): List<QuizQuestion> {
        val batch = fetchBatch(maxId = 151, needed = count + 6)
        return batch.mapIndexed { i, p ->
            if (i % 2 == 0) primaryTypeQuestion(p, id = 20_000 + p.id, difficulty = QuizDifficulty.EASY)
            else             abilityQuestion(p,     id = 21_000 + p.id, difficulty = QuizDifficulty.EASY)
        }.filterNotNull().take(count)
    }

    // ── MEDIUM: generation / secondary-type / ability for Gen 1-4 Pokémon ─────
    private suspend fun mediumQuestions(count: Int): List<QuizQuestion> {
        val batch = fetchBatch(maxId = 493, needed = count + 8)
        return batch.mapIndexed { i, p ->
            when (i % 3) {
                0    -> generationQuestion(p, id = 30_000 + p.id)
                1    -> secondaryTypeQuestion(p, id = 31_000 + p.id, difficulty = QuizDifficulty.MEDIUM)
                         ?: abilityQuestion(p, id = 31_500 + p.id, difficulty = QuizDifficulty.MEDIUM)
                else -> abilityQuestion(p,    id = 32_000 + p.id, difficulty = QuizDifficulty.MEDIUM)
            }
        }.filterNotNull().take(count)
    }

    // ── HARD: hidden ability / secondary-type / type-weakness for Gen 1-6 ─────
    // Weakness questions are restricted to single-type Pokémon to avoid incorrect results
    // from dual-type immunities and resistances.
    private suspend fun hardQuestions(count: Int): List<QuizQuestion> {
        val batch = fetchBatch(maxId = 721, needed = count + 10)
        return batch.mapIndexed { i, p ->
            when (i % 3) {
                0    -> hiddenAbilityQuestion(p, id = 40_000 + p.id)
                         ?: secondaryTypeQuestion(p, id = 40_500 + p.id, difficulty = QuizDifficulty.HARD)
                         ?: abilityQuestion(p,       id = 40_800 + p.id, difficulty = QuizDifficulty.HARD)
                1    -> secondaryTypeQuestion(p, id = 41_000 + p.id, difficulty = QuizDifficulty.HARD)
                         ?: hiddenAbilityQuestion(p, id = 41_500 + p.id)
                         ?: abilityQuestion(p,       id = 41_800 + p.id, difficulty = QuizDifficulty.HARD)
                else -> typeWeaknessQuestion(p, id = 42_000 + p.id)
                         ?: secondaryTypeQuestion(p, id = 42_500 + p.id, difficulty = QuizDifficulty.HARD)
                         ?: abilityQuestion(p,       id = 42_800 + p.id, difficulty = QuizDifficulty.HARD)
            }
        }.filterNotNull().take(count)
    }

    // ── Question builders ─────────────────────────────────────────────────────

    private fun primaryTypeQuestion(p: PokemonResponse, id: Int, difficulty: QuizDifficulty): QuizQuestion? {
        val type = p.types.firstOrNull()?.type?.name?.cap() ?: return null
        val wrong = allTypes.filter { it != type }.shuffled().take(3)
        val options = (listOf(type) + wrong).shuffled()
        return QuizQuestion(
            id = id,
            question = "What type is ${p.name.formatName()}?",
            options = options,
            correctAnswerIndex = options.indexOf(type),
            difficulty = difficulty,
            category = QuizCategory.TYPES,
            explanation = "${p.name.formatName()} is a $type-type Pokémon."
        )
    }

    private fun abilityQuestion(p: PokemonResponse, id: Int, difficulty: QuizDifficulty): QuizQuestion? {
        val correctAbility = p.abilities.firstOrNull { !it.is_hidden }?.ability?.name
            ?: p.abilities.firstOrNull()?.ability?.name ?: return null
        val pokemonAbilities = p.abilities.map { it.ability.name }.toSet()
        val display = correctAbility.formatName()
        val wrong = abilityPool
            .filter { it !in pokemonAbilities }
            .shuffled().take(3)
            .map { it.formatName() }
        if (wrong.size < 3) return null
        val options = (listOf(display) + wrong).shuffled()
        return QuizQuestion(
            id = id,
            question = "Which ability does ${p.name.formatName()} have?",
            options = options,
            correctAnswerIndex = options.indexOf(display),
            difficulty = difficulty,
            category = QuizCategory.ABILITIES,
            explanation = "${p.name.formatName()}'s ability is $display."
        )
    }

    private fun generationQuestion(p: PokemonResponse, id: Int): QuizQuestion {
        val gen = generationOf(p.id)
        val allGens = listOf("Generation I", "Generation II", "Generation III", "Generation IV")
        val wrong = allGens.filter { it != gen }.shuffled().take(3)
        val options = (listOf(gen) + wrong).shuffled()
        return QuizQuestion(
            id = id,
            question = "Which generation is ${p.name.formatName()} from?",
            options = options,
            correctAnswerIndex = options.indexOf(gen),
            difficulty = QuizDifficulty.MEDIUM,
            category = QuizCategory.REGIONS,
            explanation = "${p.name.formatName()} was introduced in $gen."
        )
    }

    private fun secondaryTypeQuestion(p: PokemonResponse, id: Int, difficulty: QuizDifficulty): QuizQuestion? {
        if (p.types.size < 2) return null
        val primary   = p.types[0].type.name.cap()
        val secondary = p.types[1].type.name.cap()
        val wrong = allTypes.filter { it != secondary }.shuffled().take(3)
        val options = (listOf(secondary) + wrong).shuffled()
        return QuizQuestion(
            id = id,
            question = "What is ${p.name.formatName()}'s secondary type?",
            options = options,
            correctAnswerIndex = options.indexOf(secondary),
            difficulty = difficulty,
            category = QuizCategory.TYPES,
            explanation = "${p.name.formatName()} is a $primary/$secondary type."
        )
    }

    private fun hiddenAbilityQuestion(p: PokemonResponse, id: Int): QuizQuestion? {
        val hidden = p.abilities.find { it.is_hidden }?.ability?.name ?: return null
        val pokemonAbilities = p.abilities.map { it.ability.name }.toSet()
        val display = hidden.formatName()
        val wrong = abilityPool
            .filter { it !in pokemonAbilities }
            .shuffled().take(3)
            .map { it.formatName() }
        if (wrong.size < 3) return null
        val options = (listOf(display) + wrong).shuffled()
        return QuizQuestion(
            id = id,
            question = "What is ${p.name.formatName()}'s hidden ability?",
            options = options,
            correctAnswerIndex = options.indexOf(display),
            difficulty = QuizDifficulty.HARD,
            category = QuizCategory.ABILITIES,
            explanation = "${p.name.formatName()}'s hidden ability is $display."
        )
    }

    private fun typeWeaknessQuestion(p: PokemonResponse, id: Int): QuizQuestion? {
        if (p.types.size != 1) return null  // dual-type weaknesses can be cancelled — skip
        val primary    = p.types[0].type.name
        val weaknesses = typeWeaknessMap[primary] ?: return null
        val correct    = weaknesses.random()
        val wrong      = allTypes.filter { it != correct && it !in weaknesses }.shuffled().take(3)
        if (wrong.size < 3) return null
        val options = (listOf(correct) + wrong).shuffled()
        return QuizQuestion(
            id = id,
            question = "Which type is super effective against ${p.name.formatName()}?",
            options = options,
            correctAnswerIndex = options.indexOf(correct),
            difficulty = QuizDifficulty.HARD,
            category = QuizCategory.TYPES,
            explanation = "${p.name.formatName()} is ${primary.cap()}-type, which is weak to $correct-type moves."
        )
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private suspend fun fetchBatch(maxId: Int, needed: Int): List<PokemonResponse> = coroutineScope {
        (1..maxId).shuffled().take(needed).map { id ->
            async {
                try { repo.getPokemonByName(id.toString()) } catch (_: Exception) { null }
            }
        }.awaitAll().filterNotNull()
    }

    private fun generationOf(id: Int) = when {
        id <= 151 -> "Generation I"
        id <= 251 -> "Generation II"
        id <= 386 -> "Generation III"
        id <= 493 -> "Generation IV"
        id <= 649 -> "Generation V"
        else      -> "Generation VI"
    }

    private fun String.formatName() =
        split("-").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

    private fun String.cap() =
        replaceFirstChar { it.uppercase() }
}
