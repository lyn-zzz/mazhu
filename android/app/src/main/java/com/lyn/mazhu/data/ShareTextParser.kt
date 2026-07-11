package com.lyn.mazhu.data

import android.net.Uri

object ShareTextParser {
    private val urlPattern = Regex("""https?://[^\s]+""")
    private val trailingPunctuation = charArrayOf('。', '，', ',', '.', ')', '）', ']', '】', '"', '\'')

    fun extractUrl(sharedText: String): String? =
        urlPattern.find(sharedText)
            ?.value
            ?.trimEnd(*trailingPunctuation)

    fun normalizeUrl(url: String): String {
        val uri = Uri.parse(url.trim())
        return uri.buildUpon()
            .fragment(null)
            .build()
            .toString()
    }

    fun extractTitle(sharedText: String, url: String): String {
        val beforeUrl = sharedText.substringBefore(url)
        return beforeUrl
            .lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .lastOrNull()
            ?.take(120)
            ?: "等待解析的公众号文章"
    }
}

