package com.aditya1875.pokeverse.feature.pokemon.settings.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenter

/**
 * RevenueCat's Customer Center — lets a subscriber manage/cancel their plan
 * and see subscription FAQs without contacting support.
 */
@Composable
fun SubscriptionCustomerCenter(visible: Boolean, onDismiss: () -> Unit) {
    if (visible) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            CustomerCenter(onDismiss = onDismiss)
        }
    }
}
