package com.aditya1875.pokeverse.feature.game.battle.presentation

import com.aditya1875.pokeverse.feature.game.battle.domain.TeamRules
import androidx.compose.material.icons.filled.Star
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.compare.presentation.components.ConfettiBurst
import com.aditya1875.pokeverse.feature.core.ui.components.LoadErrorState
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleAction
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleEngine
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleEvent
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleMath
import com.aditya1875.pokeverse.feature.game.battle.domain.BattleState
import com.aditya1875.pokeverse.feature.game.battle.domain.BotTier
import com.aditya1875.pokeverse.feature.game.battle.domain.online.OnlineBattleRules
import com.aditya1875.pokeverse.feature.game.core.presentation.GameTips
import com.aditya1875.pokeverse.feature.game.core.presentation.TipCard
import com.aditya1875.pokeverse.feature.game.battle.domain.Fighter
import com.aditya1875.pokeverse.feature.game.battle.domain.Who
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPResult
import com.aditya1875.pokeverse.feature.leaderboard.presentation.components.XPOverlay
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.repository.PokemonDetailRepo
import com.aditya1875.pokeverse.utils.Haptics
import com.aditya1875.pokeverse.utils.SoundManager
import com.aditya1875.pokeverse.utils.localizedTypeName
import com.aditya1875.pokeverse.utils.pokemonTypeColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private val Gold = Color(0xFFFFC24B)
private val Ink = Color(0xFF0E1424)

