package com.lyn.mazhu.supabase

import android.net.Uri
import com.lyn.mazhu.data.Bookmark
import com.lyn.mazhu.data.BookmarkCollection
import com.lyn.mazhu.data.BookmarkStatus
import com.lyn.mazhu.data.Collection
import java.time.Instant
import java.time.OffsetDateTime
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

    suspend fun upsertBookmarkCollection(
        session: SupabaseSession,
        relation: BookmarkCollection,
    ) {
        val body = JSONObject()
            .put("user_id", session.userId)
            .put("bookmark_id", relation.bookmarkId)
            .put("collection_id", relation.collectionId)
            .put("created_at", Instant.ofEpochMilli(relation.createdAt).toString())
        httpClient.request(
            path = "/rest/v1/bookmark_collections?on_conflict=user_id,bookmark_id,collection_id",
            method = "POST",
            accessToken = session.accessToken,
            body = JSONArray().put(body).toString(),
            prefer = "resolution=merge-duplicates,return=minimal",
        )
    }

    suspend fun listCollections(session: SupabaseSession): List<Collection> {
        val response = httpClient.request(
            path = "/rest/v1/collections?select=id,name,created_at&order=created_at.asc&limit=10000",
            method = "GET",
            accessToken = session.accessToken,
        )
        return JSONArray(response.body).mapObjectsTyped { json ->
            val createdAt = json.optTimestampMillis("created_at")
            Collection(
                id = json.getString("id"),
                name = json.getString("name"),
                sortOrder = if (json.getString("id") == "default") 0L else createdAt,
                syncStatus = BookmarkStatus.SYNC_SYNCED,
                syncError = null,
                createdAt = createdAt,
            )
        }
    }

    suspend fun listBookmarks(session: SupabaseSession): List<Bookmark> {
        val response = httpClient.request(
            path = "/rest/v1/bookmarks?select=id,original_url,normalized_url,title,account_name,cover_url,published_at,content_text,collection_id,parse_status,parse_error,created_at&order=created_at.asc&limit=10000",
            method = "GET",
            accessToken = session.accessToken,
        )
        return JSONArray(response.body).mapObjectsTyped { json ->
            Bookmark(
                id = json.getString("id"),
                originalUrl = json.getString("original_url"),
                normalizedUrl = json.getString("normalized_url"),
                title = json.getString("title"),
                accountName = json.optNullableString("account_name"),
                coverUrl = json.optNullableString("cover_url"),
                publishedAt = json.optTimestampMillisOrNull("published_at"),
                contentText = json.optNullableString("content_text"),
                collectionId = json.optNullableString("collection_id") ?: "default",
                parseStatus = json.optNullableString("parse_status")
                    ?: BookmarkStatus.PARSE_PENDING,
                parseError = json.optNullableString("parse_error"),
                syncStatus = BookmarkStatus.SYNC_SYNCED,
                syncError = null,
                createdAt = json.optTimestampMillis("created_at"),
            )
        }
    }

    suspend fun listBookmarkCollections(session: SupabaseSession): List<BookmarkCollection> {
        val response = httpClient.request(
            path = "/rest/v1/bookmark_collections?select=bookmark_id,collection_id,created_at&order=created_at.asc&limit=10000",
            method = "GET",
            accessToken = session.accessToken,
        )
        return JSONArray(response.body).mapObjectsTyped { json ->
            BookmarkCollection(
                bookmarkId = json.getString("bookmark_id"),
                collectionId = json.getString("collection_id"),
                syncStatus = BookmarkStatus.SYNC_SYNCED,
                syncError = null,
                createdAt = json.optTimestampMillis("created_at"),
            )
        }
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

    suspend fun deleteBookmark(
        session: SupabaseSession,
        bookmarkId: String,
    ) {
        val userId = Uri.encode(session.userId)
        val id = Uri.encode(bookmarkId)
        httpClient.request(
            path = "/rest/v1/bookmarks?user_id=eq.$userId&id=eq.$id",
            method = "DELETE",
            accessToken = session.accessToken,
            prefer = "return=minimal",
        )
    }

    suspend fun deleteBookmarkCollection(
        session: SupabaseSession,
        bookmarkId: String,
        collectionId: String,
    ) {
        val userId = Uri.encode(session.userId)
        val encodedBookmarkId = Uri.encode(bookmarkId)
        val encodedCollectionId = Uri.encode(collectionId)
        httpClient.request(
            path = "/rest/v1/bookmark_collections?user_id=eq.$userId&bookmark_id=eq.$encodedBookmarkId&collection_id=eq.$encodedCollectionId",
            method = "DELETE",
            accessToken = session.accessToken,
            prefer = "return=minimal",
        )
    }

    private fun <T> JSONArray.mapObjectsTyped(
        transform: (JSONObject) -> T,
    ): List<T> = List(length()) { index -> transform(getJSONObject(index)) }

    private fun JSONObject.optNullableString(key: String): String? {
        if (!has(key) || isNull(key)) {
            return null
        }
        return optString(key).takeIf(String::isNotBlank)
    }

    private fun JSONObject.optTimestampMillis(key: String): Long =
        optTimestampMillisOrNull(key) ?: System.currentTimeMillis()

    private fun JSONObject.optTimestampMillisOrNull(key: String): Long? {
        val value = optNullableString(key) ?: return null
        return runCatching { Instant.parse(value).toEpochMilli() }
            .getOrElse {
                runCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }
                    .getOrNull()
            }
    }

    private fun JSONObject.putNullable(
        key: String,
        value: Any?,
    ): JSONObject = put(key, value ?: JSONObject.NULL)
}
