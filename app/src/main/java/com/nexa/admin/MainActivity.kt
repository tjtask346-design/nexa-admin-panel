package com.nexa.admin

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.nexa.admin.data.Prefs
import com.nexa.admin.data.Repository
import com.nexa.admin.nav.AdminNav
import com.nexa.admin.ui.theme.NexaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var prefs: Prefs
    private lateinit var repo: Repository

    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        } catch (e: Exception) {
            Log.e("NEXA_ADMIN_FCM", "Firebase init failed", e)
        }

        prefs = Prefs(applicationContext)
        repo = Repository(prefs)

        createNotificationChannel()
        askNotificationPermission()

        setContent {
            NexaTheme {
                Surface(color = Color.Transparent, modifier = Modifier.fillMaxSize()) {
                    FcmSyncEffect(prefs, repo)
                    AdminNav(prefs, repo)
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "nexa_admin_alerts",
                "Nexa Admin Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "KYC, deposits, withdrawals" }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
fun FcmSyncEffect(prefs: Prefs, repo: Repository) {
    LaunchedEffect(Unit) {
        if (prefs.fcmToken.isNullOrBlank()) {
            try {
                val token = withContext(Dispatchers.IO) {
                    FirebaseMessaging.getInstance().token.await()
                }
                if (!token.isNullOrBlank()) {
                    prefs.fcmToken = token
                    prefs.fcmTokenSynced = false
                }
            } catch (_: Exception) { }
        }
    }

    LaunchedEffect(Unit) {
        var attempts = 0
        while (attempts < 90) {
            try {
                val auth = prefs.token
                val fcm = prefs.fcmToken
                val synced = prefs.fcmTokenSynced
                if (!auth.isNullOrBlank() && !fcm.isNullOrBlank() && !synced) {
                    val res = repo.saveFcmToken(fcm)
                    if (res.isSuccess && res.getOrNull()?.success == true) {
                        prefs.fcmTokenSynced = true
                        return@LaunchedEffect
                    }
                }
            } catch (_: Exception) { }
            delay(2000)
            attempts++
        }
    }
}
