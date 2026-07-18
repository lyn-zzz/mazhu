package com.lyn.mazhu

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lyn.mazhu.data.Bookmark
import com.lyn.mazhu.data.BookmarkCollection
import com.lyn.mazhu.data.CollectionSummary
import com.lyn.mazhu.data.CreateCollectionResult
import com.lyn.mazhu.data.RenameCollectionResult
import com.lyn.mazhu.supabase.AuthResult
import com.lyn.mazhu.supabase.SupabaseConfig
import com.lyn.mazhu.supabase.SupabaseSession
import com.lyn.mazhu.worker.ParseWorkScheduler
import com.lyn.mazhu.worker.SyncWorkScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as MazhuApplication
    private val repository = (application as MazhuApplication).bookmarkRepository
    private val authRepository = app.authRepository
    private val supabaseConfigStore = app.supabaseConfigStore

    val session: StateFlow<SupabaseSession?> = authRepository.session
    val supabaseConfig: StateFlow<SupabaseConfig> = supabaseConfigStore.config

    val collections: StateFlow<List<CollectionSummary>> = repository.observeCollections()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val bookmarks: StateFlow<List<Bookmark>> = repository.observeBookmarks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val bookmarkCollections: StateFlow<List<BookmarkCollection>> =
        repository.observeBookmarkCollections()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    fun createCollection(
        name: String,
        onResult: (CreateCollectionResult) -> Unit,
    ) {
        viewModelScope.launch {
            onResult(repository.createCollection(name))
            SyncWorkScheduler.enqueue(app)
        }
    }

    fun saveSharedText(
        sharedText: String,
        onResult: (com.lyn.mazhu.data.SaveResult) -> Unit,
    ) {
        viewModelScope.launch {
            val result = repository.saveSharedText(sharedText)
            if (result is com.lyn.mazhu.data.SaveResult.Saved) {
                ParseWorkScheduler.enqueue(app, result.bookmarkId)
                SyncWorkScheduler.enqueue(app)
            }
            onResult(result)
        }
    }

    fun renameCollection(
        collectionId: String,
        name: String,
        onResult: (RenameCollectionResult) -> Unit,
    ) {
        viewModelScope.launch {
            onResult(repository.renameCollection(collectionId, name))
            SyncWorkScheduler.enqueue(app)
        }
    }

    fun deleteCollection(
        collectionId: String,
        onComplete: () -> Unit,
    ) {
        viewModelScope.launch {
            repository.deleteCollection(collectionId)
            SyncWorkScheduler.enqueue(app)
            onComplete()
        }
    }

    fun reorderCollections(collectionIds: List<String>) {
        viewModelScope.launch {
            repository.reorderCollections(collectionIds)
        }
    }

    fun copyBookmarkToCollections(
        bookmarkId: String,
        collectionIds: List<String>,
    ) {
        viewModelScope.launch {
            repository.addBookmarkToCollections(bookmarkId, collectionIds)
            SyncWorkScheduler.enqueue(app)
        }
    }

    fun copyBookmarksToCollections(
        bookmarkIds: List<String>,
        collectionIds: List<String>,
    ) {
        viewModelScope.launch {
            bookmarkIds.distinct().forEach { bookmarkId ->
                repository.addBookmarkToCollections(bookmarkId, collectionIds)
            }
            SyncWorkScheduler.enqueue(app)
        }
    }

    fun setBookmarkCollections(
        bookmarkId: String,
        collectionIds: List<String>,
    ) {
        viewModelScope.launch {
            repository.setBookmarkCollections(bookmarkId, collectionIds)
            SyncWorkScheduler.enqueue(app)
        }
    }

    fun moveBookmarkFromCollection(
        bookmarkId: String,
        fromCollectionId: String?,
        toCollectionIds: List<String>,
    ) {
        viewModelScope.launch {
            repository.moveBookmarkFromCollection(
                bookmarkId = bookmarkId,
                fromCollectionId = fromCollectionId,
                toCollectionIds = toCollectionIds,
            )
            SyncWorkScheduler.enqueue(app)
        }
    }

    fun moveBookmarksFromCollection(
        bookmarkIds: List<String>,
        fromCollectionId: String?,
        toCollectionIds: List<String>,
    ) {
        viewModelScope.launch {
            bookmarkIds.distinct().forEach { bookmarkId ->
                repository.moveBookmarkFromCollection(
                    bookmarkId = bookmarkId,
                    fromCollectionId = fromCollectionId,
                    toCollectionIds = toCollectionIds,
                )
            }
            SyncWorkScheduler.enqueue(app)
        }
    }

    fun removeBookmarkFromCollection(
        bookmarkId: String,
        collectionId: String?,
    ) {
        viewModelScope.launch {
            if (collectionId == null) {
                repository.deleteBookmarkCompletely(bookmarkId)
            } else {
                repository.removeBookmarkFromCollection(bookmarkId, collectionId)
            }
            SyncWorkScheduler.enqueue(app)
        }
    }

    fun removeBookmarksFromCollection(
        bookmarkIds: List<String>,
        collectionId: String?,
    ) {
        viewModelScope.launch {
            bookmarkIds.distinct().forEach { bookmarkId ->
                if (collectionId == null) {
                    repository.deleteBookmarkCompletely(bookmarkId)
                } else {
                    repository.removeBookmarkFromCollection(bookmarkId, collectionId)
                }
            }
            SyncWorkScheduler.enqueue(app)
        }
    }

    fun signIn(
        email: String,
        password: String,
        onResult: (AuthResult) -> Unit,
    ) {
        viewModelScope.launch {
            val result = authRepository.signIn(email, password)
            if (result is AuthResult.Authenticated) {
                SyncWorkScheduler.enqueue(app)
            }
            onResult(result)
        }
    }

    fun signUp(
        email: String,
        password: String,
        onResult: (AuthResult) -> Unit,
    ) {
        viewModelScope.launch {
            val result = authRepository.signUp(email, password)
            if (result is AuthResult.Authenticated) {
                SyncWorkScheduler.enqueue(app)
            }
            onResult(result)
        }
    }

    fun signOut() {
        authRepository.signOut()
    }

    fun saveSupabaseConfig(
        url: String,
        publishableKey: String,
        onComplete: () -> Unit,
    ) {
        supabaseConfigStore.save(
            SupabaseConfig(
                url = url,
                publishableKey = publishableKey,
            ),
        )
        authRepository.signOut()
        onComplete()
    }

    fun resetSupabaseConfig() {
        supabaseConfigStore.reset()
        authRepository.signOut()
    }

    fun syncNow(onStatus: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val hasPendingSync = repository.hasPendingVisibleSync()
            SyncWorkScheduler.enqueue(app)
            onStatus(hasPendingSync)
        }
    }
}
