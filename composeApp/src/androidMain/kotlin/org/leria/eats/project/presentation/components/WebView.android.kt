package org.leria.eats.project.presentation.components

import android.graphics.Bitmap
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
actual fun WebView(
    modifier: Modifier,
    url: String,
    onSuccess: (orderId: String) -> Unit,
    onCancel: () -> Unit,
    onLoadingChanged: (Boolean) -> Unit
) {
    val webViewState = remember { mutableStateOf<WebView?>(null) }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        onLoadingChanged(true)
                        if (url?.contains("success", ignoreCase = true) == true) {
                            // Extrai o order_id da URL
                            val orderId = url.substringAfter("order_id=", "")
                            if (orderId.isNotEmpty()) {
                                onSuccess(orderId)
                            }
                        } else if (url?.contains("cancel", ignoreCase = true) == true) {
                            onCancel()
                        }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        onLoadingChanged(false)
                    }
                }
                webViewState.value = this
                loadUrl(url)
            }
        },
        update = { webView ->
            webViewState.value = webView
            webView.loadUrl(url)
        }
    )

    // Handle Android system back: navigate WebView history first, otherwise treat as cancel
    BackHandler(enabled = true) {
        val w = webViewState.value
        if (w != null && w.canGoBack()) {
            w.goBack()
        } else {
            onCancel()
        }
    }
}
