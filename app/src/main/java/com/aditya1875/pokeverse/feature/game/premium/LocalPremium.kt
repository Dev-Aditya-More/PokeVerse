package com.aditya1875.pokeverse.feature.game.premium

import androidx.compose.runtime.compositionLocalOf

/**
 * True only while billing has CONFIRMED premium. The app shell resolves it once from
 * IBillingManager and provides it here, so premium-only UI (Living Aura, …) reads
 * `LocalPremium.current` instead of each screen re-checking billing. Loading/Pending
 * count as false, so a perk never flashes on and then off.
 */
val LocalPremium = compositionLocalOf { false }
