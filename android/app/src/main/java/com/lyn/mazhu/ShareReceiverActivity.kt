package com.lyn.mazhu

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.lyn.mazhu.data.Collection
import com.lyn.mazhu.data.CreateCollectionResult
import com.lyn.mazhu.data.SaveResult
import com.lyn.mazhu.ui.theme.MazhuTheme
import com.lyn.mazhu.worker.ParseWorkScheduler
import com.lyn.mazhu.worker.SyncWorkScheduler
import kotlinx.coroutines.launch

class ShareReceiverActivity : ComponentActivity() {
    private val repository by lazy {
        (application as MazhuApplication).bookmarkRepository
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedText = extractSharedText(intent)
        if (sharedText.isNullOrBlank()) {
            Toast.makeText(this, "没有找到可以收藏的链接", Toast.LENGTH_SHORT).show()
            finishAndRemoveTask()
            return
        }

        setContent {
            MazhuTheme {
                SavingScreen()
            }
        }

        lifecycleScope.launch {
            when (val result = repository.saveSharedText(sharedText)) {
                is SaveResult.Saved -> {
                    ParseWorkScheduler.enqueue(this@ShareReceiverActivity, result.bookmarkId)
                    SyncWorkScheduler.enqueue(this@ShareReceiverActivity)
                    showCollectionPicker(
                        bookmarkId = result.bookmarkId,
                        collections = repository.getCollections(),
                        selectedCollectionIds = repository.getBookmarkCollectionIds(result.bookmarkId),
                        alreadySaved = false,
                    )
                }

                is SaveResult.AlreadySaved -> {
                    showCollectionPicker(
                        bookmarkId = result.bookmarkId,
                        collections = repository.getCollections(),
                        selectedCollectionIds = repository.getBookmarkCollectionIds(result.bookmarkId),
                        alreadySaved = true,
                    )
                }

                SaveResult.InvalidShare -> {
                    Toast.makeText(
                        this@ShareReceiverActivity,
                        "没有找到可以收藏的链接",
                        Toast.LENGTH_SHORT,
                    ).show()
                    finishAndRemoveTask()
                }
            }
        }
    }

    private fun showCollectionPicker(
        bookmarkId: String,
        collections: List<Collection>,
        selectedCollectionIds: List<String>,
        alreadySaved: Boolean,
    ) {
        setContent {
            MazhuTheme {
                SaveToCollectionsScreen(
                    collections = collections.map { collection ->
                        CollectionChoice(
                            id = collection.id,
                            name = collection.name,
                        )
                    },
                    initiallySelectedIds = selectedCollectionIds.toSet(),
                    alreadySaved = alreadySaved,
                    onConfirm = { collectionIds ->
                        lifecycleScope.launch {
                            repository.setBookmarkCollections(bookmarkId, collectionIds)
                            SyncWorkScheduler.enqueue(this@ShareReceiverActivity)
                            Toast.makeText(
                                this@ShareReceiverActivity,
                                "已保存到 ${collectionIds.size} 个收藏夹",
                                Toast.LENGTH_SHORT,
                            ).show()
                            finishAndRemoveTask()
                        }
                    },
                    onCreate = { name, onResult ->
                        lifecycleScope.launch {
                            when (val result = repository.createCollection(name)) {
                                is CreateCollectionResult.Created -> {
                                    onResult(
                                        null,
                                        CollectionChoice(
                                            id = result.collection.id,
                                            name = result.collection.name,
                                        ),
                                    )
                                }

                                CreateCollectionResult.InvalidName -> {
                                    onResult("收藏夹名称不能为空", null)
                                }

                                CreateCollectionResult.NameAlreadyExists -> {
                                    onResult("已经有同名收藏夹", null)
                                }
                            }
                        }
                    },
                    onCancelSelection = {
                        lifecycleScope.launch {
                            repository.deleteBookmarkCompletely(bookmarkId)
                            SyncWorkScheduler.enqueue(this@ShareReceiverActivity)
                            Toast.makeText(
                                this@ShareReceiverActivity,
                                "已取消收藏",
                                Toast.LENGTH_SHORT,
                            ).show()
                            finishAndRemoveTask()
                        }
                    },
                )
            }
        }
    }

    private fun extractSharedText(intent: Intent): String? =
        when (intent.action) {
            Intent.ACTION_SEND -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getStringExtra(Intent.EXTRA_TEXT)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getStringExtra(Intent.EXTRA_TEXT)
                }
            }

            Intent.ACTION_VIEW -> intent.dataString
            else -> null
        }
}

@Composable
private fun SavingScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text("正在码住文章")
        }
    }
}
