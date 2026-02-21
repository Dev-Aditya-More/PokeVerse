package com.aditya1875.pokeverse.presentation.screens.game

import android.app.Activity
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aditya1875.pokeverse.BuildConfig
import com.aditya1875.pokeverse.data.billing.SubscriptionState
import com.aditya1875.pokeverse.presentation.screens.game.pokematch.components.PremiumBanner
import com.aditya1875.pokeverse.presentation.screens.game.pokematch.components.PremiumBottomSheet
import com.aditya1875.pokeverse.presentation.ui.viewmodel.MatchViewModel
import com.aditya1875.pokeverse.presentation.viewmodel.BillingViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameHubScreen(
    onGameSelected: (String) -> Unit,
    viewModel: MatchViewModel = koinViewModel()
) {
    val subscriptionState by viewModel.subscriptionState.collectAsStateWithLifecycle()
    var showPremiumSheet by remember { mutableStateOf(false) }

    val billingViewModel: BillingViewModel = koinViewModel()
    val monthly by billingViewModel.monthlyPrice.collectAsStateWithLifecycle()
    val yearly by billingViewModel.yearlyPrice.collectAsStateWithLifecycle()
    val monthlyProduct by billingViewModel.monthlyProduct.collectAsStateWithLifecycle()
    val yearlyProduct by billingViewModel.yearlyProduct.collectAsStateWithLifecycle()
    val isBillingReady = monthlyProduct != null || yearlyProduct != null

    var showThankYouDialog by rememberSaveable { mutableStateOf(false) }
    var hasShownThankYou by rememberSaveable { mutableStateOf(false) }

    var lastSubscriptionState by rememberSaveable {
        mutableStateOf<SubscriptionState>(SubscriptionState.Loading)
    }

    val context = LocalContext.current
    val activity = context as? Activity

    LaunchedEffect(subscriptionState) {
        val wasFree = lastSubscriptionState is SubscriptionState.Free
        val isNowPremium = subscriptionState is SubscriptionState.Premium

        if (wasFree && isNowPremium && !hasShownThankYou) {
            showThankYouDialog = true
            hasShownThankYou = true
        }

        lastSubscriptionState = subscriptionState
    }

    data class GameEntry(
        val id: String,
        val title: String,
        val description: String,
        val icon: ImageVector,
        val accentColor: Color,
        val tag: String,
        val stats: String = ""
    )

    val games = listOf(
        GameEntry(
            id = "pokematch",
            title = "PokéMatch",
            description = "Flip cards and match Pokémon pairs before time runs out!",
            icon = Icons.Default.GridView,
            accentColor = Color(0xFF4CAF50),
            tag = "Memory",
            stats = "3 difficulties"
        ),
        GameEntry(
            id = "pokequiz",
            title = "PokéQuiz",
            description = "Test your Pokémon knowledge across generations!",
            icon = Icons.Default.Quiz,
            accentColor = Color(0xFF2196F3),
            tag = "Trivia",
            stats = "10 questions"
        ),
        GameEntry(
            id = "pokeguess",
            title = "Who's That Pokémon?",
            description = "Guess the Pokémon from its silhouette!",
            icon = Icons.Default.Visibility,
            accentColor = Color(0xFF9C27B0),
            tag = "Guess",
            stats = "Classic anime style"
        )
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Mini Games",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Take a break, have fun!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                },
                actions = {
                    if (subscriptionState is SubscriptionState.Premium) {
                        Box(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFFFD700).copy(alpha = 0.15f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Premium",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFD700)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            items(games) { game ->
                FeaturedGameCard(
                    title = game.title,
                    description = game.description,
                    icon = game.icon,
                    accentColor = game.accentColor,
                    tag = game.tag,
                    stats = game.stats,
                    onClick = {
                        onGameSelected(game.id)
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

            item { Spacer(Modifier.height(24.dp)) }
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
            monthlyPrice = monthly,
            yearlyPrice = yearly,
            isSubscribeEnabled = isBillingReady
        )
    }

    if (showThankYouDialog) {
        PremiumWelcomeDialog(
            onDismiss = { showThankYouDialog = false }
        )
    }
}

@Composable
fun FeaturedGameCard(
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    tag: String,
    stats: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(6.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Background glow
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                drawCircle(
                    color = accentColor.copy(alpha = 0.06f),
                    radius = size.width * 0.7f,
                    center = Offset(size.width * 0.85f, size.height * 0.2f)
                )
            }

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
                    // Icon
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Free tag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(accentColor.copy(alpha = 0.15f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "FREE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = accentColor,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Text(
                            text = stats,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Play button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Play",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

//@Composable
//fun LockedGameCard(
//    title: String,
//    description: String,
//    icon: ImageVector,
//    accentColor: Color,
//    tag: String,
//    onClick: () -> Unit
//) {
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .clickable(onClick = onClick),
//        shape = RoundedCornerShape(16.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.surface
//        ),
//        elevation = CardDefaults.cardElevation(2.dp),
//        border = BorderStroke(
//            1.dp,
//            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
//        )
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(16.dp),
//            verticalAlignment = Alignment.CenterVertically,
//            horizontalArrangement = Arrangement.spacedBy(16.dp)
//        ) {
//            Box(
//                modifier = Modifier
//                    .size(52.dp)
//                    .clip(RoundedCornerShape(14.dp))
//                    .background(
//                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
//                    ),
//                contentAlignment = Alignment.Center
//            ) {
//                Icon(
//                    imageVector = icon,
//                    contentDescription = null,
//                    tint = accentColor.copy(alpha = 0.35f),
//                    modifier = Modifier.size(28.dp)
//                )
//            }
//
//            Column(modifier = Modifier.weight(1f)) {
//                Row(
//                    verticalAlignment = Alignment.CenterVertically,
//                    horizontalArrangement = Arrangement.spacedBy(8.dp)
//                ) {
//                    Text(
//                        text = title,
//                        style = MaterialTheme.typography.titleMedium,
//                        fontWeight = FontWeight.SemiBold,
//                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
//                    )
//                    Box(
//                        modifier = Modifier
//                            .clip(RoundedCornerShape(6.dp))
//                            .background(MaterialTheme.colorScheme.surfaceVariant)
//                            .padding(horizontal = 6.dp, vertical = 2.dp)
//                    ) {
//                        Text(
//                            text = tag,
//                            style = MaterialTheme.typography.labelSmall,
//                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
//                        )
//                    }
//                }
//
//                Spacer(Modifier.height(2.dp))
//
//                Text(
//                    text = description,
//                    style = MaterialTheme.typography.bodySmall,
//                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
//                    maxLines = 1,
//                    overflow = TextOverflow.Ellipsis
//                )
//            }
//
//            // Lock icon
//            Box(
//                modifier = Modifier
//                    .size(36.dp)
//                    .clip(CircleShape)
//                    .background(Color(0xFFFFD700).copy(alpha = 0.1f)),
//                contentAlignment = Alignment.Center
//            ) {
//                Icon(
//                    imageVector = Icons.Default.Lock,
//                    contentDescription = "Locked",
//                    tint = Color(0xFFFFD700).copy(alpha = 0.8f),
//                    modifier = Modifier.size(18.dp)
//                )
//            }
//        }
//    }
//}