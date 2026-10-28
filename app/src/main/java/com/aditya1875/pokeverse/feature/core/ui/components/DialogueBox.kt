package com.aditya1875.pokeverse.feature.core.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val DialogueFill = Color(0xFF101626)

/** Characters revealed so far for one line of dialogue. Tapping the box calls [skip]. */
class TypewriterState(val text: String) {
    var shown by mutableIntStateOf(0)
    val typing: Boolean get() = shown < text.length
    fun skip() {
        shown = text.length
    }
}

/** Types [text] out one character at a time; a new string restarts it. */
@Composable
fun rememberTypewriter(text: String, charDelayMs: Long = 24L): TypewriterState {
    val state = remember(text) { TypewriterState(text) }
    LaunchedEffect(text) {
        while (state.typing) {
            delay(charDelayMs)
            state.shown++
        }
    }
    return state
}

/**
 * The Pokémon-game text box: white frame, dark fill, monospace text typed out by [typewriter],
 * and a ▼ when the line is done. Reserves the line's final height up front so the box doesn't
 * grow while typing.
 */
@Composable
fun DialogueBox(
    typewriter: TypewriterState,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(DialogueFill)
            .border(BorderStroke(3.dp, Color.White.copy(alpha = 0.85f)), shape)
            .border(BorderStroke(6.dp, accent.copy(alpha = 0.35f)), shape)
            .clickable(enabled = typewriter.typing) { typewriter.skip() }
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Text(
            text = typewriter.text,
            color = Color.Transparent,
            fontFamily = FontFamily.Monospace,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            modifier = Modifier.fillMaxWidth().padding(bottom = 22.dp),
        )
        Text(
            text = typewriter.text.take(typewriter.shown),
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            modifier = Modifier.fillMaxWidth(),
        )
        if (!typewriter.typing) {
            Text(
                "▼",
                color = accent.readableOnDark(),
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
    }
}

/** Text-field colours for the always-dark intro screens, whatever app theme is active. */
@Composable
fun darkFieldColors(accent: Color) = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedLabelColor = accent,
    unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
    focusedBorderColor = accent,
    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
    cursorColor = accent,
    focusedSupportingTextColor = Color.White.copy(alpha = 0.6f),
    unfocusedSupportingTextColor = Color.White.copy(alpha = 0.6f),
)

private val IntroInk = Color(0xFF0B0E17)

/**
 * Backdrop for the always-dark intro screens. The solid base matters: a gradient that fades through a
 * semi-transparent colour lets whatever is behind the window (a light theme, the previous screen)
 * bleed through, which made the text unreadable. The accent tint is layered on top of the opaque base.
 */
fun Modifier.introBackground(accent: Color): Modifier =
    background(IntroInk)
        .background(Brush.verticalGradient(listOf(Color.Transparent, accent.copy(alpha = 0.22f), Color.Transparent)))

/** Text/icon colour that stays legible on top of a solid [this] fill (e.g. a button in the accent colour). */
fun Color.onAccent(): Color = if (luminance() > 0.45f) Color.Black else Color.White

/** The accent, lifted if it is too dark to read as text on the dark intro background. */
fun Color.readableOnDark(): Color = if (luminance() < 0.25f) lerp(this, Color.White, 0.5f) else this

/**
 * The intro screens are always dark, so status/navigation bar icons must be light whatever the app
 * theme is, or they vanish into the background. Restored when the screen leaves.
 */
@Composable
fun ForceDarkSystemBars() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view as? DialogWindowProvider)?.window
            ?: (view.parent as? DialogWindowProvider)?.window
            ?: view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val wasLightStatus = controller?.isAppearanceLightStatusBars
        val wasLightNav = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            if (controller != null) {
                wasLightStatus?.let { controller.isAppearanceLightStatusBars = it }
                wasLightNav?.let { controller.isAppearanceLightNavigationBars = it }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
