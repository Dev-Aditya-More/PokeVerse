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
import com.aditya1875.pokeverse.feature.game.core.data.billing.IBillingManager
import com.aditya1875.pokeverse.feature.game.core.data.billing.SubscriptionState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.context.GlobalContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Everything the widget shows, computed once per update. */
data class StreakSnapshot(
    val status: StreakStatus,
    val restedXp: Int,
    val week: List<WeekDay> = emptyList(),
    /** Pro look: gold rim, gold flame and a PRO pill. */
    val isPremium: Boolean = false
) {
    val mood: PikachuMood get() = status.mood()
    val days: Int get() = status.days()
    val look: PikachuLook get() = PikachuLook.forStreak(days)

    companion object {
        fun from(profile: UserProfile, now: Calendar, isPremium: Boolean = false) = StreakSnapshot(
            status = StreakStatus.from(profile.lastDailyXpDate, profile.dailyStreak, now),
            restedXp = profile.restedXp,
            week = WeekStrip.build(profile.lastDailyXpDate, profile.dailyStreak, now),
            isPremium = isPremium
        )

        val Empty = StreakSnapshot(StreakStatus.NotStarted, restedXp = 0, week = WeekStrip.build("", 0, Calendar.getInstance()))
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
            StreakSnapshot.from(profile, Calendar.getInstance(), isPremium = isPremium())
        }.getOrDefault(StreakSnapshot.Empty)

        provideContent { StreakWidgetContent(snapshot) }
    }

    /**
     * Pro status for the signed-in account. A widget update can start the app process cold, so
     * give billing a moment to settle (its per-account cache usually answers instantly); if it
     * doesn't, show the standard look rather than guessing.
     */
    private suspend fun isPremium(): Boolean = runCatching {
        val billing = GlobalContext.get().get<IBillingManager>()
        withTimeoutOrNull(3_000) {
            billing.subscriptionState.first { it !is SubscriptionState.Loading }
        } is SubscriptionState.Premium
    }.getOrDefault(false)

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
    val ProInk = Color(0xFF3E2A00)
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

