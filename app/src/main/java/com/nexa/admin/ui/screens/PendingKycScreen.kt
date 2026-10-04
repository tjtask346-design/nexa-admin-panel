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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.nexa.admin.data.KycItem
import com.nexa.admin.data.Repository
import com.nexa.admin.ui.components.*
import com.nexa.admin.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun PendingKycScreen(nav: NavController, repo: Repository) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var list by remember { mutableStateOf<List<KycItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var rejectTarget by remember { mutableStateOf<KycItem?>(null) }
    var rejectNote by remember { mutableStateOf("") }

    fun reload() {
        scope.launch {
            loading = true
            repo.pendingKyc().onSuccess { list = it.kycs }
            loading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    Column(Modifier.fillMaxSize().background(NexaBg)) {
        Row(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BackButton { nav.popBackStack() }
            Text(
                "Pending KYC",
                color = NexaText, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                modifier = Modifier.weight(1f)
            )
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(NexaTeal.copy(alpha = 0.15f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { reload() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("Refresh", color = NexaTeal, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (loading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("Loading…", color = NexaMuted, fontSize = 13.sp)
            }
        } else if (list.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("No pending KYC", color = NexaDim, fontSize = 13.sp)
            }
        } else {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                list.forEach { k ->
                    NexaCard {
                        Column {
                            Text(
                                k.user?.fullName ?: "—",
                                color = NexaText, fontWeight = FontWeight.Bold, fontSize = 14.5.sp
                            )
                            Text(k.user?.email ?: "", color = NexaMuted, fontSize = 12.sp)
                            Spacer(Modifier.height(6.dp))
                            Text("NID: ${k.nidNumber}", color = NexaDim, fontSize = 11.5.sp)

                            Spacer(Modifier.height(12.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Thumb("Front", k.frontUrl)
                                Thumb("Back", k.backUrl)
                                Thumb("Selfie", k.selfieUrl)
                            }

                            Spacer(Modifier.height(14.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.weight(1f)) {
                                    DangerButton(
                                        text = "Reject",
                                        loading = busy,
                                        onClick = {
                                            rejectTarget = k
                                            rejectNote = ""
                                        }
                                    )
                                }
                                Box(Modifier.weight(1f)) {
                                    GradientButton(
                                        text = "Approve",
                                        loading = busy,
                                        onClick = {
                                            scope.launch {
                                                busy = true
                                                repo.approveKyc(k.id ?: "").onSuccess {
                                                    Toast.makeText(ctx, "KYC approved", Toast.LENGTH_SHORT).show()
                                                    reload()
                                                }.onFailure {
                                                    Toast.makeText(ctx, it.message ?: "Failed", Toast.LENGTH_LONG).show()
                                                }
                                                busy = false
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (rejectTarget != null) {
        AlertDialog(
            onDismissRequest = { rejectTarget = null },
            containerColor = NexaSurface,
            titleContentColor = NexaText,
            textContentColor = NexaMuted,
            title = { Text("Reject KYC", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column {
                    Text("Reason is required and will be shown to the user.", fontSize = 12.5.sp)
                    Spacer(Modifier.height(10.dp))
                    AdminInput(
                        value = rejectNote,
                        onChange = { rejectNote = it },
                        placeholder = "e.g. NID photo unclear",
                        singleLine = false, maxLines = 4
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = rejectNote.trim().isNotEmpty(),
                    onClick = {
                        val target = rejectTarget ?: return@TextButton
                        scope.launch {
                            busy = true
                            repo.rejectKyc(target.id ?: "", rejectNote.trim()).onSuccess {
                                Toast.makeText(ctx, "KYC rejected", Toast.LENGTH_SHORT).show()
                                rejectTarget = null; rejectNote = ""
                                reload()
                            }.onFailure {
                                Toast.makeText(ctx, it.message ?: "Failed", Toast.LENGTH_LONG).show()
                            }
                            busy = false
                        }
                    }
                ) { Text("Reject", color = NexaRed, fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = {
                TextButton(onClick = { rejectTarget = null }) {
                    Text("Cancel", color = NexaMuted)
                }
            }
        )
    }
}

@Composable
private fun Thumb(label: String, url: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(NexaSurface2)
                .border(1.dp, NexaBorder.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = url,
                contentDescription = label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = NexaDim, fontSize = 10.sp)
    }
}
