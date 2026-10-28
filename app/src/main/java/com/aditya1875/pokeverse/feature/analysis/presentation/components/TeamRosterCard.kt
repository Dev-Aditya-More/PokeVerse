package com.aditya1875.pokeverse.feature.analysis.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import com.aditya1875.pokeverse.feature.pokemon.dex.PartyBadge
import com.aditya1875.pokeverse.feature.pokemon.dex.PokeBallIcon
import com.aditya1875.pokeverse.feature.pokemon.dex.isTrainersParty
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.analysis.presentation.screens.AnalysisColors
import com.aditya1875.pokeverse.feature.pokemon.profile.presentation.components.TrainerChip
import com.aditya1875.pokeverse.utils.localizedTypeName
import com.aditya1875.pokeverse.utils.pokemonTypeColor

/**
 * The team being analysed, so the numbers below can be checked against who is actually in it.
 * Three to a row so names and both type chips stay readable. Sprites stay silhouettes until
 * Original Assets is on, like everywhere else; names and types still carry the information.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TeamRosterCard(team: List<TeamMemberWithTypes>, assetsEnabled: Boolean, caughtIds: Set<Int>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AnalysisColors.CARD)
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.analysis_team_title, team.size),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
            if (isTrainersParty(team.map { it.id }, caughtIds)) {
                PartyBadge(container = Color.White.copy(alpha = 0.1f), content = Color.White)
            }
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            team.forEach { member ->
                Column(
                    modifier = Modifier.fillMaxWidth(1f / 3f).padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Box(Modifier.size(60.dp)) {
                        AsyncImage(
                            model = member.imageUrl,
                            contentDescription = member.name,
                            colorFilter = if (assetsEnabled) null else ColorFilter.tint(Color.Black, BlendMode.SrcAtop),
                            modifier = Modifier.fillMaxSize(),
                        )
                        if (member.id in caughtIds) {
                            PokeBallIcon(Modifier.align(Alignment.BottomEnd).size(16.dp))
                        }
                    }
                    Text(
                        member.name.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                    member.types.forEach { type ->
                        TrainerChip(localizedTypeName(type), pokemonTypeColor(type))
                    }
                }
            }
        }
    }
}
