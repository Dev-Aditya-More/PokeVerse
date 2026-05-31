package com.aditya1875.pokeverse.feature.analysis.presentation.screens

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aditya1875.pokeverse.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.aditya1875.pokeverse.feature.core.navigation.components.Route
import com.aditya1875.pokeverse.feature.game.core.data.ads.IRewardedAdManager
import com.aditya1875.pokeverse.feature.game.core.data.ads.RewardedAdState
import com.aditya1875.pokeverse.feature.game.core.data.billing.IBillingManager
import com.aditya1875.pokeverse.feature.game.core.data.billing.SubscriptionState
import org.koin.compose.koinInject
import com.aditya1875.pokeverse.feature.analysis.presentation.screens.AnalysisColors.BLUE
import com.aditya1875.pokeverse.feature.analysis.presentation.components.AnalysisContent
import com.aditya1875.pokeverse.feature.analysis.presentation.components.ErrorView
import com.aditya1875.pokeverse.feature.analysis.presentation.components.LoadingView
import com.aditya1875.pokeverse.feature.analysis.presentation.components.TeamAnalysis
import com.aditya1875.pokeverse.feature.analysis.presentation.components.TeamAnalyzer
import com.aditya1875.pokeverse.feature.analysis.presentation.components.TeamMemberWithTypes
import com.aditya1875.pokeverse.feature.game.premium.components.PremiumBottomSheet
import com.aditya1875.pokeverse.feature.pokemon.detail.presentation.viewmodels.PokemonDetailsViewModel
import com.aditya1875.pokeverse.feature.team.presentation.viewmodels.TeamViewModel
import com.aditya1875.pokeverse.presentation.viewmodel.BillingViewModel
import org.koin.androidx.compose.koinViewModel

