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

        fun collection(collectionId: String): PendingDeletion =
            PendingDeletion(
                key = "$TYPE_COLLECTION:$collectionId",
                entityType = TYPE_COLLECTION,
                entityId = collectionId,
                createdAt = System.currentTimeMillis(),
            )
    }
}
