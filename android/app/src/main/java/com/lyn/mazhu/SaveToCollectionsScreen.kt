package com.lyn.mazhu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lyn.mazhu.data.BookmarkRepository

internal data class CollectionChoice(
    val id: String,
    val name: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SaveToCollectionsScreen(
    collections: List<CollectionChoice>,
    initiallySelectedIds: Set<String>,
    alreadySaved: Boolean,
    onConfirm: (List<String>) -> Unit,
    onCreate: (String, (String?, CollectionChoice?) -> Unit) -> Unit,
    onDismiss: (() -> Unit)? = null,
    onCancelSelection: (() -> Unit)? = null,
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var createError by remember { mutableStateOf<String?>(null) }
    var createdCollections by remember { mutableStateOf(emptyList<CollectionChoice>()) }
    var selectedIds by remember(initiallySelectedIds) {
        mutableStateOf(
            if (initiallySelectedIds.isEmpty()) {
                setOf(BookmarkRepository.DEFAULT_COLLECTION_ID)
            } else {
                initiallySelectedIds
            },
        )
    }
    val allCollections = (collections + createdCollections).distinctBy(CollectionChoice::id)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (onDismiss != null) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "返回",
                            )
                        }
                    }
                },
                title = {
                    Column {
                        Text(
                            text = "保存到收藏夹",
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (alreadySaved) {
                                "这篇文章已经码住了，可继续添加到其他收藏夹"
                            } else {
                                "可同时选择多个收藏夹"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                shadowElevation = 0.dp,
            ) {
                Button(
                    onClick = {
                        if (selectedIds.isEmpty()) {
                            onCancelSelection?.invoke() ?: onDismiss?.invoke()
                        } else {
                            onConfirm(selectedIds.toList())
                        }
                    },
                    enabled = selectedIds.isNotEmpty() || onCancelSelection != null || onDismiss != null,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(56.dp),
                ) {
                    Text(
                        if (selectedIds.isEmpty()) {
                            "取消收藏"
                        } else {
                            "完成，保存到 ${selectedIds.size} 个收藏夹"
                        },
                    )
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                val createShape = RoundedCornerShape(20.dp)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { showCreateDialog = true },
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                        focusedElevation = 0.dp,
                        hoveredElevation = 0.dp,
                        draggedElevation = 0.dp,
                        disabledElevation = 0.dp,
                    ),
                    shape = createShape,
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
                items = allCollections,
                key = CollectionChoice::id,
            ) { collection ->
                val selected = collection.id in selectedIds
                val itemShape = RoundedCornerShape(20.dp)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            selectedIds = if (selected) {
                                selectedIds - collection.id
                            } else {
                                selectedIds + collection.id
                            }
                        },
                    shape = itemShape,
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                    ),
                    border = if (selected) {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    } else {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    },
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                        focusedElevation = 0.dp,
                        hoveredElevation = 0.dp,
                        draggedElevation = 0.dp,
                        disabledElevation = 0.dp,
                    ),
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
                        if (selected) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = "已选择",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(12.dp)) }
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
                        onCreate(name) { error, collection ->
                            if (error != null) {
                                createError = error
                                return@onCreate
                            }
                            if (collection != null) {
                                createdCollections = createdCollections + collection
                                selectedIds = selectedIds + collection.id
                                showCreateDialog = false
                                createError = null
                            }
                        }
                    },
                    enabled = name.isNotBlank(),
                ) {
                    Text("创建")
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
