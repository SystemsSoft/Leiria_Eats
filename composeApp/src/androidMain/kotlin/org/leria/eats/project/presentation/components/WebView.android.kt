package org.leria.eats.project.presentation.components

import android.graphics.Bitmap
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
actual fun WebView(
    modifier: Modifier,
    url: String,
    onSuccess: () -> Unit,
    onCancel: () -> Unit
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        if (url?.contains("success", ignoreCase = true) == true) {
                            onSuccess()
                        } else if (url?.contains("cancel", ignoreCase = true) == true) {
                            onCancel()
                        }
                    }
                }
                loadUrl(url)
            }
        },
        update = { webView ->
            webView.loadUrl(url)
        }
    )
}
