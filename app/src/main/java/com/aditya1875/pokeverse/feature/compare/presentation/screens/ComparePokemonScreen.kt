package com.aditya1875.pokeverse.feature.compare.presentation.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aditya1875.pokeverse.feature.compare.domain.EdgeVerdict
import com.aditya1875.pokeverse.feature.compare.domain.computeEdgeVerdict
import com.aditya1875.pokeverse.feature.compare.presentation.components.ComparePickerField
import com.aditya1875.pokeverse.feature.compare.presentation.components.ConfettiBurst
import com.aditya1875.pokeverse.feature.compare.presentation.viewmodels.CompareViewModel
import com.aditya1875.pokeverse.feature.compare.presentation.viewmodels.Side
import com.aditya1875.pokeverse.feature.game.core.data.ads.IRewardedAdManager
import com.aditya1875.pokeverse.feature.game.core.data.ads.RewardedAdState
import com.aditya1875.pokeverse.feature.game.core.data.billing.IBillingManager
import com.aditya1875.pokeverse.feature.game.core.data.billing.SubscriptionState
import com.aditya1875.pokeverse.feature.game.premium.components.PremiumBottomSheet
import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.PokemonResponse
import com.aditya1875.pokeverse.feature.pokemon.detail.presentation.components.statLabel
import com.aditya1875.pokeverse.presentation.viewmodel.BillingViewModel
import com.aditya1875.pokeverse.utils.SearchUiState
import com.aditya1875.pokeverse.utils.SoundManager
import com.aditya1875.pokeverse.utils.pokemonTypeColor
import com.aditya1875.pokeverse.utils.pokemonTypeEffectiveness
import com.aditya1875.pokeverse.utils.typeEffectivenessMultiplier
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

private val CompareBg = Color(0xFF0B0E17)
private val CardBg = Color(0xFF141820)
private val LeftAccent = Color(0xFF4FC3F7)
private val RightAccent = Color(0xFFFF7043)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComparePokemonScreen(
    onBack: () -> Unit,
    viewModel: CompareViewModel = koinViewModel()
) {
    val billingManager: IBillingManager = koinInject()
    val subscriptionState by billingManager.subscriptionState.collectAsState()
    val isPremium = subscriptionState is SubscriptionState.Premium

    var showPremiumSheet by remember { mutableStateOf(false) }
    val billingViewModel: BillingViewModel = koinViewModel()
    val monthlyPrice by billingViewModel.monthlyPrice.collectAsStateWithLifecycle()
    val yearlyPrice by billingViewModel.yearlyPrice.collectAsStateWithLifecycle()
    val lifetimePrice by billingViewModel.lifetimePrice.collectAsStateWithLifecycle()
    val isBillingReady = monthlyPrice.isNotBlank() || yearlyPrice.isNotBlank() || lifetimePrice.isNotBlank()

    val rewardedAdManager: IRewardedAdManager = koinInject()
    val adState by rewardedAdManager.adState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity

    var adUnlocked by rememberSaveable { mutableStateOf(isPremium) }

    LaunchedEffect(isPremium) {
        if (isPremium) adUnlocked = true
    }
    LaunchedEffect(adUnlocked, isPremium) {
        if (!adUnlocked && !isPremium) rewardedAdManager.loadAd(context)
    }

    val queryLeft by viewModel.queryLeft.collectAsStateWithLifecycle()
    val queryRight by viewModel.queryRight.collectAsStateWithLifecycle()
    val leftSearch by viewModel.leftSearch.collectAsStateWithLifecycle()
    val rightSearch by viewModel.rightSearch.collectAsStateWithLifecycle()
    val leftPokemon by viewModel.leftPokemon.collectAsStateWithLifecycle()
    val rightPokemon by viewModel.rightPokemon.collectAsStateWithLifecycle()
    val leftLoading by viewModel.leftLoading.collectAsStateWithLifecycle()
    val rightLoading by viewModel.rightLoading.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compare Pokémon", color = Color.White, fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A1A1A))
            )
        },
        containerColor = CompareBg
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (!adUnlocked) {
                CompareAdGate(
                    adState = adState,
                    onWatchAd = {
                        activity?.let { rewardedAdManager.showAd(it) { adUnlocked = true } }
                    },
                    onGetPremium = { showPremiumSheet = true }
                )
            } else {
                CompareContent(
                    queryLeft = queryLeft,
                    queryRight = queryRight,
                    leftSearch = leftSearch,
                    rightSearch = rightSearch,
                    leftPokemon = leftPokemon,
                    rightPokemon = rightPokemon,
                    leftLoading = leftLoading,
                    rightLoading = rightLoading,
                    onQueryChange = viewModel::onQueryChange,
                    onSelect = viewModel::select,
                    onClear = viewModel::clear
                )
            }

            if (showPremiumSheet) {
                val purchaseError = "Unable to start purchase"
                PremiumBottomSheet(
                    onDismiss = { showPremiumSheet = false },
                    onSubscribeMonthly = {
                        showPremiumSheet = false
                        val act = context as? Activity
                        if (act != null) billingViewModel.purchaseMonthly(act)
                        else Toast.makeText(context, purchaseError, Toast.LENGTH_SHORT).show()
                    },
                    onSubscribeYearly = {
                        showPremiumSheet = false
                        val act = context as? Activity
                        if (act != null) billingViewModel.purchaseYearly(act)
                        else Toast.makeText(context, purchaseError, Toast.LENGTH_SHORT).show()
                    },
                    onSubscribeLifetime = {
                        showPremiumSheet = false
                        val act = context as? Activity
                        if (act != null) billingViewModel.purchaseLifetime(act)
                        else Toast.makeText(context, purchaseError, Toast.LENGTH_SHORT).show()
                    },
                    monthlyPrice = monthlyPrice,
                    yearlyPrice = yearlyPrice,
                    lifetimePrice = lifetimePrice,
                    isSubscribeEnabled = isBillingReady
                )
            }
        }
    }
}

