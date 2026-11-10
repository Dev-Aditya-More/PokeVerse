package com.aditya1875.pokeverse.utils

/** Switches for features that are built but can be turned off without touching anything else. */
object Features {
    /**
     * Coach's Read (Gemini via Firebase AI Logic). Needs the Gemini API billing to be funded and
     * App Check set up in the Firebase console (register the app, add debug tokens). If either
     * lapses, flip this to false and the card disappears; nothing else needs changing.
     */
    const val COACH_ENABLED = true
}
