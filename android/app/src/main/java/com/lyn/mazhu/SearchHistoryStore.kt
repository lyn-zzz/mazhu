package com.lyn.mazhu

import android.content.Context

private const val SEARCH_HISTORY_PREFS = "search_history"
private const val SEARCH_HISTORY_KEYWORDS = "keywords"
private const val MAX_SEARCH_HISTORY_SIZE = 20

internal fun loadSearchHistory(context: Context): List<String> =
    context.getSharedPreferences(SEARCH_HISTORY_PREFS, Context.MODE_PRIVATE)
        .getString(SEARCH_HISTORY_KEYWORDS, "")
        .orEmpty()
        .split("\n")
        .map(String::trim)
        .filter(String::isNotBlank)

internal fun saveSearchHistory(
    context: Context,
    keyword: String,
): List<String> {
    val normalizedKeyword = keyword.trim()
    if (normalizedKeyword.isBlank()) {
        return loadSearchHistory(context)
    }
    val updated = (listOf(normalizedKeyword) + loadSearchHistory(context))
        .distinct()
        .take(MAX_SEARCH_HISTORY_SIZE)
    persistSearchHistory(context, updated)
    return updated
}

internal fun deleteSearchHistory(
    context: Context,
    keyword: String,
): List<String> {
    val updated = loadSearchHistory(context).filterNot { it == keyword }
    persistSearchHistory(context, updated)
    return updated
}

internal fun clearSearchHistory(context: Context): List<String> {
    persistSearchHistory(context, emptyList())
    return emptyList()
}

private fun persistSearchHistory(
    context: Context,
    histories: List<String>,
) {
    context.getSharedPreferences(SEARCH_HISTORY_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(SEARCH_HISTORY_KEYWORDS, histories.joinToString("\n"))
        .apply()
}
