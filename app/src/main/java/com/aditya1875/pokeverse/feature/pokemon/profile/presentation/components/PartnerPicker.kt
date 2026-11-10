package com.aditya1875.pokeverse.feature.pokemon.profile.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.repository.PokemonDetailRepo
import com.aditya1875.pokeverse.feature.pokemon.dex.DexProgress
import com.aditya1875.pokeverse.feature.pokemon.dex.PokedexRepository
import com.aditya1875.pokeverse.feature.pokemon.onboarding.Partner
import com.aditya1875.pokeverse.feature.pokemon.onboarding.RevealArt
import com.aditya1875.pokeverse.feature.pokemon.onboarding.Starter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Pick a new partner: a starter or any Pokémon you have caught. [onPick] receives the text to save on the
 * profile (see [Partner]). Artwork follows Original Assets like everywhere else (a silhouette without it).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartnerPickerSheet(
    current: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val dex: PokedexRepository = koinInject()
    val repo: PokemonDetailRepo = koinInject()
    val progress by dex.progress.collectAsStateWithLifecycle(initialValue = DexProgress())
    val currentDex = Partner.from(current)?.dexId
    val starterDex = remember { Starter.entries.map { it.dexId }.toSet() }
    val others = remember(progress.roster) { progress.roster.filter { it !in starterDex }.sorted() }
    var busy by remember { mutableStateOf<Int?>(null) }
    var failed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.partner_pick_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.partner_pick_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (failed) {
                Text(stringResource(R.string.partner_set_failed), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(92.dp),
                modifier = Modifier.heightIn(max = 440.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(Starter.entries.toList(), key = { "s${it.id}" }) { s ->
                    PartnerCell(
                        dexId = s.dexId, name = s.displayName, selected = currentDex == s.dexId,
                        busy = false,
                    ) {
                        onPick(s.id)
                        onDismiss()
                    }
                }
                items(others, key = { it }) { id ->
                    val name by produceState<String?>(null, id) {
                        value = runCatching { repo.getPokemonByName(id.toString()).name }.getOrNull()
                    }
                    PartnerCell(
                        dexId = id, name = name?.replace('-', ' ')?.replaceFirstChar { it.uppercase() } ?: "#$id",
                        selected = currentDex == id, busy = busy == id,
                    ) {
                        if (busy != null) return@PartnerCell
                        scope.launch {
                            busy = id; failed = false
                            try {
                                val p = repo.getPokemonByName(id.toString())
                                onPick(Partner.encode(id, p.name, p.types.firstOrNull()?.type?.name))
                                onDismiss()
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                failed = true
                            } finally {
                                busy = null
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PartnerCell(dexId: Int, name: String, selected: Boolean, busy: Boolean, onClick: () -> Unit) {
    val url = remember(dexId) { Partner(dexId, name, null).artworkUrl }
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(
                BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center) {
            RevealArt(url = url, name = name, reveal = true, modifier = Modifier.size(68.dp))
            if (busy) CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        }
        Text(
            name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
            overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
        )
    }
}
