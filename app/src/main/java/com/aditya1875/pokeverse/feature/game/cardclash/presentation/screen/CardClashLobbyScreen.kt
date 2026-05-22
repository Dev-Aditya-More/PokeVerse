package com.aditya1875.pokeverse.feature.game.cardclash.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aditya1875.pokeverse.feature.game.cardclash.domain.model.ClashPhase
import com.aditya1875.pokeverse.feature.game.cardclash.domain.model.ClashUiState

@Composable
fun CardClashLobbyScreen(
    state: ClashUiState,
    onPlayRandom: () -> Unit,
    onCreateFriendRoom: () -> Unit,
    onJoinByCode: (String) -> Unit,
    onCodeChanged: (String) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            state.isLoading || state.phase == ClashPhase.DEALING -> {
                LoadingView("Preparing your team...")
            }

            state.phase == ClashPhase.WAITING_FOR_OPPONENT -> {
                WaitingView(
                    roomCode = state.roomCode,
                    isRandom = state.roomCode.isNullOrBlank()
                )
            }

            else -> {
                LobbyContent(
                    state = state,
                    onPlayRandom = onPlayRandom,
                    onCreateFriendRoom = onCreateFriendRoom,
                    onJoinByCode = onJoinByCode,
                    onCodeChanged = onCodeChanged
                )
            }
        }
    }
}

@Composable
private fun LobbyContent(
    state: ClashUiState,
    onPlayRandom: () -> Unit,
    onCreateFriendRoom: () -> Unit,
    onJoinByCode: (String) -> Unit,
    onCodeChanged: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "PokeCard Clash",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "6 secret cards. 6 rounds.\nType advantage decides the winner.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        // Play Random
        Button(
            onClick = onPlayRandom,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Play Random", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }

        Text(
            text = "— or —",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Play a Friend
        FriendRoomCard(
            enteredCode = state.enteredCode,
            onCodeChanged = onCodeChanged,
            onCreateRoom = onCreateFriendRoom,
            onJoinByCode = { onJoinByCode(state.enteredCode) }
        )

        state.error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FriendRoomCard(
    enteredCode: String,
    onCodeChanged: (String) -> Unit,
    onCreateRoom: () -> Unit,
    onJoinByCode: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Play a Friend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedButton(
                onClick = onCreateRoom,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Create Room & Share Code")
            }

            Text(
                text = "or enter a room code",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = enteredCode,
                    onValueChange = onCodeChanged,
                    placeholder = { Text("ABCD12") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { if (enteredCode.length == 6) onJoinByCode() })
                )
                Button(
                    onClick = onJoinByCode,
                    enabled = enteredCode.length == 6,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Join")
                }
            }
        }
    }
}

@Composable
private fun WaitingView(roomCode: String?, isRandom: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(24.dp)
    ) {
        CircularProgressIndicator(modifier = Modifier.size(48.dp))
        Text(
            text = if (isRandom) "Looking for an opponent..." else "Waiting for your friend...",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        if (!roomCode.isNullOrBlank()) {
            Text(
                text = "Share this code:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = roomCode,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 6.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun LoadingView(message: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator(modifier = Modifier.size(48.dp))
        Text(text = message, style = MaterialTheme.typography.bodyLarge)
    }
}
