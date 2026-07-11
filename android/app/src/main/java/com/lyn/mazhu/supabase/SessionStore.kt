package com.lyn.mazhu.supabase

import android.content.Context

class SessionStore(context: Context) {
    private val preferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun load(): SupabaseSession? {
        val accessToken = preferences.getString(KEY_ACCESS_TOKEN, null)
            ?: return null
        val refreshToken = preferences.getString(KEY_REFRESH_TOKEN, null)
            ?: return null
        val userId = preferences.getString(KEY_USER_ID, null)
            ?: return null
        return SupabaseSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            userId = userId,
            email = preferences.getString(KEY_EMAIL, null),
            expiresAtEpochSeconds = preferences.getLong(KEY_EXPIRES_AT, 0),
        )
    }

    fun save(session: SupabaseSession) {
        preferences.edit()
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .putString(KEY_USER_ID, session.userId)
            .putString(KEY_EMAIL, session.email)
            .putLong(KEY_EXPIRES_AT, session.expiresAtEpochSeconds)
            .apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "supabase_session"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_USER_ID = "user_id"
        const val KEY_EMAIL = "email"
        const val KEY_EXPIRES_AT = "expires_at"
    }
}