@Composable
private fun CompareAdGate(
    adState: RewardedAdState,
    onWatchAd: () -> Unit,
    onGetPremium: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(CompareBg), contentAlignment = Alignment.Center) {
        Column(
            Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Unlock the Compare Tool",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                "Watch a quick ad to compare Pokémon stats and types side by side this session.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            when (adState) {
                is RewardedAdState.Loading -> {
                    CircularProgressIndicator(color = Color(0xFF40C4FF), modifier = Modifier.size(40.dp))
                    Text("Loading ad…", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
                }
                is RewardedAdState.Ready, is RewardedAdState.Idle -> {
                    Button(
                        onClick = onWatchAd,
                        enabled = adState is RewardedAdState.Ready,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF40C4FF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (adState is RewardedAdState.Ready) "Watch Ad" else "Ad unavailable, try again shortly",
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
                is RewardedAdState.Showing -> {
                    CircularProgressIndicator(color = Color(0xFF40C4FF), modifier = Modifier.size(40.dp))
                }
            }
            Button(
                onClick = onGetPremium,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Go Premium", color = Color.White, modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun CompareContent(
    queryLeft: String,
    queryRight: String,
    leftSearch: SearchUiState,
    rightSearch: SearchUiState,
    leftPokemon: PokemonResponse?,
    rightPokemon: PokemonResponse?,
    leftLoading: Boolean,
    rightLoading: Boolean,
    onQueryChange: (Side, String) -> Unit,
    onSelect: (Side, String) -> Unit,
    onClear: (Side) -> Unit
) {
    val leftColor = leftPokemon?.let { pokemonTypeColor(it.types.firstOrNull()?.type?.name ?: "normal") } ?: LeftAccent
    val rightColor = rightPokemon?.let { pokemonTypeColor(it.types.firstOrNull()?.type?.name ?: "normal") } ?: RightAccent

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(leftColor.copy(alpha = 0.12f), CompareBg, rightColor.copy(alpha = 0.12f))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    ComparePickerField(
                        label = "First Pokémon",
                        accent = LeftAccent,
                        query = queryLeft,
                        searchState = leftSearch,
                        selected = leftPokemon,
                        isLoading = leftLoading,
                        onQueryChange = { onQueryChange(Side.LEFT, it) },
                        onSelect = { onSelect(Side.LEFT, it) },
                        onClear = { onClear(Side.LEFT) },
                        modifier = Modifier.weight(1f)
                    )
                    ComparePickerField(
                        label = "Second Pokémon",
                        accent = RightAccent,
                        query = queryRight,
                        searchState = rightSearch,
                        selected = rightPokemon,
                        isLoading = rightLoading,
                        onQueryChange = { onQueryChange(Side.RIGHT, it) },
                        onSelect = { onSelect(Side.RIGHT, it) },
                        onClear = { onClear(Side.RIGHT) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (leftPokemon != null && rightPokemon != null) {
                    VsBadge()
                }
            }

            AnimatedVisibility(
                visible = leftPokemon != null && rightPokemon != null,
                enter = fadeIn(tween(320)) + expandVertically(tween(320)),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
            ) {
                if (leftPokemon != null && rightPokemon != null) {
                    Column {
                        Spacer(Modifier.height(20.dp))
                        StatComparisonSection(leftPokemon, rightPokemon)
                        Spacer(Modifier.height(20.dp))
                        TypeComparisonSection(leftPokemon, rightPokemon)
                        Spacer(Modifier.height(20.dp))
                        EdgeVerdictCard(leftPokemon, rightPokemon)
                    }
                }
            }

            if (leftPokemon == null || rightPokemon == null) {
                Spacer(Modifier.height(48.dp))
                Text(
                    "Pick two Pokémon to see how they compare — no winners here, just the facts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun VsBadge() {
    val infinite = rememberInfiniteTransition(label = "vs")
    val scale by infinite.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "vsScale"
    )
    val rotation by infinite.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Reverse),
        label = "vsRotate"
    )
    Box(
        modifier = Modifier
            .size(40.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale; rotationZ = rotation }
            .clip(CircleShape)
            .background(
                Brush.horizontalGradient(listOf(LeftAccent, RightAccent))
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "VS",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
    }
}

private val STAT_ORDER = listOf("hp", "attack", "defense", "special-attack", "special-defense", "speed")
private const val MAX_STAT = 255f

@Composable
private fun StatComparisonSection(left: PokemonResponse, right: PokemonResponse) {
    val leftColor = pokemonTypeColor(left.types.firstOrNull()?.type?.name ?: "normal")
    val rightColor = pokemonTypeColor(right.types.firstOrNull()?.type?.name ?: "normal")
    val leftStats = left.stats.associate { it.stat.name to it.base_stat }
    val rightStats = right.stats.associate { it.stat.name to it.base_stat }

    SectionCard {
        Text("STATS", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Black)
        Spacer(Modifier.height(12.dp))

        STAT_ORDER.forEachIndexed { index, statKey ->
            val leftVal = leftStats[statKey] ?: 0
            val rightVal = rightStats[statKey] ?: 0

            val rowVisible = remember(left.name, right.name) { Animatable(0f) }
            LaunchedEffect(left.name, right.name) {
                delay(index * 70L)
                rowVisible.animateTo(1f, tween(280, easing = FastOutSlowInEasing))
            }
            val animatedLeft by animateIntAsState(
                targetValue = leftVal,
                animationSpec = tween(700, delayMillis = index * 70, easing = FastOutSlowInEasing),
                label = "leftStat"
            )
            val animatedRight by animateIntAsState(
                targetValue = rightVal,
                animationSpec = tween(700, delayMillis = index * 70, easing = FastOutSlowInEasing),
                label = "rightStat"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 7.dp)
                    .graphicsLayer {
                        alpha = rowVisible.value
                        translationY = (1f - rowVisible.value) * 16f
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = animatedLeft.toString(),
                    color = leftColor,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.width(32.dp),
                    textAlign = TextAlign.End
                )
                MirroredStatBar(
                    progress = (animatedLeft / MAX_STAT).coerceIn(0f, 1f),
                    color = leftColor,
                    modifier = Modifier.weight(1f).padding(horizontal = 6.dp)
                )
                Text(
                    text = statLabel(statKey),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.75f),
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(76.dp)
                )
                StatBar(
                    progress = (animatedRight / MAX_STAT).coerceIn(0f, 1f),
                    color = rightColor,
                    modifier = Modifier.weight(1f).padding(horizontal = 6.dp)
                )
                Text(
                    text = animatedRight.toString(),
                    color = rightColor,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.width(32.dp)
                )
            }
        }

        val leftTotal = leftStats.values.sum()
        val rightTotal = rightStats.values.sum()
        Spacer(Modifier.height(6.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("BST $leftTotal", color = leftColor, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
            Text("BST $rightTotal", color = rightColor, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun StatBar(progress: Float, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(10.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.08f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(10.dp)
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.6f), color)))
        )
    }
}

// Same bar, flipped so it fills from the right edge toward the shared center label.
@Composable
private fun MirroredStatBar(progress: Float, color: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier.graphicsLayer { rotationY = 180f }) {
        StatBar(progress = progress, color = color)
    }
}

@Composable
private fun TypeComparisonSection(left: PokemonResponse, right: PokemonResponse) {
    SectionCard {
        Text("TYPES", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Black)
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TypeColumn(left, modifier = Modifier.weight(1f))
            TypeColumn(right, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun TypeColumn(pokemon: PokemonResponse, modifier: Modifier = Modifier) {
    val types = pokemon.types.map { it.type.name }
    val weaknesses = pokemonTypeEffectiveness.keys
        .filter { typeEffectivenessMultiplier(it, types) > 1f }
        .sorted()
    val resistances = pokemonTypeEffectiveness.keys
        .filter { val m = typeEffectivenessMultiplier(it, types); m in 0f..0.99f }
        .sorted()

    Column(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            types.forEachIndexed { index, type ->
                var visible by remember(pokemon.name, type) { mutableStateOf(false) }
                LaunchedEffect(pokemon.name, type) {
                    delay(index * 100L)
                    visible = true
                }
                AnimatedVisibility(visible = visible, enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn()) {
                    TypeChip(type)
                }
            }
        }
        if (weaknesses.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(
                "Weak to",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.45f),
                fontWeight = FontWeight.Bold
            )
            Text(
                weaknesses.joinToString(", ") { it.replaceFirstChar { c -> c.uppercase() } },
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.75f)
            )
        }
        if (resistances.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Resists",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.45f),
                fontWeight = FontWeight.Bold
            )
            Text(
                resistances.joinToString(", ") { it.replaceFirstChar { c -> c.uppercase() } },
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
private fun TypeChip(type: String) {
    val color = pokemonTypeColor(type)
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.25f))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(type.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EdgeVerdictCard(left: PokemonResponse, right: PokemonResponse) {
    var revealed by remember(left.name, right.name) { mutableStateOf(false) }
    val soundManager: SoundManager = koinInject()
    val haptic = LocalHapticFeedback.current
    val verdict = remember(left.name, right.name) { computeEdgeVerdict(left, right) }

    val scale = remember(left.name, right.name) { Animatable(0.7f) }
    LaunchedEffect(revealed) {
        if (revealed) {
            scale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
        }
    }

    SectionCard {
        Text("THE EDGE", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Black)
        Spacer(Modifier.height(12.dp))

        Box(contentAlignment = Alignment.Center) {
            ConfettiBurst(visible = revealed)

            if (!revealed) {
                Button(
                    onClick = {
                        revealed = true
                        soundManager.play(SoundManager.Sound.LEVEL_UP)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reveal the Edge", fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.padding(vertical = 4.dp))
                }
            } else {
                val leanColor = when (verdict.leaningTo) {
                    EdgeVerdict.Side.LEFT -> pokemonTypeColor(left.types.firstOrNull()?.type?.name ?: "normal")
                    EdgeVerdict.Side.RIGHT -> pokemonTypeColor(right.types.firstOrNull()?.type?.name ?: "normal")
                    null -> Color.White.copy(alpha = 0.7f)
                }
                Text(
                    verdict.reason,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = leanColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Just for fun — type math + a curated fan-favorite list, not a battle predictor.",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.4f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = CardBg,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}
