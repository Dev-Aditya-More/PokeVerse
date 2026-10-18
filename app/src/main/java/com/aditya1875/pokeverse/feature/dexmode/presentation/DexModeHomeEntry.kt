package com.aditya1875.pokeverse.feature.dexmode.presentation

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aditya1875.pokeverse.R

/** Pulsing red "status LED" shown next to the Dexverse title until Dex Mode is first opened. */
@Composable
fun DexModeNudgeDot(modifier: Modifier = Modifier) {
    val pulse by rememberInfiniteTransition(label = "dex_dot").animateFloat(
        initialValue = 0.75f, targetValue = 1.25f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "dex_dot_scale"
    )
    Box(
        modifier
            .size(9.dp)
            .scale(pulse)
            .clip(CircleShape)
            .background(Color(0xFFFF3B3B))
    )
}

/** One-time "your Dex has arrived" announcement shown from the Home popup queue. */
@Composable
fun DexModeAnnouncementDialog(onPowerOn: () -> Unit, onLater: () -> Unit) {
    AlertDialog(
        onDismissRequest = onLater,
        shape = RoundedCornerShape(24.dp),
        icon = { MiniDexDevice() },
        title = {
            Text(
                stringResource(R.string.dex_popup_title),
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                stringResource(R.string.dex_popup_body),
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onPowerOn,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F), contentColor = Color.White)
            ) { Text(stringResource(R.string.dex_popup_cta), fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onLater) { Text(stringResource(R.string.dex_popup_later)) } }
    )
}

/** Little red handheld with a glowing lens — the dialog's icon. */
@Composable
private fun MiniDexDevice() {
    val glow by rememberInfiniteTransition(label = "mini_dex").animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "mini_glow"
    )
    Column(
        Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFFE85A5A), Color(0xFFB71C1C))))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Canvas(Modifier.size(20.dp)) {
                val r = size.minDimension / 2f
                drawCircle(Color(0xFF7FE0FF).copy(alpha = 0.5f * glow), r, center)
                drawCircle(Color.White, r * 0.85f, center)
                drawCircle(Color(0xFF1E88E5), r * 0.65f, center)
                drawCircle(Color.White.copy(alpha = 0.7f), r * 0.18f, Offset(center.x - r * 0.25f, center.y - r * 0.25f))
            }
            listOf(Color(0xFFFF3B3B), Color(0xFFFFD600), Color(0xFF3DDC4A)).forEach {
                Box(Modifier.size(5.dp).clip(CircleShape).background(it))
            }
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(30.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFFECECEC))
                .padding(3.dp)
        ) {
            Box(Modifier.fillMaxWidth().height(24.dp).background(Color(0xFF9BBC0F)))
        }
    }
}
