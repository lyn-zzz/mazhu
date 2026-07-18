package com.lyn.mazhu.data

import androidx.room.withTransaction
import java.util.UUID
import kotlinx.coroutines.flow.Flow

class BookmarkRepository(
    private val database: BookmarkDatabase,
    private val bookmarkDao: BookmarkDao,
) {
    fun observeBookmarks(): Flow<List<Bookmark>> = bookmarkDao.observeAll()

    fun observeBookmarkCollections(): Flow<List<BookmarkCollection>> =
        bookmarkDao.observeBookmarkCollections()

    fun observeBookmarks(collectionId: String): Flow<List<Bookmark>> =
        bookmarkDao.observeByCollection(collectionId)

    fun searchBookmarks(query: String): Flow<List<Bookmark>> =
        if (query.isBlank()) {
            observeBookmarks()
        } else {
            bookmarkDao.searchBookmarks(query.trim())
        }

    fun observeCollections(): Flow<List<CollectionSummary>> =
        bookmarkDao.observeCollections()

    suspend fun getCollections(): List<Collection> = bookmarkDao.getCollections()


    suspend fun hasPendingSync(): Boolean =
        bookmarkDao.getPendingCollections().isNotEmpty() ||
            bookmarkDao.getPendingBookmarks().isNotEmpty() ||
            bookmarkDao.getPendingBookmarkCollections().isNotEmpty() ||
            bookmarkDao.getPendingDeletions().isNotEmpty()

    suspend fun hasPendingVisibleSync(): Boolean =
        bookmarkDao.getPendingCollections().isNotEmpty() ||
            bookmarkDao.getPendingBookmarks().isNotEmpty() ||
            bookmarkDao.getPendingBookmarkCollections().isNotEmpty()

    suspend fun saveSharedText(sharedText: String): SaveResult {
        val url = ShareTextParser.extractUrl(sharedText)
            ?: return SaveResult.InvalidShare

        val normalizedUrl = ShareTextParser.normalizeUrl(url)
        bookmarkDao.findByNormalizedUrl(normalizedUrl)?.let { existing ->
            return SaveResult.AlreadySaved(existing.id)
        }

        return database.withTransaction {
            val bookmark = Bookmark(
                id = UUID.randomUUID().toString(),
                originalUrl = url,
                normalizedUrl = normalizedUrl,
                title = ShareTextParser.extractTitle(sharedText, url),
                accountName = null,
                coverUrl = null,
                publishedAt = null,
                contentText = null,
                collectionId = DEFAULT_COLLECTION_ID,
                parseStatus = BookmarkStatus.PARSE_PENDING,
                parseError = null,
                syncStatus = BookmarkStatus.SYNC_LOCAL_ONLY,
                syncError = null,
                createdAt = System.currentTimeMillis(),
            )
            bookmarkDao.insert(bookmark)
            addBookmarkToCollections(
                bookmarkId = bookmark.id,
                collectionIds = listOf(DEFAULT_COLLECTION_ID),
            )
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
            sortOrder = System.currentTimeMillis(),
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
            val affectedBookmarkIds = bookmarkDao.getBookmarkIdsInCollection(collectionId)
            for (bookmarkId in affectedBookmarkIds) {
                if (bookmarkDao.countBookmarkCollections(bookmarkId) <= 1) {
                    bookmarkDao.insertBookmarkCollection(
                        BookmarkCollection(
                            bookmarkId = bookmarkId,
                            collectionId = DEFAULT_COLLECTION_ID,
                            syncStatus = BookmarkStatus.SYNC_LOCAL_ONLY,
                            syncError = null,
                            createdAt = System.currentTimeMillis(),
                        ),
                    )
                }
                bookmarkDao.insertPendingDeletion(
                    PendingDeletion.bookmarkCollection(bookmarkId, collectionId),
                )
            }
            bookmarkDao.insertPendingDeletion(
                PendingDeletion.collection(collectionId),
            )
            bookmarkDao.deleteCollection(collectionId)
        }
        return true
    }

    suspend fun addBookmarkToCollections(
        bookmarkId: String,
        collectionIds: List<String>,
    ) {
        val now = System.currentTimeMillis()
        collectionIds.distinct().forEach { collectionId ->
            val inserted = bookmarkDao.insertBookmarkCollection(
                BookmarkCollection(
                    bookmarkId = bookmarkId,
                    collectionId = collectionId,
                    syncStatus = BookmarkStatus.SYNC_LOCAL_ONLY,
                    syncError = null,
                    createdAt = now,
                ),
            )
            if (inserted == -1L) {
                bookmarkDao.updateBookmarkCollectionSyncStatus(
                    bookmarkId = bookmarkId,
                    collectionId = collectionId,
                    status = BookmarkStatus.SYNC_LOCAL_ONLY,
                )
            }
        }
        bookmarkDao.updateBookmarkSyncState(
            bookmarkId = bookmarkId,
            status = BookmarkStatus.SYNC_LOCAL_ONLY,
            error = null,
        )
    }

    suspend fun setBookmarkCollections(
        bookmarkId: String,
        collectionIds: List<String>,
    ) {
        val targetIds = collectionIds
            .distinct()
            .ifEmpty { listOf(DEFAULT_COLLECTION_ID) }

        database.withTransaction {
            val existingIds = bookmarkDao.getBookmarkCollectionIds(bookmarkId).toSet()
            val targetIdSet = targetIds.toSet()
            val removedIds = existingIds - targetIdSet

            addBookmarkToCollections(bookmarkId, targetIds)
            if (targetIds.isNotEmpty()) {
                bookmarkDao.deleteBookmarkCollectionsExcept(bookmarkId, targetIds)
            }
            removedIds.forEach { collectionId ->
                bookmarkDao.insertPendingDeletion(
                    PendingDeletion.bookmarkCollection(bookmarkId, collectionId),
                )
            }
        }
    }

    suspend fun moveBookmarkFromCollection(
        bookmarkId: String,
        fromCollectionId: String?,
        toCollectionIds: List<String>,
    ) {
        database.withTransaction {
            toCollectionIds.ifEmpty { listOf(DEFAULT_COLLECTION_ID) }
                .let { addBookmarkToCollections(bookmarkId, it) }
            fromCollectionId?.let { collectionId ->
                if (collectionId !in toCollectionIds) {
                    removeBookmarkFromCollection(bookmarkId, collectionId)
                }
            }
        }
    }

    suspend fun removeBookmarkFromCollection(
        bookmarkId: String,
        collectionId: String,
    ) {
        database.withTransaction {
            bookmarkDao.deleteBookmarkCollection(bookmarkId, collectionId)
            bookmarkDao.insertPendingDeletion(
                PendingDeletion.bookmarkCollection(bookmarkId, collectionId),
            )
            if (bookmarkDao.countBookmarkCollections(bookmarkId) == 0) {
                deleteBookmarkCompletely(bookmarkId)
            }
        }
    }

    suspend fun deleteBookmarkCompletely(bookmarkId: String) {
        database.withTransaction {
            bookmarkDao.deleteBookmarkCollections(bookmarkId)
            bookmarkDao.deleteBookmark(bookmarkId)
            bookmarkDao.insertPendingDeletion(PendingDeletion.bookmark(bookmarkId))
        }
    }

    suspend fun getBookmarkCollectionIds(bookmarkId: String): List<String> {
        return bookmarkDao.getBookmarkCollectionIds(bookmarkId)
    }

    suspend fun findPendingParseIds(): List<String> = bookmarkDao.findPendingParseIds()

    suspend fun reorderCollections(collectionIds: List<String>) {
        database.withTransaction {
            collectionIds.forEachIndexed { index, collectionId ->
                bookmarkDao.updateCollectionSortOrder(
                    collectionId = collectionId,
                    sortOrder = index.toLong(),
                )
            }
        }
    }

    companion object {
        const val DEFAULT_COLLECTION_ID = "default"
        const val DEFAULT_COLLECTION_NAME = "默认收藏夹"
    }
}

sealed interface SaveResult {
    data class Saved(val bookmarkId: String) : SaveResult
    data class AlreadySaved(val bookmarkId: String) : SaveResult
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
