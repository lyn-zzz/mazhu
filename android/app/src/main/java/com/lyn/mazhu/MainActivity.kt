package com.lyn.mazhu

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.ViewTreeObserver
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lyn.mazhu.data.Bookmark
import com.lyn.mazhu.data.BookmarkRepository
import com.lyn.mazhu.data.CollectionSummary
import com.lyn.mazhu.data.CreateCollectionResult
import com.lyn.mazhu.data.RenameCollectionResult
import com.lyn.mazhu.data.SaveResult
import com.lyn.mazhu.data.ShareTextParser
import com.lyn.mazhu.supabase.AuthResult
import com.lyn.mazhu.supabase.SupabaseConfig
import com.lyn.mazhu.supabase.SupabaseSession
import com.lyn.mazhu.ui.theme.MazhuTheme
import com.lyn.mazhu.worker.ParseWorkScheduler
import com.lyn.mazhu.worker.SyncWorkScheduler
import coil3.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val PAGE_TRANSITION_DURATION_MS = 220

private enum class CollectionPickerMode {
    MOVE,
    COPY,
}

private data class CollectionPickerTarget(
    val bookmark: Bookmark,
    val mode: CollectionPickerMode,
)

private data class BatchCollectionPickerTarget(
    val bookmarkIds: List<String>,
    val mode: CollectionPickerMode,
)

internal data class ClipboardWechatLink(
    val url: String,
    val token: String,
)

private data class ClipboardSaveTarget(
    val bookmarkId: String,
    val initiallySelectedIds: Set<String>,
    val alreadySaved: Boolean,
)

private data class DeleteBookmarkTarget(
    val bookmark: Bookmark,
    val fromCollectionOnly: Boolean,
)

private data class ReaderTarget(
    val title: String,
    val url: String,
)

private data class BatchDeleteBookmarkTarget(
    val bookmarkIds: List<String>,
    val fromCollectionOnly: Boolean,
)

