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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.nexa.admin.data.AdminUser
import com.nexa.admin.data.Repository
import com.nexa.admin.nav.AdminRoutes
import com.nexa.admin.ui.components.AdminInput
import com.nexa.admin.ui.components.BackButton
import com.nexa.admin.ui.components.Badge
import com.nexa.admin.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun UsersScreen(nav: NavController, repo: Repository) {
    var query by remember { mutableStateOf("") }
    var users by remember { mutableStateOf<List<AdminUser>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(query) {
        loading = true
        delay(300)
        repo.listUsers(q = query).onSuccess { users = it.users }
        loading = false
    }

    Column(Modifier.fillMaxSize().background(NexaBg)) {
        Row(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BackButton { nav.popBackStack() }
            Text(
                "Users",
                color = NexaText, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                modifier = Modifier.weight(1f)
            )
        }

        Column(Modifier.padding(horizontal = 20.dp)) {
            AdminInput(
                value = query,
                onChange = { query = it },
                placeholder = "Search by email, name, account #",
                keyboardType = KeyboardType.Text
            )
        }

        Spacer(Modifier.height(12.dp))

        if (loading && users.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("Loading…", color = NexaMuted, fontSize = 13.sp)
            }
        } else if (users.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("No users found", color = NexaDim, fontSize = 13.sp)
            }
        } else {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                users.forEach { u ->
                    UserRow(u) {
                        nav.navigate(AdminRoutes.userDetail(u.id ?: ""))
                    }
                }
            }
        }
    }
}

@Composable
private fun UserRow(u: AdminUser, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(NexaSurface)
            .border(
                1.dp,
                if (u.isBanned) NexaRed.copy(alpha = 0.3f) else NexaBorder.copy(alpha = 0.09f),
                RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (u.isBanned) NexaRed.copy(alpha = 0.15f)
                    else NexaGreen.copy(alpha = 0.15f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                (u.fullName.firstOrNull() ?: u.email.firstOrNull() ?: '?').uppercase(),
                color = if (u.isBanned) NexaRed else NexaGreen,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    u.fullName.ifBlank { u.email.substringBefore("@") },
                    color = NexaText, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    maxLines = 1
                )
                if (u.isBanned) Badge("BANNED", NexaRed)
                if (u.role == "admin") Badge("ADMIN", NexaTeal)
            }
            Text(u.email, color = NexaMuted, fontSize = 11.5.sp, maxLines = 1)
            Text(
                "Acct: ${u.accountNumber}",
                color = NexaDim, fontSize = 10.5.sp
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "$" + String.format("%,.2f", u.balance),
                color = NexaGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp
            )
            if (u.ltcBalance > 0) {
                Text(
                    "${String.format("%.4f", u.ltcBalance)} LTC",
                    color = NexaTeal, fontSize = 10.5.sp
                )
            }
        }
    }
}
