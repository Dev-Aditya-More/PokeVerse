package com.aditya1875.pokeverse.feature.game.core.presentation

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aditya1875.pokeverse.BuildConfig
import com.aditya1875.pokeverse.feature.game.core.data.billing.SubscriptionState
import com.aditya1875.pokeverse.feature.game.premium.components.PremiumBanner
import com.aditya1875.pokeverse.feature.game.premium.components.PremiumBottomSheet
import com.aditya1875.pokeverse.presentation.viewmodel.BillingViewModel
import org.koin.androidx.compose.koinViewModel
import com.aditya1875.pokeverse.feature.game.core.presentation.backdrop.GameBackdrop
import com.aditya1875.pokeverse.feature.game.core.presentation.backdrop.GameScene
import androidx.compose.ui.graphics.Color

@Suppress("MultipleContentEmitters")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDifficultyLayout(
    gameTitle: String,
    gameSubtitle: String,
    difficultyHint: String,
    onBack: () -> Unit,
    subscriptionState: SubscriptionState,
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit
) {

    var showPremiumSheet by remember { mutableStateOf(false) }

    @Suppress("ExplicitDependencies") val billingViewModel: BillingViewModel = koinViewModel()
    val monthly by billingViewModel.monthlyPrice.collectAsStateWithLifecycle()
    val yearly by billingViewModel.yearlyPrice.collectAsStateWithLifecycle()
    val lifetime by billingViewModel.lifetimePrice.collectAsStateWithLifecycle()

    val isBillingReady = monthly.isNotBlank() || yearly.isNotBlank() || lifetime.isNotBlank()

    val context = LocalContext.current
    val activity = context as? Activity

    val isPremium = subscriptionState is SubscriptionState.Premium

    GameBackdrop(scene = GameScene.Hub, modifier = modifier) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(gameTitle, fontWeight = FontWeight.Bold)
                            Text(
                                gameSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color.Transparent
        ) { padding ->

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                item(contentType = "contentType1") { Spacer(Modifier.height(8.dp)) }

                item(contentType = "contentType2") {
                    Column {
                        Text(
                            "Select Difficulty",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            difficultyHint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }

                content()

                if (BuildConfig.ENABLE_BILLING && !isPremium) {
                    item(contentType = "contentType3") {
                        PremiumBanner(
                            price = monthly,
                            onSubscribe = { showPremiumSheet = true }
                        )
                    }
                }

                item(contentType = "contentType4") { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    if (showPremiumSheet) {
        PremiumBottomSheet(
            onDismiss = { showPremiumSheet = false },
            onSubscribeMonthly = {
                showPremiumSheet = false
                activity?.let { billingViewModel.purchaseMonthly(it) }
            },
            onSubscribeYearly = {
                showPremiumSheet = false
                activity?.let { billingViewModel.purchaseYearly(it) }
            },
            onSubscribeLifetime = {
                showPremiumSheet = false
                activity?.let { billingViewModel.purchaseLifetime(it) }
            },
            lifetimePrice = lifetime,
            monthlyPrice = monthly,
            yearlyPrice = yearly,
            isSubscribeEnabled = isBillingReady
        )
    }
}