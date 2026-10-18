package com.aditya1875.pokeverse.feature.widget.streak

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aditya1875.pokeverse.R

/**
 * "Put your streak on your home screen?" — the only way most people ever find the
 * widget. When the launcher can't pin widgets, it explains the manual steps instead.
 */
@Composable
fun StreakWidgetPromptDialog(
    streakDays: Int,
    pinSupported: Boolean,
    onAdd: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                stringResource(R.string.widget_prompt_title),
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                StreakWidgetPreview(streakDays)
                Text(
                    stringResource(R.string.widget_prompt_body),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!pinSupported) ManualSteps()
            }
        },
        confirmButton = {
            if (pinSupported) {
                Button(onClick = onAdd, shape = RoundedCornerShape(12.dp)) {
                    Text(stringResource(R.string.widget_prompt_add), fontWeight = FontWeight.Bold)
                }
            } else {
                Button(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                    Text(stringResource(R.string.ad_unavailable_ok), fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = if (pinSupported) {
            { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_not_now)) } }
        } else null
    )
}

/** Shown when the launcher can't add widgets for us (or the system sheet was refused). */
@Composable
fun StreakWidgetManualStepsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text(stringResource(R.string.widget_manual_title), fontWeight = FontWeight.Black) },
        text = { ManualSteps() },
        confirmButton = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                Text(stringResource(R.string.ad_unavailable_ok), fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun ManualSteps() {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(R.string.widget_manual_step_1, R.string.widget_manual_step_2, R.string.widget_manual_step_3)
                .forEachIndexed { i, step ->
                    Text(
                        "${i + 1}. ${stringResource(step)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
        }
    }
}

/**
 * A Compose look-alike of the real widget (happy Pikachu + this user's streak), so the
 * ask shows exactly what they'd get. Drawn here rather than reusing the widget's
 * layer-list background: Compose's painterResource can't load layer-list drawables.
 */
@Composable
private fun StreakWidgetPreview(streakDays: Int) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, Color(0x55FFC24B)),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(Color(0xFF101A14), Color(0xFF14241A), Color(0xFF2E2208))))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                Image(
                    painter = painterResource(R.drawable.widget_pika_classic),
                    contentDescription = null,
                    modifier = Modifier.size(72.dp)
                )
                Text("✨", fontSize = 14.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    stringResource(R.string.widget_streak_title),
                    color = Color(0xFFFFC24B),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔥", fontSize = 22.sp)
                    Spacer(Modifier.width(4.dp))
                    Text("$streakDays", color = Color(0xFFFFF8EC), fontSize = 30.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        pluralStringResource(R.plurals.widget_streak_days, if (streakDays == 0) 2 else streakDays),
                        color = Color(0xFFB7B0C4),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    stringResource(R.string.widget_streak_safe),
                    color = Color(0xFF6EE7A0),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
