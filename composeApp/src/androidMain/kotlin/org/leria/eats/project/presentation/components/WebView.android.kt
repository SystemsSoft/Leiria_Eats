package org.leria.eats.project.presentation.components

import android.graphics.Bitmap
import android.graphics.Color
import android.view.View
import android.webkit.WebSettings
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
                // Improve navigation/rendering smoothness
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                // Allow mixed content in case the payment page loads resources from different schemes
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                // Visual tweaks for smoother transition
                setBackgroundColor(Color.WHITE)
                overScrollMode = View.OVER_SCROLL_NEVER
                scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
                // Prefer hardware layer when available for smoother rendering
                setLayerType(View.LAYER_TYPE_HARDWARE, null)

                alpha = 0f // start transparent and fade in when ready

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        onLoadingChanged(true)
                        // keep transparent while loading to allow smooth fade-in
                        view?.alpha = 0f

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
                        // fade in the webview content for a smooth transition
                        view?.animate()?.alpha(1f)?.setDuration(250)?.start()
                    }
                }
                webViewState.value = this
                loadUrl(url)
            }
        },
        update = { webView ->
            webViewState.value = webView
            // if URL changed, load it smoothly
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
