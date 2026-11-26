package com.novabrowse.browser.browser

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.view.View
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.novabrowse.browser.data.HistoryEntry
import java.io.ByteArrayInputStream

class NovaWebViewClient(
    private val tab: BrowserTab,
    private val deps: BrowserDeps
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val uri = request.url
        val scheme = uri.scheme?.lowercase() ?: return false
        return when (scheme) {
            "http", "https", "file", "about", "data", "blob", "javascript", "content" -> false
            else -> deps.openExternal(uri)
        }
    }

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        val current = url ?: return
        tab.isLoading = true
        tab.progress = 5
        tab.url = current
        tab.isSecure = UrlTools.isHttps(current)
        tab.errorText = null
        tab.blockedTrackers = 0
        tab.canGoBack = view.canGoBack()
        tab.canGoForward = view.canGoForward()
        deps.onUrlUpdated(tab, current)
    }

    override fun onPageFinished(view: WebView, url: String?) {
        super.onPageFinished(view, url)
        val current = url ?: return
        tab.isLoading = false
        tab.progress = 100
        tab.canGoBack = view.canGoBack()
        tab.canGoForward = view.canGoForward()
        tab.isSecure = UrlTools.isHttps(current)
        if (!tab.isPrivate && deps.settings.value.historyEnabled && current.startsWith("http")) {
            recordHistory(current, view.title ?: UrlTools.displayUrl(current))
        }
        deps.onUrlUpdated(tab, current)
        deps.onPageFinished(tab)
    }

    private fun recordHistory(url: String, title: String) {
        val existing = deps.history.value.firstOrNull { it.url == url }
        if (existing != null) {
            deps.history.update({ it.url == url }) {
                it.copy(title = if (title.isBlank()) it.title else title,
                    visitedAt = System.currentTimeMillis(), visits = it.visits + 1)
            }
        } else {
            deps.history.add(HistoryEntry(title = title.ifBlank { UrlTools.displayUrl(url) }, url = url))
        }
    }

    override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
        super.doUpdateVisitedHistory(view, url, isReload)
        val current = url ?: return
        tab.url = current
        tab.canGoBack = view.canGoBack()
        tab.canGoForward = view.canGoForward()
    }

    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
        super.onReceivedError(view, request, error)
        if (request.isForMainFrame) {
            tab.isLoading = false
            tab.errorCode = error.errorCode
            tab.errorText = error.description?.toString()?.ifBlank { "This page could not be loaded." }
        }
    }

    override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
        super.onReceivedHttpError(view, request, errorResponse)
        if (request.isForMainFrame && errorResponse.statusCode >= 400) {
            tab.isLoading = false
            tab.errorCode = errorResponse.statusCode
            tab.errorText = "The server responded with HTTP ${errorResponse.statusCode}."
        }
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        // NovaBrowse never bypasses certificate validation.
        handler.cancel()
        tab.isLoading = false
        tab.errorText = "This connection is not secure. NovaBrowse blocked the page because its " +
            "certificate could not be verified."
        tab.errorCode = -11
    }

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        if (!NovaFlags.trackingProtection) return null
        val url = request.url.toString()
        if (!url.startsWith("http")) return null
        if (TrackingProtection.isTracker(url)) {
            tab.blockedTrackers += 1
            return WebResourceResponse(
                "text/plain", "utf-8", 204, "No Content",
                mapOf("Cache-Control" to "no-store"), ByteArrayInputStream(ByteArray(0))
            )
        }
        return null
    }

    override fun onSafeBrowsingHit(
        view: WebView,
        request: WebResourceRequest,
        threatType: Int,
        callback: android.webkit.SafeBrowsingResponse
    ) {
        // Back to safety: do not let the user continue into a known-bad page.
        callback.backToSafety(true)
        tab.errorText = "NovaBrowse blocked this page because it was flagged as unsafe."
    }
}

class NovaWebChromeClient(
    private val tab: BrowserTab,
    private val deps: BrowserDeps
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        tab.progress = newProgress
        tab.isLoading = newProgress < 100
    }

    override fun onReceivedTitle(view: WebView, title: String?) {
        if (!title.isNullOrBlank()) {
            tab.title = title
            deps.onTabTitle(tab, title)
        }
    }

    override fun onReceivedIcon(view: WebView, icon: Bitmap?) {
        if (icon != null) tab.favicon = icon
    }

    override fun onPermissionRequest(request: PermissionRequest) {
        val origin = request.origin?.host ?: tab.domain()
        tab.pendingPermission = PagePermission(origin, request.resources, request)
    }

    override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) {
        if (origin == null || callback == null) return
        val remembered = deps.permissions.decision(origin, "geolocation")
        if (remembered != null) {
            callback.invoke(origin, remembered, false)
            return
        }
        tab.pendingGeo = GeoPermission(origin, callback)
    }

    override fun onJsAlert(view: WebView, url: String?, message: String?, result: JsResult?): Boolean {
        if (message == null || result == null) return false
        tab.jsDialog = JsDialog(message, null, JsDialog.Type.ALERT, result)
        return true
    }

    override fun onJsConfirm(view: WebView, url: String?, message: String?, result: JsResult?): Boolean {
        if (message == null || result == null) return false
        tab.jsDialog = JsDialog(message, null, JsDialog.Type.CONFIRM, result)
        return true
    }

    override fun onJsPrompt(
        view: WebView,
        url: String?,
        message: String?,
        defaultValue: String?,
        result: JsPromptResult?
    ): Boolean {
        if (message == null || result == null) return false
        tab.jsDialog = JsDialog(message, defaultValue, JsDialog.Type.PROMPT, result)
        return true
    }

    override fun onShowFileChooser(
        webView: WebView,
        filePathCallback: android.webkit.ValueCallback<Array<Uri>>,
        fileChooserParams: FileChooserParams
    ): Boolean {
        return FileChooserBridge.launch(filePathCallback, fileChooserParams)
    }

    override fun onShowCustomView(view: View, callback: CustomViewCallback?) {
        if (callback == null) return
        deps.onFullscreenEntered(view, callback)
    }

    override fun onHideCustomView() {
        deps.onFullscreenExited()
    }

    override fun onCreateWindow(
        view: WebView,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: android.os.Message
    ): Boolean {
        val transportWebView = WebView(view.context)
        transportWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(inner: WebView, request: WebResourceRequest): Boolean {
                deps.openNewTab(request.url.toString(), tab.isPrivate)
                inner.destroy()
                return true
            }
        }
        (resultMsg.obj as? WebView.WebViewTransport)?.webView = transportWebView
        resultMsg.sendToTarget()
        return true
    }

    override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage): Boolean = true

    override fun onGeolocationPermissionsHidePrompt() {
        tab.pendingGeo = null
    }

    override fun onPermissionRequestCanceled(request: PermissionRequest?) {
        tab.pendingPermission = null
    }
}

/** Bridges WebView file pickers to the Activity's ActivityResult launcher. */
object FileChooserBridge {
    var launcher: ((android.webkit.ValueCallback<Array<Uri>>, WebChromeClient.FileChooserParams) -> Boolean)? = null

    fun launch(
        callback: android.webkit.ValueCallback<Array<Uri>>,
        params: WebChromeClient.FileChooserParams
    ): Boolean {
        val l = launcher ?: return false
        return runCatching { l(callback, params) }.getOrDefault(false)
    }
}

fun cookieFor(url: String): String? = runCatching { CookieManager.getInstance().getCookie(url) }.getOrNull()
