package com.aditya1875.pokeverse.utils

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi

/**
 * App-wide haptics with a semantic vocabulary ([Haptic]) instead of raw durations.
 *
 * - Android 11+: rich composition primitives (rises, thuds, clicks) when the motor supports them.
 * - Older / basic motors: tuned waveforms, then plain one-shots.
 * - Always tagged as touch feedback, so the system "Touch feedback / Vibration" setting is respected.
 */
class Haptics(context: Context) {

    enum class Haptic {
        TICK,          // lane switch, card flip, light UI
        CLICK,         // button press, selection
        CONFIRM,       // correct answer, pickup
        REJECT,        // wrong answer
        HEAVY,         // hit taken, big impact
        WARNING,       // timer running out, incoming danger
        CHARGE,        // building up (splash ball growing)
        POP,           // release / reveal
        SUCCESS,       // win, match found
        LEVEL_UP,      // celebration
        RUMBLE         // thunderbolt, agility start
    }

    private val vibrator: Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }.getOrNull()?.takeIf { it.hasVibrator() }

    private val hasAmplitude = vibrator?.hasAmplitudeControl() == true

    @Volatile
    var enabled: Boolean = true

    fun play(haptic: Haptic) {
        val v = vibrator ?: return
        if (!enabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val effect = runCatching { effectFor(haptic) }.getOrNull() ?: return
        runCatching { vibrate(v, effect) }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun effectFor(h: Haptic): VibrationEffect? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) composition(h)?.let { return it }
        return waveform(h)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun composition(h: Haptic): VibrationEffect? {
        val v = vibrator ?: return null
        fun supported(vararg ids: Int) = v.areAllPrimitivesSupported(*ids)
        val c = VibrationEffect.startComposition()
        val ok: Boolean = when (h) {
            Haptic.TICK -> supported(VibrationEffect.Composition.PRIMITIVE_TICK).also {
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.5f)
            }
            Haptic.CLICK -> supported(VibrationEffect.Composition.PRIMITIVE_CLICK).also {
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.7f)
            }
            Haptic.CONFIRM -> supported(VibrationEffect.Composition.PRIMITIVE_CLICK).also {
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.6f)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1f, 70)
            }
            Haptic.REJECT -> supported(VibrationEffect.Composition.PRIMITIVE_THUD, VibrationEffect.Composition.PRIMITIVE_CLICK).also {
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 0.8f)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.4f, 90)
            }
            Haptic.HEAVY -> supported(VibrationEffect.Composition.PRIMITIVE_THUD).also {
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 1f)
            }
            Haptic.WARNING -> supported(VibrationEffect.Composition.PRIMITIVE_TICK).also {
                repeat(3) { i -> c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.8f, if (i == 0) 0 else 80) }
            }
            Haptic.CHARGE -> supported(VibrationEffect.Composition.PRIMITIVE_SLOW_RISE).also {
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_SLOW_RISE, 0.6f)
            }
            Haptic.POP -> supported(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, VibrationEffect.Composition.PRIMITIVE_THUD).also {
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.8f)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 1f, 20)
            }
            Haptic.SUCCESS -> supported(VibrationEffect.Composition.PRIMITIVE_CLICK, VibrationEffect.Composition.PRIMITIVE_QUICK_RISE).also {
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.6f)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.9f, 60)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1f, 40)
            }
            Haptic.LEVEL_UP -> supported(VibrationEffect.Composition.PRIMITIVE_SLOW_RISE, VibrationEffect.Composition.PRIMITIVE_CLICK).also {
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_SLOW_RISE, 0.7f)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1f, 30)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.8f, 90)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1f, 90)
            }
            Haptic.RUMBLE -> supported(VibrationEffect.Composition.PRIMITIVE_THUD, VibrationEffect.Composition.PRIMITIVE_QUICK_FALL).also {
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 1f)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 0.7f, 60)
                c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, 0.8f, 40)
            }
        }
        return if (ok) c.compose() else null
    }

    /** (timings ms, amplitudes 0..255) — first timing is an initial delay. */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun waveform(h: Haptic): VibrationEffect? {
        val (t, a) = when (h) {
            Haptic.TICK -> longArrayOf(0, 8) to intArrayOf(0, 90)
            Haptic.CLICK -> longArrayOf(0, 14) to intArrayOf(0, 160)
            Haptic.CONFIRM -> longArrayOf(0, 14, 60, 22) to intArrayOf(0, 120, 0, 220)
            Haptic.REJECT -> longArrayOf(0, 45, 60, 20) to intArrayOf(0, 200, 0, 90)
            Haptic.HEAVY -> longArrayOf(0, 60) to intArrayOf(0, 255)
            Haptic.WARNING -> longArrayOf(0, 12, 70, 12, 70, 12) to intArrayOf(0, 180, 0, 180, 0, 180)
            Haptic.CHARGE -> longArrayOf(0, 60, 60, 60, 60) to intArrayOf(0, 40, 80, 130, 180)
            Haptic.POP -> longArrayOf(0, 25, 10, 45) to intArrayOf(0, 140, 0, 255)
            Haptic.SUCCESS -> longArrayOf(0, 14, 50, 30, 40, 18) to intArrayOf(0, 120, 0, 200, 0, 255)
            Haptic.LEVEL_UP -> longArrayOf(0, 80, 30, 18, 70, 18, 70, 24) to intArrayOf(0, 90, 0, 200, 0, 200, 0, 255)
            Haptic.RUMBLE -> longArrayOf(0, 70, 40, 60, 30, 40) to intArrayOf(0, 255, 0, 180, 0, 120)
        }
        if (hasAmplitude) return VibrationEffect.createWaveform(t, a, -1)
        // No amplitude control: keep only the "on" segments' rhythm.
        return VibrationEffect.createWaveform(t, -1)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun vibrate(v: Vibrator, effect: VibrationEffect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            v.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(
                effect,
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).build()
            )
        }
    }
}
