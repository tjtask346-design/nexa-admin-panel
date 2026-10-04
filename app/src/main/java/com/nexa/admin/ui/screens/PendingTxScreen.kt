package com.nexa.admin.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.nexa.admin.data.Repository
import com.nexa.admin.data.TxItem
import com.nexa.admin.ui.components.*
import com.nexa.admin.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun PendingTxScreen(nav: NavController, repo: Repository) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var filter by remember { mutableStateOf("all") }
    var list by remember { mutableStateOf<List<TxItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var actionTarget by remember { mutableStateOf<Pair<TxItem, String>?>(null) }
    var adminNote by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    fun reload() {
        scope.launch {
            loading = true
            val type = when (filter) {
                "deposit" -> "deposit"
                "cashout" -> "cashout"
                else -> null
            }
            repo.pendingTx(type).onSuccess { list = it.transactions }
            loading = false
        }
    }

    LaunchedEffect(filter) { reload() }

    Column(Modifier.fillMaxSize().background(NexaBg)) {
        Row(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BackButton { nav.popBackStack() }
            Text(
                "Pending Transactions",
                color = NexaText, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("all", "deposit", "cashout").forEach { f ->
                val selected = filter == f
                Box(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) NexaGreen.copy(alpha = 0.15f) else NexaSurface)
                        .border(
                            1.dp,
                            if (selected) NexaGreen.copy(alpha = 0.5f) else NexaBorder.copy(alpha = 0.1f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { filter = f }
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Text(
                        f.replaceFirstChar { it.uppercase() },
                        color = if (selected) NexaGreen else NexaMuted,
                        fontWeight = FontWeight.Bold, fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (loading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("Loading…", color = NexaMuted, fontSize = 13.sp)
            }
        } else if (list.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("No pending transactions", color = NexaDim, fontSize = 13.sp)
            }
        } else {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp).padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                list.forEach { tx ->
                    TxCard(tx) { action ->
                        actionTarget = tx to action
                        adminNote = ""
                    }
                }
            }
        }
    }

    if (actionTarget != null) {
        val (tx, action) = actionTarget!!
        val isApprove = action == "approved"
        AlertDialog(
            onDismissRequest = { actionTarget = null },
            containerColor = NexaSurface,
            titleContentColor = NexaText,
            textContentColor = NexaMuted,
            title = {
                Text(
                    "${if (isApprove) "Approve" else "Reject"} ${tx.type}",
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Column {
                    Text("User: ${tx.user?.email ?: "—"}", fontSize = 12.5.sp)
                    Text("Amount: $${String.format("%,.2f", tx.amount)}", fontSize = 12.5.sp)
                    if (tx.trxId != null) Text("TrxID: ${tx.trxId}", fontSize = 12.sp)
                    Spacer(Modifier.height(10.dp))
                    AdminInput(
                        value = adminNote,
                        onChange = { adminNote = it },
                        placeholder = "Admin note (optional)",
                        singleLine = false, maxLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        scope.launch {
                            busy = true
                            val call = if (tx.type == "deposit")
                                repo.approveDeposit(tx.id ?: "", action, adminNote.trim().ifBlank { null })
                            else
                                repo.approveCashout(tx.id ?: "", action, adminNote.trim().ifBlank { null })
                            call.onSuccess {
                                Toast.makeText(ctx, "${tx.type} $action", Toast.LENGTH_SHORT).show()
                                actionTarget = null
                                reload()
                            }.onFailure {
                                Toast.makeText(ctx, it.message ?: "Failed", Toast.LENGTH_LONG).show()
                            }
                            busy = false
                        }
                    }
                ) {
                    Text(
                        if (isApprove) "Approve" else "Reject",
                        color = if (isApprove) NexaGreen else NexaRed,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { actionTarget = null }) {
                    Text("Cancel", color = NexaMuted)
                }
            }
        )
    }
}

@Composable
private fun TxCard(tx: TxItem, onAction: (String) -> Unit) {
    NexaCard(borderColor = if (tx.type == "deposit") NexaGreen.copy(alpha = 0.25f) else NexaTeal.copy(alpha = 0.25f)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Badge(tx.type.uppercase(), if (tx.type == "deposit") NexaGreen else NexaTeal)
                Text(
                    "$" + String.format("%,.2f", tx.amount),
                    color = NexaText, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp
                )
                Spacer(Modifier.weight(1f))
                Text(
                    (tx.createdAt ?: "").take(10),
                    color = NexaDim, fontSize = 11.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(tx.user?.fullName ?: "—", color = NexaText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(tx.user?.email ?: "", color = NexaMuted, fontSize = 11.5.sp)
            if (tx.trxId != null) {
                Spacer(Modifier.height(4.dp))
                Text("TrxID: ${tx.trxId}", color = NexaDim, fontSize = 11.sp)
            }
            if (tx.paymentMethodNumber != null) {
                Text("Method: ${tx.paymentMethodNumber}", color = NexaDim, fontSize = 11.sp)
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) {
                    DangerButton("Reject") { onAction("rejected") }
                }
                Box(Modifier.weight(1f)) {
                    GradientButton("Approve", onClick = { onAction("approved") })
                }
            }
        }
    }
}
