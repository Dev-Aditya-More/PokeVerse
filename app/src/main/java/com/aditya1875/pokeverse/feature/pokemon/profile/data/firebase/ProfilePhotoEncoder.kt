package com.aditya1875.pokeverse.feature.pokemon.profile.data.firebase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Turns a picked photo into a small square-ish avatar JPEG before upload. A raw camera photo is
 * several MB, but it's only ever drawn at avatar size. Shrinking it makes leaderboard avatars
 * load fast, keeps Storage costs flat, and stays far under the Storage rule's size limit.
 */
object ProfilePhotoEncoder {
    private const val MAX_EDGE_PX = 512
    private const val JPEG_QUALITY = 85

    suspend fun encode(context: Context, uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = decodeScaled(context, uri) ?: return@runCatching null
            ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                bitmap.recycle()
                out.toByteArray()
            }
        }.getOrNull()
    }

    private fun decodeScaled(context: Context, uri: Uri): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // ImageDecoder applies EXIF rotation, so portrait selfies stay upright.
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val (w, h) = info.size.width to info.size.height
                val scale = minOf(1f, MAX_EDGE_PX.toFloat() / maxOf(w, h))
                decoder.setTargetSize((w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1))
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE // compress() needs a CPU bitmap
            }
        }
        // API 25–27: power-of-two subsampling keeps memory low (EXIF rotation isn't applied here).
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_EDGE_PX) sample *= 2
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
        val scale = minOf(1f, MAX_EDGE_PX.toFloat() / maxOf(decoded.width, decoded.height))
        if (scale >= 1f) return decoded
        return Bitmap.createScaledBitmap(
            decoded, (decoded.width * scale).toInt(), (decoded.height * scale).toInt(), true
        ).also { if (it !== decoded) decoded.recycle() }
    }
}
