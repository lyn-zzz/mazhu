package com.lyn.mazhu
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lyn.mazhu.data.Bookmark
import com.lyn.mazhu.data.CollectionSummary
import kotlinx.coroutines.delay

private const val SEARCH_KEYBOARD_DELAY_MS = 50L

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun SearchPage(
    bookmarks: List<Bookmark>,
    bookmarkCollectionIds: Map<String, List<String>>,
    collectionById: Map<String, CollectionSummary>,
    placeholder: String,
    resultFooter: String,
    onOpenBookmark: (Bookmark) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var query by remember { mutableStateOf("") }
    var histories by remember { mutableStateOf(loadSearchHistory(context)) }
    val trimmedQuery = query.trim()
    val results = remember(trimmedQuery, bookmarks) {
        if (trimmedQuery.isBlank()) {
            emptyList()
        } else {
            bookmarks.filter { bookmark ->
                bookmark.title.contains(trimmedQuery, ignoreCase = true) ||
                    bookmark.originalUrl.contains(trimmedQuery, ignoreCase = true) ||
                    bookmark.accountName?.contains(trimmedQuery, ignoreCase = true) == true ||
                    bookmark.contentText?.contains(trimmedQuery, ignoreCase = true) == true
            }
        }
    }

    fun commitSearch(value: String = query) {
        val keyword = value.trim()
        if (keyword.isBlank()) {
            return
        }
        histories = saveSearchHistory(context, keyword)
    }

    BackHandler(onBack = onDismiss)

    LaunchedEffect(Unit) {
        delay(SEARCH_KEYBOARD_DELAY_MS)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = {
                        Icon(Icons.Outlined.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (query.isNotBlank()) {
                            IconButton(onClick = { query = "" }) {
                                Text("×", style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    },
                    placeholder = { Text(placeholder) },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        errorIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            commitSearch()
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        },
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                )
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(Unit) {
                    detectTapGestures {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }
                },
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 40.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (trimmedQuery.isBlank()) {
                if (histories.isEmpty()) {
                    item {
                        EmptySearchHistory()
                    }
                } else {
                    items(histories, key = { it }) { history ->
                        SearchHistoryRow(
                            keyword = history,
                            onSelect = {
                                query = history
                                commitSearch(history)
                            },
                            onDelete = {
                                histories = deleteSearchHistory(context, history)
                            },
                        )
                    }
                    item {
                        TextButton(
                            onClick = { histories = clearSearchHistory(context) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("清除搜索历史")
                        }
                    }
                }
            } else {
                if (results.isEmpty()) {
                    item {
                        EmptySearchResults(trimmedQuery)
                    }
                } else {
                    items(results, key = Bookmark::id) { bookmark ->
                        SearchResultRow(
                            bookmark = bookmark,
                            collectionNames = bookmarkCollectionIds[bookmark.id]
                                .orEmpty()
                                .mapNotNull { collectionById[it]?.name },
                            onClick = { onOpenBookmark(bookmark) },
                        )
                    }
                    item {
                        Text(
                            text = resultFooter,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchHistoryRow(
    keyword: String,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = keyword,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        )
        IconButton(onClick = onDelete) {
            Text("×", style = MaterialTheme.typography.titleLarge)
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun SearchResultRow(
    bookmark: Bookmark,
    collectionNames: List<String>,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        RemoteCoverImage(
            url = bookmark.coverUrl,
            modifier = Modifier.size(width = 112.dp, height = 84.dp),
            cornerRadius = 16,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp),
        ) {
            Text(
                text = bookmark.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = bookmark.accountName ?: "未知公众号",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (collectionNames.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = collectionNames.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun EmptySearchHistory() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "暂无搜索历史",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptySearchResults(query: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "没有找到“$query”",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "换个关键词再试试",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
