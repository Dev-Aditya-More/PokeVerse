package com.aditya1875.pokeverse.feature.game.premium.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CatchingPokemon
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.game.core.data.billing.PremiumPlan
import com.aditya1875.pokeverse.feature.game.core.data.billing.SubscriptionState
import com.aditya1875.pokeverse.presentation.viewmodel.BillingViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private val Gold = Color(0xFFFFC53D)
private val GoldDeep = Color(0xFFFF8F1F)

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/**
 * App-wide premium upsell, drawn with the app's own theme. Prices and the purchase itself come
 * from RevenueCat (via [BillingViewModel]); no sign-in is needed. The sheet closes by itself the
 * moment the `dexverse_pro` entitlement becomes active.
 *
 * The per-plan callbacks/prices are the old sheet's plumbing; they're kept (ignored) so existing
 * call sites keep compiling — the sheet now talks to [BillingViewModel] directly.
 */
@Suppress("UNUSED_PARAMETER")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumBottomSheet(
    onDismiss: () -> Unit,
    onSubscribeMonthly: () -> Unit = {},
    onSubscribeYearly: () -> Unit = {},
    onSubscribeLifetime: () -> Unit = {},
    monthlyPrice: String = "",
    yearlyPrice: String = "",
    lifetimePrice: String = "",
    isSubscribeEnabled: Boolean = true,
    billingViewModel: BillingViewModel = koinViewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val subscriptionState by billingViewModel.subscriptionState.collectAsStateWithLifecycle()
    val monthly by billingViewModel.monthlyPrice.collectAsStateWithLifecycle()
    val yearly by billingViewModel.yearlyPrice.collectAsStateWithLifecycle()
    val lifetime by billingViewModel.lifetimePrice.collectAsStateWithLifecycle()
    val purchasing by billingViewModel.purchaseInProgress.collectAsStateWithLifecycle()
    val billingError by billingViewModel.billingError.collectAsStateWithLifecycle()

    var selectedPlan by remember { mutableStateOf(PremiumPlan.YEARLY) }
    var isRestoring by remember { mutableStateOf(false) }
    var restoreMessage by remember { mutableStateOf<String?>(null) }

    // Unlocked (bought here, restored, or already owned): nothing left to sell
    LaunchedEffect(subscriptionState) {
        if (subscriptionState is SubscriptionState.Premium) onDismiss()
    }
    // Make sure prices exist whenever the sheet opens (no-op if already loaded)
    LaunchedEffect(Unit) {
        billingViewModel.clearError()
        billingViewModel.retryLoadPlans()
    }

    val pricesReady = monthly.isNotEmpty() && yearly.isNotEmpty() && lifetime.isNotEmpty()
    val busy = purchasing || isRestoring
    val selectedPrice = when (selectedPlan) {
        PremiumPlan.MONTHLY -> monthly
        PremiumPlan.YEARLY -> yearly
        PremiumPlan.LIFETIME -> lifetime
    }

    ModalBottomSheet(
        onDismissRequest = { if (!purchasing) onDismiss() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PremiumHero()

            Spacer(Modifier.height(14.dp))

            Text(
                text = stringResource(R.string.premium_sheet_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.premium_sheet_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(18.dp))

            // Benefits
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BenefitRow(
                        Icons.Default.EmojiEvents,
                        stringResource(R.string.premium_feature_hard_mode_title),
                        stringResource(R.string.premium_feature_hard_mode_subtitle)
                    )
                    BenefitRow(
                        Icons.Default.CatchingPokemon,
                        stringResource(R.string.premium_feature_items_title),
                        stringResource(R.string.premium_feature_items_subtitle)
                    )
                    BenefitRow(
                        Icons.Default.Palette,
                        stringResource(R.string.premium_feature_themes_title),
                        stringResource(R.string.premium_feature_themes_subtitle)
                    )
                    BenefitRow(
                        Icons.Default.Block,
                        stringResource(R.string.premium_feature_ad_free_title),
                        stringResource(R.string.premium_feature_ad_free_subtitle)
                    )
                    BenefitRow(
                        Icons.Default.AutoAwesome,
                        stringResource(R.string.premium_feature_shiny_title),
                        stringResource(R.string.premium_feature_shiny_subtitle)
                    )
                    BenefitRow(
                        Icons.Default.WorkspacePremium,
                        stringResource(R.string.premium_feature_flair_title),
                        stringResource(R.string.premium_feature_flair_subtitle)
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Plans
            if (pricesReady) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PlanCard(
                        title = stringResource(R.string.premium_plan_yearly),
                        price = yearly,
                        suffix = stringResource(R.string.premium_suffix_year),
                        badge = stringResource(R.string.premium_best_value),
                        selected = selectedPlan == PremiumPlan.YEARLY,
                        enabled = !busy,
                        onClick = { selectedPlan = PremiumPlan.YEARLY }
                    )
                    PlanCard(
                        title = stringResource(R.string.premium_plan_monthly),
                        price = monthly,
                        suffix = stringResource(R.string.premium_suffix_month),
                        selected = selectedPlan == PremiumPlan.MONTHLY,
                        enabled = !busy,
                        onClick = { selectedPlan = PremiumPlan.MONTHLY }
                    )
                    PlanCard(
                        title = stringResource(R.string.premium_plan_lifetime),
                        price = lifetime,
                        suffix = stringResource(R.string.premium_suffix_onetime),
                        selected = selectedPlan == PremiumPlan.LIFETIME,
                        enabled = !busy,
                        onClick = { selectedPlan = PremiumPlan.LIFETIME }
                    )
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = when (selectedPlan) {
                        PremiumPlan.MONTHLY -> stringResource(R.string.premium_renewal_monthly)
                        PremiumPlan.YEARLY -> stringResource(R.string.premium_renewal_yearly)
                        PremiumPlan.LIFETIME -> stringResource(R.string.premium_renewal_lifetime)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                PlansPlaceholder(
                    failed = billingError != null,
                    onRetry = {
                        billingViewModel.clearError()
                        billingViewModel.retryLoadPlans()
                    }
                )
            }

            // Errors (declined, offline, misconfigured entitlement...) are shown right here
            // instead of the sheet silently closing.
            if (billingError != null && pricesReady) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = billingError.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(16.dp))

            // CTA
            Button(
                onClick = {
                    val activity = context.findActivity() ?: return@Button
                    billingViewModel.clearError()
                    when (selectedPlan) {
                        PremiumPlan.MONTHLY -> billingViewModel.purchaseMonthly(activity)
                        PremiumPlan.YEARLY -> billingViewModel.purchaseYearly(activity)
                        PremiumPlan.LIFETIME -> billingViewModel.purchaseLifetime(activity)
                    }
                },
                enabled = pricesReady && !busy && subscriptionState !is SubscriptionState.Pending,
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .alpha(if (pricesReady && !busy) 1f else 0.55f)
                    .background(
                        Brush.horizontalGradient(
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                        )
                    )
            ) {
                if (purchasing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = if (subscriptionState is SubscriptionState.Pending)
                            "Purchase pending…"
                        else if (selectedPrice.isNotEmpty())
                            "${stringResource(R.string.action_unlock_premium)}  ·  $selectedPrice"
                        else stringResource(R.string.action_unlock_premium),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        scope.launch {
                            isRestoring = true
                            restoreMessage = null
                            val restored = billingViewModel.restorePurchases()
                            isRestoring = false
                            restoreMessage = if (!restored)
                                context.getString(R.string.premium_restore_not_found)
                            else null
                        }
                    },
                    enabled = !busy
                ) {
                    if (isRestoring) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 1.5.dp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        stringResource(R.string.premium_restore_purchases),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onDismiss, enabled = !purchasing) {
                    Text(
                        stringResource(R.string.action_maybe_later),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (restoreMessage != null) {
                Text(
                    text = restoreMessage.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.premium_payment_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 17.sp
            )
        }

        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun PremiumHero() {
    val pulse by rememberInfiniteTransition(label = "hero").animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
        label = "glow"
    )
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(96.dp)) {
        Box(
            Modifier
                .size(96.dp)
                .background(
                    Brush.radialGradient(listOf(Gold.copy(alpha = 0.40f * pulse), Color.Transparent)),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(Brush.linearGradient(listOf(Gold, GoldDeep)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.WorkspacePremium,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
private fun BenefitRow(icon: ImageVector, title: String, subtitle: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PlanCard(
    title: String,
    price: String,
    suffix: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    badge: String? = null
) {
    val borderColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        label = "plan_border"
    )
    val container by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        else MaterialTheme.colorScheme.surfaceContainerLow,
        label = "plan_bg"
    )

    Box {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = container,
            border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable(enabled = enabled, onClick = onClick)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (selected) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = price,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = suffix,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (badge != null) {
            Text(
                text = badge,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = Color(0xFF3A2500),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 14.dp)
                    .offset(y = (-8).dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(Gold, GoldDeep)))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun PlansPlaceholder(failed: Boolean, onRetry: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (failed) {
                Text(
                    text = stringResource(R.string.premium_prices_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                TextButton(onClick = onRetry) { Text(stringResource(R.string.premium_retry)) }
            } else {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                Text(
                    text = stringResource(R.string.premium_price_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
