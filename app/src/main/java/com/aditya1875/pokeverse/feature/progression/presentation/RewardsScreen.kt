package com.aditya1875.pokeverse.feature.progression.presentation

import androidx.compose.material.icons.filled.ShoppingBag
import com.aditya1875.pokeverse.feature.progression.domain.BagRules
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.AcUnit
import com.aditya1875.pokeverse.feature.core.ui.components.CoinAmount
import com.aditya1875.pokeverse.feature.pokemon.onboarding.Partner
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.pokemon.onboarding.Starter
import com.aditya1875.pokeverse.feature.pokemon.settings.presentation.viewmodels.SettingsViewModel
import com.aditya1875.pokeverse.feature.pokemon.theme_selector.getStarterThemes
import com.aditya1875.pokeverse.feature.progression.data.ProgressionState
import com.aditya1875.pokeverse.feature.progression.domain.CoinEconomy
import com.aditya1875.pokeverse.feature.progression.domain.Cosmetic
import com.aditya1875.pokeverse.feature.progression.domain.CosmeticKind
import com.aditya1875.pokeverse.feature.progression.domain.Cosmetics
import com.aditya1875.pokeverse.feature.progression.domain.LevelRewards
import com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.BallType
import com.aditya1875.pokeverse.feature.game.wildcatch.presentation.components.BallIcon
import com.aditya1875.pokeverse.feature.progression.domain.ShopItem
import com.aditya1875.pokeverse.feature.progression.domain.ShopItems
import com.aditya1875.pokeverse.utils.rememberAdaptiveHPadding
import org.koin.compose.viewmodel.koinViewModel

private val CoinGold = Color(0xFFFFC24B)
private const val MAX_PATH_LEVEL = 100

