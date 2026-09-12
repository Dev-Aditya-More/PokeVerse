package com.aditya1875.pokeverse.feature.battlestats.presentation.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aditya1875.pokeverse.feature.battlestats.presentation.viewmodels.StatCalculatorViewModel
import com.aditya1875.pokeverse.feature.compare.presentation.components.ComparePickerField
import com.aditya1875.pokeverse.feature.game.core.data.ads.IRewardedAdManager
import com.aditya1875.pokeverse.feature.game.core.data.ads.RewardedAdState
import com.aditya1875.pokeverse.feature.game.core.data.billing.IBillingManager
import com.aditya1875.pokeverse.feature.game.core.data.billing.SubscriptionState
import com.aditya1875.pokeverse.feature.game.premium.components.PremiumBottomSheet
import com.aditya1875.pokeverse.feature.pokemon.detail.presentation.components.GoBattlePowerCard
import com.aditya1875.pokeverse.feature.pokemon.detail.presentation.components.StatCalculatorCard
import com.aditya1875.pokeverse.presentation.viewmodel.BillingViewModel
import com.aditya1875.pokeverse.utils.pokemonTypeColor
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

private val CalculatorBg = Color(0xFF0B0E17)
private val PickerAccent = Color(0xFF66BB6A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandaloneStatCalculatorScreen(
    onBack: () -> Unit,
    viewModel: StatCalculatorViewModel = koinViewModel()
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

    val query by viewModel.query.collectAsStateWithLifecycle()
    val search by viewModel.search.collectAsStateWithLifecycle()
    val pokemon by viewModel.pokemon.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stat Calculator", color = Color.White, fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A1A1A))
            )
        },
        containerColor = CalculatorBg
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (!adUnlocked) {
                CalculatorAdGate(
                    adState = adState,
                    onWatchAd = {
                        activity?.let { rewardedAdManager.showAd(it) { adUnlocked = true } }
                    },
                    onGetPremium = { showPremiumSheet = true }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    ComparePickerField(
                        label = "Pick a Pokémon",
                        accent = PickerAccent,
                        query = query,
                        searchState = search,
                        selected = pokemon,
                        isLoading = loading,
                        onQueryChange = viewModel::onQueryChange,
                        onSelect = viewModel::select,
                        onClear = viewModel::clear,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val currentPokemon = pokemon
                    if (currentPokemon != null) {
                        val accentColor = pokemonTypeColor(currentPokemon.types.firstOrNull()?.type?.name ?: "normal")
                        Spacer(Modifier.height(20.dp))
                        StatCalculatorCard(pokemon = currentPokemon, accentColor = accentColor)
                        Spacer(Modifier.height(16.dp))
                        GoBattlePowerCard(pokemonId = currentPokemon.id, accentColor = accentColor)
                    } else {
                        Spacer(Modifier.height(48.dp))
                        Text(
                            "Pick a Pokémon to calculate its IV/EV stats and GO combat power.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
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
private fun CalculatorAdGate(
    adState: RewardedAdState,
    onWatchAd: () -> Unit,
    onGetPremium: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(CalculatorBg), contentAlignment = Alignment.Center) {
        Column(
            Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Unlock the Stat Calculator",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                "Watch a quick ad to calculate IV/EV stats and GO combat power for any Pokémon this session.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            when (adState) {
                is RewardedAdState.Loading -> {
                    CircularProgressIndicator(color = PickerAccent, modifier = Modifier.size(40.dp))
                    Text("Loading ad…", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
                }
                is RewardedAdState.Ready, is RewardedAdState.Idle -> {
                    Button(
                        onClick = onWatchAd,
                        enabled = adState is RewardedAdState.Ready,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PickerAccent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (adState is RewardedAdState.Ready) "Watch Ad" else "Ad unavailable, try again shortly",
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
                is RewardedAdState.Showing -> {
                    CircularProgressIndicator(color = PickerAccent, modifier = Modifier.size(40.dp))
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
