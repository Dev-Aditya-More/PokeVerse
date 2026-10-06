package com.aditya1875.pokeverse.feature.widget.streak

import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ColorFilter
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.pokemon.profile.data.firebase.UserProfileRepository
import com.aditya1875.pokeverse.feature.pokemon.profile.data.source.remote.model.UserProfile
import kotlinx.coroutines.flow.first
import org.koin.core.context.GlobalContext
import java.util.Calendar

/** Everything the widget shows, computed once per update. */
data class StreakSnapshot(
    val status: StreakStatus,
    val restedXp: Int
) {
    val mood: PikachuMood get() = status.mood()
    val days: Int get() = status.days()
    val look: PikachuLook get() = PikachuLook.forStreak(days)

    companion object {
        fun from(profile: UserProfile, now: Calendar) = StreakSnapshot(
            status = StreakStatus.from(profile.lastDailyXpDate, profile.dailyStreak, now),
            restedXp = profile.restedXp
        )

        val Empty = StreakSnapshot(StreakStatus.NotStarted, restedXp = 0)
    }
}

/**
 * Home-screen streak widget, starring Pikachu. Its mood (and the card's colour)
 * follows the streak: celebrating, waiting, panicking, or asleep. Its outfit
 * levels up with the streak and is lost with it. Layout and scale follow the
 * widget's actual size — see [WidgetLayout].
 */
class StreakWidget : GlanceAppWidget() {

    // Exact: re-rendered for the real size, so art and type scale with every resize.
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // A widget must never crash the launcher: any read failure renders the sleeping state.
        val snapshot = runCatching {
            val profile = GlobalContext.get().get<UserProfileRepository>().profileFlow.first()
            StreakSnapshot.from(profile, Calendar.getInstance())
        }.getOrDefault(StreakSnapshot.Empty)

        provideContent { StreakWidgetContent(snapshot) }
    }

}

class StreakWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StreakWidget()
}

// ── Palette ─────────────────────────────────────────────────────────────────

private object WidgetColors {
    val Text = Color(0xFFFFF8EC)
    val Muted = Color(0xFFB7B0C4)
    val Gold = Color(0xFFFFC24B)
    val Safe = Color(0xFF6EE7A0)
    val Urgent = Color(0xFFFF6B6B)
    val Sleep = Color(0xFF9CC4FF)
    val BubbleText = Color(0xFF1F1A2E)
    val Silhouette = Color(0xFF2A2438)
}

private fun color(c: Color) = ColorProvider(c)

// ── Mood → visuals ──────────────────────────────────────────────────────────

@DrawableRes
private fun PikachuMood.background(): Int = when (this) {
    PikachuMood.Happy -> R.drawable.widget_bg_safe
    PikachuMood.Waiting -> R.drawable.widget_bg_risk
    PikachuMood.Panicking -> R.drawable.widget_bg_urgent
    PikachuMood.Sleeping -> R.drawable.widget_bg_sleep
}

/** The little effect floating by Pikachu's head. */
private fun PikachuMood.effect(): String = when (this) {
    PikachuMood.Happy -> "✨"
    PikachuMood.Waiting -> "❔"
    PikachuMood.Panicking -> "💦"
    PikachuMood.Sleeping -> "💤"
}

private fun PikachuMood.accent(): Color = when (this) {
    PikachuMood.Happy -> WidgetColors.Gold
    PikachuMood.Waiting -> WidgetColors.Gold
    PikachuMood.Panicking -> WidgetColors.Urgent
    PikachuMood.Sleeping -> WidgetColors.Sleep
}

@DrawableRes
private fun pikachuArt(snapshot: StreakSnapshot): Int =
    if (snapshot.mood == PikachuMood.Sleeping) R.drawable.widget_pika_sleepy else snapshot.look.art()

@DrawableRes
private fun PikachuLook.art(): Int =
    when (this) {
        PikachuLook.Classic -> R.drawable.widget_pika_classic
        PikachuLook.Cap -> R.drawable.widget_pika_cap
        PikachuLook.PopStar -> R.drawable.widget_pika_popstar
        PikachuLook.Libre -> R.drawable.widget_pika_libre
        PikachuLook.PhD -> R.drawable.widget_pika_phd
        PikachuLook.RockStar -> R.drawable.widget_pika_rockstar
        PikachuLook.Belle -> R.drawable.widget_pika_belle
    }

