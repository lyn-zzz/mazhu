package com.lyn.mazhu.parser

import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class WechatArticleParser {
    suspend fun parse(url: String): ParsedArticle = withContext(Dispatchers.IO) {
        val response = Jsoup.connect(url)
            .userAgent(ANDROID_WECHAT_USER_AGENT)
            .referrer("https://mp.weixin.qq.com/")
            .header("Accept-Language", "zh-CN,zh;q=0.9")
            .followRedirects(true)
            .ignoreHttpErrors(false)
            .maxBodySize(0)
            .timeout(30_000)
            .execute()

        parseHtml(response.body())
    }

    internal fun parseHtml(html: String): ParsedArticle {
        val document = Jsoup.parse(html)
        val title = document.selectFirst("meta[property=og:title]")
            ?.attr("content")
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?: document.selectFirst("#activity-name")
                ?.text()
                ?.trim()
                ?.takeIf(String::isNotBlank)
            ?: throw ArticleParseException("未找到文章标题")

        val accountName = document.selectFirst("#js_name")
            ?.text()
            ?.trim()
            ?.takeIf(String::isNotBlank)

        val coverUrl = document.selectFirst("meta[property=og:image]")
            ?.attr("content")
            ?.trim()
            ?.takeIf(String::isNotBlank)

        val contentHtml = extractContentHtml(document, html)
        val contentText = htmlToReadableText(contentHtml)
        if (contentText.length < MIN_CONTENT_LENGTH) {
            throw ArticleParseException("文章正文过短，可能遇到微信访问校验")
        }

        return ParsedArticle(
            title = title,
            accountName = accountName,
            coverUrl = coverUrl,
            publishedAt = extractPublishedAt(html),
            contentText = contentText,
        )
    }

    private fun extractContentHtml(document: Document, html: String): String {
        val directContent = document.selectFirst("#js_content")
            ?.html()
            ?.takeIf { Jsoup.parseBodyFragment(it).text().length >= MIN_CONTENT_LENGTH }
        if (directContent != null) {
            return directContent
        }

        val encodedContent = CONTENT_PATTERN.findAll(html)
            .map { it.groupValues[1] }
            .maxByOrNull(String::length)
            ?: throw ArticleParseException("未找到微信正文数据")

        return decodeJavaScriptString(encodedContent)
    }

    private fun htmlToReadableText(contentHtml: String): String {
        val body = Jsoup.parseBodyFragment(contentHtml).body()
        body.select("script,style,mp-common-profile").remove()
        body.select("br").after("\n")
        body.select("p,h1,h2,h3,h4,h5,h6,li,blockquote,pre,section").forEach { element ->
            element.before("\n")
            element.after("\n")
        }
        appendExternalLinks(body)

        return body.wholeText()
            .lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .fold(mutableListOf<String>()) { lines, line ->
                if (lines.lastOrNull() != line) {
                    lines += line
                }
                lines
            }
            .joinToString("\n")
            .trim()
    }

    private fun appendExternalLinks(body: Element) {
        body.select("a[href]").forEach { link ->
            val href = link.absUrl("href").ifBlank { link.attr("href") }
            if (href.startsWith("http") && !href.contains("mp.weixin.qq.com")) {
                val text = link.text().trim()
                link.text(if (text.isBlank()) href else "$text ($href)")
            }
        }
    }

    private fun extractPublishedAt(html: String): Long? {
        val timestamp = TIMESTAMP_PATTERN.find(html)
            ?.groupValues
            ?.get(1)
            ?.toLongOrNull()
        if (timestamp != null) {
            return timestamp * 1_000
        }

        val dateText = DATE_PATTERN.find(html)?.groupValues?.get(1) ?: return null
        return runCatching {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).parse(dateText)?.time
        }.getOrNull()
    }

    private fun decodeJavaScriptString(value: String): String {
        val result = StringBuilder(value.length)
        var index = 0
        while (index < value.length) {
            val current = value[index]
            if (current != '\\' || index + 1 >= value.length) {
                result.append(current)
                index += 1
                continue
            }

            when (val escaped = value[index + 1]) {
                'x' -> {
                    val hex = value.substringOrNull(index + 2, index + 4)
                    if (hex != null) {
                        result.append(hex.toInt(16).toChar())
                        index += 4
                    } else {
                        result.append(escaped)
                        index += 2
                    }
                }

                'u' -> {
                    val hex = value.substringOrNull(index + 2, index + 6)
                    if (hex != null) {
                        result.append(hex.toInt(16).toChar())
                        index += 6
                    } else {
                        result.append(escaped)
                        index += 2
                    }
                }

                'n' -> {
                    result.append('\n')
                    index += 2
                }

                'r' -> {
                    result.append('\r')
                    index += 2
                }

                't' -> {
                    result.append('\t')
                    index += 2
                }

                else -> {
                    result.append(escaped)
                    index += 2
                }
            }
        }
        return result.toString()
    }

    private fun String.substringOrNull(start: Int, end: Int): String? =
        if (end <= length) substring(start, end) else null

    companion object {
        private const val MIN_CONTENT_LENGTH = 80
        private const val ANDROID_WECHAT_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 16; Mobile) AppleWebKit/537.36 " +
                "Chrome/135.0 Mobile Safari/537.36 MicroMessenger/8.0.58"

        private val CONTENT_PATTERN = Regex(
            pattern = """content_noencode:\s*'((?:\\.|[^'])*)'""",
            options = setOf(RegexOption.DOT_MATCHES_ALL),
        )
        private val TIMESTAMP_PATTERN = Regex(
            """(?:oriCreateTime|create_time)\s*(?:=|:)\s*['"](\d{10})['"]""",
        )
        private val DATE_PATTERN = Regex(
            """create_time:\s*['"](\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2})['"]""",
        )
    }
}

class ArticleParseException(message: String) : Exception(message)
