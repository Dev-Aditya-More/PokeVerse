package com.aditya1875.pokeverse.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavigationBar(navController: NavHostController) {

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route

    BottomAppBar(
        containerColor = Color.Black,
        contentColor = Color.White,
    ) {
        val items = listOf(
            Triple("home", Icons.Default.Home, Color(0xFFEF5350)),
            Triple("dream_team", Icons.Default.Star, Color(0xFFFFD54F)),
            Triple("settings", Icons.Default.Settings, Color(0xFF42A5F5))
        )

        items.forEach { (route, icon, accentColor) ->
            val selected = currentDestination == route

            val iconTint by animateColorAsState(
                targetValue = if (selected) accentColor else Color.Gray,
                animationSpec = tween(durationMillis = 300)
            )

            val textAlpha by animateFloatAsState(
                targetValue = if (selected) 1f else 0f,
                animationSpec = tween(durationMillis = 250)
            )

            val bgColor by animateColorAsState(
                targetValue = if (selected) accentColor.copy(alpha = 0.15f)
                else Color.Transparent,
                animationSpec = tween(durationMillis = 350)
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
                            imageVector = icon,
                            contentDescription = route,
                            tint = iconTint
                        )
                    }
                },
                label = {
                    Text(
                        text = when (route) {
                            "home" -> "Home"
                            "dream_team" -> "Team"
                            "settings" -> "Settings"
                            else -> ""
                        },
                        color = Color.White.copy(alpha = textAlpha),
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