@StringRes
private fun PikachuLook.nameRes(): Int = when (this) {
    PikachuLook.Classic -> R.string.widget_look_classic
    PikachuLook.Cap -> R.string.widget_look_cap
    PikachuLook.PopStar -> R.string.widget_look_popstar
    PikachuLook.Libre -> R.string.widget_look_libre
    PikachuLook.PhD -> R.string.widget_look_phd
    PikachuLook.RockStar -> R.string.widget_look_rockstar
    PikachuLook.Belle -> R.string.widget_look_belle
}

private fun Context.speech(snapshot: StreakSnapshot): String = when (val status = snapshot.status) {
    is StreakStatus.SafeToday -> getString(R.string.widget_pika_happy)
    is StreakStatus.AtRisk ->
        if (status.isUrgent) getString(R.string.widget_pika_panic, status.hoursLeft)
        else getString(R.string.widget_pika_waiting)
    StreakStatus.NotStarted -> getString(R.string.widget_pika_sleepy)
}

// ── Layout ──────────────────────────────────────────────────────────────────

/**
 * Which arrangement fits the space, and how much to scale it. Sizes are clamped
 * so art never pixelates or crowds out the text, however the widget is resized.
 */
private sealed interface WidgetLayout {
    val scale: Float
    val artSize: Dp

    /** Narrow (e.g. 2×2, 2×3): stacked. */
    data class Compact(override val scale: Float, override val artSize: Dp) : WidgetLayout

    /** Short and wide (e.g. 4×1, 4×2): Pikachu beside the details. [slim] = count + status only. */
    data class Wide(override val scale: Float, override val artSize: Dp, val slim: Boolean) : WidgetLayout

    /** Big (e.g. 4×3, 4×4): hero Pikachu plus the outfit collection. */
    data class Large(override val scale: Float, override val artSize: Dp, val thumbSize: Dp) : WidgetLayout

    companion object {
        fun forSize(size: DpSize): WidgetLayout {
            val w = size.width.value
            val h = size.height.value
            // Art gets whatever is left after the text it shares the space with (heights in dp,
            // pre-scale: bubble ≈ 22, count row ≈ 30, details block ≈ 115) plus 24 dp of padding.
            return when {
                w < 200f -> {
                    val scale = minOf(w / 130f, h / 140f).coerceIn(0.85f, 1.45f)
                    Compact(scale, artSize = minOf(w - 24f, h - 24f - scale * (22f + 30f)).coerceIn(36f, 180f).dp)
                }
                h < 260f -> {
                    // Under ~130 dp tall (e.g. 4×1) only the count + status fit, and Pikachu loses the bubble.
                    val slim = h < 130f
                    val detailsHeight = if (slim) 54f else 115f
                    val scale = minOf(w / 260f, (h - 24f) / detailsHeight).coerceIn(0.7f, 1.4f)
                    val bubble = if (slim) 0f else scale * 22f
                    // Capped by width too, so the details column keeps room for its text.
                    Wide(scale, artSize = minOf(h - 24f - bubble, w * 0.36f).coerceIn(40f, 150f).dp, slim = slim)
                }
                else -> {
                    val scale = minOf(w / 280f, h / 300f).coerceIn(0.9f, 1.5f)
                    val thumb = ((w - 40f) / PikachuLook.entries.size - 6f).coerceIn(22f, 44f)
                    Large(
                        scale = scale,
                        artSize = minOf(w * 0.5f, h - 24f - scale * (22f + 115f + 10f) - thumb).coerceIn(64f, 220f).dp,
                        thumbSize = thumb.dp
                    )
                }
            }
        }
    }
}

private fun Float.sp(scale: Float) = (this * scale).sp

@Composable
private fun StreakWidgetContent(snapshot: StreakSnapshot) {
    val context = LocalContext.current
    val layout = WidgetLayout.forSize(LocalSize.current)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(snapshot.mood.background()))
            .clickable(actionStartActivity(launchIntent(context)))
            .padding((12 * layout.scale).dp)
    ) {
        when (layout) {
            is WidgetLayout.Compact -> CompactLayout(snapshot, layout)
            is WidgetLayout.Wide -> WideLayout(snapshot, layout)
            is WidgetLayout.Large -> LargeLayout(snapshot, layout)
        }
    }
}

