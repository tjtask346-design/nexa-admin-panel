package com.nexa.admin.nav

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nexa.admin.data.Prefs
import com.nexa.admin.data.Repository
import com.nexa.admin.ui.screens.*
import com.nexa.admin.ui.theme.*

object AdminRoutes {
    const val SPLASH        = "splash"
    const val LOGIN         = "login"
    const val DASHBOARD     = "dashboard"
    const val USERS         = "users"
    const val USER_DETAIL   = "user_detail"
    const val PENDING_KYC   = "pending_kyc"
    const val PENDING_TX    = "pending_tx"
    const val NOTIFICATIONS = "notifications"

    fun userDetail(id: String) = "$USER_DETAIL/$id"
}

@Composable
fun AdminNav(prefs: Prefs, repo: Repository) {
    val nav = rememberNavController()

    Box(Modifier.fillMaxSize().background(NexaBg)) {
        Canvas(Modifier.fillMaxSize()) {
            val gC = Offset(0f, 0f)
            val gR = size.width * 0.85f
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to NexaGreen.copy(alpha = 0.20f),
                        0.68f to Color.Transparent
                    ),
                    center = gC, radius = gR
                ),
                radius = gR, center = gC
            )
            val tC = Offset(size.width, size.height * 0.14f)
            val tR = size.width * 0.75f
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to NexaTeal.copy(alpha = 0.14f),
                        0.70f to Color.Transparent
                    ),
                    center = tC, radius = tR
                ),
                radius = tR, center = tC
            )
        }

        NavHost(
            navController = nav,
            startDestination = AdminRoutes.SPLASH
        ) {
            composable(AdminRoutes.SPLASH) {
                var done by remember { mutableStateOf(false) }
                var destination by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    destination = if (!prefs.token.isNullOrBlank() && prefs.role == "admin") {
                        val res = repo.me()
                        if (res.isSuccess && res.getOrNull()?.success == true && res.getOrNull()?.user?.role == "admin") {
                            AdminRoutes.DASHBOARD
                        } else {
                            val err = res.exceptionOrNull()?.message ?: ""
                            val isAuthFailure = err.contains("401") || err.contains("Invalid") ||
                                err.contains("expired") || err.contains("suspended", true) ||
                                err.contains("403") || err.contains("banned", true)
                            if (isAuthFailure) {
                                repo.clearSession()
                                AdminRoutes.LOGIN
                            } else {
                                AdminRoutes.DASHBOARD
                            }
                        }
                    } else {
                        AdminRoutes.LOGIN
                    }
                    done = true
                }

                SplashScreen { }

                LaunchedEffect(done) {
                    if (done && destination != null) {
                        nav.navigate(destination!!) {
                            popUpTo(AdminRoutes.SPLASH) { inclusive = true }
                        }
                    }
                }
            }

            composable(AdminRoutes.LOGIN)         { LoginScreen(nav, prefs, repo) }
            composable(AdminRoutes.DASHBOARD)     { DashboardScreen(nav, prefs, repo) }
            composable(AdminRoutes.USERS)         { UsersScreen(nav, repo) }
            composable(AdminRoutes.PENDING_KYC)   { PendingKycScreen(nav, repo) }
            composable(AdminRoutes.PENDING_TX)    { PendingTxScreen(nav, repo) }
            composable(AdminRoutes.NOTIFICATIONS) { AdminNotificationsScreen(nav, repo) }

            composable(
                route = "${AdminRoutes.USER_DETAIL}/{userId}",
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) { entry ->
                val id = entry.arguments?.getString("userId") ?: ""
                UserDetailScreen(nav, repo, id)
            }
        }
    }
}
