package com.lyn.mazhu

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.lyn.mazhu.data.BookmarkRepository
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
                    )
                }

                SaveResult.AlreadySaved -> {
                    Toast.makeText(
                        this@ShareReceiverActivity,
                        "这篇文章已经码住了",
                        Toast.LENGTH_SHORT,
                    ).show()
                    finishAndRemoveTask()
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
    ) {
        setContent {
            MazhuTheme {
                ShareCollectionPicker(
                    collections = collections,
                    onSelect = { collection ->
                        lifecycleScope.launch {
                            repository.moveBookmark(bookmarkId, collection.id)
                            SyncWorkScheduler.enqueue(this@ShareReceiverActivity)
                            Toast.makeText(
                                this@ShareReceiverActivity,
                                "已保存到${collection.name}",
                                Toast.LENGTH_SHORT,
                            ).show()
                            finishAndRemoveTask()
                        }
                    },
                    onCreate = { name, onResult ->
                        lifecycleScope.launch {
                            when (val result = repository.createCollection(name)) {
                                is CreateCollectionResult.Created -> {
                                    repository.moveBookmark(
                                        bookmarkId = bookmarkId,
                                        collectionId = result.collection.id,
                                    )
                                    SyncWorkScheduler.enqueue(this@ShareReceiverActivity)
                                    Toast.makeText(
                                        this@ShareReceiverActivity,
                                        "已保存到${result.collection.name}",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                    finishAndRemoveTask()
                                }

                                CreateCollectionResult.InvalidName -> {
                                    onResult("收藏夹名称不能为空")
                                }

                                CreateCollectionResult.NameAlreadyExists -> {
                                    onResult("已经有同名收藏夹")
                                }
                            }
                        }
                    },
                    onKeepDefault = {
                        Toast.makeText(
                            this@ShareReceiverActivity,
                            "已保存到默认收藏夹",
                            Toast.LENGTH_SHORT,
                        ).show()
                        finishAndRemoveTask()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareCollectionPicker(
    collections: List<Collection>,
    onSelect: (Collection) -> Unit,
    onCreate: (String, (String) -> Unit) -> Unit,
    onKeepDefault: () -> Unit,
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var createError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "保存到收藏夹",
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "文章已先保存在默认收藏夹",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCreateDialog = true },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                        )
                        Text(
                            text = "新建收藏夹",
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 14.dp),
                        )
                    }
                }
            }

            items(
                items = collections,
                key = Collection::id,
            ) { collection ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (collection.id == BookmarkRepository.DEFAULT_COLLECTION_ID) {
                                onKeepDefault()
                            } else {
                                onSelect(collection)
                            }
                        },
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp),
                        )
                        Text(
                            text = collection.name,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 14.dp),
                        )
                        if (collection.id == BookmarkRepository.DEFAULT_COLLECTION_ID) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = "当前收藏夹",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = {
                showCreateDialog = false
                createError = null
            },
            title = { Text("新建收藏夹") },
            text = {
                Column {
                    TextField(
                        value = name,
                        onValueChange = {
                            name = it
                            createError = null
                        },
                        label = { Text("收藏夹名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    createError?.let { error ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onCreate(name) { error ->
                            createError = error
                        }
                    },
                    enabled = name.isNotBlank(),
                ) {
                    Text("创建并保存")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreateDialog = false
                        createError = null
                    },
                ) {
                    Text("取消")
                }
            },
        )
    }
}
