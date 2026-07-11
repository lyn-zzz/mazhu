package com.lyn.mazhu.parser

data class ParsedArticle(
    val title: String,
    val accountName: String?,
    val coverUrl: String?,
    val publishedAt: Long?,
    val contentText: String,
)

