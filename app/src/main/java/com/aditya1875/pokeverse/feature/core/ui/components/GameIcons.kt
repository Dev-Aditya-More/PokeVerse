package com.aditya1875.pokeverse.feature.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Small drawn icons for the in-app currencies, so prices and balances don't lean on emoji, which render
 * differently on every phone. Pure vector drawing: sharp at any size and the same everywhere.
 */

private val CoinGold = Color(0xFFFFC24B)
private val CoinRim = Color(0xFFE09A1A)

/** A Poké Coin: a gold disc with a rim and a highlight. */
@Composable
fun CoinIcon(size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val r = this.size.minDimension / 2f
        val c = Offset(this.size.width / 2f, this.size.height / 2f)
        drawCircle(Brush.linearGradient(listOf(Color(0xFFFFE082), CoinGold, CoinRim)), r, c)
        drawCircle(CoinRim, r * 0.74f, c, style = Stroke(width = r * 0.14f))
        // A small "P" is too fine at icon sizes, so the centre is a plain raised disc, like a stamped coin.
        drawCircle(Color(0xFFFFE9A8).copy(alpha = 0.9f), r * 0.28f, c)
        drawCircle(Color.White.copy(alpha = 0.55f), r * 0.12f, Offset(c.x - r * 0.38f, c.y - r * 0.42f))
    }
}

/** A coin icon followed by an amount, for balances and price buttons. */
@Composable
fun CoinAmount(
    amount: Int,
    modifier: Modifier = Modifier,
    iconSize: Dp = 16.dp,
    color: Color = Color.Unspecified,
    prefix: String = "",
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        CoinIcon(iconSize)
        Text(
            prefix + amount,
            color = if (color == Color.Unspecified) LocalContentColor.current else color,
            style = LocalTextStyle.current,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** An Evo Shard: a cut gem. */
@Composable
fun ShardGem(size: Dp, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val c = Offset(this.size.width / 2f, this.size.height / 2f)
        val r = this.size.minDimension / 2f
        val gem = Path().apply {
            moveTo(c.x, c.y - r)
            lineTo(c.x + r * 0.72f, c.y - r * 0.1f)
            lineTo(c.x, c.y + r)
            lineTo(c.x - r * 0.72f, c.y - r * 0.1f)
            close()
        }
        drawPath(gem, color)
        // A facet line and a bright edge, so it reads as a gem and not a flat diamond.
        drawLine(Color.White.copy(alpha = 0.5f), Offset(c.x - r * 0.72f, c.y - r * 0.1f), Offset(c.x + r * 0.72f, c.y - r * 0.1f), strokeWidth = r * 0.1f)
        drawLine(Color.White.copy(alpha = 0.35f), Offset(c.x, c.y - r), Offset(c.x, c.y + r), strokeWidth = r * 0.08f)
    }
}
