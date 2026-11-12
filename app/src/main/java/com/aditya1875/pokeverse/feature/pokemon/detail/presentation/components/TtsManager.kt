package com.aditya1875.pokeverse.feature.pokemon.detail.presentation.components

import com.aditya1875.pokeverse.R

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException

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

    @Volatile
    private var tts: TextToSpeech? = null
    private var mediaPlayer: MediaPlayer? = null

    // Every TextToSpeech call is a synchronous binder call into the engine app and takes a lock the
    // engine's own connect task holds while it binds. On a cold or busy engine that blocks for seconds,
    // which on the main thread is an ANR (stop() blocking in TextToSpeech.runAction). So the engine is
    // only ever touched from this one worker: calls stay in order and main never waits on them.
    private val worker: ExecutorService = Executors.newSingleThreadExecutor { r ->
        Thread(r, "tts-worker").apply { isDaemon = true }
    }
    private val mainHandler = Handler(Looper.getMainLooper())

    private fun onEngine(block: (TextToSpeech) -> Unit) {
        val engine = tts ?: return
        try {
            worker.execute {
                try { block(engine) } catch (e: Exception) { Log.w("TTS", "Engine call failed", e) }
            }
        } catch (_: RejectedExecutionException) {
            // Shut down: nothing to speak with.
        }
    }

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    // The engine binds asynchronously (often 0.5-2s). A request made before then used to be
    // dropped, so the first Dex Mode entry played its cry and then said nothing. Keep the latest.
    @Volatile
    private var pending: Pair<String, Boolean>? = null

    init {
        initializeTTS(context)
    }

    private fun initializeTTS(context: Context) {
        // Initialize TTS
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // This callback runs on the main thread; setLanguage / voices / voice are blocking
                // binder calls, so the whole setup happens on the worker.
                onEngine { engine ->
                    val result = engine.setLanguage(Locale.US)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "Language not supported")
                        _isReady.value = false
                    } else {

                        val voices = engine.voices.orEmpty()

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

                        Log.d("TTS", "TTS initialized successfully")
                        // Back on main for the rest: ready flag, the beep (MediaPlayer) and any queued line.
                        mainHandler.post {
                            _isReady.value = true
                            pending?.let { (text, withBeep) ->
                                pending = null
                                speak(text, withBeep)
                            }
                        }
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
            Log.w("TTS", "TTS not ready yet; will speak once it is")
            pending = text to withBeep
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
                    onEngine { it.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId) }
                }

                try {
                    if (isPlaying) seekTo(0) else start()
                } catch (e: Exception) {
                    Log.e("TTS", "Beep failed, speaking directly", e)
                    onStart?.invoke()
                    onEngine { it.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId) }
                }
            }
        } else {
            // Speak immediately
            onStart?.invoke()
            onEngine { it.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId) }
        }
    }

    /**
     * Stop current speech
     */
    fun stop() {
        pending = null
        onEngine { it.stop() }
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
        // Queued behind the stop above, so the engine is released in order, off the main thread.
        val engine = tts
        tts = null
        if (engine != null) {
            try {
                worker.execute { runCatching { engine.shutdown() } }
            } catch (_: RejectedExecutionException) {
            }
        }
        worker.shutdown()
        mediaPlayer?.release()
        mediaPlayer = null
        _isReady.value = false
    }
}

// Extension function for easy access
fun Context.getTTSManager(): TTSManager = TTSManager.getInstance(this)