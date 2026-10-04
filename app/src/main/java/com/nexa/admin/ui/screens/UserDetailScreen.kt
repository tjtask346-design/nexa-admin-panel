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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.nexa.admin.data.AdminUser
import com.nexa.admin.data.Repository
import com.nexa.admin.ui.components.*
import com.nexa.admin.ui.theme.*
import kotlinx.coroutines.launch

private val CURRENCIES = listOf("usdt", "ltc", "nexa")

@Composable
fun UserDetailScreen(nav: NavController, repo: Repository, userId: String) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var user by remember { mutableStateOf<AdminUser?>(null) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var currency by remember { mutableStateOf("usdt") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isCredit by remember { mutableStateOf(true) }

    var showBanDialog by remember { mutableStateOf(false) }
    var banReason by remember { mutableStateOf("") }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteConfirm by remember { mutableStateOf("") }

    var showNotifyDialog by remember { mutableStateOf(false) }
    var notifyTitle by remember { mutableStateOf("") }
    var notifyBody by remember { mutableStateOf("") }

    fun reload() {
        scope.launch {
            loading = true
            error = null
            val res = repo.getUser(userId)
            if (res.isSuccess) {
                user = res.getOrNull()?.user
                if (user == null) error = "User not found: " + userId
            } else {
                error = res.exceptionOrNull()?.message ?: "Failed"
            }
            loading = false
        }
    }

    LaunchedEffect(userId) { reload() }

    Column(Modifier.fillMaxSize().background(NexaBg)) {
        Row(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BackButton { nav.popBackStack() }
            Text(
                "User Details",
                color = NexaText, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                modifier = Modifier.weight(1f)
            )
        }

        if (loading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("Loading…", color = NexaMuted, fontSize = 13.sp)
            }
        } else if (user == null) {
            Box(Modifier.fillMaxSize().padding(40.dp), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Failed to load user", color = NexaRed, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(10.dp))
                    Text(error ?: "Check connection", color = NexaMuted, fontSize = 12.5.sp)
                    Spacer(Modifier.height(18.dp))
                    Box(Modifier.clip(RoundedCornerShape(12.dp)).background(NexaGreen.copy(alpha = 0.15f)).clickable { reload() }.padding(horizontal = 20.dp, vertical = 10.dp)) {
                        Text("Retry", color = NexaGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        } else {
            val u = user!!
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 30.dp)
            ) {
                NexaCard(borderColor = if (u.isBanned) NexaRed.copy(alpha = 0.4f) else NexaBorder.copy(alpha = 0.15f)) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                Modifier.size(52.dp).clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (u.isBanned) NexaRed.copy(alpha = 0.15f)
                                        else NexaGreen.copy(alpha = 0.15f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    (u.fullName.firstOrNull() ?: '?').uppercase(),
                                    color = if (u.isBanned) NexaRed else NexaGreen,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 22.sp
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(u.fullName.ifBlank { "—" }, color = NexaText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(u.email, color = NexaMuted, fontSize = 12.sp)
                            }
                            if (u.isBanned) Badge("BANNED", NexaRed)
                            else if (u.role == "admin") Badge("ADMIN", NexaTeal)
                        }
                        Spacer(Modifier.height(12.dp))
                        InfoRow("Account #", u.accountNumber)
                        InfoRow("UID", u.uid ?: "—")
                        InfoRow("Role", u.role)
                        InfoRow("KYC", u.kycStatus.uppercase())
                        if (u.isBanned && !u.banReason.isNullOrBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Box(
                                Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(NexaRed.copy(alpha = 0.1f))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    "BAN REASON: ${u.banReason}",
                                    color = NexaRed, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                NexaCard {
                    Column {
                        Text("BALANCES", color = NexaMuted, fontSize = 11.sp,
                             fontWeight = FontWeight.ExtraBold, letterSpacing = 0.7.sp)
                        Spacer(Modifier.height(10.dp))
                        BalRow("USDT (BEP20)", "$" + String.format("%,.2f", u.balance), NexaGreen)
                        BalRow("Nexa USD (internal)", "$" + String.format("%,.2f", u.balance), NexaTeal)
                        BalRow("Litecoin", String.format("%.6f", u.ltcBalance) + " LTC", Color(0xFF345D9D))
                    }
                }

                Spacer(Modifier.height(14.dp))

                NexaCard {
                    Column {
                        Text("ADJUST BALANCE", color = NexaMuted, fontSize = 11.sp,
                             fontWeight = FontWeight.ExtraBold, letterSpacing = 0.7.sp)
                        Spacer(Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CURRENCIES.forEach { c ->
                                val selected = currency == c
                                Box(
                                    Modifier.weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selected) NexaGreen.copy(alpha = 0.16f) else NexaSurface2)
                                        .border(
                                            1.dp,
                                            if (selected) NexaGreen.copy(alpha = 0.5f) else NexaBorder.copy(alpha = 0.1f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { currency = c }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        c.uppercase(),
                                        color = if (selected) NexaGreen else NexaMuted,
                                        fontWeight = FontWeight.Bold, fontSize = 11.5.sp
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(true to "Credit +", false to "Debit −").forEach { (isC, label) ->
                                val selected = isCredit == isC
                                Box(
                                    Modifier.weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selected) {
                                            (if (isC) NexaGreen else NexaRed).copy(alpha = 0.15f)
                                        } else NexaSurface2)
                                        .border(
                                            1.dp,
                                            if (selected) (if (isC) NexaGreen else NexaRed).copy(alpha = 0.5f)
                                            else NexaBorder.copy(alpha = 0.1f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { isCredit = isC }
                                        .padding(vertical = 11.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        label,
                                        color = if (selected) (if (isC) NexaGreen else NexaRed) else NexaMuted,
                                        fontWeight = FontWeight.Bold, fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        AdminInput(
                            value = amount,
                            onChange = { amount = it },
                            placeholder = "Amount",
                            keyboardType = KeyboardType.Decimal
                        )
                        Spacer(Modifier.height(10.dp))
                        AdminInput(
                            value = note,
                            onChange = { note = it },
                            placeholder = "Note (optional)"
                        )

                        Spacer(Modifier.height(12.dp))

                        GradientButton(
                            text = if (isCredit) "Credit Balance" else "Debit Balance",
                            enabled = amount.toDoubleOrNull()?.let { it > 0 } == true && !busy,
                            loading = busy,
                            onClick = {
                                val amt = amount.toDoubleOrNull() ?: return@GradientButton
                                val signedAmt = if (isCredit) amt else -amt
                                scope.launch {
                                    busy = true
                                    repo.adjustBalance(u.id ?: "", currency, signedAmt, note)
                                        .onSuccess {
                                            Toast.makeText(ctx, "Balance updated ✓", Toast.LENGTH_SHORT).show()
                                            amount = ""; note = ""
                                            reload()
                                        }
                                        .onFailure {
                                            Toast.makeText(ctx, it.message ?: "Failed", Toast.LENGTH_LONG).show()
                                        }
                                    busy = false
                                }
                            }
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                NexaCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("ACTIONS", color = NexaMuted, fontSize = 11.sp,
                             fontWeight = FontWeight.ExtraBold, letterSpacing = 0.7.sp)

                        if (u.isBanned) {
                            GradientButton(
                                text = "Unban User",
                                onClick = {
                                    scope.launch {
                                        repo.setBan(u.id ?: "", false, null).onSuccess {
                                            Toast.makeText(ctx, "User unbanned", Toast.LENGTH_SHORT).show()
                                            reload()
                                        }
                                    }
                                }
                            )
                        } else {
                            DangerButton(
                                text = "Ban User",
                                onClick = { showBanDialog = true }
                            )
                        }

                        GradientButton(
                            text = "Send Notification",
                            onClick = { showNotifyDialog = true }
                        )

                        DangerButton(
                            text = "Delete Account (Firebase + DB)",
                            onClick = { deleteConfirm = ""; showDeleteDialog = true }
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
            }
        }
    }

    if (showBanDialog) {
        AlertDialog(
            onDismissRequest = { showBanDialog = false },
            containerColor = NexaSurface,
            titleContentColor = NexaText,
            textContentColor = NexaMuted,
            title = { Text("Ban this user?", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column {
                    Text("User will be force-logged out and see a suspension screen.", fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    AdminInput(
                        value = banReason,
                        onChange = { banReason = it },
                        placeholder = "Reason (e.g. Suspicious activity)"
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val reason = banReason.trim().ifBlank { "Suspicious activity detected." }
                    scope.launch {
                        repo.setBan(user?.id ?: "", true, reason).onSuccess {
                            Toast.makeText(ctx, "User banned", Toast.LENGTH_SHORT).show()
                            showBanDialog = false; banReason = ""
                            reload()
                        }.onFailure {
                            Toast.makeText(ctx, it.message ?: "Failed", Toast.LENGTH_LONG).show()
                        }
                    }
                }) { Text("Ban", color = NexaRed, fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = {
                TextButton(onClick = { showBanDialog = false }) {
                    Text("Cancel", color = NexaMuted)
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = NexaSurface,
            titleContentColor = NexaText,
            textContentColor = NexaMuted,
            title = { Text("Delete account permanently?", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column {
                    Text(
                        "This will delete the user from Firebase Auth AND MongoDB (KYC, transactions, notifications). This cannot be undone.",
                        fontSize = 13.sp, lineHeight = 19.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Type the account number to confirm:",
                        fontSize = 12.sp, color = NexaMuted
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            user?.accountNumber ?: "",
                            color = NexaRed, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold
                        )
                        Box(
                            Modifier.clip(RoundedCornerShape(8.dp))
                                .background(NexaGreen.copy(alpha = 0.15f))
                                .clickable {
                                    clipboard.setText(AnnotatedString(user?.accountNumber ?: ""))
                                    Toast.makeText(ctx, "Copied", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Copy", color = NexaGreen, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    AdminInput(
                        value = deleteConfirm,
                        onChange = { deleteConfirm = it },
                        placeholder = "Account number",
                        keyboardType = KeyboardType.Number
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = deleteConfirm == user?.accountNumber,
                    onClick = {
                        scope.launch {
                            android.util.Log.d("NEXA_DELETE", "Deleting id=${user?.id} acc=${user?.accountNumber}")
                            repo.deleteUser(user?.id ?: "").onSuccess {
                                Toast.makeText(ctx, "User deleted permanently", Toast.LENGTH_LONG).show()
                                showDeleteDialog = false
                                nav.popBackStack()
                            }.onFailure {
                                Toast.makeText(ctx, it.message ?: "Failed", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) { Text("DELETE", color = NexaRed, fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = NexaMuted)
                }
            }
        )
    }

    if (showNotifyDialog) {
        AlertDialog(
            onDismissRequest = { showNotifyDialog = false },
            containerColor = NexaSurface,
            titleContentColor = NexaText,
            textContentColor = NexaMuted,
            title = { Text("Send Notification", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column {
                    Text("Will appear in-app + FCM push.", fontSize = 12.sp)
                    Spacer(Modifier.height(10.dp))
                    AdminInput(
                        value = notifyTitle,
                        onChange = { notifyTitle = it },
                        placeholder = "Title"
                    )
                    Spacer(Modifier.height(8.dp))
                    AdminInput(
                        value = notifyBody,
                        onChange = { notifyBody = it },
                        placeholder = "Message body",
                        singleLine = false, maxLines = 4
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = notifyTitle.isNotBlank() && notifyBody.isNotBlank(),
                    onClick = {
                        scope.launch {
                            repo.sendNotify(user?.id ?: "", notifyTitle.trim(), notifyBody.trim())
                                .onSuccess {
                                    Toast.makeText(ctx, "Notification sent ✓", Toast.LENGTH_SHORT).show()
                                    showNotifyDialog = false
                                    notifyTitle = ""; notifyBody = ""
                                }
                                .onFailure {
                                    Toast.makeText(ctx, it.message ?: "Failed", Toast.LENGTH_LONG).show()
                                }
                        }
                    }
                ) { Text("Send", color = NexaGreen, fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = {
                TextButton(onClick = { showNotifyDialog = false }) {
                    Text("Cancel", color = NexaMuted)
                }
            }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = NexaMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Text(value, color = NexaText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BalRow(label: String, value: String, color: Color) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = NexaMuted, fontSize = 12.5.sp)
        Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
