package com.aditya1875.pokeverse.feature.pokemon.profile.presentation.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.core.ui.components.NewBadge
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aditya1875.pokeverse.feature.pokemon.profile.data.source.remote.model.UserProfile
import com.aditya1875.pokeverse.feature.pokemon.profile.presentation.viewmodels.ProfileViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aditya1875.pokeverse.feature.pokemon.onboarding.Starter
import com.aditya1875.pokeverse.feature.pokemon.settings.presentation.viewmodels.SettingsViewModel
import com.aditya1875.pokeverse.utils.localizedTypeName
import com.aditya1875.pokeverse.utils.pokemonTypeColor
import com.google.firebase.auth.FirebaseUser
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.IconButton
import androidx.compose.ui.res.pluralStringResource

@Composable
fun ProfileHeader(
    profile: UserProfile,
    currentUser: FirebaseUser? = null,
    onEditName: () -> Unit,
    onEditPhoto: () -> Unit,
    photoUploading: Boolean = false,
    onSettingsClick: (() -> Unit)? = null,
    viewModel: ProfileViewModel = koinViewModel(),
    settingsViewModel: SettingsViewModel = koinViewModel()
) {
    val assetsEnabled by settingsViewModel.originalAssetsEnabled.collectAsStateWithLifecycle()
    var prevLevel by remember { mutableIntStateOf(profile.level) }
    var levelChanged by remember { mutableStateOf(false) }

    var showTrainerCard by remember { mutableStateOf(false) }

    LaunchedEffect(profile.level) {
        if (profile.level != prevLevel) {
            levelChanged = true
            prevLevel = profile.level
        }
    }

    if (showTrainerCard) {
        TrainerCardSetup(
            profile = profile,
            onSave = viewModel::saveTrainerCard,
            onDismiss = { showTrainerCard = false }
        )
    }

    val levelScale by animateFloatAsState(
        targetValue = if (levelChanged) 1.25f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        finishedListener = { levelChanged = false },
        label = "levelScale"
    )

    val xpProgress by animateFloatAsState(
        targetValue = (profile.currentXp.toFloat() / profile.nextLevelXp.coerceAtLeast(1)).coerceIn(0f, 1f),
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "xpRing"
    )
    val ringColor = MaterialTheme.colorScheme.primary
    val ringTrack = MaterialTheme.colorScheme.surfaceVariant

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
      // XP ring around the avatar replaces the old separate "Trainer progress" card.
      Box(Modifier.size(88.dp), contentAlignment = Alignment.Center) {
        if (!profile.isGuest) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 5.dp.toPx()
                drawArc(ringTrack, 0f, 360f, false, style = Stroke(stroke))
                drawArc(ringColor, -90f, 360f * xpProgress, false, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
        Box(
            modifier = Modifier
                .size(74.dp)
                .clip(CircleShape)
                .background(
                    if (profile.photoUrl.isNotEmpty() || currentUser?.photoUrl != null || (assetsEnabled && profile.starter.isNotEmpty()))
                        Color.Transparent
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
                .border(
                    2.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    CircleShape
                )
                .combinedClickable(
                    onClick = onEditPhoto,
                    onLongClick = onEditPhoto
                ),
            contentAlignment = Alignment.Center
        ) {
            val photo = when {
                profile.photoUrl.isNotEmpty() -> profile.photoUrl
                currentUser?.photoUrl != null -> currentUser.photoUrl.toString()
                // Franchise art only when Original Assets is on; otherwise the initial, as before.
                assetsEnabled -> Starter.fromId(profile.starter)?.artworkUrl
                else -> null
            }

            if (photo != null) {
                AsyncImage(
                    model = photo,
                    contentDescription = stringResource(R.string.a11y_profile_photo),
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = profile.username.firstOrNull()?.uppercase() ?: "—",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (photoUploading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                }
            }
        }
        if (!profile.isGuest) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.BottomCenter).scale(levelScale)
            ) {
                Text(
                    text = stringResource(R.string.level_short, profile.level),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
      }

        Spacer(Modifier.width(14.dp))

        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.clickable { showTrainerCard = true }
            ) {
                Text(
                    text = profile.username,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )

                Spacer(Modifier.width(2.dp))

                Icon(
                    Icons.Default.Edit,
                    contentDescription = stringResource(R.string.a11y_edit_name),
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.clickable { showTrainerCard = true }
            ) {
                Text(
                    text = profile.bio.ifBlank { stringResource(R.string.profile_bio_empty_hint) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (profile.bio.isBlank()) 0.5f else 0.85f),
                    maxLines = 2
                )
                if (profile.bio.isBlank()) {
                    NewBadge()
                }
            }

            val style = PlayStyle.fromId(profile.playStyle)
            if (profile.favoriteType.isNotEmpty() || style != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (profile.favoriteType.isNotEmpty()) {
                        TrainerChip(localizedTypeName(profile.favoriteType), pokemonTypeColor(profile.favoriteType))
                    }
                    if (style != null) {
                        TrainerChip("${style.emoji} ${stringResource(style.label)}", MaterialTheme.colorScheme.primary)
                    }
                }
            }

            if (!profile.isGuest) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.xp_progress, profile.currentXp, profile.nextLevelXp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (profile.dailyStreak > 0) {
                        Text(
                            text = "🔥 " + pluralStringResource(R.plurals.profile_day_streak, profile.dailyStreak, profile.dailyStreak),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFF8A3D)
                        )
                    }
                }
            }
        }

        onSettingsClick?.let {
            IconButton(onClick = it, modifier = Modifier.align(Alignment.Top)) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = stringResource(R.string.profile_settings),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}