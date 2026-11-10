package com.aditya1875.pokeverse.feature.challenges.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya1875.pokeverse.feature.challenges.data.ChallengeRepository
import com.aditya1875.pokeverse.feature.challenges.domain.BallReward
import com.aditya1875.pokeverse.feature.challenges.domain.Challenge
import com.aditya1875.pokeverse.feature.challenges.domain.ChallengeProgress
import com.aditya1875.pokeverse.feature.challenges.domain.ChallengeRules
import com.aditya1875.pokeverse.feature.pokemon.dex.PokedexRepository
import com.aditya1875.pokeverse.feature.progression.data.ProgressionRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One row: a challenge, how far it has got and whether its reward is waiting or taken. */
data class ChallengeRow(
    val challenge: Challenge,
    val progress: Int,
    val claimed: Boolean,
) {
    val complete: Boolean get() = progress >= challenge.target
    val claimable: Boolean get() = complete && !claimed
}

data class ChallengesUi(
    val daily: List<ChallengeRow> = emptyList(),
    /** Today's all-done bonus: shown once every daily is claimed. */
    val sweepReady: Boolean = false,
    val sweepClaimed: Boolean = false,
    val milestones: List<ChallengeRow> = emptyList(),
    val progress: ChallengeProgress = ChallengeProgress(),
    val loaded: Boolean = false,
) {
    /** Rewards waiting to be taken, for the badge on the Home card. */
    val waiting: Int get() = daily.count { it.claimable } + milestones.count { it.claimable } + if (sweepReady) 1 else 0
    val dailyDone: Int get() = daily.count { it.claimed }
}

class ChallengesViewModel(
    private val challenges: ChallengeRepository,
    private val pokedex: PokedexRepository,
    private val progression: ProgressionRepository,
) : ViewModel() {

    val ui: StateFlow<ChallengesUi> = combine(challenges.today, pokedex.progress, progression.state) { today, dex, wallet ->
        val progress = today.copy(dexCaught = dex.caught.size, dexTiers = dex.tierCounts)
        val claimed = wallet.challengesClaimed
        val day = progress.day
        ChallengesUi(
            daily = ChallengeRules.dailyFor(day).map { ChallengeRow(it, ChallengeRules.progressOf(it, progress), it.id in claimed) },
            sweepReady = ChallengeRules.sweepReady(day, claimed),
            sweepClaimed = ChallengeRules.sweepId(day) in claimed,
            milestones = ChallengeRules.nextMilestones(claimed, progress)
                .map { ChallengeRow(it, ChallengeRules.progressOf(it, progress), false) },
            progress = progress,
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChallengesUi())

    private val _paid = MutableSharedFlow<BallReward>(extraBufferCapacity = 4)
    /** Fires with the balls that were just paid, for the "you got" toast. */
    val paid: SharedFlow<BallReward> = _paid

    fun claim(row: ChallengeRow) {
        if (!row.claimable) return
        viewModelScope.launch {
            val r = row.challenge.reward
            if (progression.claimChallenge(row.challenge.id, r.great, r.ultra)) _paid.emit(r)
        }
    }

    fun claimSweep() {
        viewModelScope.launch {
            val state = ui.value
            if (!state.sweepReady) return@launch
            val r = ChallengeRules.SWEEP_BONUS
            if (progression.claimChallenge(ChallengeRules.sweepId(state.progress.day), r.great, r.ultra)) _paid.emit(r)
        }
    }

    /** Takes everything that is ready, in one tap. */
    fun claimAll() {
        viewModelScope.launch {
            var total = BallReward()
            val state = ui.value
            for (row in state.daily + state.milestones) {
                if (!row.claimable) continue
                val r = row.challenge.reward
                if (progression.claimChallenge(row.challenge.id, r.great, r.ultra)) total += r
            }
            // The bonus opens once the dailies are claimed, so it is re-read from the wallet after the loop.
            val day = state.progress.day
            if (ChallengeRules.sweepReady(day, progression.current().challengesClaimed)) {
                val r = ChallengeRules.SWEEP_BONUS
                if (progression.claimChallenge(ChallengeRules.sweepId(day), r.great, r.ultra)) total += r
            }
            if (!total.isEmpty) _paid.emit(total)
        }
    }
}