@Composable
private fun CompactLayout(snapshot: StreakSnapshot, layout: WidgetLayout) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SpeechBubble(LocalContext.current.speech(snapshot), layout.scale)
        PikachuStage(snapshot, layout, modifier = GlanceModifier.defaultWeight().fillMaxWidth())
        StreakCount(snapshot, numberSize = 22f, scale = layout.scale)
    }
}

@Composable
private fun WideLayout(snapshot: StreakSnapshot, layout: WidgetLayout.Wide) {
    val context = LocalContext.current
    Row(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Column(
            modifier = GlanceModifier.width(layout.artSize + 28.dp).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!layout.slim) SpeechBubble(context.speech(snapshot), layout.scale)
            PikachuStage(snapshot, layout, modifier = GlanceModifier.defaultWeight().fillMaxWidth())
        }
        Spacer(GlanceModifier.width((10 * layout.scale).dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            if (layout.slim) {
                StreakCount(snapshot, numberSize = 26f, scale = layout.scale)
                Spacer(GlanceModifier.height((4 * layout.scale).dp))
                StatusPill(snapshot.status, layout.scale)
            } else {
                Details(snapshot, layout.scale, numberSize = 34f)
            }
        }
    }
}

@Composable
private fun LargeLayout(snapshot: StreakSnapshot, layout: WidgetLayout.Large) {
    val context = LocalContext.current
    Column(modifier = GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        SpeechBubble(context.speech(snapshot), layout.scale)
        PikachuStage(snapshot, layout, modifier = GlanceModifier.defaultWeight().fillMaxWidth())
        Details(snapshot, layout.scale, numberSize = 40f, centered = true)
        Spacer(GlanceModifier.height((10 * layout.scale).dp))
        OutfitCollection(snapshot, layout.thumbSize)
    }
}

/** Title, count, status, next outfit and rested XP — shared by the wide and large layouts. */
@Composable
private fun Details(snapshot: StreakSnapshot, scale: Float, numberSize: Float, centered: Boolean = false) {
    val context = LocalContext.current
    Column(horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start) {
        Text(
            text = context.getString(R.string.widget_streak_title),
            style = TextStyle(color = color(snapshot.mood.accent()), fontSize = 10f.sp(scale), fontWeight = FontWeight.Bold)
        )
        StreakCount(snapshot, numberSize = numberSize, scale = scale)
        Spacer(GlanceModifier.height((4 * scale).dp))
        StatusPill(snapshot.status, scale)
        Spacer(GlanceModifier.height((6 * scale).dp))
        NextLookLine(snapshot, scale)
        if (snapshot.restedXp > 0 && snapshot.mood != PikachuMood.Sleeping) {
            Spacer(GlanceModifier.height((4 * scale).dp))
            Text(
                text = context.getString(R.string.widget_streak_rested),
                style = TextStyle(color = color(WidgetColors.Sleep), fontSize = 10f.sp(scale))
            )
        }
    }
}

/** Pikachu with its mood effect floating at the top-right. */
@Composable
private fun PikachuStage(snapshot: StreakSnapshot, layout: WidgetLayout, modifier: GlanceModifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Image(
            provider = ImageProvider(pikachuArt(snapshot)),
            contentDescription = LocalContext.current.getString(snapshot.look.nameRes()),
            contentScale = ContentScale.Fit,
            modifier = GlanceModifier.size(layout.artSize)
        )
        Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
            Text(text = snapshot.mood.effect(), style = TextStyle(fontSize = 16f.sp(layout.scale)))
        }
    }
}

/**
 * Every outfit in a row: unlocked ones in colour, locked ones as dark silhouettes
 * ("Who's that Pokémon?"), so the whole collection is a goal you can see.
 */
