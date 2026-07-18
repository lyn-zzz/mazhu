package com.lyn.mazhu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lyn.mazhu.data.Bookmark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ArticleActionSheet(
    bookmark: Bookmark,
    inCollection: Boolean,
    onDismissRequest: () -> Unit,
    onMove: () -> Unit,
    onCopyToCollection: () -> Unit,
    onCopyLink: () -> Unit,
    onRemoveFromCollection: () -> Unit,
    onDeleteCompletely: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
        ) {
            Text(
                text = bookmark.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            ArticleActionRow(
                icon = Icons.Outlined.Folder,
                title = "移动到收藏夹",
                subtitle = "从当前分类移到其他收藏夹",
                onClick = onMove,
            )
            ArticleActionRow(
                icon = Icons.Outlined.ContentCopy,
                title = "复制到其他收藏夹",
                subtitle = "保留当前位置，同时加入新收藏夹",
                onClick = onCopyToCollection,
            )
            ArticleActionRow(
                icon = Icons.Outlined.ContentCopy,
                title = "复制链接",
                subtitle = "复制公众号文章原始链接",
                onClick = onCopyLink,
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            if (inCollection) {
                ArticleActionRow(
                    icon = Icons.Outlined.RemoveCircleOutline,
                    title = "从该收藏夹中移除",
                    subtitle = "其他收藏夹中的这篇文章不受影响",
                    tone = ArticleActionTone.Remove,
                    onClick = onRemoveFromCollection,
                )
            }
            ArticleActionRow(
                icon = Icons.Outlined.DeleteForever,
                title = "删除该文章",
                subtitle = "从所有收藏夹中彻底删除",
                tone = ArticleActionTone.Delete,
                onClick = onDeleteCompletely,
            )
        }
    }
}

@Composable
private fun ArticleActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tone: ArticleActionTone = ArticleActionTone.Neutral,
    onClick: () -> Unit,
) {
    val iconContainerColor = when (tone) {
        ArticleActionTone.Neutral -> MaterialTheme.colorScheme.primaryContainer
        ArticleActionTone.Remove -> MaterialTheme.colorScheme.tertiaryContainer
        ArticleActionTone.Delete -> MaterialTheme.colorScheme.errorContainer
    }
    val iconContentColor = when (tone) {
        ArticleActionTone.Neutral -> MaterialTheme.colorScheme.onPrimaryContainer
        ArticleActionTone.Remove -> MaterialTheme.colorScheme.onTertiaryContainer
        ArticleActionTone.Delete -> MaterialTheme.colorScheme.onErrorContainer
    }
    val titleColor = when (tone) {
        ArticleActionTone.Neutral -> MaterialTheme.colorScheme.onSurface
        ArticleActionTone.Remove -> MaterialTheme.colorScheme.tertiary
        ArticleActionTone.Delete -> MaterialTheme.colorScheme.error
    }

    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = iconContainerColor,
                contentColor = iconContentColor,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(44.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