private enum class RewardsTab { SHOP, ITEMS, PATH }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardsScreen(
    navController: NavController,
    viewModel: RewardsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var tab by remember { mutableStateOf(RewardsTab.SHOP) }
    var confirming by remember { mutableStateOf<Cosmetic?>(null) }
    var confirmingItem by remember { mutableStateOf<ShopItem?>(null) }
    var confirmingBag by remember { mutableStateOf(false) }
    var showGuide by remember { mutableStateOf(false) }
    val settingsViewModel: SettingsViewModel = koinViewModel()
    val assetsEnabled = true
    val hPad = rememberAdaptiveHPadding()

    LaunchedEffect(Unit) { viewModel.markSeen() }

    // Messages need string resources, so resolve them where we have a Context.
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.messages.collect { msg ->
            val text = when (msg) {
                is RewardsMessage.Bought -> context.getString(
                    if (msg.item.kind == CosmeticKind.THEME) R.string.rewards_msg_bought_theme else R.string.rewards_msg_bought,
                    context.getString(msg.item.name)
                )
                RewardsMessage.FreezeBought -> context.getString(R.string.rewards_msg_freeze_bought)
                RewardsMessage.FreezeFull -> context.getString(R.string.rewards_msg_freeze_full)
                RewardsMessage.NotEnoughCoins -> context.getString(R.string.rewards_msg_not_enough)
                is RewardsMessage.ItemBought -> context.getString(R.string.rewards_msg_item_bought, context.getString(msg.item.name))
                // Say WHY it was refused: the bag is full of this item, and how full.
                is RewardsMessage.ItemFull -> context.getString(
                    R.string.rewards_msg_bag_full, context.getString(msg.item.name), msg.held, msg.capacity
                )
                is RewardsMessage.BagUpgraded -> context.getString(R.string.rewards_msg_bag_upgraded, msg.level)
                RewardsMessage.BagMaxed -> context.getString(R.string.rewards_msg_bag_maxed)
            }
            // A full bag offers the way out right there in the message.
            val action = (msg as? RewardsMessage.ItemFull)?.takeIf { it.canUpgrade }
                ?.let { context.getString(R.string.rewards_bag_upgrade_action) }
            val result = snackbar.showSnackbar(text, actionLabel = action)
            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                tab = RewardsTab.ITEMS
                confirmingBag = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.rewards_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    CoinBalance(state.balance)
                    IconButton(onClick = { showGuide = true }) {
                        Icon(Icons.Outlined.HelpOutline, contentDescription = stringResource(R.string.guide_title))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = hPad, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = tab == RewardsTab.SHOP,
                    onClick = { tab = RewardsTab.SHOP },
                    label = { Text(stringResource(R.string.rewards_tab_shop)) }
                )
                FilterChip(
                    selected = tab == RewardsTab.ITEMS,
                    onClick = { tab = RewardsTab.ITEMS },
                    label = { Text(stringResource(R.string.rewards_tab_items)) }
                )
                FilterChip(
                    selected = tab == RewardsTab.PATH,
                    onClick = { tab = RewardsTab.PATH },
                    label = { Text(stringResource(R.string.rewards_tab_path)) }
                )
            }

            when (tab) {
                RewardsTab.SHOP -> ShopTab(
                    state = state,
                    isGuest = profile.isGuest,
                    partner = if (assetsEnabled) (Partner.from(profile.starter) ?: Partner.of(Starter.PIKACHU)) else null,
                    hPad = hPad,
                    onBuy = { confirming = it },
                    onBuyFreeze = viewModel::buyFreeze,
                    onEquip = viewModel::equip,
                    onUnequip = viewModel::unequip
                )
                RewardsTab.ITEMS -> ItemsTab(
                    state = state,
                    isGuest = profile.isGuest,
                    hPad = hPad,
                    onBuy = { confirmingItem = it },
                    onUpgradeBag = { confirmingBag = true }
                )
                RewardsTab.PATH -> PathTab(state = state, level = profile.level, hPad = hPad)
            }
        }
    }

    if (showGuide) CoinGuideSheet(onDismiss = { showGuide = false })

    confirming?.let { item ->
        val price = item.price ?: 0
        AlertDialog(
            onDismissRequest = { confirming = null },
            title = { Text(stringResource(R.string.rewards_confirm_title, stringResource(item.name))) },
            text = { Text(stringResource(R.string.rewards_confirm_body, price, state.balance)) },
            confirmButton = {
                TextButton(
                    enabled = state.balance >= price,
                    onClick = { viewModel.buy(item); confirming = null }
                ) { Text(stringResource(R.string.rewards_buy)) }
            },
            dismissButton = {
                TextButton(onClick = { confirming = null }) { Text(stringResource(R.string.rewards_cancel)) }
            }
        )
    }

    if (confirmingBag) {
        val nextLevel = state.bagLevel + 1
        val price = BagRules.upgradePrice(state.bagLevel)
        if (price == null) confirmingBag = false
        else AlertDialog(
            onDismissRequest = { confirmingBag = false },
            title = { Text(stringResource(R.string.rewards_confirm_bag_title)) },
            text = { Text(stringResource(R.string.rewards_confirm_bag_body, nextLevel, BagRules.multiplier(nextLevel), price, state.balance)) },
            confirmButton = {
                TextButton(
                    enabled = state.balance >= price,
                    onClick = { viewModel.upgradeBag(); confirmingBag = false }
                ) { Text(stringResource(R.string.rewards_bag_upgrade)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmingBag = false }) { Text(stringResource(R.string.rewards_cancel)) }
            }
        )
    }

    confirmingItem?.let { item ->
        AlertDialog(
            onDismissRequest = { confirmingItem = null },
            title = { Text(stringResource(R.string.rewards_confirm_title, stringResource(item.name))) },
            text = { Text(stringResource(R.string.rewards_confirm_body, item.price, state.balance)) },
            confirmButton = {
                TextButton(
                    enabled = state.balance >= item.price,
                    onClick = { viewModel.buyItem(item); confirmingItem = null }
                ) { Text(stringResource(R.string.rewards_buy)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmingItem = null }) { Text(stringResource(R.string.rewards_cancel)) }
            }
        )
    }
}

