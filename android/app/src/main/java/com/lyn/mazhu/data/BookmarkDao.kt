package com.lyn.mazhu.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Bookmark>>

    @Query("SELECT * FROM bookmarks WHERE collectionId = :collectionId ORDER BY createdAt DESC")
    fun observeByCollection(collectionId: String): Flow<List<Bookmark>>

    @Query(
        """
        SELECT
            collections.id AS id,
            collections.name AS name,
            collections.createdAt AS createdAt,
            COUNT(bookmarks.id) AS articleCount,
            COALESCE(
                SUM(CASE WHEN bookmarks.syncStatus != 'synced' THEN 1 ELSE 0 END),
                0
            ) AS unsyncedCount
        FROM collections
        LEFT JOIN bookmarks ON bookmarks.collectionId = collections.id
        GROUP BY collections.id, collections.name, collections.createdAt
        ORDER BY
            CASE WHEN collections.id = 'default' THEN 0 ELSE 1 END,
            collections.createdAt ASC
        """,
    )
    fun observeCollections(): Flow<List<CollectionSummary>>

    @Query(
        """
        SELECT * FROM collections
        ORDER BY
            CASE WHEN id = 'default' THEN 0 ELSE 1 END,
            createdAt ASC
        """,
    )
    suspend fun getCollections(): List<Collection>

    @Query("SELECT * FROM collections WHERE syncStatus != 'synced' ORDER BY createdAt ASC")
    suspend fun getPendingCollections(): List<Collection>

    @Query("SELECT * FROM bookmarks WHERE syncStatus != 'synced' ORDER BY createdAt ASC")
    suspend fun getPendingBookmarks(): List<Bookmark>

    @Query("SELECT * FROM pending_deletions ORDER BY createdAt ASC")
    suspend fun getPendingDeletions(): List<PendingDeletion>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCollection(collection: Collection): Long

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

    @Query(
        """
        UPDATE bookmarks
        SET collectionId = :collectionId,
            syncStatus = 'local_only',
            syncError = NULL
        WHERE id = :bookmarkId
        """,
    )
    suspend fun moveBookmark(bookmarkId: String, collectionId: String)

    @Query(
        """
        UPDATE bookmarks
        SET collectionId = :targetCollectionId,
            syncStatus = 'local_only',
            syncError = NULL
        WHERE collectionId = :sourceCollectionId
        """,
    )
    suspend fun moveAllBookmarks(
        sourceCollectionId: String,
        targetCollectionId: String,
    )

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
