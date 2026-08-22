package com.aditya1875.pokeverse.feature.facematch.domain

import kotlin.random.Random

/**
 * Fun heuristic matching, not real face recognition — scores every Pokémon in the pool by
 * color closeness (primary signal) plus a bonus for matching face-shape bucket (secondary,
 * lighter-weight tiebreaker), then picks among the closest few with some randomness so
 * re-scanning the same face doesn't always land on the identical result.
 */
object FaceMatcher {

    // Shape mismatch costs less than a very close color match ever would (max possible color
    // distance is 3.0 — three maxed-out 0..1 channel differences), so color always dominates.
    private const val SHAPE_MISMATCH_PENALTY = 0.15f

    private fun colorDistanceSquared(a: RgbColor, b: RgbColor): Float {
        val dr = a.r - b.r
        val dg = a.g - b.g
        val db = a.b - b.b
        return dr * dr + dg * dg + db * db
    }

    fun rankMatches(
        skinColor: RgbColor,
        faceShape: FaceShape,
        pool: List<PokemonLookalike> = PokemonLookalikeData.all
    ): List<PokemonLookalike> {
        require(pool.isNotEmpty()) { "Lookalike pool must not be empty" }
        return pool.sortedBy { candidate ->
            val colorScore = colorDistanceSquared(skinColor, candidate.representativeColor)
            val shapePenalty = if (candidate.shape == faceShape) 0f else SHAPE_MISMATCH_PENALTY
            colorScore + shapePenalty
        }
    }

    /** Weighted pick among the top few ranked matches — mostly the best match, occasionally a
     * close runner-up, so the feature stays a little surprising on repeat tries. */
    fun pickMatch(
        skinColor: RgbColor,
        faceShape: FaceShape,
        pool: List<PokemonLookalike> = PokemonLookalikeData.all
    ): PokemonLookalike {
        val ranked = rankMatches(skinColor, faceShape, pool)
        val topCandidates = ranked.take(3)
        val weights = listOf(0.6f, 0.25f, 0.15f).take(topCandidates.size)
        val totalWeight = weights.sum()
        var roll = Random.nextFloat() * totalWeight
        for (i in topCandidates.indices) {
            roll -= weights[i]
            if (roll <= 0f) return topCandidates[i]
        }
        return topCandidates.first()
    }
}
