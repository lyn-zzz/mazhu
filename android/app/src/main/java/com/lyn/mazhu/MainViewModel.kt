package com.lyn.mazhu

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lyn.mazhu.data.Bookmark
import com.lyn.mazhu.data.CollectionSummary
import com.lyn.mazhu.data.CreateCollectionResult
import com.lyn.mazhu.data.RenameCollectionResult
import com.lyn.mazhu.supabase.AuthResult
import com.lyn.mazhu.supabase.SupabaseSession
import com.lyn.mazhu.worker.SyncWorkScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as MazhuApplication
    private val repository = (application as MazhuApplication).bookmarkRepository
    private val authRepository = app.authRepository

    val session: StateFlow<SupabaseSession?> = authRepository.session

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

    fun createCollection(
        name: String,
        onResult: (CreateCollectionResult) -> Unit,
    ) {
        viewModelScope.launch {
            onResult(repository.createCollection(name))
            SyncWorkScheduler.enqueue(app)
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

    fun moveBookmark(
        bookmarkId: String,
        collectionId: String,
    ) {
        viewModelScope.launch {
            repository.moveBookmark(bookmarkId, collectionId)
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

    fun syncNow() {
        SyncWorkScheduler.enqueue(app)
    }
}
