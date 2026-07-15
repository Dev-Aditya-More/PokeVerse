package com.aditya1875.pokeverse.feature.friends.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.friends.data.model.FriendProfile
import com.aditya1875.pokeverse.feature.friends.data.model.FriendStatus
import com.aditya1875.pokeverse.feature.friends.presentation.viewmodels.FriendsViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    onBack: () -> Unit,
    viewModel: FriendsViewModel = koinViewModel()
) {
    val friends by viewModel.friends.collectAsStateWithLifecycle()
    val friendsLoading by viewModel.friendsLoading.collectAsStateWithLifecycle()
    val incoming by viewModel.incomingRequests.collectAsStateWithLifecycle()
    val outgoing by viewModel.outgoingRequests.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searching by viewModel.searching.collectAsStateWithLifecycle()
    val results by viewModel.searchResults.collectAsStateWithLifecycle()
    val myProfile by viewModel.myProfile.collectAsStateWithLifecycle()

    var compareTarget by remember { mutableStateOf<FriendProfile?>(null) }

    compareTarget?.let { friend ->
        ModalBottomSheet(onDismissRequest = { compareTarget = null }) {
            CompareSheet(
                me = Triple(myProfile.username, myProfile.photoUrl, myProfile.level),
                myTotalXp = myProfile.totalXp,
                myWeeklyXp = myProfile.weeklyXp,
                friend = friend,
                onRemove = {
                    viewModel.removeFriend(friend.uid)
                    compareTarget = null
                }
            )
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Single column on phones; centered, width-capped column on tablets
            val contentWidth = if (maxWidth >= 600.dp) 560.dp else maxWidth

            LazyColumn(
                modifier = Modifier
                    .widthIn(max = contentWidth)
                    .align(Alignment.TopCenter)
                    .fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 20.dp, end = 20.dp, bottom = 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.friends_back)
                            )
                        }
                        Text(
                            stringResource(R.string.friends_title),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                item(key = "search") {
                    OutlinedTextField(
                        value = query,
                        onValueChange = viewModel::onSearchQueryChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.friends_search_hint)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                    Icon(Icons.Default.Close, contentDescription = null)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                // ── Search mode ────────────────────────────────────────────────
                if (query.trim().length >= 2) {
                    if (searching) {
                        item(key = "search_loading") {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) { CircularProgressIndicator(Modifier.size(28.dp)) }
                        }
                    } else if (results.isEmpty()) {
                        item(key = "search_empty") {
                            EmptyHint(
                                emoji = "🔍",
                                text = stringResource(R.string.friends_search_empty)
                            )
                        }
                    } else {
                        items(results, key = { "res_${it.profile.uid}" }) { result ->
                            SearchResultRow(
                                result = result,
                                onAdd = { viewModel.sendRequest(result.profile) },
                                onCancel = { viewModel.cancelRequest(result.profile.uid) },
                                onAccept = {
                                    incoming.find { it.fromUid == result.profile.uid }
                                        ?.let(viewModel::acceptRequest)
                                }
                            )
                        }
                    }
                } else {
                    // ── Requests section ──────────────────────────────────────
                    if (incoming.isNotEmpty()) {
                        item(key = "req_header") {
                            SectionHeader(
                                stringResource(R.string.friends_requests_header, incoming.size)
                            )
                        }
                        items(incoming, key = { "in_${it.id}" }) { request ->
                            RequestRow(
                                name = request.fromName,
                                photoUrl = request.fromPhotoUrl,
                                level = request.fromLevel,
                                onAccept = { viewModel.acceptRequest(request) },
                                onDecline = { viewModel.declineRequest(request) }
                            )
                        }
                    }

                    if (outgoing.isNotEmpty()) {
                        item(key = "out_header") {
                            SectionHeader(stringResource(R.string.friends_sent_header))
                        }
                        items(outgoing, key = { "out_${it.id}" }) { request ->
                            SentRequestRow(
                                name = request.toName,
                                photoUrl = request.toPhotoUrl,
                                onCancel = { viewModel.cancelRequest(request.toUid) }
                            )
                        }
                    }

                    // ── Friends list ──────────────────────────────────────────
                    item(key = "friends_header") {
                        SectionHeader(
                            stringResource(R.string.friends_list_header, friends.size)
                        )
                    }

                    if (friendsLoading) {
                        item(key = "friends_loading") {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) { CircularProgressIndicator(Modifier.size(28.dp)) }
                        }
                    } else if (friends.isEmpty()) {
                        item(key = "friends_empty") {
                            EmptyHint(
                                emoji = "🤝",
                                text = stringResource(R.string.friends_empty)
                            )
                        }
                    } else {
                        items(friends, key = { "fr_${it.uid}" }) { friend ->
                            FriendRow(
                                friend = friend,
                                onClick = { compareTarget = friend }
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
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)
    )
}

@Composable
private fun EmptyHint(emoji: String, text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 40.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TrainerAvatar(photoUrl: String, size: androidx.compose.ui.unit.Dp) {
    if (photoUrl.isNotBlank()) {
        AsyncImage(
            model = photoUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(size * 0.55f)
            )
        }
    }
}

@Composable
private fun FriendRow(friend: FriendProfile, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrainerAvatar(friend.photoUrl, 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    friend.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    stringResource(R.string.friends_row_stats, friend.level, friend.totalXp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Text(
                    stringResource(R.string.friends_weekly_chip, friend.weeklyXp),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun RequestRow(
    name: String,
    photoUrl: String,
    level: Int,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrainerAvatar(photoUrl, 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    stringResource(R.string.friends_request_level, level),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalIconButton(onClick = onAccept) {
                Icon(Icons.Default.Check, contentDescription = stringResource(R.string.friends_accept))
            }
            IconButton(onClick = onDecline) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.friends_decline),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SentRequestRow(name: String, photoUrl: String, onCancel: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrainerAvatar(photoUrl, 36.dp)
        Spacer(Modifier.width(12.dp))
        Text(
            name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        TextButton(onClick = onCancel) {
            Text(stringResource(R.string.friends_cancel_request))
        }
    }
}

@Composable
private fun SearchResultRow(
    result: com.aditya1875.pokeverse.feature.friends.data.model.TrainerSearchResult,
    onAdd: () -> Unit,
    onCancel: () -> Unit,
    onAccept: () -> Unit
) {
    val profile = result.profile
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrainerAvatar(profile.photoUrl, 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                profile.displayName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                stringResource(R.string.friends_row_stats, profile.level, profile.totalXp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        when (result.status) {
            FriendStatus.NONE -> FilledTonalIconButton(onClick = onAdd) {
                Icon(Icons.Default.PersonAdd, contentDescription = stringResource(R.string.friends_add))
            }
            FriendStatus.REQUEST_SENT -> TextButton(onClick = onCancel) {
                Text(stringResource(R.string.friends_requested))
            }
            FriendStatus.REQUEST_RECEIVED -> FilledTonalIconButton(onClick = onAccept) {
                Icon(Icons.Default.Check, contentDescription = stringResource(R.string.friends_accept))
            }
            FriendStatus.FRIENDS -> Text(
                stringResource(R.string.friends_already),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            FriendStatus.SELF -> Text(
                stringResource(R.string.leaderboard_you_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Compare sheet: you vs friend ─────────────────────────────────────────────
@Composable
private fun CompareSheet(
    me: Triple<String, String, Int>,
    myTotalXp: Int,
    myWeeklyXp: Int,
    friend: FriendProfile,
    onRemove: () -> Unit
) {
    val (myName, myPhoto, myLevel) = me
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TrainerAvatar(myPhoto, 64.dp)
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.leaderboard_you_label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                "VS",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TrainerAvatar(friend.photoUrl, 64.dp)
                Spacer(Modifier.height(6.dp))
                Text(
                    friend.displayName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 120.dp)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        CompareStatRow(stringResource(R.string.friends_stat_level), myLevel, friend.level)
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        CompareStatRow(stringResource(R.string.friends_stat_total_xp), myTotalXp, friend.totalXp)
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        CompareStatRow(stringResource(R.string.friends_stat_weekly_xp), myWeeklyXp, friend.weeklyXp)

        Spacer(Modifier.height(20.dp))

        TextButton(onClick = onRemove) {
            Text(
                stringResource(R.string.friends_remove),
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun CompareStatRow(label: String, mine: Int, theirs: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "$mine",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (mine >= theirs) FontWeight.Black else FontWeight.Normal,
            color = if (mine >= theirs) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Start
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            "$theirs",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (theirs >= mine) FontWeight.Black else FontWeight.Normal,
            color = if (theirs >= mine) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}
