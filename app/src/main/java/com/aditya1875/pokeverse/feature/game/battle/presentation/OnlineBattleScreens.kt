package com.aditya1875.pokeverse.feature.game.battle.presentation

import androidx.compose.material.icons.filled.Star
import com.aditya1875.pokeverse.feature.core.ui.components.ShardGem
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.core.ui.components.LoadErrorState
import com.aditya1875.pokeverse.feature.game.battle.domain.online.OnlineBattleRules
import com.aditya1875.pokeverse.feature.game.core.presentation.GameTips
import com.aditya1875.pokeverse.feature.game.core.presentation.TipCard

private val Gold = Color(0xFFFFC24B)
private val Ink = Color(0xFF0E1424)
private val Mint = Color(0xFF69F0AE)

private fun art(id: Int) =
    "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$id.png"

/** Practice (against the bot) or Online (against another trainer). Same team, same arena. */
internal enum class BattleMode { PRACTICE, ONLINE }

/** The two-way switch at the top of the team picker. Built by hand so it matches the gold-on-ink style. */
@Composable
internal fun ModeSwitch(mode: BattleMode, onMode: (BattleMode) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)), RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ModeOption(R.string.battle_mode_practice, Icons.Default.SmartToy, mode == BattleMode.PRACTICE, Modifier.weight(1f)) {
            onMode(BattleMode.PRACTICE)
        }
        ModeOption(R.string.battle_mode_online, Icons.Default.Public, mode == BattleMode.ONLINE, Modifier.weight(1f)) {
            onMode(BattleMode.ONLINE)
        }
    }
}

@Composable
private fun ModeOption(
    label: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Gold else Color.Transparent)
            .semantics { this.selected = selected; role = Role.Tab }
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) Color.Black else Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            stringResource(label),
            color = if (selected) Color.Black else Color.White.copy(alpha = 0.85f),
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}

/** What Online mode is, and what it pays, shown where the bot difficulty cards are in Practice. */
@Composable
internal fun OnlineIntroCard(modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Gold.copy(alpha = 0.12f))
            .border(BorderStroke(1.dp, Gold.copy(alpha = 0.5f)), RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.battle_online_title), color = Gold, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.battle_online_desc), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RewardPill(stringResource(R.string.battle_online_pill_shards)) { ShardGem(12.dp, Gold) }
            RewardPill(stringResource(R.string.battle_online_pill_xp)) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Gold, modifier = Modifier.size(13.dp))
            }
        }
        Text(
            stringResource(R.string.battle_online_bot_note, OnlineBattleRules.QUEUE_SECONDS),
            color = Color.White.copy(alpha = 0.65f),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun RewardPill(text: String, icon: @Composable () -> Unit) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(Gold.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        icon()
        Text(text, color = Gold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

// ── Waiting for a rival ─────────────────────────────────────────────────────────────────────────────

/** Searching for a trainer (quick match) or waiting for a friend (room code). */
@Composable
internal fun OnlineSearching(lobby: OnlineBattleViewModel.Lobby, onCancel: () -> Unit) {
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Radar(partnerId = lobby.team.firstOrNull())

        when {
            lobby.friendRoom && lobby.roomCode.isNotEmpty() -> {
                Text(
                    stringResource(R.string.battle_online_waiting_friend),
                    color = Color.White, fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(R.string.battle_online_waiting_friend_sub),
                    color = Color.White.copy(alpha = 0.72f), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
                RoomCodeCard(lobby.roomCode, onCopy = {
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    clipboard?.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.battle_online_room_code), lobby.roomCode))
                }, onShare = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, context.getString(R.string.battle_online_share_text, lobby.roomCode))
                    }
                    context.startActivity(Intent.createChooser(send, null))
                })
            }
            lobby.busy || lobby.roomCode.isEmpty() && lobby.friendRoom -> {
                Text(
                    stringResource(R.string.battle_online_connecting),
                    color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge,
                )
            }
            else -> {
                Text(
                    stringResource(R.string.battle_online_searching),
                    color = Color.White, fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(R.string.battle_online_searching_sub),
                    color = Color.White.copy(alpha = 0.72f), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (lobby.secondsLeft > 0) BotCountdown(lobby.secondsLeft)
            }
        }

        TeamStrip(lobby.team)

        TipCard(tips = androidx.compose.runtime.remember { GameTips.battle(online = true) })

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
        ) { Text(stringResource(R.string.battle_online_cancel), color = Color.White, fontWeight = FontWeight.SemiBold) }
    }
}

/** Gold rings spreading out from your lead Pokémon, so waiting feels like something is happening. */
@Composable
private fun Radar(partnerId: Int?) {
    val t = rememberInfiniteTransition(label = "radar")
    val a by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart), label = "r1")
    val b by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart, androidx.compose.animation.core.StartOffset(800)), label = "r2")
    val c by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart, androidx.compose.animation.core.StartOffset(1600)), label = "r3")
    Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            listOf(a, b, c).forEach { p ->
                drawCircle(
                    color = Gold.copy(alpha = (1f - p) * 0.55f),
                    radius = size.minDimension / 2f * (0.35f + 0.65f * p),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx()),
                )
            }
        }
        Box(
            Modifier.size(104.dp).clip(CircleShape).background(Gold.copy(alpha = 0.18f))
                .border(BorderStroke(2.dp, Gold), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (partnerId != null) AsyncImage(art(partnerId), contentDescription = null, modifier = Modifier.size(84.dp))
        }
    }
}

