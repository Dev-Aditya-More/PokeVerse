package com.aditya1875.pokeverse.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.annotation.RawRes
import com.aditya1875.pokeverse.R

class SoundManager(private val context: Context) {

    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<Sound, Int>()
    private var isEnabled = true

    enum class Sound {
        // UI Sounds
        BUTTON_CLICK,
        BACK,
        SUCCESS,
        ERROR,

        // Game Sounds
        CARD_FLIP,
        MATCH_FOUND,
        GAME_WIN,
        GAME_LOSE,
        TIMER_UP,

        // Quiz Sounds
        CORRECT_ANSWER,
        WRONG_ANSWER,
        QUIZ_COMPLETE,

        // Guess Sounds
        POKEMON_APPEAR,
        POKEMON_REVEAL,
        GUESS_CORRECT,
        GUESS_WRONG
    }

    init {
        initSoundPool()
    }

    private fun initSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        loadSounds()
    }

    private fun loadSounds() {
        // Load all sound files
        // For now using system sounds, replace with custom sounds later
        soundMap[Sound.BUTTON_CLICK] = loadSound(R.raw.button_click)
        soundMap[Sound.GAME_WIN] = loadSound(R.raw.game_win)
        soundMap[Sound.CARD_FLIP] = loadSound(R.raw.card_flip)
        soundMap[Sound.MATCH_FOUND] = loadSound(R.raw.match_found)
        soundMap[Sound.GAME_LOSE] = loadSound(R.raw.wrong_answer)
        soundMap[Sound.CORRECT_ANSWER] = loadSound(R.raw.correct_answer)
        soundMap[Sound.WRONG_ANSWER] = loadSound(R.raw.wrong_answer)
        soundMap[Sound.TIMER_UP] = loadSound(R.raw.time_up)
        // Add more mappings when you have custom sound files
    }

    private fun loadSound(@RawRes resId: Int): Int {
        return soundPool?.load(context, resId, 1) ?: -1
    }

    fun play(sound: Sound, volume: Float = 1.0f) {
        if (!isEnabled) return

        soundMap[sound]?.let { soundId ->
            soundPool?.play(
                soundId,
                volume,
                volume,
                1,
                0,
                1.0f
            )
        }
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        soundMap.clear()
    }
}

// Extension function for easy access
fun Context.playSound(sound: SoundManager.Sound) {
    // Will be injected via Koin
}