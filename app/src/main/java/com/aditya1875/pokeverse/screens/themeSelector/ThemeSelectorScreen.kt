package com.aditya1875.pokeverse.screens.themeSelector

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.aditya1875.pokeverse.data.preferences.ThemePreferences
import com.aditya1875.pokeverse.ui.theme.AppTheme
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectorScreen(
    navController: NavController,
    themePreferences: ThemePreferences = koinInject(),
    onThemeSelected: (AppTheme) -> Unit
) {
    val scope = rememberCoroutineScope()
    val currentTheme by themePreferences.selectedTheme.collectAsState(initial = AppTheme.CHARIZARD)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Choose Your Starter",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "🔥 💧 🌿",
                            fontSize = 48.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Choose Your Theme",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Pick a legendary starter to personalize your Pokéverse experience",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Theme Options
            items(getStarterThemes()) { starterTheme ->
                StarterThemeCard(
                    starterTheme = starterTheme,
                    isSelected = currentTheme == starterTheme.theme,
                    onClick = {
                        scope.launch {
                            themePreferences.setTheme(starterTheme.theme)
                            onThemeSelected(starterTheme.theme)
                        }
                    }
                )
            }

            // Fun fact footer
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Your theme choice will be saved and applied across the entire app",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

data class StarterTheme(
    val theme: AppTheme,
    val pokemonName: String,
    val pokemonNumber: String,
    val type: String,
    val emoji: String,
    val description: String,
    val colors: List<Color>
)

fun getStarterThemes(): List<StarterTheme> = listOf(
    StarterTheme(
        theme = AppTheme.CHARIZARD,
        pokemonName = "Charizard",
        pokemonNumber = "#006",
        type = "Fire • Flying",
        emoji = "🔥",
        description = "Fierce and bold like a fire-breathing dragon",
        colors = listOf(
            Color(0xFFFF6D00),  // Charizard Orange
            Color(0xFFE65100),  // Deep Orange
            Color(0xFF0091EA)   // Blue Wings
        )
    ),
    StarterTheme(
        theme = AppTheme.VENUSAUR,
        pokemonName = "Venusaur",
        pokemonNumber = "#003",
        type = "Grass • Poison",
        emoji = "🌿",
        description = "Fresh and vibrant like a blooming garden",
        colors = listOf(
            Color(0xFF4CAF50),  // Grass Green
            Color(0xFF2E7D32),  // Deep Green
            Color(0xFF26A69A)   // Teal
        )
    ),
    StarterTheme(
        theme = AppTheme.BLASTOISE,
        pokemonName = "Blastoise",
        pokemonNumber = "#009",
        type = "Water",
        emoji = "💧",
        description = "Cool and calm like the deep ocean",
        colors = listOf(
            Color(0xFF2196F3),  // Ocean Blue
            Color(0xFF1565C0),  // Deep Blue
            Color(0xFF00BCD4)   // Cyan
        )
    )
)

@Composable
fun StarterThemeCard(
    starterTheme: StarterTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 3.dp,
                        brush = Brush.linearGradient(starterTheme.colors),
                        shape = RoundedCornerShape(20.dp)
                    )
                } else Modifier
            )
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 12.dp else 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Pokemon Info
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = starterTheme.emoji,
                            fontSize = 32.sp
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = starterTheme.pokemonName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = starterTheme.pokemonNumber,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                // Selection Indicator
                AnimatedVisibility(
                    visible = isSelected,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(starterTheme.colors)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                if (!isSelected) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Type Badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = starterTheme.colors[0].copy(alpha = 0.15f)
            ) {
                Text(
                    text = starterTheme.type,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = starterTheme.colors[0],
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))

            // Description
            Text(
                text = starterTheme.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(16.dp))

            // Color Preview
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                starterTheme.colors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = 2.dp,
                                color = if (isSelected) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}