@Composable
fun CoinBalance(balance: Int, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(50), color = CoinGold.copy(alpha = 0.18f), modifier = modifier) {
        CoinAmount(
            amount = balance,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = CoinGold
        )
    }
}

// ── Shop ─────────────────────────────────────────────────────────────────────

@Composable
private fun ShopTab(
    state: ProgressionState,
    isGuest: Boolean,
    partner: Partner?,
    hPad: androidx.compose.ui.unit.Dp,
    onBuy: (Cosmetic) -> Unit,
    onBuyFreeze: () -> Unit,
    onEquip: (Cosmetic) -> Unit,
    onUnequip: (CosmeticKind) -> Unit
) {
    val sections = listOf(
        CosmeticKind.FRAME to R.string.rewards_section_frames,
        CosmeticKind.TITLE to R.string.rewards_section_titles,
        CosmeticKind.BACKDROP to R.string.rewards_section_backdrops,
        CosmeticKind.WIDGET to R.string.rewards_section_widgets,
    )
    val themeColors = remember { getStarterThemes().associate { it.theme.name to it.colors } }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 156.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = hPad, end = hPad, top = 4.dp, bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isGuest) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(
                        stringResource(R.string.rewards_guest_banner),
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            FreezeCard(state = state, canBuy = !isGuest, onBuy = onBuyFreeze)
        }

        sections.forEach { (kind, titleRes) ->
            item(key = "header_$kind", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    stringResource(titleRes),
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            // Buyable first (cheapest first), then level-earned in level order.
            val items = Cosmetics.ofKind(kind).sortedWith(
                compareBy<Cosmetic> { it.price == null }.thenBy { it.price ?: 0 }.thenBy { it.levelUnlock ?: 0 }
            )
            items(items, key = { it.id }) { item ->
                CosmeticCard(
                    item = item,
                    state = state,
                    partner = partner,
                    themeColors = themeColors[item.themeName],
                    canBuy = !isGuest,
                    onBuy = { onBuy(item) },
                    onEquip = { onEquip(item) },
                    onUnequip = { onUnequip(item.kind) }
                )
            }
        }
    }
}

// ── Items ────────────────────────────────────────────────────────────────────

/** Things that get used up: balls and a lure for Wild Catch, stones and held items for evolving, shard pouches. */
@Composable
private fun ItemsTab(
    state: ProgressionState,
    isGuest: Boolean,
    hPad: androidx.compose.ui.unit.Dp,
    onBuy: (ShopItem) -> Unit,
    onUpgradeBag: () -> Unit
) {
    val sections = listOf(
        R.string.rewards_section_balls to (ShopItems.balls + ShopItems.boosts),
        R.string.rewards_section_stones to ShopItems.stones,
        R.string.rewards_section_evo_items to ShopItems.evoItems,
    )
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 156.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = hPad, end = hPad, top = 4.dp, bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isGuest) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(
                        stringResource(R.string.rewards_guest_banner),
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
        item(key = "bag", span = { GridItemSpan(maxLineSpan) }) {
            BagCard(state = state, canBuy = !isGuest, onUpgrade = onUpgradeBag)
        }
        sections.forEach { (titleRes, items) ->
            item(key = "items_header_$titleRes", span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.padding(top = 8.dp)) {
                    Text(
                        stringResource(titleRes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            items(items, key = { it.id }) { item ->
                ItemCard(item = item, state = state, canBuy = !isGuest, onBuy = { onBuy(item) })
            }
        }
    }
}

/** The bag: its level and the next upgrade. Tap it for what it does. */
@Composable
private fun BagCard(state: ProgressionState, canBuy: Boolean, onUpgrade: () -> Unit) {
    val price = BagRules.upgradePrice(state.bagLevel)
    var info by remember { mutableStateOf(false) }
    Card(
        onClick = { info = true },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = CoinGold, modifier = Modifier.size(32.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.rewards_bag_title), fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.rewards_bag_level, state.bagLevel, BagRules.MAX_LEVEL, BagRules.multiplier(state.bagLevel)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Button(onClick = onUpgrade, enabled = canBuy && price != null && state.balance >= price) {
                if (price == null) Text(stringResource(R.string.rewards_bag_max)) else CoinAmount(price)
            }
        }
    }
    if (info) InfoDialog(stringResource(R.string.rewards_bag_title), stringResource(R.string.rewards_bag_desc)) { info = false }
}

/** The explanation of something, shown only when the player taps it, so the lists stay clean. */
@Composable
private fun InfoDialog(title: String, body: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.battle_help_got_it)) } },
    )
}

