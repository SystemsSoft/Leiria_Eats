package org.leria.eats.project.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import platform.CoreGraphics.CGRect
import platform.Foundation.NSURLRequest
import platform.Foundation.NSURL
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIApplication
import platform.UIKit.UIColor
import platform.UIKit.UIModalPresentationFullScreen
import platform.UIKit.UIViewController
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKNavigationActionPolicy
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.WKUserContentController
import platform.WebKit.WKScriptMessage
import platform.WebKit.WKScriptMessageHandlerProtocol
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun WebView(
    modifier: Modifier,
    url: String,
    onSuccess: (orderId: String) -> Unit,
    onCancel: () -> Unit,
    onLoadingChanged: (Boolean) -> Unit
) {
    val onSuccessRef = rememberUpdatedState(onSuccess)
    val onCancelRef  = rememberUpdatedState(onCancel)
    val onLoadRef    = rememberUpdatedState(onLoadingChanged)

    DisposableEffect(url) {
        val userContent = WKUserContentController()
        val config      = WKWebViewConfiguration()
        config.userContentController = userContent

        val webView = WKWebView(frame = cValue<CGRect>(), configuration = config).apply {
            backgroundColor = UIColor.blackColor
            opaque          = false
            scrollView.bounces = false
        }

        // WKScriptMessageHandler — receives messages posted by JS running in the page.
        // We inject a small script via evaluateJavaScript after each page load to post
        // "success:<orderId>" or "cancel" based on the URL.
        val messageHandler = object : NSObject(), WKScriptMessageHandlerProtocol {
            override fun userContentController(
                userContentController: WKUserContentController,
                didReceiveScriptMessage: WKScriptMessage
            ) {
                val msg = didReceiveScriptMessage.body as? String ?: return
                when {
                    msg.startsWith("success") -> {
                        val orderId = msg.substringAfter("success:", "")
                        onSuccessRef.value(orderId)
                    }
                    msg == "cancel" -> onCancelRef.value()
                }
            }
        }
        userContent.addScriptMessageHandler(messageHandler, name = "checkoutBridge")

        // Navigation delegate — intercept redirect URLs before they load
        val navDelegate = object : NSObject(), WKNavigationDelegateProtocol {
            @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
            override fun webView(
                webView: WKWebView,
                decidePolicyForNavigationAction: WKNavigationAction,
                decisionHandler: (WKNavigationActionPolicy) -> Unit
            ) {
                val navUrl = decidePolicyForNavigationAction.request.URL?.absoluteString ?: ""
                when {
                    navUrl.contains("success", ignoreCase = true) -> {
                        val orderId = navUrl.substringAfter("order_id=", "").substringBefore("&")
                        onSuccessRef.value(orderId)
                        decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
                    }
                    navUrl.contains("cancel", ignoreCase = true) -> {
                        onCancelRef.value()
                        decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
                    }
                    else -> decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyAllow)
                }
            }
        }
        webView.navigationDelegate = navDelegate

        // Host inside a full-screen UIViewController
        val vc = UIViewController()
        vc.modalPresentationStyle = UIModalPresentationFullScreen
        vc.view.backgroundColor  = UIColor.blackColor

        webView.translatesAutoresizingMaskIntoConstraints = false
        vc.view.addSubview(webView)

        NSLayoutConstraint.activateConstraints(listOf(
            webView.topAnchor.constraintEqualToAnchor(vc.view.topAnchor),
            webView.bottomAnchor.constraintEqualToAnchor(vc.view.bottomAnchor),
            webView.leadingAnchor.constraintEqualToAnchor(vc.view.leadingAnchor),
            webView.trailingAnchor.constraintEqualToAnchor(vc.view.trailingAnchor)
        ))

        // Load checkout URL
        NSURL.URLWithString(url)?.let { nsUrl ->
            onLoadRef.value(true)
            webView.loadRequest(NSURLRequest(uRL = nsUrl))
        }

        // Present over Compose UI
        val rootVc    = UIApplication.sharedApplication.keyWindow?.rootViewController
        val presenter = rootVc?.presentedViewController ?: rootVc
        presenter?.presentViewController(vc, animated = true, completion = null)

        // Keep strong refs alive for the lifetime of this DisposableEffect.
        // WKWebView holds only weak refs to navigationDelegate and messageHandlers,
        // so we capture them here to prevent premature deallocation.
        val keepAlive = listOf(messageHandler, navDelegate)

        onDispose {
            onLoadRef.value(false)
            vc.dismissViewControllerAnimated(true, completion = null)
            // keepAlive stays in scope until onDispose runs, holding refs
            keepAlive.size // no-op reference to prevent compiler from optimising away
        }
    }
}
