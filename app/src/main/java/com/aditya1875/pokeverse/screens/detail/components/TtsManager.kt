package com.aditya1875.pokeverse.screens.detail.components

import com.aditya1875.pokeverse.R

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

class TTSManager private constructor(context: Context) {

    companion object {
        @Volatile
        private var instance: TTSManager? = null

        fun getInstance(context: Context): TTSManager {
            return instance ?: synchronized(this) {
                instance ?: TTSManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private var tts: TextToSpeech? = null
    private var mediaPlayer: MediaPlayer? = null

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    init {
        initializeTTS(context)
    }

    private fun initializeTTS(context: Context) {
        // Initialize TTS
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.let { engine ->
                    val result = engine.setLanguage(Locale.US)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "Language not supported")
                        _isReady.value = false
                    } else {

                        val voices = engine.voices

                        val preferred = voices.firstOrNull {
                            it.name.contains("en-us-x-iom")
                        }

                        if (preferred != null) {
                            engine.voice = preferred
                        }

                        engine.setPitch(1.2f)  // Slightly higher pitch
                        engine.setSpeechRate(1.0f)  // Normal speed (faster than 0.9f)

                        // Set progress listener
                        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {
                                _isSpeaking.value = true
                            }

                            override fun onDone(utteranceId: String?) {
                                _isSpeaking.value = false
                            }

                            @Deprecated("Deprecated in Java")
                            override fun onError(utteranceId: String?) {
                                _isSpeaking.value = false
                                Log.e("TTS", "Speech error")
                            }

                            override fun onError(utteranceId: String?, errorCode: Int) {
                                _isSpeaking.value = false
                                Log.e("TTS", "Speech error: $errorCode")
                            }
                        })

                        _isReady.value = true
                        Log.d("TTS", "TTS initialized successfully")
                    }
                }
            } else {
                Log.e("TTS", "TTS initialization failed: $status")
                _isReady.value = false
            }
        }

        // Initialize MediaPlayer for beep sound
        try {
            mediaPlayer = MediaPlayer.create(context, R.raw.beepeffect)?.apply {
                // Pre-prepare for instant playback
                setVolume(0.6f, 0.6f)
            }
        } catch (e: Exception) {
            Log.e("TTS", "Failed to load beep sound", e)
        }
    }

    /**
     * Speak with optional beep sound before
     */
    fun speak(
        text: String,
        withBeep: Boolean = true,
        onStart: (() -> Unit)? = null,
        onComplete: (() -> Unit)? = null
    ) {
        if (!_isReady.value) {
            Log.w("TTS", "TTS not ready yet")
            return
        }

        val utteranceId = UUID.randomUUID().toString()

        // Stop any current speech
        stop()

        if (withBeep && mediaPlayer != null) {
            // Play beep, then speak
            mediaPlayer?.apply {
                setOnCompletionListener {
                    onStart?.invoke()
                    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                }

                try {
                    if (isPlaying) seekTo(0) else start()
                } catch (e: Exception) {
                    Log.e("TTS", "Beep failed, speaking directly", e)
                    onStart?.invoke()
                    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                }
            }
        } else {
            // Speak immediately
            onStart?.invoke()
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
    }

    /**
     * Stop current speech
     */
    fun stop() {
        tts?.stop()
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                it.seekTo(0)
            }
        }
        _isSpeaking.value = false
    }

    /**
     * Check if currently speaking
     */
    fun isSpeakingNow(): Boolean = _isSpeaking.value

    /**
     * Clean up resources - call from Application.onTerminate()
     */
    fun shutdown() {
        stop()
        tts?.shutdown()
        mediaPlayer?.release()
        tts = null
        mediaPlayer = null
        _isReady.value = false
    }
}

// Extension function for easy access
fun Context.getTTSManager(): TTSManager = TTSManager.getInstance(this)