package com.lyn.mazhu

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.MoreVert
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private enum class CollectionPickerMode {
    MOVE,
    COPY,
}

private data class CollectionPickerTarget(
    val bookmark: Bookmark,
    val mode: CollectionPickerMode,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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

@OptIn(ExperimentalMaterial3Api::class)
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
    var selectedCollectionId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<CollectionSummary?>(null) }
    var deleteTarget by remember { mutableStateOf<CollectionSummary?>(null) }
    var actionTarget by remember { mutableStateOf<Bookmark?>(null) }
    var deleteBookmarkTarget by remember { mutableStateOf<Bookmark?>(null) }
    var collectionPickerTarget by remember { mutableStateOf<CollectionPickerTarget?>(null) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var showSyncSettingsDialog by remember { mutableStateOf(false) }
    var clipboardUrl by remember { mutableStateOf<String?>(null) }

    val selectedCollection = collections.firstOrNull { it.id == selectedCollectionId }
    val collectionById = collections.associateBy { it.id }
    val bookmarkCollectionIds = bookmarkCollections
        .groupBy { it.bookmarkId }
        .mapValues { entry -> entry.value.map { it.collectionId } }
    val selectedBookmarkIds = selectedCollectionId?.let { collectionId ->
        bookmarkCollections
            .filter { it.collectionId == collectionId }
            .map { it.bookmarkId }
            .toSet()
    }
    val query = searchQuery.trim()
    val visibleBookmarks = bookmarks
        .filter { bookmark -> selectedBookmarkIds == null || bookmark.id in selectedBookmarkIds }
        .filter { bookmark ->
            query.isBlank() ||
                bookmark.title.contains(query, ignoreCase = true) ||
                bookmark.originalUrl.contains(query, ignoreCase = true) ||
                bookmark.accountName?.contains(query, ignoreCase = true) == true ||
                bookmark.contentText?.contains(query, ignoreCase = true) == true
        }

    LaunchedEffect(selectedCollectionId) {
        listState.scrollToItem(0)
    }

    LaunchedEffect(Unit) {
        val detectedUrl = detectWechatClipboardUrl(context)
        if (detectedUrl != null && !wasClipboardUrlHandled(context, detectedUrl)) {
            clipboardUrl = detectedUrl
        }
    }

    BackHandler(enabled = selectedCollectionId != null) {
        selectedCollectionId = null
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (selectedCollection != null) {
                        IconButton(onClick = { selectedCollectionId = null }) {
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
                            text = selectedCollection?.name ?: "码住",
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = selectedCollection?.let { "${it.articleCount} 篇文章" }
                                ?: "把值得看的文章先存下来",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { searchQuery = if (searchQuery.isBlank()) " " else "" }) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "搜索",
                        )
                    }
                    IconButton(
                        onClick = {
                            if (session == null) {
                                if (supabaseConfig.isConfigured) {
                                    showAuthDialog = true
                                } else {
                                    showSyncSettingsDialog = true
                                }
                            } else {
                                viewModel.syncNow()
                                scope.launch {
                                    snackbarHostState.showSnackbar("已开始同步")
                                }
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CloudUpload,
                            contentDescription = "同步",
                        )
                    }
                    IconButton(
                        onClick = {
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
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccountCircle,
                            contentDescription = "账号",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            if (selectedCollectionId == null) {
                FloatingActionButton(onClick = { showCreateDialog = true }) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "新建收藏夹",
                    )
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            state = listState,
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SearchField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                )
            }

            if (selectedCollectionId == null) {
                if (session == null) {
                    item {
                        LoginSyncBanner(
                            configured = supabaseConfig.isConfigured,
                            onClick = {
                                if (supabaseConfig.isConfigured) {
                                    showAuthDialog = true
                                } else {
                                    showSyncSettingsDialog = true
                                }
                            },
                        )
                    }
                }

                item {
                    Text(
                        text = "收藏夹",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                items(
                    items = collections,
                    key = CollectionSummary::id,
                ) { collection ->
                    CollectionCard(
                        collection = collection,
                        onOpen = { selectedCollectionId = collection.id },
                        onRename = { renameTarget = collection },
                        onDelete = { deleteTarget = collection },
                    )
                }

                item {
                    Text(
                        text = "最近收藏",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }

            if (visibleBookmarks.isEmpty()) {
                item {
                    EmptyState(inCollection = selectedCollectionId != null)
                }
            } else {
                items(
                    items = visibleBookmarks,
                    key = Bookmark::id,
                ) { bookmark ->
                    BookmarkRow(
                        bookmark = bookmark,
                        syncEnabled = supabaseConfig.isConfigured,
                        collectionNames = bookmarkCollectionIds[bookmark.id]
                            .orEmpty()
                            .mapNotNull { collectionById[it]?.name },
                        onMenu = { actionTarget = bookmark },
                    )
                }
            }
        }
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

    clipboardUrl?.let { url ->
        AlertDialog(
            onDismissRequest = {
                markClipboardUrlHandled(context, url)
                clipboardUrl = null
            },
            title = { Text("检测到公众号文章链接") },
            text = { Text(url) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.saveSharedText(url) { result ->
                            markClipboardUrlHandled(context, url)
                            clipboardUrl = null
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    when (result) {
                                        is SaveResult.Saved -> "已保存到默认收藏夹"
                                        is SaveResult.AlreadySaved -> "这篇文章已经码住了"
                                        SaveResult.InvalidShare -> "没有找到可以收藏的链接"
                                    },
                                )
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
                        markClipboardUrlHandled(context, url)
                        clipboardUrl = null
                    },
                ) {
                    Text("忽略")
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
        AlertDialog(
            onDismissRequest = { actionTarget = null },
            title = { Text(bookmark.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            collectionPickerTarget = CollectionPickerTarget(
                                bookmark = bookmark,
                                mode = CollectionPickerMode.MOVE,
                            )
                            actionTarget = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("移动到收藏夹", modifier = Modifier.fillMaxWidth())
                    }
                    TextButton(
                        onClick = {
                            collectionPickerTarget = CollectionPickerTarget(
                                bookmark = bookmark,
                                mode = CollectionPickerMode.COPY,
                            )
                            actionTarget = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("复制到其他收藏夹", modifier = Modifier.fillMaxWidth())
                    }
                    TextButton(
                        onClick = {
                            copyToClipboard(context, bookmark.originalUrl)
                            actionTarget = null
                            scope.launch {
                                snackbarHostState.showSnackbar("链接已复制")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("复制链接", modifier = Modifier.fillMaxWidth())
                    }
                    TextButton(
                        onClick = {
                            deleteBookmarkTarget = bookmark
                            actionTarget = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("删除", modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { actionTarget = null }) {
                    Text("取消")
                }
            },
        )
    }

    deleteBookmarkTarget?.let { bookmark ->
        AlertDialog(
            onDismissRequest = { deleteBookmarkTarget = null },
            title = { Text("删除文章？") },
            text = {
                Text(
                    if (selectedCollectionId == null) {
                        "这会从所有收藏夹中删除这篇文章。"
                    } else {
                        "这只会从当前收藏夹移除，其他收藏夹中仍会保留。"
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeBookmarkFromCollection(
                            bookmarkId = bookmark.id,
                            collectionId = selectedCollectionId,
                        )
                        deleteBookmarkTarget = null
                    },
                ) {
                    Text("删除")
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
                viewModel.syncNow()
                showAccountDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("已开始同步")
                }
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

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    TextField(
        value = value.trimStart(),
        onValueChange = onValueChange,
        leadingIcon = {
            Icon(Icons.Outlined.Search, contentDescription = null)
        },
        label = { Text("搜索收藏文章") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun MultiCollectionDialog(
    title: String,
    collections: List<CollectionSummary>,
    initiallySelectedIds: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
) {
    var selectedIds by remember(initiallySelectedIds) {
        mutableStateOf(initiallySelectedIds)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                collections.forEach { collection ->
                    val selected = collection.id in selectedIds
                    TextButton(
                        onClick = {
                            selectedIds = if (selected) {
                                selectedIds - collection.id
                            } else {
                                selectedIds + collection.id
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = if (selected) "✓" else "",
                                modifier = Modifier.size(24.dp),
                            )
                            Text(
                                text = collection.name,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedIds.toList()) },
                enabled = selectedIds.isNotEmpty(),
            ) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun LoginSyncBanner(
    configured: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.CloudUpload,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
            ) {
                Text(
                    text = if (configured) "登录后同步到云端" else "当前仅保存在本机",
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (configured) {
                        "未登录时仍会正常保存在手机本地"
                    } else {
                        "启用云同步后可在换手机和电脑端继续使用"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Text(
                text = if (configured) "登录" else "设置",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun SyncSettingsDialog(
    config: SupabaseConfig,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onReset: () -> Unit,
) {
    var url by remember(config) { mutableStateOf(config.url) }
    var publishableKey by remember(config) { mutableStateOf(config.publishableKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("云同步设置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "不启用云同步也可以正常收藏文章。启用后，收藏数据会同步到 Supabase，电脑端 CLI 和 Skill 也能读取。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Supabase URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextField(
                    value = publishableKey,
                    onValueChange = { publishableKey = it },
                    label = { Text("Publishable key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(url, publishableKey) },
                enabled = url.isNotBlank() && publishableKey.isNotBlank(),
            ) {
                Text("保存并登录")
            }
        },
        dismissButton = {
            Row {
                if (config.isConfigured) {
                    TextButton(onClick = onReset) {
                        Text("关闭云同步")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        },
    )
}

private enum class AuthMode {
    SIGN_IN,
    SIGN_UP,
}

@Composable
private fun AuthDialog(
    onDismiss: () -> Unit,
    onSignIn: (String, String, (AuthResult) -> Unit) -> Unit,
    onSignUp: (String, String, (AuthResult) -> Unit) -> Unit,
) {
    var mode by remember { mutableStateOf(AuthMode.SIGN_IN) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    fun handleResult(result: AuthResult) {
        loading = false
        when (result) {
            is AuthResult.Authenticated -> onDismiss()
            AuthResult.EmailVerificationRequired -> {
                mode = AuthMode.SIGN_IN
                message = "注册邮件已发送，请验证邮箱后返回登录"
            }
            is AuthResult.Failed -> message = result.message
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!loading) {
                onDismiss()
            }
        },
        title = {
            Text(if (mode == AuthMode.SIGN_IN) "登录云同步" else "注册云同步账号")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "账号只用于同步你的收藏数据，本地收藏不依赖登录。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextField(
                    value = email,
                    onValueChange = {
                        email = it
                        message = null
                    },
                    label = { Text("邮箱") },
                    singleLine = true,
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextField(
                    value = password,
                    onValueChange = {
                        password = it
                        message = null
                    },
                    label = { Text("密码（至少 6 位）") },
                    singleLine = true,
                    enabled = !loading,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                message?.let {
                    Text(
                        text = it,
                        color = if (it.startsWith("注册邮件")) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                TextButton(
                    onClick = {
                        mode = if (mode == AuthMode.SIGN_IN) {
                            AuthMode.SIGN_UP
                        } else {
                            AuthMode.SIGN_IN
                        }
                        message = null
                    },
                    enabled = !loading,
                ) {
                    Text(
                        if (mode == AuthMode.SIGN_IN) {
                            "还没有账号？注册"
                        } else {
                            "已经有账号？登录"
                        },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    loading = true
                    message = null
                    if (mode == AuthMode.SIGN_IN) {
                        onSignIn(email, password, ::handleResult)
                    } else {
                        onSignUp(email, password, ::handleResult)
                    }
                },
                enabled = !loading &&
                    email.isNotBlank() &&
                    password.length >= 6,
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(if (mode == AuthMode.SIGN_IN) "登录" else "注册")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !loading,
            ) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun AccountDialog(
    session: SupabaseSession,
    config: SupabaseConfig,
    onDismiss: () -> Unit,
    onSync: () -> Unit,
    onSignOut: () -> Unit,
    onSettings: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("云同步账号") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(session.email ?: "已登录")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.CloudDone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "会话已保存在本机",
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = config.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSync) {
                Text("立即同步")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onSettings) {
                    Text("设置")
                }
                TextButton(onClick = onSignOut) {
                    Text("退出登录")
                }
                TextButton(onClick = onDismiss) {
                    Text("关闭")
                }
            }
        },
    )
}

@Composable
private fun CollectionCard(
    collection: CollectionSummary,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val isDefault = collection.id == BookmarkRepository.DEFAULT_COLLECTION_ID

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, top = 18.dp, bottom = 18.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(13.dp)
                        .size(30.dp),
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
            ) {
                Text(
                    text = collection.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${collection.articleCount} 篇文章",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (collection.unsyncedCount > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CloudOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(17.dp),
                    )
                    Text(
                        text = "${collection.unsyncedCount} 未同步",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (!isDefault) {
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

@Composable
private fun CollectionNameDialog(
    title: String,
    confirmLabel: String,
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            TextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("收藏夹名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun EmptyState(inCollection: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = if (inCollection) {
                Icons.Outlined.Folder
            } else {
                Icons.AutoMirrored.Outlined.Article
            },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(42.dp),
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = if (inCollection) "这个收藏夹还是空的" else "还没有码住任何文章",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
        if (!inCollection) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "在微信文章右上角选择分享，然后选择“码住”",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BookmarkRow(
    bookmark: Bookmark,
    syncEnabled: Boolean,
    collectionNames: List<String>,
    onMenu: () -> Unit,
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(bookmark.originalUrl)),
                )
            },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = bookmark.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
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
            Spacer(Modifier.height(8.dp))
            Text(
                text = bookmark.originalUrl,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            bookmark.accountName?.takeIf(String::isNotBlank)?.let { accountName ->
                Spacer(Modifier.height(6.dp))
                Text(
                    text = accountName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (collectionNames.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = collectionNames.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(10.dp))
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

private fun copyToClipboard(
    context: Context,
    text: String,
) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("文章链接", text))
}

private fun detectWechatClipboardUrl(context: Context): String? {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val text = clipboard.primaryClip
        ?.takeIf { it.itemCount > 0 }
        ?.getItemAt(0)
        ?.coerceToText(context)
        ?.toString()
        .orEmpty()
    val url = ShareTextParser.extractUrl(text) ?: return null
    return url.takeIf { Uri.parse(it).host == "mp.weixin.qq.com" }
}

private fun wasClipboardUrlHandled(
    context: Context,
    url: String,
): Boolean {
    val preferences = context.getSharedPreferences("clipboard_prompt", Context.MODE_PRIVATE)
    return preferences.getString("last_url", null) == ShareTextParser.normalizeUrl(url)
}

private fun markClipboardUrlHandled(
    context: Context,
    url: String,
) {
    context.getSharedPreferences("clipboard_prompt", Context.MODE_PRIVATE)
        .edit()
        .putString("last_url", ShareTextParser.normalizeUrl(url))
        .apply()
}

private fun formatTime(timestamp: Long): String =
    SimpleDateFormat("MM月dd日 HH:mm", Locale.CHINA).format(Date(timestamp))

private fun Bookmark.statusLabel(syncEnabled: Boolean): String =
    when (parseStatus) {
        com.lyn.mazhu.data.BookmarkStatus.PARSE_PENDING -> "等待解析"
        com.lyn.mazhu.data.BookmarkStatus.PARSE_PROCESSING -> "正在解析"
        com.lyn.mazhu.data.BookmarkStatus.PARSE_FAILED -> "解析失败"
        else -> if (!syncEnabled) {
            "仅本地"
        } else when (syncStatus) {
            com.lyn.mazhu.data.BookmarkStatus.SYNC_SYNCED -> "已同步"
            com.lyn.mazhu.data.BookmarkStatus.SYNC_SYNCING -> "同步中"
            else -> "未同步"
        }
    }
