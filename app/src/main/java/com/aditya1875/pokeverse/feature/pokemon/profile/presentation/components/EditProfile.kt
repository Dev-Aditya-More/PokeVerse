package com.aditya1875.pokeverse.feature.pokemon.profile.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aditya1875.pokeverse.feature.pokemon.profile.presentation.viewmodels.ProfileViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun EditProfileDialog(
    onDismiss: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel()
) {

    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    var name by remember(profile.username) { mutableStateOf(profile.username) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Edit Trainer Name",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("Trainer Name") },
                modifier = Modifier.fillMaxWidth()
            )
        },

        confirmButton = {
            Button(
                onClick = {
                    viewModel.updateUsername(name.trim())
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },

        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}