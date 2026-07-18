package com.lyn.mazhu

import android.view.MotionEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.lyn.mazhu.data.BookmarkRepository
import com.lyn.mazhu.data.CollectionSummary
import kotlin.math.roundToInt

internal enum class HomeSection {
    COLLECTIONS,
    RECENT,
}

@Composable
internal fun HomeSegmentedSwitch(
    selected: HomeSection,
    position: Float,
    onSelected: (HomeSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 3.dp,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
        ) {
            val segmentWidth = maxWidth / 2
            val indicatorOffset = segmentWidth * position.coerceIn(0f, 1f)

            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(segmentWidth)
                    .height(42.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.surface),
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                HomeSegmentButton(
                    text = "收藏夹",
                    selected = selected == HomeSection.COLLECTIONS,
                    onClick = { onSelected(HomeSection.COLLECTIONS) },
                    modifier = Modifier.weight(1f),
                )
                HomeSegmentButton(
                    text = "最近收藏",
                    selected = selected == HomeSection.RECENT,
                    onClick = { onSelected(HomeSection.RECENT) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun HomeSegmentButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textColor = if (selected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
        )
    }
}

@Composable
internal fun HomeStatsLine(
    selected: HomeSection,
    collectionCount: Int,
    bookmarkCount: Int,
    unsyncedCount: Long,
    modifier: Modifier = Modifier,
) {
    val text = if (selected == HomeSection.COLLECTIONS) {
        "$collectionCount 个收藏夹 · $bookmarkCount 篇文章"
    } else if (unsyncedCount > 0) {
        "$bookmarkCount 篇文章 · $unsyncedCount 篇待同步"
    } else {
        "$bookmarkCount 篇文章 · 已全部同步"
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 2.dp, bottom = 2.dp),
    )
}

@Composable
internal fun ReorderableCollectionItem(
    collection: CollectionSummary,
    collections: List<CollectionSummary>,
    coverUrl: String?,
    draggingCollectionId: String?,
    draggingOffset: Float,
    dragStepPx: Float,
    onDraggingOffsetChange: (Float) -> Unit,
    onDraggingCollectionChange: (String?) -> Unit,
    onCollectionsChange: (List<CollectionSummary>) -> Unit,
    onReorderFinished: (List<CollectionSummary>) -> Unit,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val isDefault = collection.id == BookmarkRepository.DEFAULT_COLLECTION_ID
    val isDragging = draggingCollectionId == collection.id
    val latestCollections by rememberUpdatedState(collections)
    var gestureCollections by remember { mutableStateOf(collections) }
    var gestureOffset by remember { mutableStateOf(0f) }
    var lastDragY by remember { mutableStateOf(0f) }

    fun updateDrag(delta: Float) {
        val currentIndex = gestureCollections.indexOfFirst { it.id == collection.id }
        if (currentIndex > 0) {
            gestureOffset += delta
            if (gestureOffset > dragStepPx / 2 && currentIndex < gestureCollections.lastIndex) {
                gestureCollections = gestureCollections.moveItem(currentIndex, currentIndex + 1)
                gestureOffset -= dragStepPx
            } else if (gestureOffset < -dragStepPx / 2 && currentIndex > 1) {
                gestureCollections = gestureCollections.moveItem(currentIndex, currentIndex - 1)
                gestureOffset += dragStepPx
            }
            onCollectionsChange(gestureCollections)
            onDraggingOffsetChange(gestureOffset)
        }
    }

    fun finishDrag() {
        onDraggingCollectionChange(null)
        onDraggingOffsetChange(0f)
        onReorderFinished(gestureCollections)
    }

    val dragModifier = if (isDefault) {
        Modifier
    } else {
        Modifier.pointerInteropFilter { event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    gestureCollections = latestCollections
                    gestureOffset = 0f
                    lastDragY = event.rawY
                    onDraggingCollectionChange(collection.id)
                    onDraggingOffsetChange(0f)
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val delta = event.rawY - lastDragY
                    lastDragY = event.rawY
                    updateDrag(delta)
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    finishDrag()
                    true
                }

                else -> true
            }
        }
    }

    Box(
        modifier = Modifier
            .zIndex(if (isDragging) 1f else 0f)
            .offset {
                IntOffset(
                    x = 0,
                    y = if (isDragging) draggingOffset.roundToInt() else 0,
                )
            },
    ) {
        CollectionCard(
            collection = collection,
            coverUrl = coverUrl,
            dragModifier = dragModifier,
            onOpen = onOpen,
            onRename = onRename,
            onDelete = onDelete,
        )
    }
}

private fun <T> List<T>.moveItem(
    fromIndex: Int,
    toIndex: Int,
): List<T> {
    if (fromIndex == toIndex || fromIndex !in indices || toIndex !in indices) {
        return this
    }
    return toMutableList().apply {
        add(toIndex, removeAt(fromIndex))
    }
}

@Composable
internal fun CollectionCard(
    collection: CollectionSummary,
    coverUrl: String?,
    modifier: Modifier = Modifier,
    dragModifier: Modifier = Modifier,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val isDefault = collection.id == BookmarkRepository.DEFAULT_COLLECTION_ID

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onOpen,
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RemoteCoverImage(
                url = coverUrl,
                modifier = Modifier.size(70.dp),
                cornerRadius = 20,
                fallback = {
                    Icon(
                        imageVector = Icons.Outlined.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(13.dp)
                            .size(30.dp),
                    )
                },
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
            ) {
                Text(
                    text = collection.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isDefault) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            shape = RoundedCornerShape(999.dp),
                        ) {
                            Text(
                                text = "默认",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            )
                        }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Text(
                            text = "${collection.articleCount} 篇文章",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                    }
                    if (collection.unsyncedCount > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            shape = RoundedCornerShape(999.dp),
                        ) {
                            Text(
                                text = "${collection.unsyncedCount} 待同步",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            )
                        }
                    }
                }
            }

            if (!isDefault) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .then(dragModifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DragIndicator,
                            contentDescription = "拖动排序",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Column {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = "管理收藏夹",
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("重命名") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Edit, contentDescription = null)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onRename()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("删除") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Delete, contentDescription = null)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
