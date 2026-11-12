package com.aditya1875.pokeverse.utils

import android.os.Looper
import android.util.Log
import java.io.IOException

/**
 * A dropped connection is not a bug. When a request fails with an [IOException] (timeout, DNS, reset...)
 * on a background thread and nobody caught it, the player should see a failed load, not a crash.
 *
 * This wraps whatever default uncaught-exception handler is already installed (Crashlytics), so:
 *  - an [IOException] (or one anywhere in a cause chain) thrown OFF the main thread is logged and
 *    swallowed, and reported through [reporter] as a non-fatal so it still shows up in the console;
 *  - everything else, including any exception on the main thread, goes to the previous handler untouched.
 *
 * Call sites should still catch their own errors and show a message: this is only the safety net.
 */
object NetworkCrashGuard {

    private const val TAG = "NetworkCrashGuard"

    /** Records a swallowed network failure as a non-fatal; set by the flavor that has crash reporting. */
    @Volatile
    var reporter: ((Throwable) -> Unit)? = null

    @Volatile
    private var installed = false

    fun install() {
        if (installed) return
        installed = true
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (isBackgroundNetworkFailure(thread, throwable)) {
                Log.w(TAG, "Swallowed network failure on ${thread.name}", throwable)
                runCatching { reporter?.invoke(throwable) }
            } else if (previous != null) {
                previous.uncaughtException(thread, throwable)
            } else {
                // No handler to hand it to: behave like the platform default.
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        }
    }

    private fun isBackgroundNetworkFailure(thread: Thread, throwable: Throwable): Boolean =
        isSwallowable(thread === Looper.getMainLooper().thread, throwable)

    /** Pure rule, split out so it can be unit tested without a Looper. */
    internal fun isSwallowable(onMainThread: Boolean, throwable: Throwable): Boolean {
        if (onMainThread) return false
        return generateSequence(throwable) { it.cause }.take(8).any { it is IOException }
    }
}
