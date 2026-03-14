package com.aditya1875.pokeverse.presentation.screens.profile.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aditya1875.pokeverse.data.remote.model.UserProfile
import com.google.firebase.auth.FirebaseUser

@Composable
fun ProfileHeader(
    profile: UserProfile,
    currentUser: FirebaseUser? = null,
    onEditName: () -> Unit,
    onEditPhoto: () -> Unit
) {
    // Animate level number changes — scale pop when it changes
    var prevLevel by remember { mutableIntStateOf(profile.level) }
    var levelChanged by remember { mutableStateOf(false) }

    LaunchedEffect(profile.level) {
        if (profile.level != prevLevel) {
            levelChanged = true
            prevLevel = profile.level
        }
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

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(
                    if (profile.photoUrl.isNotEmpty() || currentUser?.photoUrl != null)
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
                    onClick = onEditName,
                    onLongClick = onEditPhoto
                ),
            contentAlignment = Alignment.Center
        ) {

            val photo = when {
                profile.photoUrl.isNotEmpty() -> profile.photoUrl
                currentUser?.photoUrl != null -> currentUser.photoUrl.toString()
                else -> null
            }

            if (photo != null) {
                AsyncImage(
                    model = photo,
                    contentDescription = "Profile Photo",
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
        }

        Spacer(Modifier.width(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = currentUser?.displayName ?: profile.username,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )

                IconButton(
                    onClick = onEditName,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Name",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if(!profile.isGuest) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.scale(levelScale)
                ) {
                    Text(
                        text = "⚡ Level ${profile.level}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}