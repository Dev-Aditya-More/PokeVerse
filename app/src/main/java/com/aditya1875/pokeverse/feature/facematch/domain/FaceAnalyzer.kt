package com.aditya1875.pokeverse.feature.facematch.domain

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.Rect

/**
 * Turns a detected face's bounding box into the two simple signals FaceMatcher needs. ML Kit's
 * bounding box can legitimately extend past the bitmap edges (it's estimated, not clipped), so
 * every coordinate here is clamped before touching the bitmap — this is the one place a bad
 * rect could otherwise crash with an IllegalArgumentException from Bitmap.getPixel.
 */
object FaceAnalyzer {

    private val FallbackSkinTone = RgbColor(0.8f, 0.65f, 0.55f)

    fun sampleSkinColor(bitmap: Bitmap, faceBounds: Rect): RgbColor {
        if (bitmap.width <= 0 || bitmap.height <= 0) return FallbackSkinTone

        val left = faceBounds.left.coerceIn(0, bitmap.width - 1)
        val top = faceBounds.top.coerceIn(0, bitmap.height - 1)
        val right = faceBounds.right.coerceIn(left + 1, bitmap.width)
        val bottom = faceBounds.bottom.coerceIn(top + 1, bitmap.height)

        // Sample the central third of the box (roughly cheek/nose-bridge area) rather than the
        // whole bounding box, which often includes hairline/background at its edges.
        val sampleLeft = left + (right - left) * 0.35f
        val sampleRight = left + (right - left) * 0.65f
        val sampleTop = top + (bottom - top) * 0.35f
        val sampleBottom = top + (bottom - top) * 0.65f

        val steps = 8
        val stepX = ((sampleRight - sampleLeft) / steps).coerceAtLeast(1f)
        val stepY = ((sampleBottom - sampleTop) / steps).coerceAtLeast(1f)

        var rSum = 0L
        var gSum = 0L
        var bSum = 0L
        var count = 0

        var y = sampleTop
        while (y < sampleBottom) {
            var x = sampleLeft
            while (x < sampleRight) {
                val px = x.toInt().coerceIn(0, bitmap.width - 1)
                val py = y.toInt().coerceIn(0, bitmap.height - 1)
                val pixel = bitmap.getPixel(px, py)
                rSum += AndroidColor.red(pixel)
                gSum += AndroidColor.green(pixel)
                bSum += AndroidColor.blue(pixel)
                count++
                x += stepX
            }
            y += stepY
        }

        if (count == 0) return FallbackSkinTone
        return RgbColor(
            r = (rSum.toFloat() / count) / 255f,
            g = (gSum.toFloat() / count) / 255f,
            b = (bSum.toFloat() / count) / 255f
        )
    }

    fun classifyShape(faceBounds: Rect): FaceShape {
        val width = faceBounds.width().toFloat().coerceAtLeast(1f)
        val height = faceBounds.height().toFloat()
        val ratio = height / width
        return when {
            ratio < 1.15f -> FaceShape.ROUND
            ratio < 1.4f -> FaceShape.OVAL
            else -> FaceShape.LONG
        }
    }
}
