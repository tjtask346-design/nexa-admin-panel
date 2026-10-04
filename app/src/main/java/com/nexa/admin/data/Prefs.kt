package com.nexa.admin.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class Prefs(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sp = EncryptedSharedPreferences.create(
        context, "nexa_admin_secure", masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var token: String?
        get() = sp.getString("token", null)
        set(v) = sp.edit().putString("token", v).apply()

    var email: String?
        get() = sp.getString("email", null)
        set(v) = sp.edit().putString("email", v).apply()

    var name: String?
        get() = sp.getString("name", null)
        set(v) = sp.edit().putString("name", v).apply()

    var role: String?
        get() = sp.getString("role", null)
        set(v) = sp.edit().putString("role", v).apply()

    var fcmToken: String?
        get() = sp.getString("fcmToken", null)
        set(v) = sp.edit().putString("fcmToken", v).apply()

    var fcmTokenSynced: Boolean
        get() = sp.getBoolean("fcmTokenSynced", false)
        set(v) = sp.edit().putBoolean("fcmTokenSynced", v).apply()

    fun logout() {
        val savedFcm = fcmToken
        sp.edit().clear().apply()
        fcmToken = savedFcm
    }

    fun clear() = sp.edit().clear().apply()
}
