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
    private val officialUrl = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
    private val officialPublishableKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY.trim()
    private val officialAvailable = officialUrl.isNotBlank() &&
        officialPublishableKey.isNotBlank()
    private val mutableConfig = MutableStateFlow(load())

    val config: StateFlow<SupabaseConfig> = mutableConfig

    fun current(): SupabaseConfig = mutableConfig.value

    fun useOfficial() {
        preferences.edit()
            .putString(KEY_MODE, SupabaseSyncMode.OFFICIAL.name)
            .apply()
        mutableConfig.value = load()
    }

    fun useLocalOnly() {
        preferences.edit()
            .putString(KEY_MODE, SupabaseSyncMode.LOCAL_ONLY.name)
            .apply()
        mutableConfig.value = load()
    }

    fun saveCustom(
        url: String,
        publishableKey: String,
    ) {
        preferences.edit()
            .putString(KEY_MODE, SupabaseSyncMode.CUSTOM.name)
            .putString(KEY_URL, url.trim().trimEnd('/'))
            .putString(KEY_PUBLISHABLE_KEY, publishableKey.trim())
            .apply()
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
        val mode = preferences.getString(KEY_MODE, null)
            ?.let { value ->
                runCatching { SupabaseSyncMode.valueOf(value) }.getOrNull()
            }
            ?: if (savedUrl.isNotBlank() || savedKey.isNotBlank()) {
                SupabaseSyncMode.CUSTOM
            } else if (officialAvailable) {
                SupabaseSyncMode.OFFICIAL
            } else {
                SupabaseSyncMode.LOCAL_ONLY
            }

        return when (mode) {
            SupabaseSyncMode.OFFICIAL -> SupabaseConfig(
                mode = if (officialAvailable) {
                    SupabaseSyncMode.OFFICIAL
                } else {
                    SupabaseSyncMode.LOCAL_ONLY
                },
                url = officialUrl,
                publishableKey = officialPublishableKey,
                customUrl = savedUrl,
                customPublishableKey = savedKey,
                officialAvailable = officialAvailable,
            )
            SupabaseSyncMode.LOCAL_ONLY -> SupabaseConfig(
                mode = SupabaseSyncMode.LOCAL_ONLY,
                url = "",
                publishableKey = "",
                customUrl = savedUrl,
                customPublishableKey = savedKey,
                officialAvailable = officialAvailable,
            )
            SupabaseSyncMode.CUSTOM -> SupabaseConfig(
                mode = SupabaseSyncMode.CUSTOM,
                url = savedUrl,
                publishableKey = savedKey,
                customUrl = savedUrl,
                customPublishableKey = savedKey,
                officialAvailable = officialAvailable,
            )
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "supabase_config"
        const val KEY_MODE = "mode"
        const val KEY_URL = "url"
        const val KEY_PUBLISHABLE_KEY = "publishable_key"
    }
}
