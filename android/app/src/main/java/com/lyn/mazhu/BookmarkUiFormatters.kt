package com.lyn.mazhu

import com.lyn.mazhu.data.Bookmark
import com.lyn.mazhu.data.BookmarkStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun formatTime(timestamp: Long): String =
    SimpleDateFormat("MM月dd日 HH:mm", Locale.CHINA).format(Date(timestamp))

internal fun Bookmark.statusLabel(syncEnabled: Boolean): String =
    when (parseStatus) {
        BookmarkStatus.PARSE_PENDING -> "等待解析"
        BookmarkStatus.PARSE_PROCESSING -> "正在解析"
        BookmarkStatus.PARSE_FAILED -> "解析失败"
        else -> if (!syncEnabled) {
            "仅本地"
        } else when (syncStatus) {
            BookmarkStatus.SYNC_SYNCED -> "已同步"
            BookmarkStatus.SYNC_SYNCING -> "同步中"
            else -> "未同步"
        }
    }
