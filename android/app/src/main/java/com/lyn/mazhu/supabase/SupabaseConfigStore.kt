package com.lyn.mazhu.supabase

import android.content.Context
import com.lyn.mazhu.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SupabaseConfigStore(context: Context) {
    private val preferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )
    private val fallbackConfig = SupabaseConfig(
        url = BuildConfig.SUPABASE_URL,
        publishableKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
    )
    private val mutableConfig = MutableStateFlow(load())

    val config: StateFlow<SupabaseConfig> = mutableConfig

    fun current(): SupabaseConfig = mutableConfig.value

    fun save(config: SupabaseConfig) {
        preferences.edit()
            .putString(KEY_URL, config.url.trim().trimEnd('/'))
            .putString(KEY_PUBLISHABLE_KEY, config.publishableKey.trim())
            .apply()
        mutableConfig.value = load()
    }

    fun reset() {
        preferences.edit().clear().apply()
        mutableConfig.value = load()
    }

    private fun load(): SupabaseConfig {
        val savedUrl = preferences.getString(KEY_URL, null)
            ?.trim()
            ?.trimEnd('/')
            .orEmpty()
        val savedKey = preferences.getString(KEY_PUBLISHABLE_KEY, null)
            ?.trim()
            .orEmpty()
        if (savedUrl.isNotBlank() || savedKey.isNotBlank()) {
            return SupabaseConfig(savedUrl, savedKey)
        }
        return fallbackConfig
    }

    private companion object {
        const val PREFERENCES_NAME = "supabase_config"
        const val KEY_URL = "url"
        const val KEY_PUBLISHABLE_KEY = "publishable_key"
    }
}
