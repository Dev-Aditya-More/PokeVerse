package com.aditya1875.pokeverse

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * Debug builds: the debug provider. On first use it logs a secret ("Enter this debug secret into the
 * allow list...") that must be added under App Check > Apps > Manage debug tokens in the Firebase console.
 */
object AppCheckSetup {
    fun install() {
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
    }
}
