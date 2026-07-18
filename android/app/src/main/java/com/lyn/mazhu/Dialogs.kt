package com.lyn.mazhu

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lyn.mazhu.data.CollectionSummary
import com.lyn.mazhu.supabase.AuthResult
import com.lyn.mazhu.supabase.SupabaseConfig
import com.lyn.mazhu.supabase.SupabaseSession

private enum class AuthMode {
    SIGN_IN,
    SIGN_UP,
}

@Composable
internal fun MultiCollectionDialog(
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
internal fun LoginSyncBanner(
    configured: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(20.dp),
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
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.CloudUpload,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
internal fun SyncSettingsDialog(
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

@Composable
internal fun AuthDialog(
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
internal fun AccountDialog(
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
internal fun CollectionNameDialog(
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