/** Every item card is the same size: icon, name, buy button. Tap it for the description and how many you hold. */
private val ItemCardHeight = 176.dp

@Composable
private fun ItemCard(item: ShopItem, state: ProgressionState, canBuy: Boolean, onBuy: () -> Unit) {
    val held = state.held(item.id)
    val capacity = state.capacityOf(item)
    val full = item.isInventory && held + item.quantity > capacity
    var info by remember { mutableStateOf(false) }
    val name = stringResource(item.name) + if (item.quantity > 1) "  ×${item.quantity}" else ""
    Card(
        onClick = { info = true },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth().height(ItemCardHeight),
    ) {
        Column(
            Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // The real item sprite (or the drawn ball), never an emoji.
            ItemIcon(item, size = 56.dp)
            Text(
                name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                minLines = 2,
                maxLines = 2,
            )
            Button(
                onClick = onBuy,
                enabled = canBuy && !full && state.balance >= item.price,
                modifier = Modifier.fillMaxWidth()
            ) { if (full) Text(stringResource(R.string.rewards_item_full)) else CoinAmount(item.price) }
        }
    }
    if (info) {
        val desc = if (item.shards > 0) stringResource(item.desc, item.shards) else stringResource(item.desc)
        val extra = if (!item.isInventory) "" else {
            "\n\n" + stringResource(R.string.rewards_item_held_cap, held, capacity) + if (full) {
                "\n" + stringResource(
                    if (state.bagLevel < BagRules.MAX_LEVEL && item.id != ShopItems.ID_MASTER_BALL) R.string.rewards_bag_full_hint
                    else R.string.rewards_bag_full_max
                )
            } else ""
        }
        InfoDialog(name, desc + extra) { info = false }
    }
}

@Composable
private fun FreezeCard(state: ProgressionState, canBuy: Boolean, onBuy: () -> Unit) {
    var info by remember { mutableStateOf(false) }
    Card(
        onClick = { info = true },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.AcUnit, contentDescription = null, tint = Color(0xFF4FC3F7), modifier = Modifier.size(32.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.rewards_freeze_title), fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.rewards_freeze_held, state.freezes, CoinEconomy.FREEZE_MAX),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Button(
                onClick = onBuy,
                enabled = canBuy && state.freezes < CoinEconomy.FREEZE_MAX && state.balance >= CoinEconomy.FREEZE_PRICE
            ) { CoinAmount(CoinEconomy.FREEZE_PRICE) }
        }
    }
    if (info) InfoDialog(stringResource(R.string.rewards_freeze_title), stringResource(R.string.rewards_freeze_desc)) { info = false }
}

/** Every cosmetic card is the same size: preview, name, then one action. */
private val CosmeticCardHeight = 216.dp

