package com.lyn.mazhu.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.room.withTransaction
import androidx.work.WorkerParameters
import com.lyn.mazhu.MazhuApplication
import com.lyn.mazhu.data.BookmarkStatus
import com.lyn.mazhu.data.PendingDeletion

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val application = applicationContext as MazhuApplication
        if (!application.supabaseConfigStore.current().isConfigured) {
            return Result.success()
        }
        val session = application.authRepository.getValidSession()
            ?: return Result.success()
        val dao = application.database.bookmarkDao()
        val client = application.supabaseDataClient

        return try {
            dao.getPendingCollections().forEach { collection ->
                dao.updateCollectionSyncState(
                    collectionId = collection.id,
                    status = BookmarkStatus.SYNC_SYNCING,
                    error = null,
                )
                client.upsertCollection(session, collection)
                dao.updateCollectionSyncState(
                    collectionId = collection.id,
                    status = BookmarkStatus.SYNC_SYNCED,
                    error = null,
                )
            }

            dao.getPendingBookmarks().forEach { bookmark ->
                dao.updateBookmarkSyncState(
                    bookmarkId = bookmark.id,
                    status = BookmarkStatus.SYNC_SYNCING,
                    error = null,
                )
                client.upsertBookmark(session, bookmark)
                dao.updateBookmarkSyncState(
                    bookmarkId = bookmark.id,
                    status = BookmarkStatus.SYNC_SYNCED,
                    error = null,
                )
            }

            dao.getPendingBookmarkCollections().forEach { relation ->
                dao.updateBookmarkCollectionSyncState(
                    bookmarkId = relation.bookmarkId,
                    collectionId = relation.collectionId,
                    status = BookmarkStatus.SYNC_SYNCING,
                    error = null,
                )
                client.upsertBookmarkCollection(session, relation)
                dao.updateBookmarkCollectionSyncState(
                    bookmarkId = relation.bookmarkId,
                    collectionId = relation.collectionId,
                    status = BookmarkStatus.SYNC_SYNCED,
                    error = null,
                )
            }

            dao.getPendingDeletions().forEach { deletion ->
                if (deletion.entityType == PendingDeletion.TYPE_COLLECTION) {
                    client.deleteCollection(session, deletion.entityId)
                }
                if (deletion.entityType == PendingDeletion.TYPE_BOOKMARK) {
                    client.deleteBookmark(session, deletion.entityId)
                }
                if (deletion.entityType == PendingDeletion.TYPE_BOOKMARK_COLLECTION) {
                    val parts = deletion.entityId.split(":", limit = 2)
                    if (parts.size == 2) {
                        client.deleteBookmarkCollection(
                            session = session,
                            bookmarkId = parts[0],
                            collectionId = parts[1],
                        )
                    }
                }
                dao.deletePendingDeletion(deletion.key)
            }

            val remoteCollections = client.listCollections(session)
            val remoteBookmarks = client.listBookmarks(session)
            val remoteRelations = client.listBookmarkCollections(session)
            application.database.withTransaction {
                if (remoteCollections.isNotEmpty()) {
                    dao.upsertCollections(remoteCollections)
                }
                if (remoteBookmarks.isNotEmpty()) {
                    dao.upsertBookmarks(remoteBookmarks)
                }
                if (remoteRelations.isNotEmpty()) {
                    dao.upsertBookmarkCollections(remoteRelations)
                }
            }
            Result.success()
        } catch (error: Exception) {
            resetSyncingRows(error.message)
            if (runAttemptCount < MAX_AUTOMATIC_RETRIES) {
                Result.retry()
            } else {
                Result.success()
            }
        }
    }

    private suspend fun resetSyncingRows(errorMessage: String?) {
        val dao = (applicationContext as MazhuApplication).database.bookmarkDao()
        val error = errorMessage?.take(500)
        dao.getPendingCollections()
            .filter { it.syncStatus == BookmarkStatus.SYNC_SYNCING }
            .forEach { collection ->
                dao.updateCollectionSyncState(
                    collectionId = collection.id,
                    status = BookmarkStatus.SYNC_LOCAL_ONLY,
                    error = error,
                )
            }
        dao.getPendingBookmarks()
            .filter { it.syncStatus == BookmarkStatus.SYNC_SYNCING }
            .forEach { bookmark ->
                dao.updateBookmarkSyncState(
                    bookmarkId = bookmark.id,
                    status = BookmarkStatus.SYNC_LOCAL_ONLY,
                    error = error,
                )
            }
        dao.getPendingBookmarkCollections()
            .filter { it.syncStatus == BookmarkStatus.SYNC_SYNCING }
            .forEach { relation ->
                dao.updateBookmarkCollectionSyncState(
                    bookmarkId = relation.bookmarkId,
                    collectionId = relation.collectionId,
                    status = BookmarkStatus.SYNC_LOCAL_ONLY,
                    error = error,
                )
            }
    }

    private companion object {
        const val MAX_AUTOMATIC_RETRIES = 3
    }
}
