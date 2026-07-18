package com.lyn.mazhu

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lyn.mazhu.data.Bookmark
import com.lyn.mazhu.data.BookmarkStatus

@Composable
internal fun BatchActionBar(
    selectedCount: Int,
    onMove: () -> Unit,
    onCopy: () -> Unit,
    onCopyLinks: () -> Unit,
    onRemove: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BatchActionButton(
                icon = Icons.Outlined.Folder,
                label = "移动",
                onClick = onMove,
            )
            BatchActionButton(
                icon = Icons.Outlined.ContentCopy,
                label = "复制",
                onClick = onCopy,
            )
            BatchActionButton(
                icon = Icons.Outlined.ContentCopy,
                label = "链接",
                onClick = onCopyLinks,
            )
            BatchActionButton(
                icon = Icons.Outlined.RemoveCircleOutline,
                label = "移除",
                tone = ArticleActionTone.Remove,
                onClick = onRemove,
            )
            BatchActionButton(
                icon = Icons.Outlined.DeleteForever,
                label = "删除",
                tone = ArticleActionTone.Delete,
                onClick = onDelete,
            )
        }
    }
}

@Composable
private fun BatchActionButton(
    icon: ImageVector,
    label: String,
    tone: ArticleActionTone = ArticleActionTone.Neutral,
    onClick: () -> Unit,
) {
    val contentColor = when (tone) {
        ArticleActionTone.Neutral -> MaterialTheme.colorScheme.onSurface
        ArticleActionTone.Remove -> MaterialTheme.colorScheme.tertiary
        ArticleActionTone.Delete -> MaterialTheme.colorScheme.error
    }
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
        modifier = Modifier.width(64.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = label,
                color = contentColor,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }
    }
}

@Composable
internal fun BookmarkRow(
    bookmark: Bookmark,
    syncEnabled: Boolean,
    collectionNames: List<String>,
    selected: Boolean = false,
    selectionMode: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onMenu: () -> Unit,
) {
    val context = LocalContext.current
    val cardBorder = if (selected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }
    val cardColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(bookmark.id, selectionMode) {
                detectTapGestures(
                    onLongPress = { onLongClick?.invoke() },
                    onTap = {
                        if (onClick != null) {
                            onClick()
                        } else {
                            openBookmark(context, bookmark)
                        }
                    },
                )
            },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp,
            draggedElevation = 0.dp,
            disabledElevation = 0.dp,
        ),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                RemoteCoverImage(
                    url = bookmark.coverUrl,
                    modifier = Modifier.size(width = 108.dp, height = 86.dp),
                    cornerRadius = 20,
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = bookmark.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (selectionMode) {
                            SelectionMark(selected = selected)
                        } else {
                            IconButton(
                                onClick = onMenu,
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = "文章操作",
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    bookmark.accountName?.takeIf(String::isNotBlank)?.let { accountName ->
                        MetadataPill(
                            text = accountName,
                            emphasized = true,
                        )
                    }
                    if (collectionNames.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        MetadataPill(
                            text = collectionNames.joinToString(" · "),
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatTime(bookmark.createdAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (
                        bookmark.syncStatus ==
                        com.lyn.mazhu.data.BookmarkStatus.SYNC_SYNCED
                    ) {
                        Icons.Outlined.CloudDone
                    } else if (!syncEnabled) {
                        Icons.Outlined.Settings
                    } else {
                        Icons.Outlined.CloudOff
                    },
                    contentDescription = null,
                    tint = if (
                        bookmark.syncStatus ==
                        com.lyn.mazhu.data.BookmarkStatus.SYNC_SYNCED
                    ) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = bookmark.statusLabel(syncEnabled),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 5.dp),
                )
            }
        }
    }
}
