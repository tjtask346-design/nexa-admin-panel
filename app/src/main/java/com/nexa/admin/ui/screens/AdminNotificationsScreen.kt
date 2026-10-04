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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.nexa.admin.data.NotificationItem
import com.nexa.admin.data.Repository
import com.nexa.admin.ui.components.BackButton
import com.nexa.admin.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AdminNotificationsScreen(nav: NavController, repo: Repository) {
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    suspend fun load() {
        repo.getNotifications().onSuccess { list = it.notifications }
        loading = false
    }

    LaunchedEffect(Unit) {
        load()
        while (true) { delay(10_000); load() }
    }

    Column(Modifier.fillMaxSize().background(NexaBg)) {
        Row(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BackButton { nav.popBackStack() }
            Text(
                "Notifications",
                color = NexaText, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                modifier = Modifier.weight(1f)
            )
            if (list.any { !it.read }) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(NexaGreen.copy(alpha = 0.15f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            scope.launch { repo.markAllRead(); load() }
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("Read all", color = NexaGreen, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (loading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("Loading…", color = NexaMuted, fontSize = 13.sp)
            }
        } else if (list.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("No notifications yet", color = NexaDim, fontSize = 13.sp)
            }
        } else {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp).padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                list.forEach { n ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (n.read) NexaSurface.copy(alpha = 0.6f) else NexaSurface)
                            .border(
                                1.dp,
                                if (n.read) NexaBorder.copy(alpha = 0.06f) else NexaBorder.copy(alpha = 0.2f),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    n.title, color = NexaText,
                                    fontWeight = if (n.read) FontWeight.SemiBold else FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                if (!n.read) {
                                    Box(
                                        Modifier.size(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(NexaGreen)
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(n.body, color = NexaMuted, fontSize = 12.5.sp, lineHeight = 18.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                (n.createdAt ?: "").replace("T", " ").take(16),
                                color = NexaDim, fontSize = 10.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