object AnalysisColors {
    val BG = Color(0xFF0B0E17)
    val CARD = Color(0xFF141820)
    val CARD2 = Color(0xFF1C2230)
    val GREEN = Color(0xFF00E676)
    val RED = Color(0xFFFF4C60)
    val AMBER = Color(0xFFFFB300)
    val BLUE = Color(0xFF40C4FF)
    val PURPLE = Color(0xFFCE93D8)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TeamAnalysisScreen(
    navController: NavController,
    viewModel: TeamViewModel = koinViewModel(),
    pokemonDetailsViewModel: PokemonDetailsViewModel = koinViewModel(),
    teamId: String? = null
) {
    val billingManager: IBillingManager = koinInject()
    val subscriptionState by billingManager.subscriptionState.collectAsState()
    val isPremium = subscriptionState is SubscriptionState.Premium

    var showPremiumSheet by remember { mutableStateOf(false) }
    val billingViewModel: BillingViewModel = koinViewModel()
    val monthlyPrice by billingViewModel.monthlyPrice.collectAsStateWithLifecycle()
    val yearlyPrice by billingViewModel.yearlyPrice.collectAsStateWithLifecycle()
    val lifetimePrice by billingViewModel.lifetimePrice.collectAsStateWithLifecycle()
    val monthlyProduct by billingViewModel.monthlyProduct.collectAsStateWithLifecycle()
    val yearlyProduct by billingViewModel.yearlyProduct.collectAsStateWithLifecycle()
    val lifetimeProduct by billingViewModel.lifetimeProduct.collectAsStateWithLifecycle()
    val isBillingReady = monthlyProduct != null || yearlyProduct != null || lifetimeProduct != null

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

    val team by remember(teamId) {
        if (teamId != null) {
            viewModel.getTeamMembers(teamId)
        } else {
            viewModel.currentTeamMembers
        }
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    var teamWithTypes by remember { mutableStateOf<List<TeamMemberWithTypes>>(emptyList()) }
    var analysis by remember { mutableStateOf<TeamAnalysis?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Fetch types when team changes
    LaunchedEffect(team) {
        if (team.isEmpty()) {
            teamWithTypes = emptyList()
            analysis = null
            return@LaunchedEffect
        }

        isLoading = true
        errorMessage = null

        try {
            val withTypes = mutableListOf<TeamMemberWithTypes>()

            team.forEach { member ->
                try {
                    val pokemon = pokemonDetailsViewModel.getPokemonByName(member.name)
                    val types = pokemon.types.map { it.type.name }

                    withTypes.add(
                        TeamMemberWithTypes(
                            name = member.name,
                            types = types,
                            imageUrl = member.imageUrl
                        )
                    )
                } catch (e: Exception) {
                    Log.e("TeamAnalysis", "Failed to fetch data for ${member.name}", e)
                    withTypes.add(
                        TeamMemberWithTypes(
                            name = member.name,
                            types = listOf("normal"),
                            imageUrl = member.imageUrl
                        )
                    )
                }
            }

            teamWithTypes = withTypes
            analysis = TeamAnalyzer.analyzeTeam(withTypes)

            Log.d("TeamAnalysis", "Analysis complete: score=${analysis?.coverageScore}")

        } catch (e: Exception) {
            Log.e("TeamAnalysis", "Failed to analyze team", e)
            errorMessage = "Failed to analyze team: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        containerColor = Color(0xFF0F0F0F),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.screen_title_team_analysis),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A1A)
                )
            )
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                team.isEmpty() -> EmptyAnalysisView(navController)

                !adUnlocked -> AnalysisAdGate(
                    adState = adState,
                    onWatchAd = {
                        activity?.let {
                            rewardedAdManager.showAd(it) { adUnlocked = true }
                        }
                    },
                    onGetPremium = { showPremiumSheet = true }
                )

                isLoading -> LoadingView()

                errorMessage != null -> ErrorView(errorMessage!!, navController)

                analysis != null -> AnalysisContent(
                    analysis = analysis!!,
                    teamWithTypes = teamWithTypes
                )
            }
        }

        if (showPremiumSheet) {
            val purchaseError = stringResource(R.string.game_hub_purchase_error)
            PremiumBottomSheet(
                onDismiss = { showPremiumSheet = false },
                onSubscribeMonthly = {
                    showPremiumSheet = false
                    val activity = context as? Activity
                    if (activity != null) billingViewModel.purchaseMonthly(activity)
                    else Toast.makeText(context, purchaseError, Toast.LENGTH_SHORT).show()
                },
                onSubscribeYearly = {
                    showPremiumSheet = false
                    val activity = context as? Activity
                    if (activity != null) billingViewModel.purchaseYearly(activity)
                    else Toast.makeText(context, purchaseError, Toast.LENGTH_SHORT).show()
                },
                onSubscribeLifetime = {
                    showPremiumSheet = false
                    val activity = context as? Activity
                    if (activity != null) billingViewModel.purchaseLifetime(activity)
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

@Composable
private fun AnalysisAdGate(
    adState: RewardedAdState,
    onWatchAd: () -> Unit,
    onGetPremium: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(AnalysisColors.BG),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                stringResource(R.string.analysis_ad_title),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                stringResource(R.string.analysis_ad_body),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            when (adState) {
                is RewardedAdState.Loading -> {
                    CircularProgressIndicator(color = BLUE, modifier = Modifier.size(40.dp))
                    Text(
                        stringResource(R.string.analysis_ad_loading),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
                is RewardedAdState.Ready, is RewardedAdState.Idle -> {
                    Button(
                        onClick = onWatchAd,
                        enabled = adState is RewardedAdState.Ready,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BLUE),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (adState is RewardedAdState.Ready) stringResource(R.string.action_watch_ad)
                            else stringResource(R.string.analysis_ad_unavailable),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
                is RewardedAdState.Showing -> {
                    CircularProgressIndicator(color = BLUE, modifier = Modifier.size(40.dp))
                }
            }
            Button(
                onClick = onGetPremium,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.12f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(R.string.action_go_premium),
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyAnalysisView(navController: NavController) {
    Box(Modifier.fillMaxSize().background(AnalysisColors.BG), contentAlignment = Alignment.Center) {
        Column(
            Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("📋", fontSize = 56.sp)
            Text(stringResource(R.string.analysis_empty_title), style = MaterialTheme.typography.headlineSmall,
                color = Color.White, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.analysis_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f), textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { navController.navigate(Route.BottomBar.Home.route) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BLUE)
            ) {
                Text(stringResource(R.string.action_build_team), modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }
        }
    }
}