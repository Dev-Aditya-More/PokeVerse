package com.aditya1875.pokeverse.feature.game.premium.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CatchingPokemon
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.game.core.data.billing.PremiumPlan
import com.aditya1875.pokeverse.feature.pokemon.profile.presentation.viewmodels.ProfileViewModel
import com.aditya1875.pokeverse.presentation.auth.AuthResult
import com.aditya1875.pokeverse.presentation.auth.AuthState
import com.aditya1875.pokeverse.presentation.viewmodel.BillingViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumBottomSheet(
    onDismiss: () -> Unit,
    onSubscribeMonthly: () -> Unit,
    onSubscribeYearly: () -> Unit,
    onSubscribeLifetime: () -> Unit,
    monthlyPrice: String,
    yearlyPrice: String,
    lifetimePrice: String,
    isSubscribeEnabled: Boolean,
    profileViewModel: ProfileViewModel = koinViewModel(),
    billingViewModel: BillingViewModel = koinViewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val authState by profileViewModel.authState.collectAsState()
    val isLoggedIn = authState is AuthState.Authenticated
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSigningIn by remember { mutableStateOf(false) }
    var signInError by remember { mutableStateOf<String?>(null) }
    var isRestoring by remember { mutableStateOf(false) }
    var restoreMessage by remember { mutableStateOf<String?>(null) }

    var selectedPlan by remember { mutableStateOf(PremiumPlan.YEARLY) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFD700).copy(alpha = 0.3f),
                                Color(0xFFFF8C00).copy(alpha = 0.1f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.premium_sheet_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.premium_sheet_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            val features = listOf(
                Triple(Icons.Default.EmojiEvents, stringResource(R.string.premium_feature_hard_mode_title), stringResource(R.string.premium_feature_hard_mode_subtitle)),
                Triple(Icons.Default.CatchingPokemon, stringResource(R.string.premium_feature_items_title), stringResource(R.string.premium_feature_items_subtitle)),
                Triple(Icons.Default.Palette, stringResource(R.string.premium_feature_themes_title), stringResource(R.string.premium_feature_themes_subtitle)),
            )

            features.forEach { (icon, title, subtitle) ->
                PremiumFeatureRow(
                    icon = icon,
                    title = title,
                    subtitle = subtitle
                )
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(8.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Spacer(Modifier.height(20.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedPlan == PremiumPlan.MONTHLY,
                    onClick = { selectedPlan = PremiumPlan.MONTHLY },
                    label = { Text(stringResource(R.string.premium_plan_monthly)) },
                    border = BorderStroke(
                        1.dp,
                        if (selectedPlan == PremiumPlan.MONTHLY)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.outlineVariant
                    )
                )
                FilterChip(
                    selected = selectedPlan == PremiumPlan.YEARLY,
                    onClick = { selectedPlan = PremiumPlan.YEARLY },
                    label = {
                        Row {
                            Text(stringResource(R.string.premium_plan_yearly))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.premium_plan_yearly_best), color = Color(0xFFFF9800), fontSize = 10.sp)
                        }
                    },
                    border = BorderStroke(
                        1.dp,
                        if (selectedPlan == PremiumPlan.YEARLY)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.outlineVariant
                    )
                )
                FilterChip(
                    selected = selectedPlan == PremiumPlan.LIFETIME,
                    onClick = { selectedPlan = PremiumPlan.LIFETIME },
                    label = { Text(stringResource(R.string.premium_plan_lifetime)) },
                    border = BorderStroke(
                        1.dp,
                        if (selectedPlan == PremiumPlan.LIFETIME)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val renewalNote = when (selectedPlan) {
                PremiumPlan.MONTHLY -> stringResource(R.string.premium_renewal_monthly)
                PremiumPlan.YEARLY -> stringResource(R.string.premium_renewal_yearly)
                PremiumPlan.LIFETIME -> stringResource(R.string.premium_renewal_lifetime)
            }
            Text(
                text = renewalNote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                val price = when (selectedPlan) {
                    PremiumPlan.MONTHLY -> monthlyPrice
                    PremiumPlan.YEARLY -> yearlyPrice
                    PremiumPlan.LIFETIME -> lifetimePrice
                }

                Text(
                    text = price.ifEmpty { stringResource(R.string.premium_price_loading) },
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(Modifier.width(4.dp))

                val suffix = when (selectedPlan) {
                    PremiumPlan.MONTHLY -> stringResource(R.string.premium_suffix_month)
                    PremiumPlan.YEARLY -> stringResource(R.string.premium_suffix_year)
                    PremiumPlan.LIFETIME -> stringResource(R.string.premium_suffix_onetime)
                }

                Text(text = suffix)
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.premium_not_required),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    if (!isLoggedIn) {
                        val activity = context.findActivity() ?: return@Button
                        scope.launch {
                            isSigningIn = true
                            signInError = null
                            val result = profileViewModel.signInWithGoogle(activity)
                            isSigningIn = false
                            if (result is AuthResult.Error && result.message != "cancelled") {
                                signInError = when {
                                    "no_credentials" in result.message ->
                                        context.getString(R.string.profile_error_no_credentials)
                                    "network" in result.message || "internet" in result.message ->
                                        context.getString(R.string.profile_error_no_internet)
                                    "sign_in_failed" in result.message || "credential_error" in result.message ->
                                        context.getString(R.string.profile_error_sign_in_failed)
                                    else ->
                                        context.getString(R.string.profile_error_generic)
                                }
                            }
                        }
                        return@Button
                    }
                    when (selectedPlan) {
                        PremiumPlan.MONTHLY -> onSubscribeMonthly()
                        PremiumPlan.YEARLY -> onSubscribeYearly()
                        PremiumPlan.LIFETIME -> onSubscribeLifetime()
                    }
                },
                enabled = (if (isLoggedIn) isSubscribeEnabled else true) && !isSigningIn && !isRestoring,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                if (isSigningIn) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        if (!isLoggedIn) stringResource(R.string.premium_sign_in_to_continue)
                        else stringResource(R.string.action_unlock_premium)
                    )
                }
            }

            if (!isLoggedIn) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.premium_sign_in_required),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            if (signInError != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = signInError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(4.dp))

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
                enabled = !isRestoring && !isSigningIn
            ) {
                if (isRestoring) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 1.5.dp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            stringResource(R.string.premium_restore_purchases),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        stringResource(R.string.premium_restore_purchases),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (restoreMessage != null) {
                Text(
                    text = restoreMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(4.dp))

            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.action_maybe_later),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.premium_payment_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }

        Spacer(Modifier.height(16.dp))
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun PremiumFeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFFFD700).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color(0xFFFFD700), modifier = Modifier.size(18.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }

        Icon(Icons.Default.Check, null, tint = Color(0xFF4CAF50))
    }
}
