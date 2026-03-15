package com.aditya1875.pokeverse.presentation.screens.theme

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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.aditya1875.pokeverse.data.billing.SubscriptionState
import com.aditya1875.pokeverse.data.preferences.ThemePreferences
import com.aditya1875.pokeverse.presentation.ui.theme.AppTheme
import com.aditya1875.pokeverse.presentation.viewmodel.BillingViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectorScreen(
    navController: NavController,
    themePreferences: ThemePreferences = koinInject(),
    onThemeSelected: (AppTheme) -> Unit
) {
    val scope = rememberCoroutineScope()
    val currentTheme by themePreferences.selectedTheme.collectAsState(initial = AppTheme.DEXVERSE)

    val billingViewModel: BillingViewModel = koinViewModel()
    val subscriptionState by billingViewModel.subscriptionState.collectAsStateWithLifecycle()

    val isPremium = subscriptionState is SubscriptionState.Premium

    var showPremiumSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Choose Your Vibe",
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

            items(getStarterThemes()) { starterTheme ->
                val canUseTheme = !starterTheme.premium || isPremium
                val locked = starterTheme.premium && !isPremium
                StarterThemeCard(
                    starterTheme = starterTheme,
                    isSelected = currentTheme == starterTheme.theme,
                    isLocked = locked,
                    onClick = {
                        if (canUseTheme) {
                            scope.launch {
                                themePreferences.setTheme(starterTheme.theme)
                                onThemeSelected(starterTheme.theme)
                            }
                        } else {
                            showPremiumSheet = true
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
    val colors: List<Color>,
    val premium: Boolean = false
)

fun getStarterThemes(): List<StarterTheme> = listOf(
    StarterTheme(
        theme = AppTheme.DEXVERSE,
        pokemonName = "Dexverse",
        pokemonNumber = "Brand",
        type = "Official Theme",
        emoji = "✨",
        description = "The classic Dexverse theme",
        colors = listOf(
            Color(0xFF7C4DFF), // Purple
            Color(0xFF00E5FF), // Neon cyan
            Color(0xFF0B0F1A)  // Deep space navy
        )
    ),

    StarterTheme(
        theme = AppTheme.PIKACHU,
        pokemonName = "Pikachu",
        pokemonNumber = "#025",
        type = "Electric",
        emoji = "⚡",
        description = "Bright and energetic like everyone's favorite electric mouse",
        colors = listOf(
            Color(0xFFFFD600), // Pikachu Yellow
            Color(0xFFFFEA00), // Bright Yellow
            Color(0xFF212121)  // Dark contrast (ears/tail tip)
        ),
        premium = true
    ),

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
    isLocked: Boolean,
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
            .alpha(if (isLocked) 0.7f else 1f)
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

                Column(modifier = Modifier.weight(1f)) {

                    if (starterTheme.theme == AppTheme.DEXVERSE) {
                        Text(
                            text = "Classic",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                    }

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

                            if (starterTheme.premium) {

                                Spacer(Modifier.height(6.dp))

                                AssistChip(
                                    onClick = {},
                                    enabled = false,
                                    label = { Text("Premium") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.WorkspacePremium,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                if (isLocked) {

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Premium Theme",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                } else if (isSelected) {

                    AnimatedVisibility(
                        visible = true,
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

                } else {

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

            Text(
                text = starterTheme.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(16.dp))

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
