package com.aditya1875.pokeverse.feature.game.chase.presentation.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sign

/**
 * Lane steering: a horizontal swipe moves one lane as soon as it passes the
 * threshold (a long swipe can move two), and a plain tap steers toward the
 * tapped half of the screen. [onMove] receives -1 (left) or +1 (right).
 */
@Composable
fun Modifier.laneSteering(onMove: (Int) -> Unit): Modifier {
    val currentOnMove by rememberUpdatedState(onMove)
    return pointerInput(Unit) {
        val threshold = 28.dp.toPx()
        awaitEachGesture {
            val down = awaitFirstDown()
            var dragX = 0f
            var swiped = false
            do {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                dragX += change.positionChange().x
                if (abs(dragX) > threshold) {
                    currentOnMove(sign(dragX).toInt())
                    dragX = 0f
                    swiped = true
                    change.consume()
                }
            } while (change.pressed)
            if (!swiped) currentOnMove(if (down.position.x < size.width / 2f) -1 else 1)
        }
    }
}
