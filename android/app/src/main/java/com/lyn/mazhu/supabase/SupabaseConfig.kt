package com.lyn.mazhu.supabase

enum class SupabaseSyncMode {
    OFFICIAL,
    LOCAL_ONLY,
    CUSTOM,
}

data class SupabaseConfig(
    val mode: SupabaseSyncMode,
    val url: String,
    val publishableKey: String,
    val customUrl: String = "",
    val customPublishableKey: String = "",
    val officialAvailable: Boolean = false,
) {
    val isConfigured: Boolean
        get() = mode != SupabaseSyncMode.LOCAL_ONLY &&
            url.isNotBlank() &&
            publishableKey.isNotBlank()

    val isOfficial: Boolean
        get() = mode == SupabaseSyncMode.OFFICIAL

    val isCustom: Boolean
        get() = mode == SupabaseSyncMode.CUSTOM
}
