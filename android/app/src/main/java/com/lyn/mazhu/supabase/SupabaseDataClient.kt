package com.lyn.mazhu.supabase

import android.net.Uri
import com.lyn.mazhu.data.Bookmark
import com.lyn.mazhu.data.Collection
import java.time.Instant
import org.json.JSONArray
import org.json.JSONObject

class SupabaseDataClient(
    private val httpClient: SupabaseHttpClient,
) {
    suspend fun upsertCollection(
        session: SupabaseSession,
        collection: Collection,
    ) {
        val body = JSONObject()
            .put("user_id", session.userId)
            .put("id", collection.id)
            .put("name", collection.name)
            .put("created_at", Instant.ofEpochMilli(collection.createdAt).toString())
        httpClient.request(
            path = "/rest/v1/collections?on_conflict=user_id,id",
            method = "POST",
            accessToken = session.accessToken,
            body = JSONArray().put(body).toString(),
            prefer = "resolution=merge-duplicates,return=minimal",
        )
    }

    suspend fun upsertBookmark(
        session: SupabaseSession,
        bookmark: Bookmark,
    ) {
        val body = JSONObject()
            .put("user_id", session.userId)
            .put("id", bookmark.id)
            .put("original_url", bookmark.originalUrl)
            .put("normalized_url", bookmark.normalizedUrl)
            .put("title", bookmark.title)
            .putNullable("account_name", bookmark.accountName)
            .putNullable("cover_url", bookmark.coverUrl)
            .putNullable(
                "published_at",
                bookmark.publishedAt?.let { Instant.ofEpochMilli(it).toString() },
            )
            .putNullable("content_text", bookmark.contentText)
            .put("collection_id", bookmark.collectionId)
            .put("parse_status", bookmark.parseStatus)
            .putNullable("parse_error", bookmark.parseError)
            .put("created_at", Instant.ofEpochMilli(bookmark.createdAt).toString())
        httpClient.request(
            path = "/rest/v1/bookmarks?on_conflict=user_id,id",
            method = "POST",
            accessToken = session.accessToken,
            body = JSONArray().put(body).toString(),
            prefer = "resolution=merge-duplicates,return=minimal",
        )
    }

    suspend fun deleteCollection(
        session: SupabaseSession,
        collectionId: String,
    ) {
        val userId = Uri.encode(session.userId)
        val id = Uri.encode(collectionId)
        httpClient.request(
            path = "/rest/v1/collections?user_id=eq.$userId&id=eq.$id",
            method = "DELETE",
            accessToken = session.accessToken,
            prefer = "return=minimal",
        )
    }

    private fun JSONObject.putNullable(
        key: String,
        value: Any?,
    ): JSONObject = put(key, value ?: JSONObject.NULL)
}
