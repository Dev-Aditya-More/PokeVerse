package com.aditya1875.pokeverse.feature.game.chase.data

import android.content.Context
import android.graphics.drawable.Animatable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

private const val SPRITES = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites"

/**
 * Every image Rocket Chase draws. Pokémon use PokeAPI's animated Showdown GIFs
 * (with the classic static pixel sprite as a fallback); items are static.
 */
enum class ChaseSprite(val url: String, val fallbackUrl: String? = null) {
    PIKACHU_BACK("$SPRITES/pokemon/other/showdown/back/25.gif", "$SPRITES/pokemon/back/25.png"),
    MEOWTH("$SPRITES/pokemon/other/showdown/52.gif", "$SPRITES/pokemon/52.png"),
    KOFFING("$SPRITES/pokemon/other/showdown/109.gif", "$SPRITES/pokemon/109.png"),
    EKANS("$SPRITES/pokemon/other/showdown/23.gif", "$SPRITES/pokemon/23.png"),
    WOBBUFFET("$SPRITES/pokemon/other/showdown/202.gif", "$SPRITES/pokemon/202.png"),
    ORAN_BERRY("$SPRITES/items/oran-berry.png"),
    THUNDER_STONE("$SPRITES/items/thunder-stone.png")
}

/** Loaded drawables, ready for the renderer. Missing entries just fall back to a drawn shape. */
class ChaseSprites(private val drawables: Map<ChaseSprite, Drawable>) {
    operator fun get(sprite: ChaseSprite): Drawable? = drawables[sprite]

    fun setAnimating(animating: Boolean) {
        drawables.values.filterIsInstance<Animatable>().forEach { if (animating) it.start() else it.stop() }
    }
}

class ChaseSpriteLoader(
    private val context: Context,
    private val imageLoader: ImageLoader
) {
    /** Never throws — a sprite that can't be fetched is simply absent. */
    suspend fun load(): ChaseSprites = coroutineScope {
        val loaded = ChaseSprite.entries
            .map { sprite -> async { sprite to loadOne(sprite) } }
            .awaitAll()
            .mapNotNull { (sprite, drawable) -> drawable?.let { sprite to it } }
            .toMap()
        ChaseSprites(loaded)
    }

    private suspend fun loadOne(sprite: ChaseSprite): Drawable? =
        fetch(sprite.url) ?: sprite.fallbackUrl?.let { fetch(it) }

    private suspend fun fetch(url: String): Drawable? = try {
        decode(url)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // Coil normally reports failures as ErrorResult; this catches anything that escapes it.
        null
    }

    private suspend fun decode(url: String): Drawable? {
        val request = ImageRequest.Builder(context)
            .data(url)
            .apply {
                // The app-wide ImageLoader has no GIF support, so animated sprites opt in per request.
                if (url.endsWith(".gif")) decoderFactory(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) ImageDecoderDecoder.Factory()
                    else GifDecoder.Factory()
                )
            }
            .build()
        val drawable = (imageLoader.execute(request) as? SuccessResult)?.drawable ?: return null
        // Keep pixel art crisp when it's scaled up on the track.
        (drawable as? BitmapDrawable)?.isFilterBitmap = false
        return drawable
    }
}
