package com.lyn.mazhu.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "collections",
    indices = [Index(value = ["name"], unique = true)],
)
data class Collection(
    @PrimaryKey val id: String,
    val name: String,
    val sortOrder: Long,
    val syncStatus: String,
    val syncError: String?,
    val createdAt: Long,
)

data class CollectionSummary(
    val id: String,
    val name: String,
    val sortOrder: Long,
    val createdAt: Long,
    val articleCount: Long,
    val unsyncedCount: Long,
)
