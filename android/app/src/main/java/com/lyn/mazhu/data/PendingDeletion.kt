package com.lyn.mazhu.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_deletions")
data class PendingDeletion(
    @PrimaryKey val key: String,
    val entityType: String,
    val entityId: String,
    val createdAt: Long,
) {
    companion object {
        const val TYPE_COLLECTION = "collection"
        const val TYPE_BOOKMARK = "bookmark"
        const val TYPE_BOOKMARK_COLLECTION = "bookmark_collection"

        fun collection(collectionId: String): PendingDeletion =
            PendingDeletion(
                key = "$TYPE_COLLECTION:$collectionId",
                entityType = TYPE_COLLECTION,
                entityId = collectionId,
                createdAt = System.currentTimeMillis(),
            )

        fun bookmark(bookmarkId: String): PendingDeletion =
            PendingDeletion(
                key = "$TYPE_BOOKMARK:$bookmarkId",
                entityType = TYPE_BOOKMARK,
                entityId = bookmarkId,
                createdAt = System.currentTimeMillis(),
            )

        fun bookmarkCollection(
            bookmarkId: String,
            collectionId: String,
        ): PendingDeletion =
            PendingDeletion(
                key = "$TYPE_BOOKMARK_COLLECTION:$bookmarkId:$collectionId",
                entityType = TYPE_BOOKMARK_COLLECTION,
                entityId = "$bookmarkId:$collectionId",
                createdAt = System.currentTimeMillis(),
            )
    }
}
