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
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.nexa.admin.data.Repository
import com.nexa.admin.data.UpdateVersionRequest
import com.nexa.admin.ui.components.*
import com.nexa.admin.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun VersionControlScreen(nav: NavController, repo: Repository) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedApp by remember { mutableStateOf("user") }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }

    var latestVersion by remember { mutableStateOf("1") }
    var latestVersionName by remember { mutableStateOf("1.0.0") }
    var minVersion by remember { mutableStateOf("1") }
    var forceUpdate by remember { mutableStateOf(false) }
    var updateUrl by remember { mutableStateOf("") }
    var releaseNotes by remember { mutableStateOf("") }

    fun loadConfig() {
        scope.launch {
            loading = true
            repo.getVersionConfig(selectedApp).onSuccess { res ->
                res.config?.let { c ->
                    latestVersion = c.latestVersion.toString()
                    latestVersionName = c.latestVersionName
                    minVersion = c.minVersion.toString()
                    forceUpdate = c.forceUpdate
                    updateUrl = c.updateUrl
                    releaseNotes = c.releaseNotes
                }
            }.onFailure {
                Toast.makeText(ctx, "Load failed: ${it.message}", Toast.LENGTH_LONG).show()
            }
            loading = false
        }
    }

    LaunchedEffect(selectedApp) { loadConfig() }

    Column(Modifier.fillMaxSize().background(NexaBg)) {
        Row(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BackButton { nav.popBackStack() }
            Text(
                "Version Control",
                color = NexaText, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                modifier = Modifier.weight(1f)
            )
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(bottom = 30.dp)
        ) {
            // ═══ App selector ═══
            Text(
                "SELECT APP",
                color = NexaMuted, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.7.sp, modifier = Modifier.padding(bottom = 10.dp)
            )
            Row(
                Modifier.fillMaxWidth().padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf("user" to "Nexa User App", "admin" to "Nexa Admin").forEach { (key, label) ->
                    val isSel = selectedApp == key
                    Box(
                        Modifier.weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSel) NexaGreen.copy(alpha = 0.15f) else NexaSurface)
                            .border(
                                1.5.dp,
                                if (isSel) NexaGreen.copy(alpha = 0.6f) else NexaBorder.copy(alpha = 0.09f),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { selectedApp = key }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            color = if (isSel) NexaGreen else NexaMuted,
                            fontWeight = FontWeight.Bold, fontSize = 12.5.sp
                        )
                    }
                }
            }

            if (loading) {
                Box(Modifier.fillMaxWidth().padding(60.dp), contentAlignment = Alignment.Center) {
                    Text("Loading…", color = NexaMuted, fontSize = 13.sp)
                }
                return@Column
            }

            // ═══ Latest Version ═══
            Text(
                "LATEST VERSION (versionCode)",
                color = NexaMuted, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.7.sp, modifier = Modifier.padding(bottom = 8.dp)
            )
            AdminInput(
                value = latestVersion,
                onChange = { if (it.all { c -> c.isDigit() }) latestVersion = it },
                placeholder = "e.g. 2",
                keyboardType = KeyboardType.Number
            )
            Text(
                "Integer only. build.gradle.kts এ versionCode এর সাথে match করতে হবে।",
                color = NexaDim, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            // ═══ Version Name ═══
            Text(
                "VERSION NAME (human readable)",
                color = NexaMuted, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.7.sp, modifier = Modifier.padding(bottom = 8.dp)
            )
            AdminInput(
                value = latestVersionName,
                onChange = { latestVersionName = it },
                placeholder = "e.g. 1.0.1"
            )
            Text(
                "User এইটা দেখবে (e.g. v1.0.1)",
                color = NexaDim, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            // ═══ Min Version ═══
            Text(
                "MINIMUM VERSION (force update threshold)",
                color = NexaMuted, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.7.sp, modifier = Modifier.padding(bottom = 8.dp)
            )
            AdminInput(
                value = minVersion,
                onChange = { if (it.all { c -> c.isDigit() }) minVersion = it },
                placeholder = "e.g. 2",
                keyboardType = KeyboardType.Number
            )
            Text(
                "এই versionCode এর নিচের সবাই force update পাবে। Soft update এর জন্য latestVersion এর সমান বা ছোট রাখো।",
                color = NexaDim, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            // ═══ Force Update toggle ═══
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Force Update All",
                        color = NexaText, fontWeight = FontWeight.Bold, fontSize = 14.sp
                    )
                    Text(
                        "সব version এর user কে block করবে",
                        color = NexaDim, fontSize = 11.sp
                    )
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (forceUpdate) NexaRed.copy(alpha = 0.15f) else NexaSurface2)
                        .border(
                            1.dp,
                            if (forceUpdate) NexaRed.copy(alpha = 0.6f) else NexaBorder.copy(alpha = 0.15f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { forceUpdate = !forceUpdate }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        if (forceUpdate) "ON" else "OFF",
                        color = if (forceUpdate) NexaRed else NexaMuted,
                        fontWeight = FontWeight.ExtraBold, fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ═══ Update URL ═══
            Text(
                "UPDATE URL (APK download link)",
                color = NexaMuted, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.7.sp, modifier = Modifier.padding(bottom = 8.dp)
            )
            AdminInput(
                value = updateUrl,
                onChange = { updateUrl = it },
                placeholder = "https://www.mediafire.com/file/..."
            )
            Text(
                "User 'Update Now' tap করলে এই URL এ যাবে",
                color = NexaDim, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            // ═══ Release notes ═══
            Text(
                "RELEASE NOTES",
                color = NexaMuted, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.7.sp, modifier = Modifier.padding(bottom = 8.dp)
            )
            AdminInput(
                value = releaseNotes,
                onChange = { releaseNotes = it },
                placeholder = "Bug fixes, new features...",
                singleLine = false,
                maxLines = 4
            )

            Spacer(Modifier.height(24.dp))

            // ═══ Preview ═══
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(NexaSurface)
                    .border(1.dp, NexaBorder.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        "PREVIEW",
                        color = NexaGreen, fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Latest: v$latestVersionName (code $latestVersion)",
                        color = NexaText, fontSize = 12.5.sp
                    )
                    Text(
                        "Force threshold: code $minVersion",
                        color = NexaMuted, fontSize = 12.sp
                    )
                    Text(
                        if (forceUpdate) "⚠️ Force update ALL users"
                        else "Soft update — user চাইলে skip করতে পারবে",
                        color = if (forceUpdate) NexaRed else NexaTeal,
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            GradientButton(
                text = if (saving) "Saving…" else "Save Changes",
                loading = saving,
                onClick = {
                    scope.launch {
                        saving = true
                        val req = UpdateVersionRequest(
                            app = selectedApp,
                            latestVersion = latestVersion.toIntOrNull() ?: 1,
                            latestVersionName = latestVersionName.trim().ifBlank { "1.0.0" },
                            minVersion = minVersion.toIntOrNull() ?: 1,
                            forceUpdate = forceUpdate,
                            updateUrl = updateUrl.trim(),
                            releaseNotes = releaseNotes.trim()
                        )
                        repo.updateVersionConfig(req).onSuccess {
                            Toast.makeText(ctx, "Saved ✓", Toast.LENGTH_SHORT).show()
                        }.onFailure {
                            Toast.makeText(ctx, "Failed: ${it.message}", Toast.LENGTH_LONG).show()
                        }
                        saving = false
                    }
                }
            )

            Spacer(Modifier.height(30.dp))
        }
    }
}
