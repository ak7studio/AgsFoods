package com.ak7studio.agsfood

import android.content.Context
import android.content.SharedPreferences

object UserPrefs {
    private const val PREFS_NAME = "user_prefs"
    private const val KEY_USERNAME = "username"
    private const val KEY_ROLE = "role"
    const val KEY_CASHIER = "cashier"
    const val KEY_MANAGER = "manager"
    const val KEY_ADMIN = "admin"

    private lateinit var prefs: SharedPreferences

    // Initialize SharedPreferences instance (call once e.g. in Application or first Activity)
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Save username
    fun setUsername(username: String) {
        prefs.edit().putString(KEY_USERNAME, username).apply()
    }

    // Get username, default empty string
    fun getUsername(): String {
        return prefs.getString(KEY_USERNAME, "") ?: ""
    }

    // Save role
    fun setRole(role: String) {
        prefs.edit().putString(KEY_ROLE, role).apply()
    }

    // Get role, default "cashier"
    fun getRole(): String {
        return prefs.getString(KEY_ROLE, KEY_CASHIER) ?: KEY_CASHIER
    }

    // Clear all user prefs (optional)
    fun clear() {
        prefs.edit().clear().apply()
    }
}
