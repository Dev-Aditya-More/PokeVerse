package com.aditya1875.pokeverse.feature.pokemon.settings.presentation.components

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri

/**
 * Opens Google Play's native subscription-management page. Used to show RevenueCat's
 * Customer Center here; that's detached along with the rest of RevenueCat for now (see
 * BillingManager), so this falls back to the standard pre-RevenueCat approach — Play's own
 * subscriptions screen handles cancel/change-plan/refund without needing any in-app UI.
 */
@Composable
fun SubscriptionCustomerCenter(visible: Boolean, onDismiss: () -> Unit) {
    if (!visible) return

    val context = LocalContext.current
    val uri = "https://play.google.com/store/account/subscriptions?package=${context.packageName}".toUri()
    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    onDismiss()
}