/** The little effect floating by Pikachu's head (vector art, no emoji). */
@DrawableRes
private fun PikachuMood.effect(): Int = when (this) {
    PikachuMood.Happy -> R.drawable.ic_widget_sparkle
    PikachuMood.Waiting -> R.drawable.ic_widget_question
    PikachuMood.Panicking -> R.drawable.ic_widget_sweat
    PikachuMood.Sleeping -> R.drawable.ic_widget_zzz
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

    /**
     * Narrow (e.g. 2×2, 2×3): stacked. The week strip only joins when Pikachu still keeps a
     * decent size, and its circles are sized from the real width so all seven fit.
     */
    data class Compact(
        override val scale: Float,
        override val artSize: Dp,
        val showWeek: Boolean,
        val weekDot: Dp,
        val weekGap: Dp
    ) : WidgetLayout

    /**
     * Short and wide (e.g. 4×1, 4×2): Pikachu beside the details. [slim] = count + status only.
     * Lower-priority lines drop first when it's short, so nothing gets clipped mid-line.
     */
    data class Wide(
        override val scale: Float,
        override val artSize: Dp,
        val slim: Boolean,
        val showNext: Boolean,
        val showRested: Boolean
    ) : WidgetLayout

    /** Big (e.g. 4×3, 4×4): hero Pikachu plus the outfit collection. */
    data class Large(override val scale: Float, override val artSize: Dp, val thumbSize: Dp) : WidgetLayout

    companion object {
        fun forSize(size: DpSize): WidgetLayout {
            val w = size.width.value
            val h = size.height.value
            // Art gets whatever is left after the text it shares the space with (heights in dp,
            // pre-scale: bubble ≈ 22, count row ≈ 30, week strip ≈ 34, details block ≈ 150)
            // plus 24 dp of padding.
            return when {
                w < 200f -> {
                    val scale = minOf(w / 130f, h / 140f).coerceIn(0.85f, 1.45f)
                    val padding = 24f * scale
                    // Pikachu comes first: the strip only joins when the art keeps >= 80 dp.
                    val artWithoutWeek = h - padding - scale * (22f + 30f)
                    val showWeek = artWithoutWeek - scale * 34f >= 80f
                    val slot = (w - padding) / 7f
                    val dot = minOf(13f * scale, slot * 0.74f)
                    Compact(
                        scale,
                        artSize = minOf(w - padding, artWithoutWeek - if (showWeek) scale * 34f else 0f).coerceIn(36f, 180f).dp,
                        showWeek = showWeek,
                        weekDot = dot.dp,
                        weekGap = (slot - dot).coerceAtLeast(2f).dp
                    )
                }
                h < 260f -> {
                    // Under ~130 dp tall (e.g. 4×1) only the count + status fit, and Pikachu loses the bubble.
                    val slim = h < 130f
                    // Measured on device: count + week + pill ≈ 115 dp; next outfit +22; rested +20.
                    val showNext = !slim && h >= 170f
                    val showRested = !slim && h >= 205f
                    val detailsHeight = if (slim) 54f else 115f + (if (showNext) 22f else 0f) + (if (showRested) 20f else 0f)
                    val scale = minOf(w / 260f, (h - 24f) / detailsHeight).coerceIn(0.7f, 1.4f)
                    val bubble = if (slim) 0f else scale * 22f
                    // Capped by width too, so the details column keeps room for its text.
                    Wide(
                        scale,
                        artSize = minOf(h - 24f - bubble, w * 0.36f).coerceIn(40f, 150f).dp,
                        slim = slim,
                        showNext = showNext,
                        showRested = showRested
                    )
                }
                else -> {
                    val scale = minOf(w / 280f, h / 300f).coerceIn(0.9f, 1.5f)
                    val thumb = ((w - 40f) / PikachuLook.entries.size - 6f).coerceIn(22f, 44f)
                    Large(
                        scale = scale,
                        artSize = minOf(w * 0.5f, h - 24f - scale * (22f + 150f + 10f) - thumb).coerceIn(64f, 220f).dp,
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
    ) {
        Box(modifier = GlanceModifier.fillMaxSize().padding((12 * layout.scale).dp)) {
            when (layout) {
                is WidgetLayout.Compact -> CompactLayout(snapshot, layout)
                is WidgetLayout.Wide -> WideLayout(snapshot, layout)
                is WidgetLayout.Large -> LargeLayout(snapshot, layout)
            }
        }
        // Pro: a double gold rim over the card, edge to edge.
        if (snapshot.isPremium) {
            Box(modifier = GlanceModifier.fillMaxSize().background(ImageProvider(R.drawable.widget_frame_pro))) {}
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
        if ((layout as WidgetLayout.Compact).showWeek) {
            Spacer(GlanceModifier.height((4 * layout.scale).dp))
            WeekStripRow(snapshot, layout.scale, dot = layout.weekDot, gap = layout.weekGap)
        }
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
                Details(snapshot, layout.scale, numberSize = 34f, showNext = layout.showNext, showRested = layout.showRested)
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
private fun Details(
    snapshot: StreakSnapshot,
    scale: Float,
    numberSize: Float,
    centered: Boolean = false,
    showNext: Boolean = true,
    showRested: Boolean = true
) {
    val context = LocalContext.current
    Column(horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start) {
        StreakCount(snapshot, numberSize = numberSize, scale = scale)
        Spacer(GlanceModifier.height((4 * scale).dp))
        // The Duolingo-style row: this week's days, today's circle waiting to be filled.
        WeekStripRow(snapshot, scale, dot = (16 * scale).dp, gap = (4 * scale).dp)
        Spacer(GlanceModifier.height((6 * scale).dp))
        StatusPill(snapshot.status, scale)
        if (showNext) {
            Spacer(GlanceModifier.height((6 * scale).dp))
            NextLookLine(snapshot, scale)
        }
        if (showRested && snapshot.restedXp > 0 && snapshot.mood != PikachuMood.Sleeping) {
            Spacer(GlanceModifier.height((4 * scale).dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_rested),
                    contentDescription = null,
                    modifier = GlanceModifier.size((11 * scale).dp)
                )
                Spacer(GlanceModifier.width(4.dp))
                Text(
                    text = context.getString(R.string.widget_streak_rested),
                    style = TextStyle(color = color(WidgetColors.Sleep), fontSize = 10f.sp(scale))
                )
            }
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
            // Fill the stage's real (weighted) slot and scale to fit, rather than a size computed
            // from estimated text heights: the estimate drifted at some sizes and clipped Pikachu.
            modifier = GlanceModifier.fillMaxSize()
        )
        Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
            Image(
                provider = ImageProvider(snapshot.mood.effect()),
                contentDescription = null,
                modifier = GlanceModifier.size((20 * layout.scale).dp)
            )
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
            val unlocked = snapshot.days >= look.minDays && snapshot.mood != PikachuMood.Sleeping
            Image(
                provider = ImageProvider(look.art()),
                contentDescription = context.getString(look.nameRes()),
                contentScale = ContentScale.Fit,
                colorFilter = if (unlocked) null else ColorFilter.tint(color(WidgetColors.Silhouette)),
                // Gap as padding, not a Spacer: a Glance Row keeps at most 10 children.
                modifier = GlanceModifier.padding(start = if (index > 0) 6.dp else 0.dp).size(thumbSize + if (index > 0) 6.dp else 0.dp)
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
        Image(
            provider = ImageProvider(
                when {
                    !alive -> R.drawable.ic_widget_flame_off
                    snapshot.isPremium -> R.drawable.ic_widget_flame_gold
                    else -> R.drawable.ic_widget_flame
                }
            ),
            contentDescription = null,
            modifier = GlanceModifier.size((numberSize * 0.85f * scale).dp)
        )
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
        if (snapshot.isPremium) {
            Spacer(GlanceModifier.width((6 * scale).dp))
            Box(
                modifier = GlanceModifier
                    .background(ImageProvider(R.drawable.widget_pill_pro))
                    .padding(horizontal = (6 * scale).dp, vertical = (1.5f * scale).dp)
            ) {
                Text(
                    text = context.getString(R.string.leaderboard_pro_chip),
                    style = TextStyle(color = color(WidgetColors.ProInk), fontSize = 8f.sp(scale), fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun StatusPill(status: StreakStatus, scale: Float) {
    val context = LocalContext.current
    when (status) {
        is StreakStatus.SafeToday -> Pill(
            context.getString(R.string.widget_streak_safe), R.drawable.widget_pill_safe, WidgetColors.Safe, scale,
            icon = R.drawable.ic_widget_check
        )
        is StreakStatus.AtRisk ->
            if (status.isUrgent) Pill(
                context.getString(R.string.widget_streak_hours_left, status.hoursLeft),
                R.drawable.widget_pill_urgent, WidgetColors.Urgent, scale,
                icon = R.drawable.ic_widget_clock
            )
            else Pill(context.getString(R.string.widget_streak_at_risk), R.drawable.widget_pill_risk, WidgetColors.Gold, scale)
        StreakStatus.NotStarted -> Pill(
            context.getString(R.string.widget_streak_start), R.drawable.widget_pill_neutral, WidgetColors.Text, scale
        )
    }
}

@Composable
private fun Pill(text: String, @DrawableRes background: Int, textColor: Color, scale: Float, @DrawableRes icon: Int? = null) {
    Row(
        modifier = GlanceModifier
            .background(ImageProvider(background))
            .padding(horizontal = (10 * scale).dp, vertical = (3 * scale).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Image(provider = ImageProvider(icon), contentDescription = null, modifier = GlanceModifier.size((12 * scale).dp))
            Spacer(GlanceModifier.width((4 * scale).dp))
        }
        Text(
            text = text,
            maxLines = 1,
            style = TextStyle(color = color(textColor), fontSize = 11f.sp(scale), fontWeight = FontWeight.Medium)
        )
    }
}

/**
 * This week as seven circles (locale order): gold with a check for played days, a faint ring for
 * missed ones, today's ring (red in the last hours) waiting to be filled, dim dots ahead.
 */
@Composable
private fun WeekStripRow(snapshot: StreakSnapshot, scale: Float, dot: Dp, gap: Dp) {
    val urgent = (snapshot.status as? StreakStatus.AtRisk)?.isUrgent == true
    val initials = SimpleDateFormat("EEEEE", Locale.getDefault())
    val cal = Calendar.getInstance()
    Row(verticalAlignment = Alignment.CenterVertically) {
        snapshot.week.forEachIndexed { index, day ->
            // Gap as padding, not a Spacer: a Glance Row keeps at most 10 children.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = GlanceModifier.padding(start = if (index > 0) gap else 0.dp)
            ) {
                cal.set(Calendar.DAY_OF_WEEK, day.dayOfWeek)
                Text(
                    text = initials.format(cal.time),
                    style = TextStyle(
                        color = color(if (day.isToday) snapshot.mood.accent() else WidgetColors.Muted),
                        fontSize = 9f.sp(scale),
                        fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal
                    )
                )
                Spacer(GlanceModifier.height((2 * scale).dp))
                Image(
                    provider = ImageProvider(
                        when (day.state) {
                            DayState.Played -> R.drawable.widget_day_played
                            DayState.Missed -> R.drawable.widget_day_missed
                            DayState.TodayOpen -> if (urgent) R.drawable.widget_day_today_urgent else R.drawable.widget_day_today
                            DayState.Future -> R.drawable.widget_day_future
                        }
                    ),
                    contentDescription = null,
                    modifier = GlanceModifier.size(dot)
                )
            }
        }
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
    Row(verticalAlignment = Alignment.CenterVertically) {
        // The goal you can see: the next outfit as a locked silhouette (a crown once all are won).
        Image(
            provider = ImageProvider(next?.first?.art() ?: R.drawable.ic_widget_crown),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            colorFilter = if (next != null) ColorFilter.tint(color(WidgetColors.Silhouette)) else null,
            modifier = GlanceModifier.size((18 * scale).dp)
        )
        Spacer(GlanceModifier.width((4 * scale).dp))
        Text(text = text, maxLines = 1, style = TextStyle(color = color(WidgetColors.Muted), fontSize = 10f.sp(scale)))
    }
}

/** Opens the app's launcher activity — no hard dependency on a flavor-specific Activity class. */
private fun launchIntent(context: Context): Intent =
    context.packageManager.getLaunchIntentForPackage(context.packageName)
        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        ?: Intent()
