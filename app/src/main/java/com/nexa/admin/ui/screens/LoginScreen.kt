package com.nexa.admin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.nexa.admin.data.Prefs
import com.nexa.admin.data.Repository
import com.nexa.admin.nav.AdminRoutes
import com.nexa.admin.ui.components.AdminInput
import com.nexa.admin.ui.components.GradientButton
import com.nexa.admin.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(nav: NavController, prefs: Prefs, repo: Repository) {
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf(prefs.email ?: "") }
    var pin   by remember { mutableStateOf("") }
    var code  by remember { mutableStateOf("") }
    var needsTotp by remember { mutableStateOf(false) }
    var busy  by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun doLogin() {
        if (busy) return
        scope.launch {
            busy = true; error = null
            val res = repo.loginPin(email.trim(), pin, code.takeIf { it.length == 6 })
            busy = false

            res.onSuccess { r ->
                when {
                    r.banned == true -> error = r.reason ?: "Account suspended"
                    r.requiresTotp == true && code.length != 6 -> {
                        needsTotp = true
                        error = "Enter the 6-digit code from your authenticator app"
                    }
                    r.success && r.token != null -> {
                        if (r.user?.role != "admin") {
                            error = "This account is not an admin. Access denied."
                            repo.clearSession()
                        } else {
                            repo.saveSession(r.token, r.user)
                            nav.navigate(AdminRoutes.DASHBOARD) {
                                popUpTo(AdminRoutes.LOGIN) { inclusive = true }
                            }
                        }
                    }
                    else -> error = r.message ?: "Login failed"
                }
            }.onFailure {
                error = it.message ?: "Network error"
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(NexaBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp)
    ) {
        Spacer(Modifier.height(80.dp))

        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "NEXA ADMIN",
                color = NexaGreen,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 5.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Sign in to manage your platform",
                color = NexaMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(44.dp))

        Text(
            "EMAIL",
            color = NexaMuted, fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold, letterSpacing = 0.7.sp
        )
        Spacer(Modifier.height(8.dp))
        AdminInput(
            value = email,
            onChange = { email = it },
            placeholder = "admin@nexa.com",
            keyboardType = KeyboardType.Email
        )

        Spacer(Modifier.height(18.dp))

        Text(
            "PIN",
            color = NexaMuted, fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold, letterSpacing = 0.7.sp
        )
        Spacer(Modifier.height(8.dp))
        AdminInput(
            value = pin,
            onChange = { if (it.length <= 5) pin = it.filter { c -> c.isDigit() } },
            placeholder = "5-digit PIN",
            keyboardType = KeyboardType.Number
        )

        Spacer(Modifier.height(18.dp))
        Text(
            if (needsTotp) "2FA CODE (required)" else "2FA CODE (optional)",
            color = NexaMuted, fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold, letterSpacing = 0.7.sp
        )
        Spacer(Modifier.height(8.dp))
        AdminInput(
            value = code,
            onChange = { if (it.length <= 6) code = it.filter { c -> c.isDigit() } },
            placeholder = "6-digit code (leave blank if 2FA off)",
            keyboardType = KeyboardType.Number
        )

        if (error != null) {
            Spacer(Modifier.height(16.dp))
            Text(
                error!!,
                color = NexaRed, fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(26.dp))

        GradientButton(
            text = "Sign In",
            enabled = email.isNotBlank() && pin.length == 5 && !busy,
            loading = busy,
            onClick = { doLogin() }
        )

        Spacer(Modifier.height(28.dp))
        Text(
            "Only accounts with role = admin can sign in.\nContact the platform owner if you need access.",
            color = NexaDim, fontSize = 11.5.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(40.dp))
    }
}