private fun artworkUrl(id: Int) =
    "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$id.png"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BattleScreen(
    onBack: () -> Unit,
    onPlayWildCatch: () -> Unit,
    viewModel: BattleViewModel = koinViewModel(),
    online: OnlineBattleViewModel = koinViewModel(),
) {
    val phase by viewModel.phase.collectAsStateWithLifecycle()
    val picker by viewModel.picker.collectAsStateWithLifecycle()
    val battle by viewModel.battle.collectAsStateWithLifecycle()
    val lobby by online.lobby.collectAsStateWithLifecycle()
    val onlineBattle by online.battle.collectAsStateWithLifecycle()
    var mode by rememberSaveable { mutableStateOf(BattleMode.PRACTICE) }
    var pendingXp by remember { mutableStateOf<XPResult?>(null) }
    var showExit by remember { mutableStateOf(false) }
    var showFriend by remember { mutableStateOf(false) }
    var botNotice by remember { mutableStateOf(false) }

    val stage = lobby.stage
    val practiceFighting = phase is BattleViewModel.Phase.Battling && battle?.over == false
    val onlineFighting = stage == OnlineBattleViewModel.Stage.BATTLING && onlineBattle?.over == false
    val onlineWaiting = stage == OnlineBattleViewModel.Stage.SEARCHING || stage == OnlineBattleViewModel.Stage.PREPARING
    val fighting = practiceFighting || onlineFighting

    LaunchedEffect(Unit) { viewModel.xpResult.collect { pendingXp = it } }
    LaunchedEffect(Unit) { online.xpResult.collect { pendingXp = it } }
    // Nobody online in time: the same team goes into a practice battle, and the player is told why.
    LaunchedEffect(Unit) {
        online.fallbackToBot.collect {
            showFriend = false
            viewModel.setTier(BotTier.TRAINER)
            botNotice = true
            viewModel.start()
        }
    }
    LaunchedEffect(botNotice) {
        if (botNotice) { delay(5_000); botNotice = false }
    }
    // Close the friend sheet as soon as the room is open or joined.
    LaunchedEffect(stage) { if (stage != OnlineBattleViewModel.Stage.IDLE) showFriend = false }

    val goBack: () -> Unit = {
        when {
            fighting -> showExit = true
            onlineWaiting -> online.cancelSearch()
            stage == OnlineBattleViewModel.Stage.FAILED -> online.reset()
            else -> onBack()
        }
    }
    BackHandler(enabled = fighting || onlineWaiting || stage == OnlineBattleViewModel.Stage.FAILED, onBack = goBack)

    XPOverlay(result = pendingXp, onDismiss = { pendingXp = null }) {
        Scaffold(
            containerColor = Ink,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.game_name_battle_title), color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = goBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (stage) {
                    OnlineBattleViewModel.Stage.SEARCHING -> OnlineSearching(lobby, onCancel = online::cancelSearch)
                    OnlineBattleViewModel.Stage.PREPARING -> OnlinePreparing(lobby)
                    OnlineBattleViewModel.Stage.FAILED -> OnlineFailed(lobby, onBack = online::reset)
                    OnlineBattleViewModel.Stage.BATTLING -> onlineBattle?.let { state ->
                        BattleArena(
                            state = state,
                            host = online,
                            onRematch = { online.reset(); online.quickMatch(picker.selected) },
                            onChangeTeam = { online.reset(); mode = BattleMode.ONLINE },
                            onBack = { online.reset(); onBack() },
                        )
                    }

                    OnlineBattleViewModel.Stage.IDLE -> when (val p = phase) {
                        BattleViewModel.Phase.Picking -> PickerContent(
                            picker = picker,
                            mode = mode,
                            lobby = lobby,
                            onMode = { mode = it },
                            onToggle = viewModel::toggle,
                            onTier = viewModel::setTier,
                            onStart = viewModel::start,
                            onPlayWildCatch = onPlayWildCatch,
                            onFindMatch = { online.quickMatch(picker.selected) },
                            onFriend = { showFriend = true },
                            onLoadTeam = viewModel::loadTeam,
                            onSaveTeam = viewModel::saveCurrentTeam,
                            onDeleteTeam = viewModel::deleteTeam,
                            onQuickFill = viewModel::quickFill,
                            onClearTeam = viewModel::clearTeam,
                        )

                        BattleViewModel.Phase.Preparing -> Column(
                            Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = Gold)
                            Spacer(Modifier.height(16.dp))
                            Text(stringResource(R.string.battle_scouting), color = Color.White.copy(alpha = 0.8f))
                            Spacer(Modifier.height(24.dp))
                            TipCard(tips = remember { GameTips.battle(online = false) }, modifier = Modifier.padding(horizontal = 24.dp))
                        }

                        is BattleViewModel.Phase.Failed -> LoadErrorState(
                            kind = p.kind,
                            onRetry = viewModel::retry,
                            onBack = viewModel::backToPicker,
                        )

                        BattleViewModel.Phase.Battling -> battle?.let { state ->
                            BattleArena(
                                state = state,
                                host = viewModel,
                                onRematch = viewModel::start,
                                onChangeTeam = viewModel::backToPicker,
                                onBack = onBack,
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = botNotice,
                    enter = fadeIn(tween(200)),
                    exit = fadeOut(tween(300)),
                    modifier = Modifier.align(Alignment.TopCenter).padding(12.dp),
                ) { BotNotice() }
            }
        }
    }

    if (showFriend) {
        FriendRoomSheet(
            code = lobby.enteredCode,
            errorRes = lobby.errorRes,
            busy = lobby.busy,
            onCodeChange = online::setCode,
            onCreate = { online.createFriendRoom(picker.selected) },
            onJoin = { online.joinFriendRoom(picker.selected) },
            onDismiss = { showFriend = false },
        )
    }

    if (showExit) {
        AlertDialog(
            onDismissRequest = { showExit = false },
            title = { Text(stringResource(if (onlineFighting) R.string.battle_online_exit_title else R.string.dialog_exit_game_title)) },
            text = { Text(stringResource(if (onlineFighting) R.string.battle_online_exit_body else R.string.dialog_exit_game_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showExit = false
                    // Leaving an online battle forfeits it, so tell the room before the screen goes.
                    if (onlineFighting) online.leave()
                    onBack()
                }) {
                    Text(stringResource(R.string.quiz_exit_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showExit = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

// ── Team picker ─────────────────────────────────────────────────────────────────

@Composable
private fun PickerContent(
    picker: BattleViewModel.Picker,
    mode: BattleMode,
    lobby: OnlineBattleViewModel.Lobby,
    onMode: (BattleMode) -> Unit,
    onToggle: (Int) -> Unit,
    onTier: (BotTier) -> Unit,
    onStart: () -> Unit,
    onPlayWildCatch: () -> Unit,
    onFindMatch: () -> Unit,
    onFriend: () -> Unit,
    onLoadTeam: (com.aditya1875.pokeverse.feature.game.battle.data.SavedTeam) -> Unit,
    onSaveTeam: (String) -> Unit,
    onDeleteTeam: (com.aditya1875.pokeverse.feature.game.battle.data.SavedTeam) -> Unit,
    onQuickFill: () -> Unit,
    onClearTeam: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(104.dp),
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ModeSwitch(mode = mode, onMode = onMode)
                    Text(
                        stringResource(R.string.battle_pick_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        stringResource(R.string.battle_pick_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    // The team collection: load a whole saved team in one tap, or build and save the one picked now.
                    SavedTeamsSection(
                        saved = picker.saved,
                        roster = picker.caught.toSet(),
                        selected = picker.selected,
                        activeName = picker.activeTeam,
                        onLoad = onLoadTeam,
                        onSave = onSaveTeam,
                        onDelete = onDeleteTeam,
                        onQuickFill = onQuickFill,
                        onClear = onClearTeam,
                    )
                    // How many Legendaries are on the team, and why a tap was refused.
                    if (picker.legendaries > 0 || picker.legendaryNotice) {
                        Text(
                            if (picker.legendaryNotice) stringResource(R.string.battle_legendary_limit, TeamRules.MAX_LEGENDARIES)
                            else stringResource(R.string.battle_legendary_count, picker.legendaries, TeamRules.MAX_LEGENDARIES),
                            color = if (picker.legendaryNotice) Color(0xFFFFB74D) else Gold.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (mode == BattleMode.PRACTICE) {
                        Text(
                            stringResource(R.string.battle_tier_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        BotTier.entries.forEach { tier ->
                            TierCard(tier = tier, selected = picker.tier == tier, onClick = { onTier(tier) })
                        }
                    } else {
                        OnlineIntroCard()
                        lobby.errorRes?.let {
                            Text(stringResource(it), color = Color(0xFFFF8A80), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            items(picker.caught, key = { it }) { id ->
                val order = picker.selected.indexOf(id)
                val legendary = TeamRules.isLegendary(id)
                PokemonTile(
                    id = id,
                    order = if (order >= 0) order + 1 else null,
                    legendary = legendary,
                    // Out of Legendary spots: those tiles dim, the rest stay normal.
                    locked = legendary && order < 0 && picker.legendaries >= TeamRules.MAX_LEGENDARIES,
                    onClick = { onToggle(id) }
                )
            }
            if (picker.caught.size < BattleViewModel.MAX_TEAM) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            stringResource(R.string.battle_pick_more),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                        OutlinedButton(onClick = onPlayWildCatch) { Text(stringResource(R.string.battle_play_wild)) }
                    }
                }
            }
        }
        if (mode == BattleMode.PRACTICE) {
            Button(
                onClick = onStart,
                enabled = picker.selected.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().padding(16.dp).height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black)
            ) {
                Text(stringResource(R.string.battle_start), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
            }
        } else {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onFindMatch,
                    enabled = picker.selected.isNotEmpty() && !lobby.busy,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black)
                ) {
                    Text(stringResource(R.string.battle_online_find), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                }
                OutlinedButton(
                    onClick = onFriend,
                    enabled = picker.selected.isNotEmpty() && !lobby.busy,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Gold.copy(alpha = if (picker.selected.isNotEmpty()) 0.8f else 0.25f)),
                ) {
                    Text(stringResource(R.string.battle_online_friend), color = Gold, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** One difficulty level: its name, what the bot is like, and the XP bonus for beating it. */
@Composable
private fun TierCard(tier: BotTier, selected: Boolean, onClick: () -> Unit) {
    val (name, desc) = when (tier) {
        BotTier.ROOKIE -> R.string.battle_tier_rookie to R.string.battle_tier_rookie_desc
        BotTier.TRAINER -> R.string.battle_tier_trainer to R.string.battle_tier_trainer_desc
        BotTier.CHAMPION -> R.string.battle_tier_champion to R.string.battle_tier_champion_desc
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Gold.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.06f))
            .border(BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Gold else Color.White.copy(alpha = 0.18f)), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(name), color = Color.White, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleSmall)
            if (tier.xpBonus > 0) {
                Text(stringResource(R.string.battle_tier_bonus, tier.xpBonus), color = Gold, style = MaterialTheme.typography.labelMedium)
            }
        }
        Text(stringResource(desc), color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun PokemonTile(id: Int, order: Int?, legendary: Boolean, locked: Boolean, onClick: () -> Unit) {
    val repo: PokemonDetailRepo = koinInject()
    // The name is fetched lazily, and the HTTP cache makes a revisit free.
    val name by produceState<String?>(null, id) {
        value = runCatching { repo.getPokemonByName(id.toString()).name }.getOrNull()
    }
    val selected = order != null
    Column(
        modifier = Modifier
            .graphicsLayer { alpha = if (locked) 0.4f else 1f }
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Gold.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.06f))
            .border(BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Gold else Color.White.copy(alpha = 0.14f)), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            AsyncImage(model = artworkUrl(id), contentDescription = name, modifier = Modifier.size(84.dp))
            if (order != null) {
                Box(
                    Modifier.align(Alignment.TopEnd).size(22.dp).background(Gold, CircleShape),
                    contentAlignment = Alignment.Center
                ) { Text("$order", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp) }
            }
            // A small gold star marks a Legendary or Mythical, which count towards the team's limit.
            if (legendary) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = stringResource(R.string.legendary),
                    tint = Gold,
                    modifier = Modifier.align(Alignment.TopStart).size(16.dp)
                )
            }
        }
        Text(
            name?.replace('-', ' ')?.replaceFirstChar { it.uppercase() } ?: "#%03d".format(id),
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun ResultCard(
    won: Boolean,
    turns: Int,
    shards: Int = 0,
    shardsCapped: Boolean = false,
    hud: ArenaHud = ArenaHud(),
    rivalName: String = "",
    onRematch: () -> Unit,
    onChangeTeam: () -> Unit,
    onBack: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.72f)), contentAlignment = Alignment.Center) {
        if (won) ConfettiBurst(visible = true)
        Card(
            modifier = Modifier.fillMaxWidth(0.88f),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    stringResource(if (won) R.string.battle_victory else R.string.battle_defeat),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = if (won) Gold else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    when {
                        hud.iWasDisqualified -> stringResource(R.string.battle_online_dq_you, OnlineBattleRules.MAX_MISSED_TURNS)
                        hud.opponentDisqualified -> stringResource(R.string.battle_online_dq_rival, rivalName, OnlineBattleRules.MAX_MISSED_TURNS)
                        hud.online && won && hud.opponentLeft -> stringResource(R.string.battle_online_won_left, rivalName)
                        hud.online && won -> stringResource(R.string.battle_online_victory_body, rivalName, turns)
                        hud.online -> stringResource(R.string.battle_online_defeat_body, rivalName)
                        won -> stringResource(R.string.battle_victory_body, turns)
                        else -> stringResource(R.string.battle_defeat_body)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                com.aditya1875.pokeverse.feature.evolution.presentation.ShardGainRow(gained = shards, capped = shardsCapped)
                if (hud.xpGained > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Gold, modifier = Modifier.size(16.dp))
                        Text(
                            stringResource(R.string.battle_online_xp, hud.xpGained),
                            color = Gold, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                if (hud.rewardsLimited) {
                    Text(
                        stringResource(R.string.battle_online_rewards_limited),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onRematch,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black)
                ) { Text(stringResource(if (hud.online) R.string.battle_online_again else R.string.battle_rematch), fontWeight = FontWeight.ExtraBold) }
                OutlinedButton(onClick = onChangeTeam, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.battle_change_team))
                }
                TextButton(onClick = onBack) { Text(stringResource(R.string.action_go_back)) }
            }
        }
    }
}