internal enum class ArticleActionTone {
    Neutral,
    Remove,
    Delete,
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            MazhuTheme {
                MazhuApp()
            }
        }
        lifecycleScope.launch {
            val repository = (application as MazhuApplication).bookmarkRepository
            repository.findPendingParseIds().forEach { bookmarkId ->
                ParseWorkScheduler.enqueue(this@MainActivity, bookmarkId)
            }
            SyncWorkScheduler.enqueue(this@MainActivity)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
private fun MazhuApp(
    viewModel: MainViewModel = viewModel(),
) {
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val bookmarkCollections by viewModel.bookmarkCollections.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val supabaseConfig by viewModel.supabaseConfig.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var selectedCollectionId by remember { mutableStateOf<String?>(null) }
    var showSearchPage by remember { mutableStateOf(false) }
    var searchCollectionId by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<CollectionSummary?>(null) }
    var deleteTarget by remember { mutableStateOf<CollectionSummary?>(null) }
    var actionTarget by remember { mutableStateOf<Bookmark?>(null) }
    var deleteBookmarkTarget by remember { mutableStateOf<DeleteBookmarkTarget?>(null) }
    var readerTarget by remember { mutableStateOf<ReaderTarget?>(null) }
    var collectionPickerTarget by remember { mutableStateOf<CollectionPickerTarget?>(null) }
    var batchCollectionPickerTarget by remember { mutableStateOf<BatchCollectionPickerTarget?>(null) }
    var batchDeleteBookmarkTarget by remember { mutableStateOf<BatchDeleteBookmarkTarget?>(null) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var showSyncSettingsDialog by remember { mutableStateOf(false) }
    var clipboardLink by remember { mutableStateOf<ClipboardWechatLink?>(null) }
    var clipboardSaveTarget by remember { mutableStateOf<ClipboardSaveTarget?>(null) }
    val latestClipboardLink by rememberUpdatedState(clipboardLink)
    val latestClipboardSaveTarget by rememberUpdatedState(clipboardSaveTarget)

    val collectionById = remember(collections) { collections.associateBy { it.id } }
    val bookmarkCollectionIds = remember(bookmarkCollections) {
        bookmarkCollections
            .groupBy { it.bookmarkId }
            .mapValues { entry -> entry.value.map { it.collectionId } }
    }
    val searchBookmarks = remember(bookmarks, bookmarkCollections, searchCollectionId) {
        val searchBookmarkIds = searchCollectionId?.let { collectionId ->
            bookmarkCollections
                .filter { it.collectionId == collectionId }
                .map { it.bookmarkId }
                .toSet()
        }
        bookmarks.filter { bookmark -> searchBookmarkIds == null || bookmark.id in searchBookmarkIds }
    }

    LaunchedEffect(selectedCollectionId) {
        listState.scrollToItem(0)
    }

    fun checkClipboardForWechatUrl() {
        if (latestClipboardSaveTarget != null) {
            return
        }
        val detectedLink = detectWechatClipboardLink(context)
        val currentLink = latestClipboardLink
        val isAlreadyPrompting = currentLink != null && currentLink.token == detectedLink?.token
        if (detectedLink != null && !isAlreadyPrompting && !wasClipboardLinkHandled(context, detectedLink)) {
            clipboardLink = detectedLink
        }
    }

    LaunchedEffect(Unit) {
        delay(120)
        checkClipboardForWechatUrl()
    }

    DisposableEffect(lifecycleOwner, view, context) {
        fun scheduleClipboardCheck(delayMs: Long) {
            scope.launch {
                delay(delayMs)
                checkClipboardForWechatUrl()
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START || event == Lifecycle.Event.ON_RESUME) {
                scheduleClipboardCheck(220)
            }
        }
        val focusListener = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            if (hasFocus) {
                scheduleClipboardCheck(160)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        view.viewTreeObserver.addOnWindowFocusChangeListener(focusListener)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (view.viewTreeObserver.isAlive) {
                view.viewTreeObserver.removeOnWindowFocusChangeListener(focusListener)
            }
        }
    }

    BackHandler(enabled = selectedCollectionId != null && clipboardSaveTarget == null) {
        selectedCollectionId = null
    }
    BackHandler(enabled = clipboardSaveTarget != null) {
        clipboardSaveTarget = null
    }

    MainContent(
        collections = collections,
        bookmarks = bookmarks,
        bookmarkCollectionIds = bookmarkCollectionIds,
        collectionById = collectionById,
        selectedCollectionId = selectedCollectionId,
        supabaseConfigured = supabaseConfig.isConfigured,
        session = session,
        listState = listState,
        snackbarHostState = snackbarHostState,
        onBackFromCollection = { selectedCollectionId = null },
        onOpenSearch = {
            searchCollectionId = null
            showSearchPage = true
        },
        onOpenCollectionSearch = { collectionId ->
            searchCollectionId = collectionId
            showSearchPage = true
        },
        onSyncClick = {
            if (session == null) {
                if (supabaseConfig.isConfigured) {
                    showAuthDialog = true
                } else {
                    showSyncSettingsDialog = true
                }
            } else {
                viewModel.syncNow { hasPendingSync ->
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            if (hasPendingSync) "已开始同步" else "已完成同步",
                        )
                    }
                }
            }
        },
        onAccountClick = {
            if (session == null) {
                if (supabaseConfig.isConfigured) {
                    showAuthDialog = true
                } else {
                    showSyncSettingsDialog = true
                }
            } else {
                showAccountDialog = true
            }
        },
        onCreateCollection = { showCreateDialog = true },
        onLoginSync = {
            if (supabaseConfig.isConfigured) {
                showAuthDialog = true
            } else {
                showSyncSettingsDialog = true
            }
        },
        onOpenCollection = { selectedCollectionId = it },
        onRenameCollection = { renameTarget = it },
        onDeleteCollection = { deleteTarget = it },
        onReorderCollections = viewModel::reorderCollections,
        onOpenBookmark = { bookmark ->
            readerTarget = ReaderTarget(
                title = bookmark.title,
                url = bookmark.originalUrl,
            )
        },
        onBookmarkMenu = { actionTarget = it },
        onBatchMove = { bookmarkIds ->
            batchCollectionPickerTarget = BatchCollectionPickerTarget(
                bookmarkIds = bookmarkIds,
                mode = CollectionPickerMode.MOVE,
            )
        },
        onBatchCopy = { bookmarkIds ->
            batchCollectionPickerTarget = BatchCollectionPickerTarget(
                bookmarkIds = bookmarkIds,
                mode = CollectionPickerMode.COPY,
            )
        },
        onBatchCopyLinks = { selectedBookmarks ->
            copyToClipboard(
                context = context,
                text = selectedBookmarks.joinToString(separator = "\n") { it.originalUrl },
            )
            scope.launch {
                snackbarHostState.showSnackbar("已复制 ${selectedBookmarks.size} 个链接")
            }
        },
        onBatchRemoveFromCollection = { bookmarkIds ->
            batchDeleteBookmarkTarget = BatchDeleteBookmarkTarget(
                bookmarkIds = bookmarkIds,
                fromCollectionOnly = true,
            )
        },
        onBatchDeleteCompletely = { bookmarkIds ->
            batchDeleteBookmarkTarget = BatchDeleteBookmarkTarget(
                bookmarkIds = bookmarkIds,
                fromCollectionOnly = false,
            )
        },
    )

    if (showSearchPage) {
        SearchPage(
            bookmarks = searchBookmarks,
            bookmarkCollectionIds = bookmarkCollectionIds,
            collectionById = collectionById,
            placeholder = if (searchCollectionId == null) {
                "搜索收藏文章"
            } else {
                "搜索当前收藏夹"
            },
            resultFooter = if (searchCollectionId == null) {
                "以上为全部收藏文章的搜索结果"
            } else {
                "以上为当前收藏夹的搜索结果"
            },
            onOpenBookmark = { bookmark ->
                readerTarget = ReaderTarget(
                    title = bookmark.title,
                    url = bookmark.originalUrl,
                )
            },
            onDismiss = {
                showSearchPage = false
                searchCollectionId = null
            },
        )
    }

    readerTarget?.let { target ->
        WebViewReaderScreen(
            title = target.title,
            url = target.url,
            onDismiss = { readerTarget = null },
        )
    }

    if (showCreateDialog) {
        CollectionNameDialog(
            title = "新建收藏夹",
            confirmLabel = "创建",
            initialName = "",
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                viewModel.createCollection(name) { result ->
                    when (result) {
                        is CreateCollectionResult.Created -> showCreateDialog = false
                        CreateCollectionResult.InvalidName -> scope.launch {
                            snackbarHostState.showSnackbar("收藏夹名称不能为空")
                        }
                        CreateCollectionResult.NameAlreadyExists -> scope.launch {
                            snackbarHostState.showSnackbar("已经有同名收藏夹")
                        }
                    }
                }
            },
        )
    }

    clipboardLink?.let { link ->
        AlertDialog(
            onDismissRequest = {
                markClipboardLinkHandled(context, link)
                clipboardLink = null
            },
            title = { Text("检测到公众号文章链接") },
            text = { Text(link.url) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.saveSharedText(link.url) { result ->
                            markClipboardLinkHandled(context, link)
                            clipboardLink = null
                            when (result) {
                                is SaveResult.Saved -> {
                                    clipboardSaveTarget = ClipboardSaveTarget(
                                        bookmarkId = result.bookmarkId,
                                        initiallySelectedIds = setOf(BookmarkRepository.DEFAULT_COLLECTION_ID),
                                        alreadySaved = false,
                                    )
                                }

                                is SaveResult.AlreadySaved -> {
                                    clipboardSaveTarget = ClipboardSaveTarget(
                                        bookmarkId = result.bookmarkId,
                                        initiallySelectedIds = bookmarkCollectionIds[result.bookmarkId]
                                            ?.toSet()
                                            .orEmpty(),
                                        alreadySaved = true,
                                    )
                                }

                                SaveResult.InvalidShare -> scope.launch {
                                    snackbarHostState.showSnackbar("没有找到可以收藏的链接")
                                }
                            }
                        }
                    },
                ) {
                    Text("添加")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        markClipboardLinkHandled(context, link)
                        clipboardLink = null
                    },
                ) {
                    Text("忽略")
                }
            },
        )
    }

    clipboardSaveTarget?.let { target ->
        SaveToCollectionsScreen(
            collections = collections.map { collection ->
                CollectionChoice(
                    id = collection.id,
                    name = collection.name,
                )
            },
            initiallySelectedIds = target.initiallySelectedIds,
            alreadySaved = target.alreadySaved,
            onConfirm = { selectedIds ->
                viewModel.setBookmarkCollections(
                    bookmarkId = target.bookmarkId,
                    collectionIds = selectedIds,
                )
                clipboardSaveTarget = null
                scope.launch {
                    snackbarHostState.showSnackbar(
                        "已保存到 ${selectedIds.size} 个收藏夹",
                    )
                }
            },
            onCreate = { name, onResult ->
                viewModel.createCollection(name) { result ->
                    when (result) {
                        is CreateCollectionResult.Created -> {
                            onResult(
                                null,
                                CollectionChoice(
                                    id = result.collection.id,
                                    name = result.collection.name,
                                )
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
            onDismiss = {
                clipboardSaveTarget = null
            },
            onCancelSelection = {
                viewModel.removeBookmarkFromCollection(
                    bookmarkId = target.bookmarkId,
                    collectionId = null,
                )
                clipboardSaveTarget = null
                scope.launch {
                    snackbarHostState.showSnackbar("已取消收藏")
                }
            },
        )
    }

    renameTarget?.let { collection ->
        CollectionNameDialog(
            title = "重命名收藏夹",
            confirmLabel = "保存",
            initialName = collection.name,
            onDismiss = { renameTarget = null },
            onConfirm = { name ->
                viewModel.renameCollection(collection.id, name) { result ->
                    when (result) {
                        RenameCollectionResult.Renamed -> renameTarget = null
                        RenameCollectionResult.InvalidName -> scope.launch {
                            snackbarHostState.showSnackbar("收藏夹名称不能为空")
                        }
                        RenameCollectionResult.NameAlreadyExists -> scope.launch {
                            snackbarHostState.showSnackbar("已经有同名收藏夹")
                        }
                        RenameCollectionResult.DefaultCollection -> Unit
                    }
                }
            },
        )
    }

    deleteTarget?.let { collection ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除“${collection.name}”？") },
            text = {
                Text("收藏夹中的文章不会删除，会自动移到默认收藏夹。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCollection(collection.id) {
                            if (selectedCollectionId == collection.id) {
                                selectedCollectionId = null
                            }
                            deleteTarget = null
                        }
                    },
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("取消")
                }
            },
        )
    }

    actionTarget?.let { bookmark ->
        ArticleActionSheet(
            bookmark = bookmark,
            inCollection = selectedCollectionId != null,
            onDismissRequest = { actionTarget = null },
            onMove = {
                collectionPickerTarget = CollectionPickerTarget(
                    bookmark = bookmark,
                    mode = CollectionPickerMode.MOVE,
                )
                actionTarget = null
            },
            onCopyToCollection = {
                collectionPickerTarget = CollectionPickerTarget(
                    bookmark = bookmark,
                    mode = CollectionPickerMode.COPY,
                )
                actionTarget = null
            },
            onCopyLink = {
                copyToClipboard(context, bookmark.originalUrl)
                actionTarget = null
                scope.launch {
                    snackbarHostState.showSnackbar("链接已复制")
                }
            },
            onRemoveFromCollection = {
                deleteBookmarkTarget = DeleteBookmarkTarget(
                    bookmark = bookmark,
                    fromCollectionOnly = true,
                )
                actionTarget = null
            },
            onDeleteCompletely = {
                deleteBookmarkTarget = DeleteBookmarkTarget(
                    bookmark = bookmark,
                    fromCollectionOnly = false,
                )
                actionTarget = null
            },
        )
    }

    deleteBookmarkTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteBookmarkTarget = null },
            title = {
                Text(
                    if (target.fromCollectionOnly) {
                        "从该收藏夹中移除？"
                    } else {
                        "删除该文章？"
                    },
                )
            },
            text = {
                Text(
                    if (target.fromCollectionOnly) {
                        "这只会从当前收藏夹移除，其他收藏夹中仍会保留。"
                    } else {
                        "这会从所有收藏夹中删除这篇文章。"
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeBookmarkFromCollection(
                            bookmarkId = target.bookmark.id,
                            collectionId = if (target.fromCollectionOnly) {
                                selectedCollectionId
                            } else {
                                null
                            },
                        )
                        deleteBookmarkTarget = null
                    },
                ) {
                    Text(if (target.fromCollectionOnly) "移除" else "删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteBookmarkTarget = null }) {
                    Text("取消")
                }
            },
        )
    }

    collectionPickerTarget?.let { target ->
        MultiCollectionDialog(
            title = if (target.mode == CollectionPickerMode.MOVE) {
                "移动到收藏夹"
            } else {
                "复制到收藏夹"
            },
            collections = collections,
            initiallySelectedIds = bookmarkCollectionIds[target.bookmark.id].orEmpty().toSet(),
            onDismiss = { collectionPickerTarget = null },
            onConfirm = { selectedIds ->
                if (target.mode == CollectionPickerMode.MOVE) {
                    viewModel.moveBookmarkFromCollection(
                        bookmarkId = target.bookmark.id,
                        fromCollectionId = selectedCollectionId,
                        toCollectionIds = selectedIds,
                    )
                } else {
                    viewModel.copyBookmarkToCollections(
                        bookmarkId = target.bookmark.id,
                        collectionIds = selectedIds,
                    )
                }
                collectionPickerTarget = null
            },
        )
    }

    batchCollectionPickerTarget?.let { target ->
        MultiCollectionDialog(
            title = if (target.mode == CollectionPickerMode.MOVE) {
                "批量移动到收藏夹"
            } else {
                "批量复制到收藏夹"
            },
            collections = collections,
            initiallySelectedIds = emptySet(),
            onDismiss = { batchCollectionPickerTarget = null },
            onConfirm = { selectedIds ->
                if (target.mode == CollectionPickerMode.MOVE) {
                    viewModel.moveBookmarksFromCollection(
                        bookmarkIds = target.bookmarkIds,
                        fromCollectionId = selectedCollectionId,
                        toCollectionIds = selectedIds,
                    )
                } else {
                    viewModel.copyBookmarksToCollections(
                        bookmarkIds = target.bookmarkIds,
                        collectionIds = selectedIds,
                    )
                }
                batchCollectionPickerTarget = null
            },
        )
    }

    batchDeleteBookmarkTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { batchDeleteBookmarkTarget = null },
            title = {
                Text(
                    if (target.fromCollectionOnly) {
                        "从该收藏夹中移除 ${target.bookmarkIds.size} 篇？"
                    } else {
                        "删除 ${target.bookmarkIds.size} 篇文章？"
                    },
                )
            },
            text = {
                Text(
                    if (target.fromCollectionOnly) {
                        "这只会从当前收藏夹移除，其他收藏夹中的文章不受影响。"
                    } else {
                        "这会从所有收藏夹中彻底删除这些文章。"
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeBookmarksFromCollection(
                            bookmarkIds = target.bookmarkIds,
                            collectionId = if (target.fromCollectionOnly) {
                                selectedCollectionId
                            } else {
                                null
                            },
                        )
                        batchDeleteBookmarkTarget = null
                    },
                ) {
                    Text(if (target.fromCollectionOnly) "移除" else "删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { batchDeleteBookmarkTarget = null }) {
                    Text("取消")
                }
            },
        )
    }

    if (showAuthDialog) {
        AuthDialog(
            onDismiss = { showAuthDialog = false },
            onSignIn = viewModel::signIn,
            onSignUp = viewModel::signUp,
        )
    }

    if (showSyncSettingsDialog) {
        SyncSettingsDialog(
            config = supabaseConfig,
            onDismiss = { showSyncSettingsDialog = false },
            onSave = { url, key ->
                viewModel.saveSupabaseConfig(url, key) {
                    showSyncSettingsDialog = false
                    showAuthDialog = true
                }
            },
            onReset = {
                viewModel.resetSupabaseConfig()
                showSyncSettingsDialog = false
            },
        )
    }

    if (showAccountDialog && session != null) {
        AccountDialog(
            session = session!!,
            config = supabaseConfig,
            onDismiss = { showAccountDialog = false },
            onSync = {
                viewModel.syncNow { hasPendingSync ->
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            if (hasPendingSync) "已开始同步" else "已完成同步",
                        )
                    }
                }
                showAccountDialog = false
            },
            onSignOut = {
                viewModel.signOut()
                showAccountDialog = false
            },
            onSettings = {
                showAccountDialog = false
                showSyncSettingsDialog = true
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
private fun MainContent(
    collections: List<CollectionSummary>,
    bookmarks: List<Bookmark>,
    bookmarkCollectionIds: Map<String, List<String>>,
    collectionById: Map<String, CollectionSummary>,
    selectedCollectionId: String?,
    supabaseConfigured: Boolean,
    session: SupabaseSession?,
    listState: LazyListState,
    snackbarHostState: SnackbarHostState,
    onBackFromCollection: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenCollectionSearch: (String) -> Unit,
    onSyncClick: () -> Unit,
    onAccountClick: () -> Unit,
    onCreateCollection: () -> Unit,
    onLoginSync: () -> Unit,
    onOpenCollection: (String) -> Unit,
    onRenameCollection: (CollectionSummary) -> Unit,
    onDeleteCollection: (CollectionSummary) -> Unit,
    onReorderCollections: (List<String>) -> Unit,
    onOpenBookmark: (Bookmark) -> Unit,
    onBookmarkMenu: (Bookmark) -> Unit,
    onBatchMove: (List<String>) -> Unit,
    onBatchCopy: (List<String>) -> Unit,
    onBatchCopyLinks: (List<Bookmark>) -> Unit,
    onBatchRemoveFromCollection: (List<String>) -> Unit,
    onBatchDeleteCompletely: (List<String>) -> Unit,
) {
    val firstCoverByCollectionId = remember(collections, bookmarks, bookmarkCollectionIds) {
        collections.associate { collection ->
            val ids = bookmarkCollectionIds
                .filterValues { collectionIds -> collection.id in collectionIds }
                .keys
            collection.id to bookmarks.firstOrNull { it.id in ids }?.coverUrl
        }
    }
    var homeSection by rememberSaveable { mutableStateOf(HomeSection.COLLECTIONS) }
    var orderedCollections by remember(collections) { mutableStateOf(collections) }
    var draggingCollectionId by remember { mutableStateOf<String?>(null) }
    var draggingOffset by remember { mutableStateOf(0f) }
    var selectedBookmarkIds by rememberSaveable { mutableStateOf(emptySet<String>()) }
    val collectionDragStepPx = with(LocalDensity.current) { 124.dp.toPx() }
    val homeCollectionsListState = rememberLazyListState()
    val homeRecentListState = rememberLazyListState()
    val homePagerState = rememberPagerState(
        initialPage = if (homeSection == HomeSection.COLLECTIONS) 0 else 1,
        pageCount = { 2 },
    )
    val homePagerProgress = (
        homePagerState.currentPage + homePagerState.currentPageOffsetFraction
    ).coerceIn(0f, 1f)
    val visibleHomeSection = if (homePagerProgress < 0.5f) {
        HomeSection.COLLECTIONS
    } else {
        HomeSection.RECENT
    }

    LaunchedEffect(homeSection) {
        val targetPage = if (homeSection == HomeSection.COLLECTIONS) 0 else 1
        if (homePagerState.currentPage != targetPage) {
            homePagerState.animateScrollToPage(targetPage)
        }
    }

    LaunchedEffect(homePagerState) {
        snapshotFlow { homePagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                homeSection = if (page == 0) HomeSection.COLLECTIONS else HomeSection.RECENT
            }
    }

    LaunchedEffect(selectedCollectionId) {
        selectedBookmarkIds = emptySet()
    }

    BackHandler(enabled = selectedBookmarkIds.isNotEmpty()) {
        selectedBookmarkIds = emptySet()
    }

    AnimatedContent(
        targetState = selectedCollectionId,
        transitionSpec = {
            if (targetState != null) {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(PAGE_TRANSITION_DURATION_MS),
                ) togetherWith slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(PAGE_TRANSITION_DURATION_MS),
                )
            } else {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(PAGE_TRANSITION_DURATION_MS),
                ) togetherWith slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(PAGE_TRANSITION_DURATION_MS),
                )
            }.using(SizeTransform(clip = false))
        },
        label = "collection-page-transition",
    ) { currentCollectionId ->
        val inCollection = currentCollectionId != null
        val currentCollection = collections.firstOrNull { it.id == currentCollectionId }
        val pageBookmarks = remember(currentCollectionId, bookmarks, bookmarkCollectionIds) {
            if (currentCollectionId == null) {
                emptyList()
            } else {
                val ids = bookmarkCollectionIds
                    .filterValues { collectionIds -> currentCollectionId in collectionIds }
                    .keys
                bookmarks.filter { it.id in ids }
            }
        }
        val pageBookmarkIds = remember(pageBookmarks) { pageBookmarks.map { it.id }.toSet() }
        LaunchedEffect(pageBookmarkIds) {
            selectedBookmarkIds = selectedBookmarkIds intersect pageBookmarkIds
        }
        val inSelectionMode = inCollection && selectedBookmarkIds.isNotEmpty()
        val selectedBookmarks = remember(pageBookmarks, selectedBookmarkIds) {
            pageBookmarks.filter { it.id in selectedBookmarkIds }
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        if (inSelectionMode) {
                            IconButton(onClick = { selectedBookmarkIds = emptySet() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = "取消选择",
                                )
                            }
                        } else if (inCollection) {
                            IconButton(onClick = onBackFromCollection) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = "返回",
                                )
                            }
                        }
                    },
                    title = {
                        if (inSelectionMode) {
                            Text(
                                text = "已选择 ${selectedBookmarkIds.size} 篇",
                                fontWeight = FontWeight.Bold,
                            )
                        } else if (inCollection) {
                            Column {
                                Text(
                                    text = currentCollection?.name.orEmpty(),
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = currentCollection?.let { "${it.articleCount} 篇文章" }.orEmpty(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            Image(
                                painter = painterResource(R.drawable.mz_logo_header),
                                contentDescription = "码住",
                                modifier = Modifier.size(46.dp),
                            )
                        }
                    },
                    actions = {
                        if (inSelectionMode) {
                            TextButton(
                                onClick = {
                                    selectedBookmarkIds = if (selectedBookmarkIds.size == pageBookmarkIds.size) {
                                        emptySet()
                                    } else {
                                        pageBookmarkIds
                                    }
                                },
                            ) {
                                Text(
                                    if (selectedBookmarkIds.size == pageBookmarkIds.size) {
                                        "取消全选"
                                    } else {
                                        "全选"
                                    },
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    if (currentCollectionId == null) {
                                        onOpenSearch()
                                    } else {
                                        onOpenCollectionSearch(currentCollectionId)
                                    }
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = "搜索",
                                )
                            }
                            if (!inCollection) {
                                IconButton(onClick = onSyncClick) {
                                    Icon(
                                        imageVector = Icons.Outlined.CloudUpload,
                                        contentDescription = "同步",
                                    )
                                }
                                IconButton(onClick = onAccountClick) {
                                    Icon(
                                        imageVector = Icons.Outlined.AccountCircle,
                                        contentDescription = "账号",
                                    )
                                }
                            } else {
                                Spacer(Modifier.size(12.dp))
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            },
            floatingActionButton = {
                if (!inCollection && visibleHomeSection == HomeSection.COLLECTIONS) {
                    FloatingActionButton(
                        onClick = onCreateCollection,
                        modifier = Modifier.padding(end = 8.dp, bottom = 12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "新建收藏夹",
                        )
                    }
                }
            },
            bottomBar = {
                if (inSelectionMode) {
                    BatchActionBar(
                        selectedCount = selectedBookmarkIds.size,
                        onMove = {
                            onBatchMove(selectedBookmarkIds.toList())
                            selectedBookmarkIds = emptySet()
                        },
                        onCopy = {
                            onBatchCopy(selectedBookmarkIds.toList())
                            selectedBookmarkIds = emptySet()
                        },
                        onCopyLinks = {
                            onBatchCopyLinks(selectedBookmarks)
                            selectedBookmarkIds = emptySet()
                        },
                        onRemove = {
                            onBatchRemoveFromCollection(selectedBookmarkIds.toList())
                            selectedBookmarkIds = emptySet()
                        },
                        onDelete = {
                            onBatchDeleteCompletely(selectedBookmarkIds.toList())
                            selectedBookmarkIds = emptySet()
                        },
                    )
                }
            },
        ) { padding ->
            if (inCollection) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    state = listState,
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = 132.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (pageBookmarks.isEmpty()) {
                        item {
                            EmptyState(inCollection = true)
                        }
                    } else {
                        items(
                            items = pageBookmarks,
                            key = Bookmark::id,
                            contentType = { "bookmark" },
                        ) { bookmark ->
                            BookmarkRow(
                                bookmark = bookmark,
                                syncEnabled = supabaseConfigured,
                                collectionNames = bookmarkCollectionIds[bookmark.id]
                                    .orEmpty()
                                    .mapNotNull { collectionById[it]?.name },
                                selected = bookmark.id in selectedBookmarkIds,
                                selectionMode = inSelectionMode,
                                onClick = {
                                    if (inSelectionMode) {
                                        selectedBookmarkIds = if (bookmark.id in selectedBookmarkIds) {
                                            selectedBookmarkIds - bookmark.id
                                        } else {
                                            selectedBookmarkIds + bookmark.id
                                        }
                                    } else {
                                        onOpenBookmark(bookmark)
                                    }
                                },
                                onLongClick = {
                                    if (inCollection) {
                                        selectedBookmarkIds = selectedBookmarkIds + bookmark.id
                                    }
                                },
                                onMenu = { onBookmarkMenu(bookmark) },
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    if (session == null) {
                        LoginSyncBanner(
                            configured = supabaseConfigured,
                            onClick = onLoginSync,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(top = 12.dp, bottom = 12.dp),
                        )
                    }

                    HomeSegmentedSwitch(
                        selected = visibleHomeSection,
                        position = homePagerProgress,
                        onSelected = { homeSection = it },
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(top = if (session == null) 0.dp else 12.dp),
                    )

                    HomeStatsLine(
                        selected = visibleHomeSection,
                        collectionCount = collections.size,
                        bookmarkCount = bookmarks.size,
                        unsyncedCount = collections.sumOf { it.unsyncedCount },
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(top = 8.dp, bottom = 8.dp),
                    )

                    HorizontalPager(
                        state = homePagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        userScrollEnabled = draggingCollectionId == null,
                        key = { page -> page },
                    ) { page ->
                        val section = if (page == 0) HomeSection.COLLECTIONS else HomeSection.RECENT
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = if (section == HomeSection.COLLECTIONS) {
                                homeCollectionsListState
                            } else {
                                homeRecentListState
                            },
                            userScrollEnabled = draggingCollectionId == null,
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 0.dp,
                                bottom = 132.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            if (section == HomeSection.COLLECTIONS) {
                                itemsIndexed(
                                    items = orderedCollections,
                                    key = { _, collection -> collection.id },
                                    contentType = { _, _ -> "collection" },
                                ) { _, collection ->
                                    ReorderableCollectionItem(
                                        collection = collection,
                                        collections = orderedCollections,
                                        coverUrl = firstCoverByCollectionId[collection.id],
                                        draggingCollectionId = draggingCollectionId,
                                        draggingOffset = draggingOffset,
                                        dragStepPx = collectionDragStepPx,
                                        onDraggingOffsetChange = { draggingOffset = it },
                                        onDraggingCollectionChange = { draggingCollectionId = it },
                                        onCollectionsChange = { orderedCollections = it },
                                        onReorderFinished = { reorderedCollections ->
                                            onReorderCollections(reorderedCollections.map { it.id })
                                        },
                                        onOpen = { onOpenCollection(collection.id) },
                                        onRename = { onRenameCollection(collection) },
                                        onDelete = { onDeleteCollection(collection) },
                                    )
                                }
                            } else if (bookmarks.isEmpty()) {
                                item {
                                    EmptyState(inCollection = false)
                                }
                            } else {
                                items(
                                    items = bookmarks,
                                    key = Bookmark::id,
                                    contentType = { "bookmark" },
                                ) { bookmark ->
                                    BookmarkRow(
                                        bookmark = bookmark,
                                        syncEnabled = supabaseConfigured,
                                        collectionNames = bookmarkCollectionIds[bookmark.id]
                                            .orEmpty()
                                            .mapNotNull { collectionById[it]?.name },
                                        onClick = { onOpenBookmark(bookmark) },
                                        onMenu = { onBookmarkMenu(bookmark) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
