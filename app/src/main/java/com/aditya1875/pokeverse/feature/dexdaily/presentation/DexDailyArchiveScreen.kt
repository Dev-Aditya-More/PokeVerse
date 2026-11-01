package com.aditya1875.pokeverse.feature.dexdaily.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.dexdaily.domain.DexDailyCalendar
import com.aditya1875.pokeverse.feature.dexdaily.domain.DexDailyPuzzles
import com.aditya1875.pokeverse.feature.dexdaily.domain.DexDailyRecord
import com.aditya1875.pokeverse.feature.game.premium.LocalPremium
import com.aditya1875.pokeverse.feature.game.premium.components.PremiumBottomSheet
import com.aditya1875.pokeverse.feature.leaderboard.presentation.components.PremiumGold
import com.aditya1875.pokeverse.feature.leaderboard.presentation.components.PremiumGoldDeep
import com.aditya1875.pokeverse.feature.leaderboard.presentation.components.PremiumGoldLight
import com.aditya1875.pokeverse.feature.leaderboard.presentation.components.ProChip
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Every past Dex Daily as a month calendar. Pro players can play any day; free players see
 * their own history plus the locked days they could be playing, with one quiet Pro banner.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DexDailyArchiveScreen(viewModel: DexDailyViewModel, onBack: () -> Unit) {
    val today by viewModel.today.collectAsStateWithLifecycle()
    val records by viewModel.records.collectAsStateWithLifecycle()
    val openDate by viewModel.openDate.collectAsStateWithLifecycle()
    val isPremium = LocalPremium.current
    var showPlans by remember { mutableStateOf(false) }

    // Month shown, as "yyyy-MM"; starts on the current month.
    var month by rememberSaveable { mutableStateOf(today.take(7)) }
    val firstMonth = DexDailyCalendar.EPOCH.take(7)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dex_daily_archive_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(Modifier.widthIn(max = 520.dp)) {
                if (!isPremium) {
                    ProBanner(onClick = { showPlans = true })
                    Spacer(Modifier.height(16.dp))
                }

                // Month header
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = { month = shiftMonth(month, -1) }, enabled = month > firstMonth) {
                        Icon(Icons.Default.ChevronLeft, null)
                    }
                    Text(
                        monthTitle(month),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { month = shiftMonth(month, 1) }, enabled = month < today.take(7)) {
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }

                val days = remember(month) { daysOf(month) }
                val played = days.mapNotNull { records[it] }.filter { it.finished }
                Text(
                    stringResource(R.string.dex_daily_archive_stats, played.count { it.solved }, played.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                // Weekday initials, in the player's locale and first-day-of-week.
                val weekdays = remember { weekdayInitials() }
                Row(Modifier.fillMaxWidth()) {
                    weekdays.forEach {
                        Text(
                            it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))

                val leading = remember(month) { leadingBlanks(month) }
                val cells: List<String?> = List(leading) { null } + days
                cells.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        week.forEach { date ->
                            Box(Modifier.weight(1f)) {
                                if (date != null) {
                                    DayCell(
                                        date = date,
                                        today = today,
                                        record = records[date],
                                        isPremium = isPremium,
                                        onPlay = { viewModel.open(date) },
                                        onLocked = { showPlans = true }
                                    )
                                }
                            }
                        }
                        repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(6.dp))
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    openDate?.let { date -> DexDailySheet(viewModel, date, onDismiss = { viewModel.close() }) }
    if (showPlans) PremiumBottomSheet(onDismiss = { showPlans = false })
}

@Composable
private fun DayCell(
    date: String,
    today: String,
    record: DexDailyRecord?,
    isPremium: Boolean,
    onPlay: () -> Unit,
    onLocked: () -> Unit,
) {
    val playable = DexDailyCalendar.isPlayable(date, today)
    val isToday = date == today
    // Free players can still open days they've played, and always today.
    val unlocked = playable && (isPremium || isToday || record != null)
    val finished = record?.finished == true
    val shape = RoundedCornerShape(12.dp)

    Surface(
        shape = shape,
        color = when {
            record?.solved == true -> DexGreen.copy(alpha = 0.14f)
            record?.failed == true -> DexRed.copy(alpha = 0.10f)
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        border = if (isToday) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .aspectRatio(0.8f)
            .alpha(if (playable) 1f else 0.35f)
            .clip(shape)
            .then(
                if (!playable) Modifier
                else Modifier.clickable { if (unlocked) onPlay() else onLocked() }
            )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 4.dp)) {
            Text(
                date.takeLast(2).trimStart('0'),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isToday) FontWeight.Black else FontWeight.Medium,
                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                when {
                    !playable -> {}
                    finished || (unlocked && !isToday) -> {
                        val id = remember(date) { DexDailyPuzzles.forDate(date).answerId }
                        val context = LocalContext.current
                        AsyncImage(
                            model = remember(id) { ImageRequest.Builder(context).data(DexDailyPuzzles.artworkUrl(id)).build() },
                            contentDescription = null,
                            // Only a solved day shows its colours; everything else stays a shadow.
                            colorFilter = if (record?.solved == true) null
                            else ColorFilter.tint(MaterialTheme.colorScheme.onSurface.copy(alpha = if (finished) 0.75f else 0.35f)),
                            modifier = Modifier.fillMaxSize(0.82f)
                        )
                    }
                    !unlocked -> Icon(
                        Icons.Default.Lock, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                    else -> Text("?", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
            if (record?.finished == true) {
                Box(Modifier.padding(bottom = 4.dp)) { ResultBadge(record.solved, size = 12.dp) }
            } else Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun ProBanner(onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(PremiumGold.copy(alpha = 0.22f), PremiumGold.copy(alpha = 0.06f))))
            .border(1.dp, Brush.horizontalGradient(listOf(PremiumGoldLight, PremiumGoldDeep)), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.dex_daily_archive_pro_title), fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.dex_daily_archive_pro_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(10.dp))
        ProChip(onClick = onClick)
    }
}

// ── Calendar helpers (Locale.US for keys, player's locale for labels) ─────────

private fun keyFormat() = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }

private fun monthStart(month: String): Calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US).apply {
    time = keyFormat().parse("$month-01")!!
}

private fun shiftMonth(month: String, delta: Int): String =
    keyFormat().format(monthStart(month).apply { add(Calendar.MONTH, delta) }.time).take(7)

private fun daysOf(month: String): List<String> {
    val cal = monthStart(month)
    val count = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    return (1..count).map { "$month-${it.toString().padStart(2, '0')}" }
}

private fun leadingBlanks(month: String): Int {
    val firstDow = Calendar.getInstance(Locale.getDefault()).firstDayOfWeek
    val dow = monthStart(month).get(Calendar.DAY_OF_WEEK)
    return (dow - firstDow + 7) % 7
}

private fun weekdayInitials(): List<String> {
    val locale = Locale.getDefault()
    val first = Calendar.getInstance(locale).firstDayOfWeek
    val fmt = SimpleDateFormat("EEEEE", locale)
    val cal = Calendar.getInstance(locale)
    return (0 until 7).map { i ->
        cal.set(Calendar.DAY_OF_WEEK, (first - 1 + i) % 7 + 1)
        fmt.format(cal.time)
    }
}

private fun monthTitle(month: String): String =
    SimpleDateFormat("LLLL yyyy", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }
        .format(monthStart(month).time)
        .replaceFirstChar { it.titlecase(Locale.getDefault()) }

