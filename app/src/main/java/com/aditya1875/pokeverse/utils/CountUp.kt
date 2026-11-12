package com.aditya1875.pokeverse.utils

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Remembers which numbers have already counted up during ONE visit to a screen.
 *
 * A number that counts up from zero is nice the first time you arrive on a screen and wasteful every time after:
 * scroll it out of a list and back and it would replay, and every replay is animation work for nothing. With this,
 * a number counts up once per visit. It is created when the screen enters composition and dropped when the screen
 * leaves, so coming back from another screen counts up again, and scrolling around on the same screen never does.
 */
class CountUpMemory {
    private val seen = HashSet<String>()

    /** True the first time [key] is asked during this visit, false after. */
    fun firstTime(key: String): Boolean = seen.add(key)
}

val LocalCountUpMemory = compositionLocalOf<CountUpMemory?> { null }

/** Wrap a screen in this so its numbers count up once per visit (see [CountUpMemory]). */
@Composable
fun ProvideCountUpMemory(content: @Composable () -> Unit) {
    val memory = remember { CountUpMemory() }
    CompositionLocalProvider(LocalCountUpMemory provides memory, content = content)
}

/**
 * A number that counts from 0 up to [target] the first time it is shown during a visit, then simply shows its value
 * when it comes back into view. A genuinely new value still animates to it. Outside a [ProvideCountUpMemory] screen
 * it always counts up, as it always did.
 */
@Composable
fun rememberCountUp(key: String, target: Int, durationMs: Int = 900): Int {
    val memory = LocalCountUpMemory.current
    val fromZero = remember { memory?.firstTime(key) ?: true }
    var goal by remember { mutableIntStateOf(if (fromZero) 0 else target) }
    LaunchedEffect(target) { goal = target }
    val value by animateIntAsState(goal, tween(durationMs, easing = FastOutSlowInEasing), label = "countUp")
    return value
}
