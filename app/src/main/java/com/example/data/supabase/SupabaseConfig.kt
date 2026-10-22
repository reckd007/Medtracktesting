package com.example.data.supabase

import android.content.Context
import android.content.SharedPreferences

/**
 * Configuration for the Supabase Kotlin client connecting to the local Supabase environment.
 * Default URL for Android Emulator loopback is http://10.0.2.2:54321.
 */
object SupabaseConfig {
    const val DEFAULT_LOCAL_URL = "http://10.0.2.2:54321"

    // Standard Supabase local CLI demo anon key for local development and testing
    const val DEFAULT_LOCAL_ANON_KEY =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZS1kZW1vIiwicm9sZSI6ImFub24iLCJpYXQiOjE2MTY0MzgzNDAsImV4cCI6MTkzMjAxNDM0MH0.4bpHq7kY1E5T2A1u7_9zJv4V1f3o7K4f9B4V2q8P7L8"

    private const val PREFS_NAME = "supabase_config_prefs"
    private const val KEY_URL = "supabase_url"
    private const val KEY_ANON_KEY = "supabase_anon_key"

    private var prefs: SharedPreferences? = null

    fun initialize(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    fun getSupabaseUrl(): String {
        return prefs?.getString(KEY_URL, DEFAULT_LOCAL_URL)?.takeIf { it.isNotBlank() } ?: DEFAULT_LOCAL_URL
    }

    fun getAnonKey(): String {
        return prefs?.getString(KEY_ANON_KEY, DEFAULT_LOCAL_ANON_KEY)?.takeIf { it.isNotBlank() } ?: DEFAULT_LOCAL_ANON_KEY
    }

    fun isConfigured(): Boolean {
        val url = getSupabaseUrl().trim()
        val key = getAnonKey().trim()
        if (url.isBlank() || key.isBlank()) return false
        val isExplicitCustom = prefs?.contains(KEY_URL) == true
        if (!isExplicitCustom && (url == DEFAULT_LOCAL_URL || url.contains("10.0.2.2") || url.contains("localhost"))) {
            return false
        }
        return url.startsWith("https://") || (isExplicitCustom && url.startsWith("http"))
    }

    fun isCloudConfigured(): Boolean {
        val url = getSupabaseUrl().trim()
        return url.startsWith("https://") && !url.contains("10.0.2.2")
    }

    fun updateConfig(url: String, anonKey: String) {
        prefs?.edit()?.apply {
            putString(KEY_URL, url.trim())
            putString(KEY_ANON_KEY, anonKey.trim())
            apply()
        }
    }

    fun clearConfig() {
        prefs?.edit()?.apply {
            remove(KEY_URL)
            remove(KEY_ANON_KEY)
            apply()
        }
    }
}
