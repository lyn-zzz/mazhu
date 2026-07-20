package com.lyn.mazhu

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

private const val WECHAT_ARTICLE_REFERER = "https://mp.weixin.qq.com/"
private const val WECHAT_ANDROID_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 16; Mobile) AppleWebKit/537.36 " +
        "Chrome/135.0 Mobile Safari/537.36 MicroMessenger/8.0.58"

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun WebViewReaderScreen(
    title: String,
    url: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var progress by remember(url) { mutableIntStateOf(0) }
    var isLoading by remember(url) { mutableStateOf(true) }
    var canGoBack by remember(url) { mutableStateOf(false) }
    val cookieManager = remember {
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
        }
    }
    val webView = remember(url) {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.loadsImagesAutomatically = true
            settings.blockNetworkImage = false
            settings.userAgentString = WECHAT_ANDROID_USER_AGENT
            settings.mediaPlaybackRequiresUserGesture = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            settings.javaScriptCanOpenWindowsAutomatically = true
            cookieManager.setAcceptThirdPartyCookies(this, true)
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    progress = newProgress
                    isLoading = newProgress < 100
                }
            }
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?,
                ): Boolean {
                    val nextUrl = request?.url?.toString().orEmpty()
                    if (nextUrl.startsWith("http://") || nextUrl.startsWith("https://")) {
                        return false
                    }
                    openExternalUrl(context, nextUrl)
                    return true
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    isLoading = true
                    canGoBack = view?.canGoBack() == true
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    isLoading = false
                    canGoBack = view?.canGoBack() == true
                }
            }
            loadUrl(
                url,
                mapOf("Referer" to WECHAT_ARTICLE_REFERER),
            )
        }
    }

    BackHandler {
        if (canGoBack && webView.canGoBack()) {
            webView.goBack()
            canGoBack = webView.canGoBack()
        } else {
            onDismiss()
        }
    }

    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ReaderTopBar(
                title = title,
                url = url,
                loading = isLoading,
                progress = progress,
                onBack = {
                    if (canGoBack && webView.canGoBack()) {
                        webView.goBack()
                        canGoBack = webView.canGoBack()
                    } else {
                        onDismiss()
                    }
                },
                onOpenExternal = { openExternalUrl(context, url) },
            )
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { webView },
                    modifier = Modifier.fillMaxSize(),
                )
                if (isLoading && progress == 0) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }
    }
}

@Composable
private fun ReaderTopBar(
    title: String,
    url: String,
    loading: Boolean,
    progress: Int,
    onBack: () -> Unit,
    onOpenExternal: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "返回",
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp),
            ) {
                Text(
                    text = title.ifBlank { "公众号文章" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = url,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onOpenExternal) {
                Icon(
                    imageVector = Icons.Outlined.OpenInBrowser,
                    contentDescription = "用浏览器打开",
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        if (loading) {
            LinearProgressIndicator(
                progress = { (progress.coerceIn(0, 100) / 100f).coerceAtLeast(0.08f) },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Spacer(Modifier.height(4.dp))
        }
    }
}
