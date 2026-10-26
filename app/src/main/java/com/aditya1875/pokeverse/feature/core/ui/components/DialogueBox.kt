package com.aditya1875.pokeverse.feature.core.ui.components

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
                color = accent,
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
