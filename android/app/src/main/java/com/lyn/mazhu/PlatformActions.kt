package com.lyn.mazhu

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.lyn.mazhu.data.Bookmark
import com.lyn.mazhu.data.ShareTextParser

internal fun openBookmark(
    context: Context,
    bookmark: Bookmark,
) {
    context.startActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse(bookmark.originalUrl)),
    )
}

internal fun copyToClipboard(
    context: Context,
    text: String,
) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("文章链接", text))
}

internal fun detectWechatClipboardLink(context: Context): ClipboardWechatLink? {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = runCatching { clipboard.primaryClip }.getOrNull()
    val text = runCatching {
        clip
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.coerceToText(context)
            ?.toString()
            .orEmpty()
    }.getOrDefault("")
    val url = ShareTextParser.extractUrl(text) ?: return null
    if (Uri.parse(url).host != "mp.weixin.qq.com") {
        return null
    }
    val normalizedUrl = ShareTextParser.normalizeUrl(url)
    val timestamp = runCatching { clipboard.primaryClipDescription?.timestamp }
        .getOrNull()
    val token = "$normalizedUrl|${timestamp ?: text.hashCode().toLong()}"
    return ClipboardWechatLink(url = url, token = token)
}

internal fun wasClipboardLinkHandled(
    context: Context,
    link: ClipboardWechatLink,
): Boolean {
    val preferences = context.getSharedPreferences("clipboard_prompt", Context.MODE_PRIVATE)
    return preferences.getString("last_token", null) == link.token
}

internal fun markClipboardLinkHandled(
    context: Context,
    link: ClipboardWechatLink,
) {
    context.getSharedPreferences("clipboard_prompt", Context.MODE_PRIVATE)
        .edit()
        .putString("last_token", link.token)
        .apply()
}
