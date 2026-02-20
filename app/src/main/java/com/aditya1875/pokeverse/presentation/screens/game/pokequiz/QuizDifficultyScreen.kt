package com.aditya1875.pokeverse.presentation.screens.game.pokequiz

import android.app.Activity
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aditya1875.pokeverse.BuildConfig
import com.aditya1875.pokeverse.presentation.screens.game.pokematch.components.DifficultyCard
import com.aditya1875.pokeverse.presentation.screens.game.pokematch.components.PremiumBanner
import com.aditya1875.pokeverse.presentation.screens.game.pokequiz.components.QuizDifficulty
import com.aditya1875.pokeverse.presentation.ui.viewmodel.QuizViewModel
import com.aditya1875.pokeverse.presentation.viewmodel.BillingViewModel
import com.aditya1875.pokeverse.utils.Difficulty
import com.aditya1875.pokeverse.utils.SubscriptionState
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizDifficultyScreen(
    onDifficultySelected: (Difficulty) -> Unit,
    onBack: () -> Unit,
) {
    val viewModel : QuizViewModel = koinViewModel()
    var showPremiumSheet by remember { mutableStateOf(false) }
    val topScores by viewModel.topScores.collectAsState()
    val subscriptionState by viewModel.subscriptionState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = (context as? Activity)

    val billingViewModel: BillingViewModel = koinViewModel()
    val monthly by billingViewModel.monthlyPrice.collectAsStateWithLifecycle()
    val yearly by billingViewModel.yearlyPrice.collectAsStateWithLifecycle()
    val monthlyProduct by billingViewModel.monthlyProduct.collectAsStateWithLifecycle()
    val yearlyProduct by billingViewModel.yearlyProduct.collectAsStateWithLifecycle()
    val isBillingReady = monthlyProduct != null || yearlyProduct != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "PokéQuiz",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Test your Pokémon knowledge!",
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
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            item {
                Text(
                    text = "Select Difficulty",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            items(Difficulty.entries.toTypedArray()) { difficulty ->
                val canPlay = viewModel.canPlayDifficulty(difficulty)
                DifficultyCard(
                    difficulty = difficulty,
                    canPlay = canPlay,
                    bestScore = topScores
                        .filter { it.difficulty == difficulty.name }
                        .maxByOrNull { it.score },
                    onSelect = {
                        if (canPlay) {
                            onDifficultySelected(difficulty)
                        } else {
                            showPremiumSheet = true
                        }
                    }
                )
            }

            if (BuildConfig.ENABLE_BILLING && subscriptionState is SubscriptionState.Free) {
                item {
                    PremiumBanner(
                        price = monthly,
                        onSubscribe = { showPremiumSheet = true }
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
