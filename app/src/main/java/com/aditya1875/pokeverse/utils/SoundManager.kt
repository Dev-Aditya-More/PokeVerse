package com.aditya1875.pokeverse.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import androidx.annotation.RawRes
import com.aditya1875.pokeverse.R

class SoundManager(private val context: Context) {

    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<Sound, Int>()
    private var isEnabled = true
    private var musicPlayer: MediaPlayer? = null

    enum class Sound {
        // UI Sounds
        BUTTON_CLICK,
        CARD_FLIP,
        MATCH_FOUND,
        GAME_WIN,
        GAME_LOSE,
        TIMER_UP,

        // Quiz Sounds
        CORRECT_ANSWER,
        WRONG_ANSWER,

        WHOS_THAT_POKEMON,
        LEVEL_UP,

        RUSH_CLICK
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

        soundMap[Sound.WHOS_THAT_POKEMON] = loadSound(R.raw.whos_that_pokemon)
        soundMap[Sound.LEVEL_UP] = loadSound(R.raw.level_up)
        soundMap[Sound.RUSH_CLICK] = loadSound(R.raw.click_rush)
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

    /**
     * Plays the app's startup jingle as music (MediaPlayer, not SoundPool — it's too long
     * for a pooled SFX clip). Returns the track's duration in ms, or -1 if it couldn't load.
     */
    fun playIntroMusic(): Int {
        stopIntroMusic()
        if (!isEnabled) return -1

        return try {
            val mp = MediaPlayer.create(context, R.raw.intro)
            musicPlayer = mp
            mp?.start()
            mp?.duration ?: -1
        } catch (_: Exception) {
            -1
        }
    }

    fun stopIntroMusic() {
        musicPlayer?.apply {
            try {
                if (isPlaying) stop()
            } catch (_: Exception) {
            }
            release()
        }
        musicPlayer = null
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        soundMap.clear()
        stopIntroMusic()
    }
}

// Extension function for easy access
fun Context.playSound(sound: SoundManager.Sound) {
    // Will be injected via Koin
}