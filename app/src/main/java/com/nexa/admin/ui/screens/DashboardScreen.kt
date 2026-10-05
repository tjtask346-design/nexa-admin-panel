package com.nexa.admin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.nexa.admin.data.AdminStats
import com.nexa.admin.data.Prefs
import com.nexa.admin.data.Repository
import com.nexa.admin.nav.AdminRoutes
import com.nexa.admin.ui.components.StatCard
import com.nexa.admin.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun DashboardScreen(nav: NavController, prefs: Prefs, repo: Repository) {
    var stats by remember { mutableStateOf(AdminStats()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (true) {
            repo.stats().onSuccess { stats = it; loading = false }
                .onFailure { if (loading) loading = false }
            delay(8000)
        }
    }

    Column(Modifier.fillMaxSize().background(NexaBg)) {
        Row(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Admin Dashboard",
                    color = NexaText,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    letterSpacing = (-0.5).sp
                )
                Text(prefs.email ?: "", color = NexaMuted, fontSize = 12.sp)
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(NexaRed.copy(alpha = 0.15f))
                    .border(1.dp, NexaRed.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        repo.clearSession()
                        nav.navigate(AdminRoutes.LOGIN) { popUpTo(0) { inclusive = true } }
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("Logout", color = NexaRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    label = "Total Users",
                    value = stats.totalUsers.toString(),
                    accent = NexaGreen,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Banned",
                    value = stats.bannedUsers.toString(),
                    accent = NexaRed,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    label = "Pending KYC",
                    value = stats.pendingKyc.toString(),
                    accent = NexaTeal,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Pending Tx",
                    value = stats.pendingTx.toString(),
                    accent = NexaAmber,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(10.dp))
            StatCard(
                label = "Total Balance Held",
                value = "$" + String.format("%,.2f", stats.totalBalance),
                accent = NexaGreen,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            Text(
                "QUICK ACTIONS",
                color = NexaMuted, fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold, letterSpacing = 0.7.sp
            )
            Spacer(Modifier.height(12.dp))

            QuickTile(
                title = "Users",
                subtitle = "Search, ban, delete, adjust balance",
                accent = NexaGreen
            ) { nav.navigate(AdminRoutes.USERS) }

            Spacer(Modifier.height(10.dp))

            QuickTile(
                title = "KYC Reviews",
                subtitle = "Approve or reject pending submissions",
                accent = NexaTeal,
                badge = stats.pendingKyc.takeIf { it > 0 }?.toString()
            ) { nav.navigate(AdminRoutes.PENDING_KYC) }

            Spacer(Modifier.height(10.dp))

            QuickTile(
                title = "Transactions",
                subtitle = "Pending deposits & withdrawals",
                accent = NexaAmber,
                badge = stats.pendingTx.takeIf { it > 0 }?.toString()
            ) { nav.navigate(AdminRoutes.PENDING_TX) }

            Spacer(Modifier.height(10.dp))

            QuickTile(
                title = "My Notifications",
                subtitle = "See what happened on your platform",
                accent = NexaGreen
            ) { nav.navigate(AdminRoutes.NOTIFICATIONS) }

            Spacer(Modifier.height(10.dp))

            QuickTile(
                title = "Version Control",
                subtitle = "App update + force update manage",
                accent = NexaAmber
            ) { nav.navigate(AdminRoutes.VERSION_CONTROL) }

            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun QuickTile(
    title: String,
    subtitle: String,
    accent: Color,
    badge: String? = null,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(NexaSurface)
            .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text("›", color = accent, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = NexaText, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
            Text(subtitle, color = NexaDim, fontSize = 11.5.sp)
        }
        if (badge != null) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.2f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(badge, color = accent, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
            }
        }
    }
}
