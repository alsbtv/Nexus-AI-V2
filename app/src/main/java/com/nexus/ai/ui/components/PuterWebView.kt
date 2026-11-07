package com.nexus.ai.ui.components

import android.annotation.SuppressLint
import android.os.Message
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PuterWebView(
    isVisible: Boolean,
    onWebViewCreated: (WebView) -> Unit,
    onPopupCreated: (WebView) -> Unit,
    onLoginSuccess: (String) -> Unit,
    onLoginError: (String) -> Unit,
    onResponse: (String) -> Unit,
    onModelsLoaded: (String) -> Unit,
    onImageGenerated: (String) -> Unit,
    onVideoAnalyzedCallback: (String) -> Unit
) {
    AndroidView(
        modifier = if (isVisible) Modifier.fillMaxSize() else Modifier.size(1.dp),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.javaScriptCanOpenWindowsAutomatically = true
                setBackgroundColor(android.graphics.Color.BLACK)
                webViewClient = WebViewClient()
                
                val popupWebView = WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = WebViewClient()
                    setBackgroundColor(android.graphics.Color.BLACK)
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
                        val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
                        transport.webView = popupWebView
                        onPopupCreated(popupWebView)
                        resultMsg.sendToTarget()
                        return true
                    }
                }
                
                addJavascriptInterface(object {
                    @JavascriptInterface fun onLoginSuccess(username: String) { onLoginSuccess(username) }
                    @JavascriptInterface fun onLoginError(error: String) { onLoginError(error) }
                    @JavascriptInterface fun onPuterResponse(response: String) { onResponse(response) }
                    @JavascriptInterface fun onModelsLoaded(modelsJson: String) { onModelsLoaded(modelsJson) }
                    @JavascriptInterface fun onImageGenerated(url: String) { onImageGenerated(url) }
                    @JavascriptInterface fun onVideoAnalyzed(clipsJson: String) { onVideoAnalyzedCallback(clipsJson) }
                }, "AndroidBridge")
                
                try {
                    val htmlContent = context.assets.open("puter_bridge.html").bufferedReader().use { it.readText() }
                    loadDataWithBaseURL("https://nexus.ai", htmlContent, "text/html", "UTF-8", null)
                } catch (e: Exception) {
                    loadDataWithBaseURL("https://nexus.ai", "<html><body style='background:black;color:white;'><h3>Error loading bridge</h3><p>${e.message}</p></body></html>", "text/html", "UTF-8", null)
                }
                onWebViewCreated(this)
            }
        }
    )
}
