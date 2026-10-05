package com.aditya1875.pokeverse.feature.game.chase.presentation.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import com.aditya1875.pokeverse.feature.game.chase.domain.model.ChaseConfig
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * How far past the halfway line between two lanes the finger must travel before
 * Pikachu commits to the next lane. Stops jitter when a finger rests near a boundary.
 */
private const val LANE_HYSTERESIS = 0.18f

/**
 * Direct steering: Pikachu heads for the lane under the finger, and follows it as
 * the finger drags. Every new touch also reports [onPress] (used to struggle out
 * of a net). [onSteer] gets a lane index.
 */
@Composable
fun Modifier.laneSteering(
    config: ChaseConfig,
    onPress: () -> Unit,
    onSteer: (Int) -> Unit
): Modifier {
    val currentOnPress by rememberUpdatedState(onPress)
    val currentOnSteer by rememberUpdatedState(onSteer)
    return pointerInput(config) {
        awaitEachGesture {
            val down = awaitFirstDown()
            currentOnPress()
            val track = TrackGeometry(Size(size.width.toFloat(), size.height.toFloat()), config)
            if (!track.isUsable) return@awaitEachGesture

            var lane = track.laneAt(down.position.x).roundToInt()
            currentOnSteer(lane)
            while (true) {
                val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break
                val fingerLane = track.laneAt(change.position.x)
                if (abs(fingerLane - lane) > 0.5f + LANE_HYSTERESIS) {
                    lane = fingerLane.roundToInt()
                    currentOnSteer(lane)
                }
                change.consume()
            }
        }
    }
}
