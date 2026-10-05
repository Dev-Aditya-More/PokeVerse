package com.aditya1875.pokeverse.feature.pokemon.settings.presentation.components

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenter

/**
 * RevenueCat Customer Center: lets subscribers manage/cancel/change plan, restore purchases and
 * request refunds, all configured from the RevenueCat dashboard (Tools → Customer Center).
 * Works for the anonymous RevenueCat user the app uses, so no sign-in is involved.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionCustomerCenter(visible: Boolean, onDismiss: () -> Unit) {
    if (!visible) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        CustomerCenter(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            onDismiss = onDismiss
        )
    }
}
