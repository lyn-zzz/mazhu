package com.lyn.mazhu.data

import androidx.room.withTransaction
import java.util.UUID
import kotlinx.coroutines.flow.Flow

class BookmarkRepository(
    private val database: BookmarkDatabase,
    private val bookmarkDao: BookmarkDao,
) {
    fun observeBookmarks(): Flow<List<Bookmark>> = bookmarkDao.observeAll()

    fun observeBookmarks(collectionId: String): Flow<List<Bookmark>> =
        bookmarkDao.observeByCollection(collectionId)

    fun observeCollections(): Flow<List<CollectionSummary>> =
        bookmarkDao.observeCollections()

    suspend fun getCollections(): List<Collection> = bookmarkDao.getCollections()

    suspend fun saveSharedText(
        sharedText: String,
        collectionId: String = DEFAULT_COLLECTION_ID,
    ): SaveResult {
        val url = ShareTextParser.extractUrl(sharedText)
            ?: return SaveResult.InvalidShare
        val bookmark = Bookmark(
            id = UUID.randomUUID().toString(),
            originalUrl = url,
            normalizedUrl = ShareTextParser.normalizeUrl(url),
            title = ShareTextParser.extractTitle(sharedText, url),
            accountName = null,
            coverUrl = null,
            publishedAt = null,
            contentText = null,
            collectionId = collectionId,
            parseStatus = BookmarkStatus.PARSE_PENDING,
            parseError = null,
            syncStatus = BookmarkStatus.SYNC_LOCAL_ONLY,
            syncError = null,
            createdAt = System.currentTimeMillis(),
        )
        val insertedRowId = bookmarkDao.insert(bookmark)
        return if (insertedRowId == -1L) {
            SaveResult.AlreadySaved
        } else {
            SaveResult.Saved(bookmark.id)
        }
    }

    suspend fun createCollection(name: String): CreateCollectionResult {
        val normalizedName = name.trim()
        if (normalizedName.isBlank()) {
            return CreateCollectionResult.InvalidName
        }
        val collection = Collection(
            id = UUID.randomUUID().toString(),
            name = normalizedName,
            syncStatus = BookmarkStatus.SYNC_LOCAL_ONLY,
            syncError = null,
            createdAt = System.currentTimeMillis(),
        )
        return if (bookmarkDao.insertCollection(collection) == -1L) {
            CreateCollectionResult.NameAlreadyExists
        } else {
            CreateCollectionResult.Created(collection)
        }
    }

    suspend fun renameCollection(
        collectionId: String,
        name: String,
    ): RenameCollectionResult {
        if (collectionId == DEFAULT_COLLECTION_ID) {
            return RenameCollectionResult.DefaultCollection
        }
        val normalizedName = name.trim()
        if (normalizedName.isBlank()) {
            return RenameCollectionResult.InvalidName
        }
        return try {
            bookmarkDao.renameCollection(collectionId, normalizedName)
            RenameCollectionResult.Renamed
        } catch (_: Exception) {
            RenameCollectionResult.NameAlreadyExists
        }
    }

    suspend fun deleteCollection(collectionId: String): Boolean {
        if (collectionId == DEFAULT_COLLECTION_ID) {
            return false
        }
        database.withTransaction {
            bookmarkDao.moveAllBookmarks(
                sourceCollectionId = collectionId,
                targetCollectionId = DEFAULT_COLLECTION_ID,
            )
            bookmarkDao.insertPendingDeletion(
                PendingDeletion.collection(collectionId),
            )
            bookmarkDao.deleteCollection(collectionId)
        }
        return true
    }

    suspend fun moveBookmark(bookmarkId: String, collectionId: String) {
        bookmarkDao.moveBookmark(bookmarkId, collectionId)
    }

    suspend fun findPendingParseIds(): List<String> = bookmarkDao.findPendingParseIds()

    companion object {
        const val DEFAULT_COLLECTION_ID = "default"
        const val DEFAULT_COLLECTION_NAME = "默认收藏夹"
    }
}

sealed interface SaveResult {
    data class Saved(val bookmarkId: String) : SaveResult
    data object AlreadySaved : SaveResult
    data object InvalidShare : SaveResult
}

sealed interface CreateCollectionResult {
    data class Created(val collection: Collection) : CreateCollectionResult
    data object InvalidName : CreateCollectionResult
    data object NameAlreadyExists : CreateCollectionResult
}

sealed interface RenameCollectionResult {
    data object Renamed : RenameCollectionResult
    data object InvalidName : RenameCollectionResult
    data object NameAlreadyExists : RenameCollectionResult
    data object DefaultCollection : RenameCollectionResult
}
