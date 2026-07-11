package com.lyn.mazhu.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object ParseWorkScheduler {
    fun enqueue(
        context: Context,
        bookmarkId: String,
    ) {
        val request = OneTimeWorkRequestBuilder<ParseArticleWorker>()
            .setInputData(
                Data.Builder()
                    .putString(ParseArticleWorker.KEY_BOOKMARK_ID, bookmarkId)
                    .build(),
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30,
                TimeUnit.SECONDS,
            )
            .build()

        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(
                "parse-article-$bookmarkId",
                ExistingWorkPolicy.KEEP,
                request,
            )
    }
}

