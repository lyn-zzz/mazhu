package com.lyn.mazhu

import android.app.Application
import com.lyn.mazhu.data.BookmarkDatabase
import com.lyn.mazhu.data.BookmarkRepository
import com.lyn.mazhu.supabase.AuthRepository
import com.lyn.mazhu.supabase.SessionStore
import com.lyn.mazhu.supabase.SupabaseDataClient
import com.lyn.mazhu.supabase.SupabaseHttpClient

class MazhuApplication : Application() {
    val database by lazy { BookmarkDatabase.create(this) }
    private val supabaseHttpClient by lazy { SupabaseHttpClient() }
    val authRepository by lazy {
        AuthRepository(
            httpClient = supabaseHttpClient,
            sessionStore = SessionStore(this),
        )
    }
    val supabaseDataClient by lazy {
        SupabaseDataClient(supabaseHttpClient)
    }
    val bookmarkRepository by lazy {
        BookmarkRepository(
            database = database,
            bookmarkDao = database.bookmarkDao(),
        )
    }
}