@Composable
private fun CosmeticCard(
    item: Cosmetic,
    state: ProgressionState,
    partner: Partner?,
    themeColors: List<Color>?,
    canBuy: Boolean,
    onBuy: () -> Unit,
    onEquip: () -> Unit,
    onUnequip: () -> Unit
) {
    val owned = state.owns(item.id)
    val equipped = state.equippedId(item.kind) == item.id && item.kind != CosmeticKind.THEME
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = if (equipped) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.fillMaxWidth().height(CosmeticCardHeight),
    ) {
        Column(
            Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(Modifier.height(92.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CosmeticPreview(item, state, partner, themeColors)
            }
            // A title's preview already is its name, so the line is left empty but keeps its room: all cards match.
            Text(
                if (item.kind == CosmeticKind.TITLE) "" else stringResource(item.name),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                minLines = 2,
                maxLines = 2
            )
            Box(Modifier.height(40.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                when {
                    equipped -> OutlinedButton(onClick = onUnequip, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.rewards_equipped))
                    }
                    owned && item.kind == CosmeticKind.THEME -> Text(
                        stringResource(R.string.rewards_owned),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    owned -> Button(onClick = onEquip, modifier = Modifier.fillMaxSize()) {
                        Text(stringResource(R.string.rewards_equip))
                    }
                    item.price != null -> Button(
                        onClick = onBuy,
                        enabled = canBuy && state.balance >= item.price,
                        modifier = Modifier.fillMaxSize()
                    ) { CoinAmount(item.price) }
                    else -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (item.fromChallenge) stringResource(R.string.rewards_challenge_locked) else stringResource(R.string.rewards_level_locked, item.levelUnlock ?: 0),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

/** Frames and backdrops preview on the player's own partner, combined with what they already wear in the other slot. */
@Composable
private fun CosmeticPreview(item: Cosmetic, state: ProgressionState, partner: Partner?, themeColors: List<Color>?) {
    when (item.kind) {
        CosmeticKind.FRAME -> ShowcasePreview(partner, backdropId = state.backdrop, frameId = item.id)
        CosmeticKind.BACKDROP -> ShowcasePreview(partner, backdropId = item.id, frameId = state.frame)
        CosmeticKind.TITLE -> TitleChip(item.id)
        CosmeticKind.WIDGET -> {
            val accent = widgetAccentArgb(item.id)?.let { Color(it) } ?: CoinGold
            Box(
                Modifier.size(width = 96.dp, height = 52.dp).clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1B1B26))
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.fillMaxSize().clip(RoundedCornerShape(11.dp)).background(accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                        Text("7", color = accent, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
        CosmeticKind.THEME -> Box(
            Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(themeColors ?: listOf(Color.Gray, Color.DarkGray)))
        )
    }
}

// ── Level path ───────────────────────────────────────────────────────────────

@Composable
private fun PathTab(state: ProgressionState, level: Int, hPad: androidx.compose.ui.unit.Dp) {
    val listState = rememberLazyListState()
    // Open near the player's current level so "what's next" is the first thing they see.
    LaunchedEffect(level) {
        listState.scrollToItem((level - 2).coerceIn(0, MAX_PATH_LEVEL - 2))
    }
    val levels = remember { (2..MAX_PATH_LEVEL).mapNotNull { LevelRewards.forLevel(it) } }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = hPad, end = hPad, top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(levels, key = { it.level }) { reward ->
            val reached = level >= reward.level
            val here = level == reward.level
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        here -> MaterialTheme.colorScheme.primaryContainer
                        reward.isMilestone -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                border = if (reward.isMilestone) BorderStroke(1.dp, CoinGold.copy(alpha = 0.6f)) else null
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // The level, as a badge: a check once it is behind you, the number until then.
                    Surface(
                        shape = CircleShape,
                        color = if (reached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ) {
                        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                            if (reached && !here) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.onPrimary)
                            else Text(
                                "${reward.level}",
                                fontWeight = FontWeight.Black,
                                color = if (reached) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                stringResource(R.string.rewards_path_level, reward.level),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (here) {
                                Text(
                                    stringResource(R.string.rewards_path_here).uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        LevelRewardPills(reward)
                    }
                }
            }
        }
    }
}
