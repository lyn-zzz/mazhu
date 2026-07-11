package com.lyn.mazhu.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bookmarks",
    indices = [
        Index(value = ["normalizedUrl"], unique = true),
        Index(value = ["collectionId"]),
    ],
)
data class Bookmark(
    @PrimaryKey val id: String,
    val originalUrl: String,
    val normalizedUrl: String,
    val title: String,
    val accountName: String?,
    val coverUrl: String?,
    val publishedAt: Long?,
    val contentText: String?,
    val collectionId: String,
    val parseStatus: String,
    val parseError: String?,
    val syncStatus: String,
    val syncError: String?,
    val createdAt: Long,
)

object BookmarkStatus {
    const val PARSE_PENDING = "pending"
    const val PARSE_PROCESSING = "processing"
    const val PARSE_SUCCESS = "success"
    const val PARSE_FAILED = "failed"
    const val SYNC_LOCAL_ONLY = "local_only"
    const val SYNC_SYNCING = "syncing"
    const val SYNC_SYNCED = "synced"
}