@Composable
private fun BotCountdown(secondsLeft: Int) {
    val urgent = secondsLeft <= 10
    val color = if (urgent) Gold else Color.White.copy(alpha = 0.7f)
    Row(
        Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.12f))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.4f)), CircleShape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Default.SmartToy, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(stringResource(R.string.battle_online_bot_in, secondsLeft), color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TeamStrip(team: List<Int>) {
    if (team.isEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        team.forEach { id ->
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.07f))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) { AsyncImage(art(id), contentDescription = null, modifier = Modifier.size(44.dp)) }
        }
    }
}

@Composable
private fun RoomCodeCard(code: String, onCopy: () -> Unit, onShare: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .border(BorderStroke(1.dp, Gold.copy(alpha = 0.5f)), RoundedCornerShape(20.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.battle_online_room_code), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelLarge)
        Text(
            code,
            color = Gold, fontWeight = FontWeight.Black, fontSize = 38.sp, letterSpacing = 8.sp,
            modifier = Modifier.semantics { contentDescription = code.toList().joinToString(" ") },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FilledTonalButton(onClick = onCopy, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.battle_online_copy))
            }
            FilledTonalButton(onClick = onShare, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.battle_online_share))
            }
        }
    }
}

// ── A trainer was found ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun OnlinePreparing(lobby: OnlineBattleViewModel.Lobby) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(R.string.battle_online_found),
            color = Mint, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall,
        )
        lobby.opponentName?.let {
            Spacer(Modifier.height(6.dp))
            Text(
                it, color = Color.White, fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(24.dp))
        TeamStrip(lobby.team)
        Spacer(Modifier.height(24.dp))
        CircularProgressIndicator(color = Gold)
        Spacer(Modifier.height(14.dp))
        Text(stringResource(R.string.battle_online_found_sub), color = Color.White.copy(alpha = 0.75f), textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        TipCard(tips = androidx.compose.runtime.remember { GameTips.battle(online = true) })
    }
}

@Composable
internal fun OnlineFailed(lobby: OnlineBattleViewModel.Lobby, onBack: () -> Unit) {
    val kind = lobby.loadError
    if (kind != null) {
        LoadErrorState(kind = kind, onRetry = onBack, onBack = onBack)
        return
    }
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(R.string.battle_online_failed_title),
            color = Color.White, fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center,
        )
        lobby.errorRes?.let {
            Spacer(Modifier.height(10.dp))
            Text(stringResource(it), color = Color.White.copy(alpha = 0.75f), textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black),
        ) { Text(stringResource(R.string.battle_online_back_to_team), fontWeight = FontWeight.ExtraBold) }
    }
}

// ── Play with a friend ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FriendRoomSheet(
    code: String,
    errorRes: Int?,
    busy: Boolean,
    onCodeChange: (String) -> Unit,
    onCreate: () -> Unit,
    onJoin: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Ink,
        contentColor = Color.White,
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.GroupAdd, contentDescription = null, tint = Gold)
                Text(stringResource(R.string.battle_online_friend_title), fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
            }
            Text(
                stringResource(R.string.battle_online_friend_desc),
                color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = onCreate,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black),
            ) { Text(stringResource(R.string.battle_online_create_room), fontWeight = FontWeight.ExtraBold) }

            Text(stringResource(R.string.battle_online_enter_code), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = code,
                    onValueChange = onCodeChange,
                    placeholder = { Text(stringResource(R.string.battle_online_code_placeholder), color = Color.White.copy(alpha = 0.35f)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (code.length == 6) onJoin() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Gold, unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        cursorColor = Gold,
                    ),
                )
                OutlinedButton(
                    onClick = onJoin,
                    enabled = code.length == 6 && !busy,
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (code.length == 6) Gold else Color.White.copy(alpha = 0.2f)),
                ) { Text(stringResource(R.string.battle_online_join), color = if (code.length == 6) Gold else Color.White.copy(alpha = 0.4f), fontWeight = FontWeight.Bold) }
            }
            errorRes?.let {
                Text(stringResource(it), color = Color(0xFFFF8A80), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** A short-lived note on top of the screen, e.g. when a quick match found nobody and a bot stepped in. */
@Composable
internal fun BotNotice(modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Ink.copy(alpha = 0.96f))
            .border(BorderStroke(1.dp, Gold.copy(alpha = 0.6f)), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Default.SmartToy, contentDescription = null, tint = Gold, modifier = Modifier.size(20.dp))
        Text(stringResource(R.string.battle_online_bot_notice), color = Color.White, style = MaterialTheme.typography.bodyMedium)
    }
}
