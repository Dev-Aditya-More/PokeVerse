package com.aditya1875.pokeverse.feature.leaderboard.presentation.screens

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.inbox.presentation.screens.InboxSheet
import com.aditya1875.pokeverse.feature.inbox.presentation.viewmodels.InboxViewModel
import com.aditya1875.pokeverse.feature.leaderboard.data.remote.model.LeaderboardEntry
import com.aditya1875.pokeverse.feature.leaderboard.data.repository.LeaderboardState
import com.aditya1875.pokeverse.feature.leaderboard.presentation.components.GuestLeaderboardLocked
import com.aditya1875.pokeverse.feature.leaderboard.presentation.components.RankCelebrationDialog
import com.aditya1875.pokeverse.feature.leaderboard.presentation.viewmodels.LeaderboardType
import com.aditya1875.pokeverse.feature.leaderboard.presentation.viewmodels.LeaderboardViewModel
import com.aditya1875.pokeverse.feature.pokemon.profile.presentation.viewmodels.ProfileViewModel
import com.aditya1875.pokeverse.presentation.auth.AuthState
import com.aditya1875.pokeverse.utils.ScreenStateManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    viewModel: LeaderboardViewModel = koinViewModel(),
    profileViewModel: ProfileViewModel = koinViewModel(),
    inboxViewModel: InboxViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val pullState = rememberPullToRefreshState()
    val authState by profileViewModel.authState.collectAsStateWithLifecycle()
    val type by viewModel.type.collectAsStateWithLifecycle()
    val lastWeekEntries by viewModel.lastWeekEntries.collectAsStateWithLifecycle()
    val lastWeekOf by viewModel.lastWeekOf.collectAsStateWithLifecycle()
    val lastWeekLoading by viewModel.lastWeekLoading.collectAsStateWithLifecycle()
    val unreadCount by inboxViewModel.unreadCount.collectAsStateWithLifecycle()
    var showInbox by remember { mutableStateOf(false) }

    if (authState !is AuthState.Authenticated) {
        GuestLeaderboardLocked()
        return
    }

    if (showInbox) {
        InboxSheet(onDismiss = { showInbox = false })
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (type == LeaderboardType.LAST_WEEK) {
                if (lastWeekLoading) LeaderboardSkeleton()
                else LastWeekList(
                    entries = lastWeekEntries,
                    weekOf = lastWeekOf,
                    type = type,
                    onTypeChange = { viewModel.switchType(it) },
                    unreadCount = unreadCount,
                    onBellClick = { showInbox = true }
                )
            } else when (val s = state) {
                is LeaderboardState.Loading -> LeaderboardSkeleton()
                is LeaderboardState.Error -> LeaderboardError(s.message) { viewModel.load() }
                is LeaderboardState.Success -> {

                    if (s.entries.isEmpty() && type == LeaderboardType.WEEKLY) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🏆", fontSize = 48.sp)
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    stringResource(R.string.leaderboard_weekly_empty_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    stringResource(R.string.leaderboard_weekly_empty_subtitle),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        return@Scaffold
                    }

                    val context = LocalContext.current
                    var lastCelebratedRank by remember { mutableIntStateOf(-1) }
                    var showDialog by remember { mutableStateOf(false) }
                    var rank by remember { mutableIntStateOf(0) }

                    LaunchedEffect(Unit) {
                        lastCelebratedRank = ScreenStateManager.getLastCelebratedRank(context)
                    }

                    LaunchedEffect(s.userEntry?.previousRank, type) {
                        if (type != LeaderboardType.WEEKLY) return@LaunchedEffect
                        val user = s.userEntry ?: return@LaunchedEffect
                        val lastReset = user.lastWeeklyReset ?: return@LaunchedEffect
                        val lastSeenReset = ScreenStateManager.getLastSeenReset(context)
                        val isNewWeek = lastReset > lastSeenReset

                        if (isNewWeek) {
                            viewModel.saveLastWeekSnapshot(lastReset, s.entries)
                            ScreenStateManager.setLastSeenReset(context, lastReset)

                            if (user.previousRank in 1..3) {
                                rank = user.previousRank
                                showDialog = true
                            }
                            if (user.previousRank in 1..10) {
                                inboxViewModel.sendTopRankMessage(
                                    rank = user.previousRank,
                                    displayName = user.displayName.split(" ").firstOrNull() ?: "Trainer"
                                )
                            }
                        }
                    }

                    if (showDialog && type == LeaderboardType.WEEKLY) {
                        RankCelebrationDialog(
                            rank = rank,
                            displayName = s.userEntry?.displayName?.split(" ")?.firstOrNull() ?: "",
                            onDismiss = { showDialog = false }
                        )
                    }

                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refresh() },
                        state = pullState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LeaderboardList(
                            type = type,
                            onTypeChange = { selectedType -> viewModel.switchType(selectedType) },
                            entries = s.entries,
                            userEntry = s.userEntry,
                            canLoadMore = s.canLoadMore,
                            onLoadMore = { viewModel.loadNextPage() },
                            unreadCount = unreadCount,
                            onBellClick = { showInbox = true }
                        )
                    }

                    if (s.userEntry != null && s.userEntry.rank > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                        ) {
                            UserRankBanner(type = type, entry = s.userEntry)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun LeaderboardList(
    type: LeaderboardType,
    onTypeChange: (LeaderboardType) -> Unit,
    entries: List<LeaderboardEntry>,
    userEntry: LeaderboardEntry?,
    canLoadMore: Boolean,
    onLoadMore: () -> Unit,
    unreadCount: Int,
    onBellClick: () -> Unit
) {
    val listState = rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= entries.size - 5 && canLoadMore
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item {
            LeaderboardHeader(
                type = type,
                onTypeChange = onTypeChange,
                unreadCount = unreadCount,
                onBellClick = onBellClick
            )
        }

        if (entries.size >= 3) {
            item {
                PodiumSection(
                    type = type,
                    first = entries[0],
                    second = entries[1],
                    third = entries[2]
                )
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(4.dp))
            }
        }

        itemsIndexed(
            items = entries.drop(3),
            key = { index, e -> "${e.uid}_$index" }
        ) { index, entry ->
            val isUser = entry.uid == userEntry?.uid
            LeaderboardRow(
                type = type,
                entry = entry,
                isUser = isUser,
                maxXp = entries[0].totalXp
            )
        }

        if (canLoadMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun LeaderboardHeader(
    type: LeaderboardType,
    onTypeChange: (LeaderboardType) -> Unit,
    unreadCount: Int = 0,
    onBellClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.screen_title_leaderboard),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f)
            )
            BadgedBox(
                badge = {
                    if (unreadCount > 0) {
                        Badge { Text(unreadCount.coerceAtMost(99).toString()) }
                    }
                }
            ) {
                IconButton(onClick = onBellClick) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = "Inbox",
                        tint = if (unreadCount > 0)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LeaderboardTab(
                text = stringResource(R.string.leaderboard_tab_global),
                selected = type == LeaderboardType.GLOBAL,
                onClick = { onTypeChange(LeaderboardType.GLOBAL) }
            )
            LeaderboardTab(
                text = stringResource(R.string.leaderboard_tab_weekly),
                selected = type == LeaderboardType.WEEKLY,
                onClick = { onTypeChange(LeaderboardType.WEEKLY) }
            )
            LeaderboardTab(
                text = stringResource(R.string.leaderboard_tab_last_week),
                selected = type == LeaderboardType.LAST_WEEK,
                onClick = { onTypeChange(LeaderboardType.LAST_WEEK) }
            )
        }

        Spacer(Modifier.height(6.dp))

        Text(
            when (type) {
                LeaderboardType.WEEKLY -> stringResource(R.string.leaderboard_weekly_reset)
                LeaderboardType.LAST_WEEK -> stringResource(R.string.leaderboard_last_week_title)
                else -> stringResource(R.string.leaderboard_all_time)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LeaderboardTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            color = if (selected)
                MaterialTheme.colorScheme.onPrimary
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun PodiumSection(
    type: LeaderboardType,
    first: LeaderboardEntry,
    second: LeaderboardEntry,
    third: LeaderboardEntry
) {
    val gold = Color(0xFFFFD700)
    val silver = Color(0xFFC0C0C0)
    val bronze = Color(0xFFCD7F32)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        PodiumColumn(
            type = type, entry = second, rank = 2, level = second.level,
            color = silver, avatarSize = 64.dp, podiumHeight = 70.dp
        )
        PodiumColumn(
            type = type, entry = first, rank = 1, level = first.level,
            color = gold, avatarSize = 80.dp, podiumHeight = 96.dp
        )
        PodiumColumn(
            type = type, entry = third, rank = 3, level = third.level,
            color = bronze, avatarSize = 56.dp, podiumHeight = 52.dp
        )
    }
}

@Composable
private fun PodiumColumn(
    type: LeaderboardType,
    entry: LeaderboardEntry,
    rank: Int,
    level: Int,
    color: Color,
    avatarSize: Dp,
    podiumHeight: Dp
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        if (rank == 1) Text("👑", fontSize = 22.sp)

        val xpToShow = if (type == LeaderboardType.WEEKLY || type == LeaderboardType.LAST_WEEK) entry.weeklyXp else entry.totalXp

        TrainerAvatar(
            photoUrl = entry.photoUrl,
            displayName = entry.displayName,
            size = avatarSize,
            borderColor = color
        )

        Spacer(Modifier.height(6.dp))

        Text(
            entry.displayName,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 80.dp)
        )
        Text(
            text = "$xpToShow XP",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Lv. $level",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Log.d("PodiumColumn", "xpToShow: $xpToShow")
        Log.d("PodiumColumn", "level: $level")

        Spacer(Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .width(80.dp)
                .height(podiumHeight)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(color.copy(alpha = 0.25f))
                .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "#$rank",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Regular rank row (rank 4+)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun LeaderboardRow(
    type: LeaderboardType,
    entry: LeaderboardEntry,
    isUser: Boolean,
    maxXp: Int
) {
    val bgColor = if (isUser)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    else
        Color.Transparent

    val xpToShow = if (type == LeaderboardType.WEEKLY || type == LeaderboardType.LAST_WEEK) entry.weeklyXp else entry.totalXp
    val progress = if (maxXp > 0) xpToShow / maxXp.toFloat() else 0f
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(xpToShow) {
        animatedProgress.animateTo(progress, tween(600, easing = FastOutSlowInEasing))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "#${entry.rank}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(42.dp)
        )

        TrainerAvatar(photoUrl = entry.photoUrl, displayName = entry.displayName, size = 40.dp)

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    entry.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isUser) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isUser) {
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    ) {
                        Text(
                            stringResource(R.string.leaderboard_you_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "Lv. ${entry.level}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LeagueBadge(rank = entry.rank)
            }
        }

        Text(
            "$xpToShow XP",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Pinned bottom banner
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun UserRankBanner(type: LeaderboardType, entry: LeaderboardEntry) {
    val xpToShow = if (type == LeaderboardType.WEEKLY || type == LeaderboardType.LAST_WEEK)
        entry.weeklyXp else entry.totalXp

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Person, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(stringResource(R.string.leaderboard_your_rank), style = MaterialTheme.typography.labelMedium)
                LeagueBadge(rank = entry.rank)
            }
            Spacer(Modifier.weight(1f))
            Text(
                "#${entry.rank}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(16.dp))
            Text("$xpToShow XP", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared avatar composable
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TrainerAvatar(
    photoUrl: String,
    displayName: String,
    size: Dp,
    borderColor: Color = Color.Transparent
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .border(2.dp, borderColor, CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (photoUrl.isNotEmpty()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = displayName.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LeaderboardSkeleton() {
    val shimmer by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "alpha"
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        repeat(8) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(42.dp, 16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = shimmer))
                )
                Spacer(Modifier.width(12.dp))
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = shimmer))
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        Modifier
                            .fillMaxWidth(0.5f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = shimmer))
                    )
                    Box(
                        Modifier
                            .fillMaxWidth(0.3f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = shimmer))
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// League badge
// ─────────────────────────────────────────────────────────────────────────────
private data class League(val label: String, val color: Color, val emoji: String)

private fun leagueFor(rank: Int): League? = when {
    rank in 1..10 -> League("Gold", Color(0xFFFFD700), "🥇")
    rank in 11..30 -> League("Silver", Color(0xFFC0C0C0), "🥈")
    rank in 31..100 -> League("Bronze", Color(0xFFCD7F32), "🥉")
    else -> null
}

@Composable
private fun LeagueBadge(rank: Int) {
    val league = leagueFor(rank) ?: return
    Surface(
        shape = RoundedCornerShape(50),
        color = league.color.copy(alpha = 0.18f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(league.emoji, fontSize = 9.sp)
            Text(
                text = "${league.label} League",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = league.color,
                fontSize = 9.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Last Week tab — special animated view
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun LastWeekList(
    entries: List<LeaderboardEntry>,
    weekOf: Long,
    type: LeaderboardType,
    onTypeChange: (LeaderboardType) -> Unit,
    unreadCount: Int,
    onBellClick: () -> Unit
) {
    val currentUid = remember { FirebaseAuth.getInstance().currentUser?.uid }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            LeaderboardHeader(
                type = type,
                onTypeChange = onTypeChange,
                unreadCount = unreadCount,
                onBellClick = onBellClick
            )
        }

        if (entries.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏆", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            stringResource(R.string.leaderboard_last_week_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            item { LastWeekHeroHeader(weekOf = weekOf) }

            if (entries.size >= 3) {
                item {
                    LastWeekPodiumSection(
                        first = entries[0],
                        second = entries[1],
                        third = entries[2]
                    )
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(Modifier.height(4.dp))
                }
            }

            val listEntries = if (entries.size >= 3) entries.drop(3) else entries
            itemsIndexed(
                items = listEntries,
                key = { idx, e -> "${e.uid}_$idx" }
            ) { idx, entry ->
                AnimatedLastWeekRow(
                    entry = entry,
                    index = idx,
                    isUser = entry.uid == currentUid,
                    maxXp = entries[0].weeklyXp
                )
            }
        }
    }
}

@Composable
private fun LastWeekHeroHeader(weekOf: Long) {
    val dateStr = remember(weekOf) {
        if (weekOf > 0L) SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(weekOf))
        else null
    }

    val infiniteTransition = rememberInfiniteTransition(label = "hero")
    val trophyY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "trophyFloat"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFFFFD700).copy(alpha = 0.18f), Color(0xFFFFA500).copy(alpha = 0.08f))
                )
            )
            .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "🏆",
                fontSize = 38.sp,
                modifier = Modifier.graphicsLayer { translationY = trophyY }
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    "Last Week's Champions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFB8860B)
                )
                if (dateStr != null) {
                    Text(
                        "Week of $dateStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// Animated podium for Last Week — pulsing glow borders + sparkle for #1
@Composable
private fun LastWeekPodiumSection(
    first: LeaderboardEntry,
    second: LeaderboardEntry,
    third: LeaderboardEntry
) {
    val gold = Color(0xFFFFD700)
    val silver = Color(0xFFC0C0C0)
    val bronze = Color(0xFFCD7F32)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        LastWeekPodiumColumn(
            entry = second, rank = 2, color = silver,
            avatarSize = 64.dp, podiumHeight = 70.dp, entranceDelay = 150L
        )
        LastWeekPodiumColumn(
            entry = first, rank = 1, color = gold,
            avatarSize = 80.dp, podiumHeight = 96.dp, entranceDelay = 0L,
            isChampion = true
        )
        LastWeekPodiumColumn(
            entry = third, rank = 3, color = bronze,
            avatarSize = 56.dp, podiumHeight = 52.dp, entranceDelay = 300L
        )
    }
}

@Composable
private fun LastWeekPodiumColumn(
    entry: LeaderboardEntry,
    rank: Int,
    color: Color,
    avatarSize: Dp,
    podiumHeight: Dp,
    entranceDelay: Long,
    isChampion: Boolean = false
) {
    val entranceScale = remember { Animatable(0.5f) }
    val entranceAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(entranceDelay)
        launch {
            entranceScale.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            )
        }
        launch { entranceAlpha.animateTo(1f, tween(400)) }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "podium_$rank")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900 + rank * 300), RepeatMode.Reverse),
        label = "glow"
    )
    val crownScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = if (isChampion) 1.25f else 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "crown"
    )
    val sparkleAngle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = if (isChampion) 360f else 0f,
        animationSpec = infiniteRepeatable(tween(3500, easing = LinearEasing)),
        label = "sparkle"
    )

    Column(
        modifier = Modifier.graphicsLayer {
            scaleX = entranceScale.value
            scaleY = entranceScale.value
            alpha = entranceAlpha.value
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        if (rank == 1) {
            Text(
                "👑",
                fontSize = 22.sp,
                modifier = Modifier.graphicsLayer { scaleX = crownScale; scaleY = crownScale }
            )
        }

        Box(contentAlignment = Alignment.Center) {
            if (isChampion) {
                Canvas(modifier = Modifier.size(avatarSize + 32.dp)) {
                    val cx = size.width / 2
                    val cy = size.height / 2
                    val radius = size.width / 2
                    val dotCount = 8
                    repeat(dotCount) { i ->
                        val angle = Math.toRadians((sparkleAngle + i * (360f / dotCount)).toDouble())
                        val x = cx + radius * cos(angle).toFloat()
                        val y = cy + radius * sin(angle).toFloat()
                        val dotAlpha = (if (i % 2 == 0) glowAlpha else glowAlpha * 0.45f)
                        drawCircle(
                            color = color,
                            radius = 4.dp.toPx() / 2f,
                            center = Offset(x, y),
                            alpha = dotAlpha
                        )
                    }
                }
            }
            TrainerAvatar(
                photoUrl = entry.photoUrl,
                displayName = entry.displayName,
                size = avatarSize,
                borderColor = color.copy(alpha = glowAlpha)
            )
        }

        Spacer(Modifier.height(6.dp))

        Text(
            entry.displayName,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 80.dp)
        )
        Text(
            "${entry.weeklyXp} XP",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Lv. ${entry.level}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .width(80.dp)
                .height(podiumHeight)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(color.copy(alpha = 0.22f))
                .border(
                    width = 1.5.dp,
                    color = color.copy(alpha = glowAlpha),
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "#$rank",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = color.copy(alpha = glowAlpha)
            )
        }
    }
}

// Staggered slide-in for rows 4-10 in the last week view
@Composable
private fun AnimatedLastWeekRow(
    entry: LeaderboardEntry,
    index: Int,
    isUser: Boolean,
    maxXp: Int
) {
    val offsetX = remember { Animatable(80f) }
    val rowAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(index * 80L)
        launch { offsetX.animateTo(0f, tween(280, easing = FastOutSlowInEasing)) }
        launch { rowAlpha.animateTo(1f, tween(280)) }
    }

    Box(
        modifier = Modifier.graphicsLayer {
            translationX = offsetX.value
            alpha = rowAlpha.value
        }
    ) {
        LeaderboardRow(
            type = LeaderboardType.LAST_WEEK,
            entry = entry,
            isUser = isUser,
            maxXp = maxXp
        )
    }
}

@Composable
private fun LeaderboardError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(stringResource(R.string.leaderboard_error), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Button(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
    }
}
