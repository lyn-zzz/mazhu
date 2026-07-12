package com.lyn.mazhu.supabase

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class SupabaseHttpClient(
    private val configStore: SupabaseConfigStore,
) {
    suspend fun request(
        path: String,
        method: String,
        accessToken: String? = null,
        body: String? = null,
        prefer: String? = null,
    ): HttpResponse = withContext(Dispatchers.IO) {
        val config = configStore.current()
        check(config.url.isNotBlank()) {
            "请先在设置中启用云同步"
        }
        check(config.publishableKey.isNotBlank()) {
            "请先在设置中启用云同步"
        }

        val connection = URL("${config.url}$path")
            .openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 12_000
            connection.readTimeout = 25_000
            connection.setRequestProperty(
                "apikey",
                config.publishableKey,
            )
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Content-Type", "application/json")
            accessToken?.let {
                connection.setRequestProperty("Authorization", "Bearer $it")
            }
            prefer?.let {
                connection.setRequestProperty("Prefer", it)
            }

            if (body != null) {
                connection.doOutput = true
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                    writer.write(body)
                }
            }

            val statusCode = connection.responseCode
            val responseBody = (
                if (statusCode in 200..299) connection.inputStream else connection.errorStream
                )
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                .orEmpty()

            if (statusCode !in 200..299) {
                throw SupabaseHttpException(
                    statusCode = statusCode,
                    responseBody = responseBody,
                    message = extractErrorMessage(responseBody),
                )
            }
            HttpResponse(
                statusCode = statusCode,
                body = responseBody,
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun extractErrorMessage(responseBody: String): String {
        if (responseBody.isBlank()) {
            return "Supabase request failed"
        }
        return runCatching {
            val json = JSONObject(responseBody)
            sequenceOf(
                "msg",
                "message",
                "error_description",
                "error",
                "hint",
            )
                .mapNotNull { key -> json.optString(key).takeIf(String::isNotBlank) }
                .first()
        }.getOrElse {
            responseBody.take(300)
        }
    }
}

data class HttpResponse(
    val statusCode: Int,
    val body: String,
)

class SupabaseHttpException(
    val statusCode: Int,
    val responseBody: String,
    override val message: String,
) : Exception(message)
