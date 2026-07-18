package com.lyn.mazhu.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Bookmark>>

    @Query("SELECT * FROM bookmark_collections")
    fun observeBookmarkCollections(): Flow<List<BookmarkCollection>>

    @Query(
        """
        SELECT bookmarks.* FROM bookmarks
        INNER JOIN bookmark_collections
            ON bookmark_collections.bookmarkId = bookmarks.id
        WHERE bookmark_collections.collectionId = :collectionId
        ORDER BY bookmarks.createdAt DESC
        """,
    )
    fun observeByCollection(collectionId: String): Flow<List<Bookmark>>

    @Query(
        """
        SELECT * FROM bookmarks
        WHERE title LIKE '%' || :query || '%'
            OR originalUrl LIKE '%' || :query || '%'
            OR accountName LIKE '%' || :query || '%'
            OR contentText LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
        """,
    )
    fun searchBookmarks(query: String): Flow<List<Bookmark>>

    @Query(
        """
        SELECT
            collections.id AS id,
            collections.name AS name,
            collections.sortOrder AS sortOrder,
            collections.createdAt AS createdAt,
            COUNT(DISTINCT bookmark_collections.bookmarkId) AS articleCount,
            COALESCE(
                SUM(
                    CASE
                        WHEN bookmark_collections.syncStatus != 'synced' THEN 1
                        ELSE 0
                    END
                ),
                0
            ) AS unsyncedCount
        FROM collections
        LEFT JOIN bookmark_collections
            ON bookmark_collections.collectionId = collections.id
        GROUP BY collections.id, collections.name, collections.sortOrder, collections.createdAt
        ORDER BY
            CASE WHEN collections.id = 'default' THEN 0 ELSE 1 END,
            collections.sortOrder ASC,
            collections.createdAt ASC
        """,
    )
    fun observeCollections(): Flow<List<CollectionSummary>>

    @Query(
        """
        SELECT * FROM collections
        ORDER BY
            CASE WHEN id = 'default' THEN 0 ELSE 1 END,
            sortOrder ASC,
            createdAt ASC
        """,
    )
    suspend fun getCollections(): List<Collection>

    @Query("SELECT * FROM collections WHERE syncStatus != 'synced' ORDER BY createdAt ASC")
    suspend fun getPendingCollections(): List<Collection>

    @Query("SELECT * FROM bookmarks WHERE syncStatus != 'synced' ORDER BY createdAt ASC")
    suspend fun getPendingBookmarks(): List<Bookmark>

    @Query(
        """
        SELECT * FROM bookmark_collections
        WHERE syncStatus != 'synced'
        ORDER BY createdAt ASC
        """,
    )
    suspend fun getPendingBookmarkCollections(): List<BookmarkCollection>

    @Query("SELECT * FROM pending_deletions ORDER BY createdAt ASC")
    suspend fun getPendingDeletions(): List<PendingDeletion>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCollection(collection: Collection): Long

    @Upsert
    suspend fun upsertCollections(collections: List<Collection>)

    @Query(
        """
        UPDATE collections
        SET name = :name,
            syncStatus = 'local_only',
            syncError = NULL
        WHERE id = :collectionId
        """,
    )
    suspend fun renameCollection(collectionId: String, name: String)

    @Query("DELETE FROM collections WHERE id = :collectionId")
    suspend fun deleteCollection(collectionId: String)

    @Query("UPDATE collections SET sortOrder = :sortOrder WHERE id = :collectionId")
    suspend fun updateCollectionSortOrder(collectionId: String, sortOrder: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBookmarkCollection(relation: BookmarkCollection): Long

    @Upsert
    suspend fun upsertBookmarkCollections(relations: List<BookmarkCollection>)

    @Query(
        """
        UPDATE bookmark_collections
        SET syncStatus = :status,
            syncError = NULL
        WHERE bookmarkId = :bookmarkId AND collectionId = :collectionId
        """,
    )
    suspend fun updateBookmarkCollectionSyncStatus(
        bookmarkId: String,
        collectionId: String,
        status: String,
    )

    @Query(
        """
        UPDATE bookmark_collections
        SET syncStatus = :status,
            syncError = :error
        WHERE bookmarkId = :bookmarkId AND collectionId = :collectionId
        """,
    )
    suspend fun updateBookmarkCollectionSyncState(
        bookmarkId: String,
        collectionId: String,
        status: String,
        error: String?,
    )

    @Query("DELETE FROM bookmark_collections WHERE bookmarkId = :bookmarkId AND collectionId = :collectionId")
    suspend fun deleteBookmarkCollection(bookmarkId: String, collectionId: String)

    @Query("DELETE FROM bookmark_collections WHERE bookmarkId = :bookmarkId AND collectionId NOT IN (:collectionIds)")
    suspend fun deleteBookmarkCollectionsExcept(bookmarkId: String, collectionIds: List<String>)

    @Query("DELETE FROM bookmark_collections WHERE bookmarkId = :bookmarkId")
    suspend fun deleteBookmarkCollections(bookmarkId: String)

    @Query("SELECT COUNT(*) FROM bookmark_collections WHERE bookmarkId = :bookmarkId")
    suspend fun countBookmarkCollections(bookmarkId: String): Int

    @Query("SELECT collectionId FROM bookmark_collections WHERE bookmarkId = :bookmarkId")
    suspend fun getBookmarkCollectionIds(bookmarkId: String): List<String>

    @Query("SELECT bookmarkId FROM bookmark_collections WHERE collectionId = :collectionId")
    suspend fun getBookmarkIdsInCollection(collectionId: String): List<String>

    @Query("DELETE FROM bookmarks WHERE id = :bookmarkId")
    suspend fun deleteBookmark(bookmarkId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingDeletion(deletion: PendingDeletion)

    @Query("DELETE FROM pending_deletions WHERE key = :key")
    suspend fun deletePendingDeletion(key: String)

    @Query(
        """
        UPDATE collections
        SET syncStatus = :status,
            syncError = :error
        WHERE id = :collectionId
        """,
    )
    suspend fun updateCollectionSyncState(
        collectionId: String,
        status: String,
        error: String?,
    )

    @Query(
        """
        UPDATE bookmarks
        SET syncStatus = :status,
            syncError = :error
        WHERE id = :bookmarkId
        """,
    )
    suspend fun updateBookmarkSyncState(
        bookmarkId: String,
        status: String,
        error: String?,
    )

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(bookmark: Bookmark): Long

    @Upsert
    suspend fun upsertBookmarks(bookmarks: List<Bookmark>)

    @Query("SELECT * FROM bookmarks WHERE normalizedUrl = :normalizedUrl LIMIT 1")
    suspend fun findByNormalizedUrl(normalizedUrl: String): Bookmark?

    @Query("SELECT * FROM bookmarks WHERE id = :bookmarkId LIMIT 1")
    suspend fun findById(bookmarkId: String): Bookmark?

    @Query(
        """
        SELECT id FROM bookmarks
        WHERE parseStatus IN ('pending', 'processing')
        ORDER BY createdAt ASC
        """,
    )
    suspend fun findPendingParseIds(): List<String>

    @Query(
        """
        UPDATE bookmarks
        SET parseStatus = :status,
            parseError = :error
        WHERE id = :bookmarkId
        """,
    )
    suspend fun updateParseState(
        bookmarkId: String,
        status: String,
        error: String?,
    )

    @Query(
        """
        UPDATE bookmarks
        SET title = :title,
            accountName = :accountName,
            coverUrl = :coverUrl,
            publishedAt = :publishedAt,
            contentText = :contentText,
            parseStatus = 'success',
            parseError = NULL,
            syncStatus = 'local_only',
            syncError = NULL
        WHERE id = :bookmarkId
        """,
    )
    suspend fun updateParsedContent(
        bookmarkId: String,
        title: String,
        accountName: String?,
        coverUrl: String?,
        publishedAt: Long?,
        contentText: String,
    )
}
