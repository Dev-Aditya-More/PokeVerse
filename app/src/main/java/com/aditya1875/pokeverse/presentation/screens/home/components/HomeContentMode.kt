package com.aditya1875.pokeverse.presentation.screens.home.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*

enum class HomeContentMode { POKEMON, ITEMS }

@Composable
fun HomeFabCluster(
    // Trivia
    showTriviaBadge: Boolean,
    onTriviaClick: () -> Unit,
    isGuest: Boolean,
    // Scroll-up
    showScrollUp: Boolean,
    onScrollUp: () -> Unit,
    // Content switcher
    currentMode: HomeContentMode,
    onModeChange: (HomeContentMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    // Dismiss expanded when mode changes
    LaunchedEffect(currentMode) { expanded = false }

    val rotateAngle by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "rotate"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Expanded mini-actions ─────────────────────────────────────────────
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(200)) + slideInVertically { it / 2 } +
                    expandVertically(tween(200)),
            exit  = fadeOut(tween(150)) + slideOutVertically { it / 2 } +
                    shrinkVertically(tween(150))
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Items option
                MiniActionRow(
                    label = "Items",
                    emoji = "📦",
                    isActive = currentMode == HomeContentMode.ITEMS,
                    activeColor = Color(0xFF6A1B9A),
                    onClick = {
                        onModeChange(HomeContentMode.ITEMS)
                        expanded = false
                    }
                )
                // Pokémon option
                MiniActionRow(
                    label = "Pokémon",
                    emoji = "⚡",
                    isActive = currentMode == HomeContentMode.POKEMON,
                    activeColor = MaterialTheme.colorScheme.primary,
                    onClick = {
                        onModeChange(HomeContentMode.POKEMON)
                        expanded = false
                    }
                )
            }
        }

        // ── Main FAB row: [Trivia] [Expand] [ScrollUp] ────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Trivia FAB (left)
            if (!isGuest) {
                DailyTriviaFab(
                    showBadge = showTriviaBadge,
                    onClick = onTriviaClick
                )
            } else {
                Spacer(Modifier.size(56.dp))
            }

            // Centre expand FAB — shows current mode icon
            FloatingActionButton(
                onClick = { expanded = !expanded },
                shape = RoundedCornerShape(16.dp),
                containerColor = if (currentMode == HomeContentMode.ITEMS)
                    Color(0xFF6A1B9A)
                else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Explore",
                    tint = Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(rotateAngle)
                )
            }

            // Scroll-up FAB (right)
            AnimatedVisibility(
                visible = showScrollUp,
                enter = fadeIn() + scaleIn(),
                exit  = fadeOut() + scaleOut()
            ) {
                FloatingActionButton(
                    onClick = onScrollUp,
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(52.dp),
                    elevation = FloatingActionButtonDefaults.elevation(4.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = "Scroll to top",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Placeholder to keep layout balanced when scroll-up is hidden
            if (!showScrollUp) Spacer(Modifier.size(52.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Mini action row item — label + emoji pill
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MiniActionRow(
    label: String,
    emoji: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Label chip
        AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isActive) activeColor
                else MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
                onClick = onClick
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isActive) FontWeight.Black else FontWeight.SemiBold,
                    color = if (isActive) Color.White
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Emoji FAB
        SmallFloatingActionButton(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            containerColor = if (isActive) activeColor
            else MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Text(emoji, fontSize = 18.sp)
        }
    }
}