package com.aditya1875.pokeverse.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringArrayResource
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.leaderboard.data.remote.model.LeaderboardEntry

/**
 * Trainers who never wrote a bio still get one: a friendly line picked from a fixed set by their id, so
 * it is different from person to person and always the same for the same person.
 */
object GenericBio {
    fun pick(options: Array<String>, seed: String): String =
        if (options.isEmpty()) "" else options[Math.floorMod(seed.hashCode(), options.size)]
}

@Composable
fun rememberGenericBio(seed: String): String = GenericBio.pick(stringArrayResource(R.array.generic_bios), seed)

/** The entry as it should be shown: its own bio, or the generic one when it has none. */
fun LeaderboardEntry.withGenericBio(generic: String): LeaderboardEntry =
    if (bio.isBlank()) copy(bio = generic) else this
