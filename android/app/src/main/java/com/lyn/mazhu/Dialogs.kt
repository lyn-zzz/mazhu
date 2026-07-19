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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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

private fun looksLikeEmail(value: String): Boolean {
    val trimmed = value.trim()
    return trimmed.contains("@") && trimmed.substringAfter("@").contains(".")
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
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf(false) }
    val isSignIn = mode == AuthMode.SIGN_IN

    fun setMode(nextMode: AuthMode) {
        mode = nextMode
        password = ""
        confirmPassword = ""
        passwordVisible = false
        confirmPasswordVisible = false
        message = null
        successMessage = false
    }

    fun handleResult(result: AuthResult) {
        loading = false
        when (result) {
            is AuthResult.Authenticated -> onDismiss()
            AuthResult.EmailVerificationRequired -> {
                mode = AuthMode.SIGN_IN
                password = ""
                confirmPassword = ""
                passwordVisible = false
                confirmPasswordVisible = false
                successMessage = true
                message = "验证邮件已发送。若该邮箱已注册，请直接登录。"
            }
            is AuthResult.Failed -> {
                successMessage = false
                message = result.message.ifBlank {
                    if (isSignIn) "登录失败，请检查邮箱和密码" else "注册失败，请稍后重试"
                }
            }
        }
    }

    fun submit() {
        val normalizedEmail = email.trim()
        successMessage = false
        message = when {
            !looksLikeEmail(normalizedEmail) -> "请输入有效邮箱"
            password.length < 6 -> "密码至少需要 6 位"
            !isSignIn && password != confirmPassword -> "两次输入的密码不一致"
            else -> null
        }
        if (message != null) {
            return
        }
        loading = true
        if (isSignIn) {
            onSignIn(normalizedEmail, password, ::handleResult)
        } else {
            onSignUp(normalizedEmail, password, ::handleResult)
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!loading) {
                onDismiss()
            }
        },
        title = {
            Text(if (isSignIn) "登录码住" else "创建码住账号")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (isSignIn) {
                        "登录后可把收藏同步到云端，本地收藏仍会保存在手机里。"
                    } else {
                        "创建账号后需要验证邮箱，再返回码住登录同步。"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        message = null
                        successMessage = false
                    },
                    label = { Text("邮箱") },
                    singleLine = true,
                    enabled = !loading,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        message = null
                        successMessage = false
                    },
                    label = { Text("密码（至少 6 位）") },
                    singleLine = true,
                    enabled = !loading,
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = if (isSignIn) ImeAction.Done else ImeAction.Next,
                    ),
                    trailingIcon = {
                        IconButton(
                            onClick = { passwordVisible = !passwordVisible },
                            enabled = !loading,
                        ) {
                            Icon(
                                imageVector = if (passwordVisible) {
                                    Icons.Outlined.VisibilityOff
                                } else {
                                    Icons.Outlined.Visibility
                                },
                                contentDescription = if (passwordVisible) {
                                    "隐藏密码"
                                } else {
                                    "显示密码"
                                },
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!isSignIn) {
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            message = null
                            successMessage = false
                        },
                        label = { Text("确认密码") },
                        singleLine = true,
                        enabled = !loading,
                        visualTransformation = if (confirmPasswordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        trailingIcon = {
                            IconButton(
                                onClick = { confirmPasswordVisible = !confirmPasswordVisible },
                                enabled = !loading,
                            ) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible) {
                                        Icons.Outlined.VisibilityOff
                                    } else {
                                        Icons.Outlined.Visibility
                                    },
                                    contentDescription = if (confirmPasswordVisible) {
                                        "隐藏确认密码"
                                    } else {
                                        "显示确认密码"
                                    },
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                message?.let {
                    Text(
                        text = it,
                        color = if (successMessage) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                TextButton(
                    onClick = {
                        setMode(
                            if (isSignIn) {
                                AuthMode.SIGN_UP
                            } else {
                                AuthMode.SIGN_IN
                            },
                        )
                    },
                    enabled = !loading,
                ) {
                    Text(
                        if (isSignIn) {
                            "还没有账号？创建账号"
                        } else {
                            "已有账号？返回登录"
                        },
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = ::submit,
                enabled = !loading &&
                    email.isNotBlank() &&
                    password.isNotBlank() &&
                    (isSignIn || confirmPassword.isNotBlank()),
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(if (isSignIn) "登录" else "创建账号")
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
