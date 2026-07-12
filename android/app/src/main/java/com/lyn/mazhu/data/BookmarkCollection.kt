package com.lyn.mazhu.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "bookmark_collections",
    primaryKeys = ["bookmarkId", "collectionId"],
    foreignKeys = [
        ForeignKey(
            entity = Bookmark::class,
            parentColumns = ["id"],
            childColumns = ["bookmarkId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Collection::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("bookmarkId"),
        Index("collectionId"),
    ],
)
data class BookmarkCollection(
    val bookmarkId: String,
    val collectionId: String,
    val syncStatus: String,
    val syncError: String?,
    val createdAt: Long,
)
