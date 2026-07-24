package com.lyn.mazhu.update

import android.content.Context
import com.lyn.mazhu.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class UpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val downloadPageUrl: String,
    val apkUrl: String,
    val releaseNotes: List<String>,
)

class UpdateRepository(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences("app_updates", Context.MODE_PRIVATE)

    suspend fun checkForAvailableUpdate(force: Boolean = false): UpdateInfo? {
        if (BuildConfig.UPDATE_MANIFEST_URL.isBlank()) {
            return null
        }
        val now = System.currentTimeMillis()
        if (!force && now - preferences.getLong(KEY_LAST_AUTO_CHECK_AT, 0L) < AUTO_CHECK_INTERVAL_MS) {
            return null
        }

        val manifest = runCatching {
            fetchManifest()
        }.getOrElse {
            preferences.edit()
                .putLong(KEY_LAST_AUTO_CHECK_AT, now)
                .apply()
            return null
        }

        preferences.edit()
            .putLong(KEY_LAST_AUTO_CHECK_AT, now)
            .apply()

        if (manifest.versionCode <= BuildConfig.VERSION_CODE) {
            return null
        }
        if (!force && preferences.getInt(KEY_DEFERRED_VERSION_CODE, -1) == manifest.versionCode) {
            val deferredAt = preferences.getLong(KEY_DEFERRED_AT, 0L)
            if (now - deferredAt < DEFER_INTERVAL_MS) {
                return null
            }
        }
        return manifest
    }

    fun defer(updateInfo: UpdateInfo) {
        preferences.edit()
            .putInt(KEY_DEFERRED_VERSION_CODE, updateInfo.versionCode)
            .putLong(KEY_DEFERRED_AT, System.currentTimeMillis())
            .apply()
    }

    private suspend fun fetchManifest(): UpdateInfo = withContext(Dispatchers.IO) {
        val connection = URL(BuildConfig.UPDATE_MANIFEST_URL).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            connection.setRequestProperty("Accept", "application/json")

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                error("Update manifest request failed: $responseCode")
            }

            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val json = JSONObject(body)
            val releaseNotesJson = json.optJSONArray("releaseNotes")
            val releaseNotes = buildList {
                if (releaseNotesJson != null) {
                    for (index in 0 until releaseNotesJson.length()) {
                        releaseNotesJson.optString(index)
                            .takeIf(String::isNotBlank)
                            ?.let(::add)
                    }
                }
            }
            UpdateInfo(
                versionName = json.getString("versionName"),
                versionCode = json.getInt("versionCode"),
                downloadPageUrl = json.optString("downloadPageUrl")
                    .ifBlank { BuildConfig.DOWNLOAD_PAGE_URL },
                apkUrl = json.optString("apkUrl"),
                releaseNotes = releaseNotes,
            )
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val KEY_LAST_AUTO_CHECK_AT = "last_auto_check_at"
        const val KEY_DEFERRED_VERSION_CODE = "deferred_version_code"
        const val KEY_DEFERRED_AT = "deferred_at"
        const val AUTO_CHECK_INTERVAL_MS = 24L * 60L * 60L * 1000L
        const val DEFER_INTERVAL_MS = 24L * 60L * 60L * 1000L
    }
}