@Composable
private fun OutfitCollection(snapshot: StreakSnapshot, thumbSize: Dp) {
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        PikachuLook.entries.forEachIndexed { index, look ->
            if (index > 0) Spacer(GlanceModifier.width(6.dp))
            val unlocked = snapshot.days >= look.minDays && snapshot.mood != PikachuMood.Sleeping
            Image(
                provider = ImageProvider(look.art()),
                contentDescription = context.getString(look.nameRes()),
                contentScale = ContentScale.Fit,
                colorFilter = if (unlocked) null else ColorFilter.tint(color(WidgetColors.Silhouette)),
                modifier = GlanceModifier.size(thumbSize)
            )
        }
    }
}

@Composable
private fun SpeechBubble(text: String, scale: Float) {
    Box(
        modifier = GlanceModifier
            .background(ImageProvider(R.drawable.widget_speech_bubble))
            .padding(start = (9 * scale).dp, end = (9 * scale).dp, top = (3 * scale).dp, bottom = (8 * scale).dp)
    ) {
        Text(
            text = text,
            maxLines = 1,
            style = TextStyle(color = color(WidgetColors.BubbleText), fontSize = 11f.sp(scale), fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
private fun StreakCount(snapshot: StreakSnapshot, numberSize: Float, scale: Float) {
    val context = LocalContext.current
    val alive = snapshot.days > 0
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = if (alive) "🔥" else "🌙", style = TextStyle(fontSize = (numberSize * 0.7f).sp(scale)))
        Spacer(GlanceModifier.width(3.dp))
        Text(
            text = snapshot.days.toString(),
            style = TextStyle(
                color = color(if (alive) WidgetColors.Text else WidgetColors.Muted),
                fontSize = numberSize.sp(scale),
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(GlanceModifier.width(4.dp))
        Text(
            // The number is drawn separately, so the plural carries no %d. Zero must read as plural
            // ("0 dias", "0 jours"), but pt/fr/hi put 0 in the "one" category — so ask for "other".
            text = context.resources.getQuantityString(R.plurals.widget_streak_days, if (snapshot.days == 0) 2 else snapshot.days),
            style = TextStyle(color = color(WidgetColors.Muted), fontSize = 12f.sp(scale))
        )
    }
}

@Composable
private fun StatusPill(status: StreakStatus, scale: Float) {
    val context = LocalContext.current
    when (status) {
        is StreakStatus.SafeToday -> Pill(
            context.getString(R.string.widget_streak_safe), R.drawable.widget_pill_safe, WidgetColors.Safe, scale
        )
        is StreakStatus.AtRisk ->
            if (status.isUrgent) Pill(
                context.getString(R.string.widget_streak_hours_left, status.hoursLeft),
                R.drawable.widget_pill_urgent, WidgetColors.Urgent, scale
            )
            else Pill(context.getString(R.string.widget_streak_at_risk), R.drawable.widget_pill_risk, WidgetColors.Gold, scale)
        StreakStatus.NotStarted -> Pill(
            context.getString(R.string.widget_streak_start), R.drawable.widget_pill_neutral, WidgetColors.Text, scale
        )
    }
}

@Composable
private fun Pill(text: String, @DrawableRes background: Int, textColor: Color, scale: Float) {
    Box(
        modifier = GlanceModifier
            .background(ImageProvider(background))
            .padding(horizontal = (10 * scale).dp, vertical = (3 * scale).dp)
    ) {
        Text(
            text = text,
            maxLines = 1,
            style = TextStyle(color = color(textColor), fontSize = 11f.sp(scale), fontWeight = FontWeight.Medium)
        )
    }
}

/** "Pop Star outfit in 3 days" — the reason to come back tomorrow. */
@Composable
private fun NextLookLine(snapshot: StreakSnapshot, scale: Float) {
    val context = LocalContext.current
    val next = PikachuLook.next(snapshot.days)
    val text = if (next == null) context.getString(R.string.widget_all_looks)
    else {
        val (look, daysAway) = next
        context.resources.getQuantityString(R.plurals.widget_next_look, daysAway, daysAway, context.getString(look.nameRes()))
    }
    Text(text = text, maxLines = 1, style = TextStyle(color = color(WidgetColors.Muted), fontSize = 10f.sp(scale)))
}

/** Opens the app's launcher activity — no hard dependency on a flavor-specific Activity class. */
private fun launchIntent(context: Context): Intent =
    context.packageManager.getLaunchIntentForPackage(context.packageName)
        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        ?: Intent()
