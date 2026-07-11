package com.lyn.mazhu.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lyn.mazhu.MazhuApplication
import com.lyn.mazhu.data.BookmarkStatus
import com.lyn.mazhu.parser.ArticleParseException
import com.lyn.mazhu.parser.WechatArticleParser
import java.io.IOException

class ParseArticleWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val bookmarkId = inputData.getString(KEY_BOOKMARK_ID)
            ?: return Result.failure()
        val dao = (applicationContext as MazhuApplication).database.bookmarkDao()
        val bookmark = dao.findById(bookmarkId)
            ?: return Result.success()

        if (bookmark.parseStatus == BookmarkStatus.PARSE_SUCCESS) {
            return Result.success()
        }

        dao.updateParseState(
            bookmarkId = bookmarkId,
            status = BookmarkStatus.PARSE_PROCESSING,
            error = null,
        )

        return try {
            val article = WechatArticleParser().parse(bookmark.originalUrl)
            dao.updateParsedContent(
                bookmarkId = bookmarkId,
                title = article.title,
                accountName = article.accountName,
                coverUrl = article.coverUrl,
                publishedAt = article.publishedAt,
                contentText = article.contentText,
            )
            SyncWorkScheduler.enqueue(applicationContext)
            Result.success()
        } catch (error: IOException) {
            if (runAttemptCount < MAX_NETWORK_RETRIES) {
                dao.updateParseState(
                    bookmarkId = bookmarkId,
                    status = BookmarkStatus.PARSE_PENDING,
                    error = error.message,
                )
                Result.retry()
            } else {
                markFailed(dao = dao, bookmarkId = bookmarkId, error = error)
            }
        } catch (error: ArticleParseException) {
            markFailed(dao = dao, bookmarkId = bookmarkId, error = error)
        } catch (error: Exception) {
            markFailed(dao = dao, bookmarkId = bookmarkId, error = error)
        }
    }

    private suspend fun markFailed(
        dao: com.lyn.mazhu.data.BookmarkDao,
        bookmarkId: String,
        error: Exception,
    ): Result {
        dao.updateParseState(
            bookmarkId = bookmarkId,
            status = BookmarkStatus.PARSE_FAILED,
            error = error.message ?: error.javaClass.simpleName,
        )
        return Result.success()
    }

    companion object {
        const val KEY_BOOKMARK_ID = "bookmark_id"
        private const val MAX_NETWORK_RETRIES = 4
    }
}
