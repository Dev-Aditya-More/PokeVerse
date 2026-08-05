package com.aditya1875.pokeverse.feature.pokemon.settings.presentation.components

import androidx.compose.runtime.Composable

// FOSS build has no billing/subscriptions, so there's nothing to manage.
@Composable
fun SubscriptionCustomerCenter(visible: Boolean, onDismiss: () -> Unit) {
    if (visible) onDismiss()
}
