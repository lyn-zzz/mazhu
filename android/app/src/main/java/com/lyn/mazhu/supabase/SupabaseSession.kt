package com.lyn.mazhu.supabase

data class SupabaseSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val email: String?,
    val expiresAtEpochSeconds: Long,
) {
    fun needsRefresh(nowEpochSeconds: Long = System.currentTimeMillis() / 1_000): Boolean =
        expiresAtEpochSeconds <= nowEpochSeconds + 60
}
