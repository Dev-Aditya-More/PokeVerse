package com.aditya1875.pokeverse.screens.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    selectedTab: Int,
    onTabChange: (Int) -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route

    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        val items = listOf(
            Triple("home", Icons.Default.Home, MaterialTheme.colorScheme.primary),
            Triple("dream_team", Icons.Default.Star, MaterialTheme.colorScheme.secondary),
            Triple("settings", Icons.Default.Settings, MaterialTheme.colorScheme.tertiary)
        )

        items.forEach { (route, icon, accentColor) ->
            val resolvedIcon = when (route) {
                "dream_team" if selectedTab == 1 -> Icons.Default.Star
                "dream_team" -> Icons.Default.Add
                else -> icon
            }

            val selected = currentDestination == route

            val iconTint by animateColorAsState(
                targetValue = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(durationMillis = 300),
                label = "icon_tint"
            )

            val textAlpha by animateFloatAsState(
                targetValue = if (selected) 1f else 0f,
                animationSpec = tween(durationMillis = 250),
                label = "text_alpha"
            )

            val bgColor by animateColorAsState(
                targetValue = if (selected) accentColor.copy(alpha = 0.15f)
                else Color.Transparent,
                animationSpec = tween(durationMillis = 350),
                label = "bg_color"
            )

            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Box(
                        modifier = Modifier
                            .background(bgColor, shape = CircleShape)
                            .padding(10.dp)
                    ) {
                        Icon(
                            imageVector = resolvedIcon,
                            contentDescription = route,
                            tint = iconTint
                        )
                    }
                },
                label = {
                    Text(
                        text = "",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.alpha(textAlpha)
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent,
                    selectedIconColor = Color.Unspecified,
                    unselectedIconColor = Color.Unspecified
                )
            )
        }
    }
}