package com.aditya1875.pokeverse.screens.analysis

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.aditya1875.pokeverse.screens.analysis.components.AnalysisContent
import com.aditya1875.pokeverse.screens.analysis.components.ErrorView
import com.aditya1875.pokeverse.screens.analysis.components.PokemonTypeData
import com.aditya1875.pokeverse.screens.analysis.components.TeamAnalysis
import com.aditya1875.pokeverse.screens.analysis.components.TeamAnalyzer
import com.aditya1875.pokeverse.screens.analysis.components.TeamMemberWithTypes
import com.aditya1875.pokeverse.screens.analysis.components.TypeDiversity
import com.aditya1875.pokeverse.screens.analysis.components.LoadingView
import com.aditya1875.pokeverse.ui.viewmodel.PokemonViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TeamAnalysisScreen(
    navController: NavController,
    viewModel: PokemonViewModel = koinViewModel()
) {
    val team by viewModel.team.collectAsStateWithLifecycle()

    var teamWithTypes by remember { mutableStateOf<List<TeamMemberWithTypes>>(emptyList()) }
    var analysis by remember { mutableStateOf<TeamAnalysis?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Fetch types when team changes
    LaunchedEffect(team) {
        if (team.isEmpty()) {
            teamWithTypes = emptyList()
            analysis = null
            return@LaunchedEffect
        }

        isLoading = true
        errorMessage = null

        try {
            val withTypes = mutableListOf<TeamMemberWithTypes>()

            team.forEach { member ->
                try {
                    val pokemon = viewModel.repository.getPokemonByName(member.name)
                    val types = pokemon.types.map { it.type.name }

                    withTypes.add(
                        TeamMemberWithTypes(
                            name = member.name,
                            types = types,
                            imageUrl = member.imageUrl
                        )
                    )
                } catch (e: Exception) {
                    Log.e("TeamAnalysis", "Failed to fetch data for ${member.name}", e)
                    withTypes.add(
                        TeamMemberWithTypes(
                            name = member.name,
                            types = listOf("normal"),
                            imageUrl = member.imageUrl
                        )
                    )
                }
            }

            teamWithTypes = withTypes
            analysis = TeamAnalyzer.analyzeTeam(withTypes)

            Log.d("TeamAnalysis", "Analysis complete: score=${analysis?.coverageScore}")

        } catch (e: Exception) {
            Log.e("TeamAnalysis", "Failed to analyze team", e)
            errorMessage = "Failed to analyze team: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        containerColor = Color(0xFF0F0F0F),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Team Analysis",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A1A)
                )
            )
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                team.isEmpty() -> {
                    EmptyAnalysisView(navController)
                }

                isLoading -> {
                    LoadingView()
                }

                errorMessage != null -> {
                    ErrorView(errorMessage!!, navController)
                }

                analysis != null -> {
                    AnalysisContent(
                        analysis = analysis!!,
                        teamWithTypes = teamWithTypes
                    )
                }
            }
        }
    }
}

@Composable
fun TypeDiversityCard(diversity: TypeDiversity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Type Diversity",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            // Unique types count
            InfoRow(
                label = "Unique Types",
                value = "${diversity.uniqueTypes}/18",
                color = when {
                    diversity.uniqueTypes >= 10 -> Color(0xFF4CAF50)
                    diversity.uniqueTypes >= 6 -> Color(0xFFFFC107)
                    else -> Color(0xFFFF5252)
                }
            )

            Spacer(Modifier.height(12.dp))

            // Type distribution
            Text(
                text = "Type Distribution",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                diversity.typeDistribution.entries.sortedByDescending { it.value }.forEach { (type, count) ->
                    TypeChip(type = type, count = count)
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            color = color,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TypeChip(type: String, count: Int) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = PokemonTypeData.getTypeColor(type)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = type.replaceFirstChar { it.uppercase() },
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
            if (count > 1) {
                Text(
                    text = "×$count",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
fun EmptyAnalysisView(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1C1C))
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "No Team to Analyze",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Build a team first to see analysis",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { navController.navigate("home") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDC3545)
                )
            ) {
                Text("Build Team")
            }
        }
    }
}