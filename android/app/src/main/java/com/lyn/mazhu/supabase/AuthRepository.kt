package com.lyn.mazhu.supabase

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

class AuthRepository(
    private val httpClient: SupabaseHttpClient,
    private val sessionStore: SessionStore,
) {
    private val refreshMutex = Mutex()
    private val mutableSession = MutableStateFlow(sessionStore.load())

    val session: StateFlow<SupabaseSession?> = mutableSession

    suspend fun signUp(
        email: String,
        password: String,
    ): AuthResult =
        runCatching {
            val response = httpClient.request(
                path = "/auth/v1/signup",
                method = "POST",
                body = JSONObject()
                    .put("email", email.trim())
                    .put("password", password)
                    .toString(),
            )
            val json = JSONObject(response.body)
            if (json.has("access_token")) {
                val session = parseSession(json)
                saveSession(session)
                AuthResult.Authenticated(session)
            } else {
                AuthResult.EmailVerificationRequired
            }
        }.getOrElse { error ->
            AuthResult.Failed(error.message ?: "注册失败")
        }

    suspend fun signIn(
        email: String,
        password: String,
    ): AuthResult =
        runCatching {
            val response = httpClient.request(
                path = "/auth/v1/token?grant_type=password",
                method = "POST",
                body = JSONObject()
                    .put("email", email.trim())
                    .put("password", password)
                    .toString(),
            )
            val session = parseSession(JSONObject(response.body))
            saveSession(session)
            AuthResult.Authenticated(session)
        }.getOrElse { error ->
            AuthResult.Failed(error.message ?: "登录失败")
        }

    suspend fun getValidSession(): SupabaseSession? {
        val current = mutableSession.value ?: return null
        if (!current.needsRefresh()) {
            return current
        }
        return refreshMutex.withLock {
            val latest = mutableSession.value ?: return@withLock null
            if (!latest.needsRefresh()) {
                return@withLock latest
            }
            refreshSession(latest)
        }
    }

    fun signOut() {
        sessionStore.clear()
        mutableSession.value = null
    }

    private suspend fun refreshSession(session: SupabaseSession): SupabaseSession? =
        runCatching {
            val response = httpClient.request(
                path = "/auth/v1/token?grant_type=refresh_token",
                method = "POST",
                body = JSONObject()
                    .put("refresh_token", session.refreshToken)
                    .toString(),
            )
            parseSession(JSONObject(response.body)).also(::saveSession)
        }.getOrElse {
            signOut()
            null
        }

    private fun parseSession(json: JSONObject): SupabaseSession {
        val user = json.getJSONObject("user")
        val expiresIn = json.optLong("expires_in", 3_600)
        return SupabaseSession(
            accessToken = json.getString("access_token"),
            refreshToken = json.getString("refresh_token"),
            userId = user.getString("id"),
            email = user.optString("email").takeIf(String::isNotBlank),
            expiresAtEpochSeconds = System.currentTimeMillis() / 1_000 + expiresIn,
        )
    }

    private fun saveSession(session: SupabaseSession) {
        sessionStore.save(session)
        mutableSession.value = session
    }
}

sealed interface AuthResult {
    data class Authenticated(val session: SupabaseSession) : AuthResult
    data object EmailVerificationRequired : AuthResult
    data class Failed(val message: String) : AuthResult
}
