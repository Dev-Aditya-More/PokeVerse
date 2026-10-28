package com.aditya1875.pokeverse.utils

/** Switches for features that are built but intentionally not shipped yet. */
object Features {
    /**
     * Coach's Read (Gemini via Firebase AI Logic). Off for now: the Gemini API needs prepaid credits,
     * which have run out. To bring it back: top up billing, finish the App Check setup in the Firebase
     * console (register the app, add debug tokens), then flip this to true. Nothing else needs changing.
     */
    const val COACH_ENABLED = false
}
