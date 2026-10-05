package com.aditya1875.pokeverse.feature.game.chase.presentation.components

import androidx.compose.ui.geometry.Size
import com.aditya1875.pokeverse.feature.game.chase.domain.model.ChaseConfig

/**
 * Maps world units (lanes, screen fractions) to pixels and back. The renderer
 * and the touch input both use this, so a finger is always steering to exactly
 * the lane that's drawn under it.
 */
internal class TrackGeometry(val size: Size, val config: ChaseConfig) {
    val pathLeft = size.width * PATH_MARGIN_FRACTION
    val pathWidth = size.width - pathLeft * 2
    val laneWidth = pathWidth / config.laneCount
    val spriteSize = laneWidth * 0.78f

    val isUsable: Boolean get() = laneWidth > 0f && size.height > 0f

    fun laneX(lane: Float) = pathLeft + (lane + 0.5f) * laneWidth
    fun y(fraction: Float) = fraction * size.height

    /** Continuous lane position under screen x — e.g. 1.3 is a little right of lane 1's centre. */
    fun laneAt(x: Float): Float =
        ((x - pathLeft) / laneWidth - 0.5f).coerceIn(0f, (config.laneCount - 1).toFloat())

    private companion object {
        const val PATH_MARGIN_FRACTION = 0.07f
    }
}
