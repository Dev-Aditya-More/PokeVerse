package com.aditya1875.pokeverse.feature.dexmode.presentation

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

/**
 * Tiny 8-bit sound chip: square-wave blips rendered once into static AudioTracks.
 * No audio assets — the handheld beeps are synthesized, like the real thing.
 */
class DexSfx {

    enum class Sfx { BLIP, SELECT, BACK, BOOT, ERROR }

    private val tracks: Map<Sfx, AudioTrack?> = Sfx.entries.associateWith { runCatching { build(render(it)) }.getOrNull() }

    fun play(sfx: Sfx) {
        val track = tracks[sfx] ?: return
        runCatching {
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.stop()
            track.reloadStaticData()
            track.play()
        }
    }

    fun release() {
        tracks.values.forEach { runCatching { it?.release() } }
    }

    private fun render(sfx: Sfx): ShortArray = when (sfx) {
        Sfx.BLIP -> notes(1568f to 0.022f)
        Sfx.SELECT -> notes(1047f to 0.045f, 1568f to 0.07f)
        Sfx.BACK -> notes(784f to 0.045f, 523f to 0.07f)
        Sfx.BOOT -> notes(523f to 0.08f, 659f to 0.08f, 784f to 0.08f, 1047f to 0.08f, 0f to 0.04f, 1568f to 0.2f)
        Sfx.ERROR -> notes(196f to 0.1f, 147f to 0.16f)
    }

    /** Square-wave notes (freq Hz to seconds; 0 Hz is a rest) with a short decay tail. */
    private fun notes(vararg seq: Pair<Float, Float>): ShortArray {
        val out = ArrayList<Short>()
        seq.forEach { (freq, seconds) ->
            val n = (RATE * seconds).toInt()
            for (i in 0 until n) {
                if (freq == 0f) { out += 0; continue }
                val phase = (i * freq / RATE) % 1f
                val square = if (phase < 0.5f) 1f else -1f
                // Soften the edges a touch so it's crunchy, not harsh.
                val shaped = square * 0.85f + sin(2f * PI.toFloat() * phase) * 0.15f
                val env = if (i > n * 0.6f) 1f - (i - n * 0.6f) / (n * 0.4f) else 1f
                out += (shaped * env * AMPLITUDE * Short.MAX_VALUE).toInt().toShort()
            }
        }
        return out.toShortArray()
    }

    private fun build(pcm: ShortArray): AudioTrack =
        AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * 2)
            .build()
            .apply { write(pcm, 0, pcm.size) }

    private companion object {
        const val RATE = 22050
        const val AMPLITUDE = 0.16f
    }
}
