package com.aditya1875.pokeverse.feature.referral.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPValues
import com.aditya1875.pokeverse.feature.referral.data.RedeemResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteFriendsSheet(
    viewModel: ReferralViewModel,
    onDismiss: () -> Unit,
    prefillCode: String? = null
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        viewModel.load()
        viewModel.claimRewards(force = true)
        if (prefillCode != null) viewModel.redeem(prefillCode)
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🤝", fontSize = 40.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.referral_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            RewardRow("🎁", stringResource(R.string.referral_reward_you, XPValues.REFERRAL_PER_FRIEND))
            RewardRow("✨", stringResource(R.string.referral_reward_friend, XPValues.REFERRAL_WELCOME))
            Spacer(Modifier.height(20.dp))

            when {
                !state.signedIn -> Text(
                    stringResource(R.string.referral_sign_in),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                state.info != null -> {
                    val info = state.info!!
                    CodeCard(info.code)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { copyCode(context, info.code) }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.referral_copy))
                        }
                        val shareText = stringResource(
                            R.string.referral_share_message, info.code, XPValues.REFERRAL_WELCOME, viewModel.shareLink(info.code)
                        )
                        val chooser = stringResource(R.string.referral_share_chooser)
                        Button(
                            onClick = {
                                val send = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(send, chooser))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.referral_share))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        stringResource(R.string.referral_joined_count, info.joined),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                state.loading -> CircularProgressIndicator()
                state.loadFailed -> TextButton(onClick = viewModel::load) {
                    Text(stringResource(R.string.referral_load_failed))
                }
            }

            if (state.signedIn && (state.canRedeem || state.redeemResult != null)) {
                Spacer(Modifier.height(20.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))
                RedeemSection(
                    code = state.codeInput,
                    redeeming = state.redeeming,
                    result = state.redeemResult,
                    canRedeem = state.canRedeem,
                    onCodeChange = viewModel::onCodeChange,
                    onRedeem = { viewModel.redeem() }
                )
            }
        }
    }
}

@Composable
private fun RewardRow(emoji: String, text: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 20.sp)
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun CodeCard(code: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.referral_your_code),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
            )
            Text(
                code.toCharArray().joinToString(" "),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = 30.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun RedeemSection(
    code: String,
    redeeming: Boolean,
    result: RedeemResult?,
    canRedeem: Boolean,
    onCodeChange: (String) -> Unit,
    onRedeem: () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.referral_have_code),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        if (canRedeem) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = code,
                    onValueChange = onCodeChange,
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.referral_code_hint)) },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { onRedeem() }),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = onRedeem, enabled = code.length >= 4 && !redeeming) {
                    if (redeeming) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text(stringResource(R.string.referral_redeem))
                }
            }
        }
        result?.let {
            Spacer(Modifier.height(8.dp))
            val (msg, good) = when (it) {
                RedeemResult.Success -> stringResource(R.string.referral_redeem_success, XPValues.REFERRAL_WELCOME) to true
                RedeemResult.UnknownCode -> stringResource(R.string.referral_redeem_unknown) to false
                RedeemResult.OwnCode -> stringResource(R.string.referral_redeem_own) to false
                RedeemResult.AlreadyRedeemed -> stringResource(R.string.referral_redeem_already) to false
                RedeemResult.TooLate -> stringResource(R.string.referral_redeem_too_late) to false
                RedeemResult.NotSignedIn -> stringResource(R.string.referral_sign_in) to false
                RedeemResult.Failed -> stringResource(R.string.referral_redeem_failed) to false
            }
            Text(
                msg,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (good) FontWeight.Bold else FontWeight.Normal,
                color = if (good) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
    }
}

private fun copyCode(context: Context, code: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Dexverse invite code", code))
    Toast.makeText(context, context.getString(R.string.referral_copied), Toast.LENGTH_SHORT).show()
}

// ─── Home popups ──────────────────────────────────────────────────────────────

@Composable
fun InvitePromptDialog(onInvite: () -> Unit, onLater: () -> Unit) {
    AlertDialog(
        onDismissRequest = onLater,
        shape = RoundedCornerShape(24.dp),
        icon = { Text("🤝", fontSize = 36.sp) },
        title = { Text(stringResource(R.string.referral_popup_title), fontWeight = FontWeight.Black, textAlign = TextAlign.Center) },
        text = {
            Text(
                stringResource(R.string.referral_popup_body, XPValues.REFERRAL_PER_FRIEND, XPValues.REFERRAL_WELCOME),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(onClick = onInvite, shape = RoundedCornerShape(12.dp)) {
                Text(stringResource(R.string.referral_popup_cta), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onLater) { Text(stringResource(R.string.dex_popup_later)) } }
    )
}

@Composable
fun ReferralClaimDialog(code: String, onClaim: () -> Unit, onLater: () -> Unit) {
    AlertDialog(
        onDismissRequest = onLater,
        shape = RoundedCornerShape(24.dp),
        icon = { Text("🎁", fontSize = 36.sp) },
        title = { Text(stringResource(R.string.referral_claim_title), fontWeight = FontWeight.Black, textAlign = TextAlign.Center) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.referral_claim_body, XPValues.REFERRAL_WELCOME),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    code,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        },
        confirmButton = {
            Button(onClick = onClaim, shape = RoundedCornerShape(12.dp)) {
                Text(stringResource(R.string.referral_claim_cta), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onLater) { Text(stringResource(R.string.dex_popup_later)) } }
    )
}
