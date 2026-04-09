package com.aditya1875.pokeverse.feature.leaderboard.presentation.screens

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.aditya1875.pokeverse.feature.leaderboard.data.remote.model.LeaderboardEntry
import com.aditya1875.pokeverse.feature.leaderboard.data.repository.LeaderboardState
import com.aditya1875.pokeverse.feature.leaderboard.presentation.components.ConfettiOverlay
import com.aditya1875.pokeverse.feature.leaderboard.presentation.components.GuestLeaderboardLocked
import com.aditya1875.pokeverse.feature.leaderboard.presentation.components.RankCelebrationDialog
import com.aditya1875.pokeverse.feature.leaderboard.presentation.viewmodels.LeaderboardType
import com.aditya1875.pokeverse.feature.leaderboard.presentation.viewmodels.LeaderboardViewModel
import com.aditya1875.pokeverse.feature.pokemon.profile.presentation.viewmodels.ProfileViewModel
import com.aditya1875.pokeverse.presentation.auth.AuthState
import com.aditya1875.pokeverse.utils.ScreenStateManager
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    viewModel: LeaderboardViewModel = koinViewModel(),
    profileViewModel: ProfileViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val pullState = rememberPullToRefreshState()
    val authState by profileViewModel.authState.collectAsStateWithLifecycle()
    val type by viewModel.type.collectAsStateWithLifecycle()

    if (authState !is AuthState.Authenticated) {
        GuestLeaderboardLocked()
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (val s = state) {
                is LeaderboardState.Loading -> LeaderboardSkeleton()
                is LeaderboardState.Error -> LeaderboardError(s.message) { viewModel.load() }
                is LeaderboardState.Success -> {

                    val context = LocalContext.current
                    var lastCelebratedRank by remember { mutableIntStateOf(-1) }

                    var showDialog by remember { mutableStateOf(false) }
                    var rank by remember { mutableIntStateOf(0) }

                    LaunchedEffect(Unit) {
                        lastCelebratedRank = ScreenStateManager.getLastCelebratedRank(context)
                    }

                    LaunchedEffect(s.userEntry?.previousRank, type) {

                        // Only for WEEKLY leaderboard
                        if (type != LeaderboardType.WEEKLY) return@LaunchedEffect

                        val user = s.userEntry ?: return@LaunchedEffect

                        val lastReset = user.lastWeeklyReset ?: return@LaunchedEffect

                        val lastSeenReset = ScreenStateManager.getLastSeenReset(context)

                        val isNewWeek = lastReset > lastSeenReset

                        if (isNewWeek && user.previousRank in 1..3) {

                            ScreenStateManager.setLastSeenReset(context, lastReset)

                            rank = user.previousRank
                            showDialog = true
                        }
                    }

                    if (showDialog && type == LeaderboardType.WEEKLY) {
                        RankCelebrationDialog(
                            rank = rank,
                            displayName = s.userEntry?.displayName
                                ?.split(" ")
                                ?.firstOrNull()
                                ?: "",
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
                            onTypeChange = { selectedType ->
                                viewModel.switchType(selectedType)
                            },
                            entries = s.entries,
                            userEntry = s.userEntry,
                            canLoadMore = s.canLoadMore,
                            onLoadMore = { viewModel.loadNextPage() }
                        )
                    }

                    val userInList = s.entries.any { it.uid == s.userEntry?.uid }
                    if (s.userEntry != null && !userInList) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                        ) {
                            UserRankBanner(
                                type = type,
                                entry = s.userEntry
                            )
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
    onLoadMore: () -> Unit
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
        // Header
        item {
            LeaderboardHeader(
                type = type,
                onTypeChange = onTypeChange
            )
        }

        // Top 3 podium
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
            key = { _, e -> e.uid }
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
    onTypeChange: (LeaderboardType) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            "Leaderboard",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black
        )

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

            LeaderboardTab(
                text = "Global",
                selected = type == LeaderboardType.GLOBAL,
                onClick = { onTypeChange(LeaderboardType.GLOBAL) }
            )

            LeaderboardTab(
                text = "Weekly",
                selected = type == LeaderboardType.WEEKLY,
                onClick = { onTypeChange(LeaderboardType.WEEKLY) }
            )
        }

        Spacer(Modifier.height(6.dp))

        Text(
            if (type == LeaderboardType.WEEKLY)
                "Resets every Monday 12:00 AM IST"
            else
                "Top trainers of all time",
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
            type = type,
            entry = second,
            rank = 2,
            level = second.level,
            color = silver,
            avatarSize = 64.dp,
            podiumHeight = 70.dp
        )
        PodiumColumn(
            type = type,
            entry = first,
            rank = 1,
            level = first.level,
            color = gold,
            avatarSize = 80.dp,
            podiumHeight = 96.dp
        )
        PodiumColumn(
            type = type,
            entry = third,
            rank = 3,
            level = third.level,
            color = bronze,
            avatarSize = 56.dp,
            podiumHeight = 52.dp
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
        if (rank == 1) {
            Text("👑", fontSize = 22.sp)
        }

        val xpToShow = if (type == LeaderboardType.WEEKLY) entry.weeklyXp else entry.totalXp

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

        // Podium block
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(podiumHeight)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(color.copy(alpha = 0.25f))
                .border(
                    1.dp,
                    color.copy(alpha = 0.5f),
                    RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                ),
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

    val xpToShow = if (type == LeaderboardType.WEEKLY) entry.weeklyXp else entry.totalXp

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
        // Rank number
        Text(
            text = "#${entry.rank}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(42.dp)
        )

        TrainerAvatar(
            photoUrl = entry.photoUrl,
            displayName = entry.displayName,
            size = 40.dp
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    entry.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isUser) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isUser) {
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    ) {
                        Text(
                            "You",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Text(
                "Lv. ${entry.level}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
// Pinned bottom banner when user is outside top-50
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun UserRankBanner(
    type: LeaderboardType,
    entry: LeaderboardEntry
) {

    val xpToShow = if (type == LeaderboardType.WEEKLY)
        entry.weeklyXp
    else
        entry.totalXp

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
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
            Text("Your Rank", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.weight(1f))
            Text(
                "#${entry.rank}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(16.dp))
            Text(
                "$xpToShow XP",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
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
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(20.dp)) {
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

@Composable
private fun LeaderboardError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Couldn't load leaderboard", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}