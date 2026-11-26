package com.novabrowse.browser.browser

import android.graphics.Bitmap
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.novabrowse.browser.data.newId

data class PagePermission(
    val origin: String,
    val resources: Array<String>,
    val request: android.webkit.PermissionRequest
) {
    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = System.identityHashCode(this)
}

data class GeoPermission(
    val origin: String,
    val callback: android.webkit.GeolocationPermissions.Callback
)

data class JsDialog(
    val message: String,
    val defaultValue: String?,
    val type: Type,
    val result: JsResult
) {
    enum class Type { ALERT, CONFIRM, PROMPT }
}

/**
 * A single live browser tab. Each tab owns its own WebView so that pages keep
 * running, scroll position and form state while the user switches tabs.
 */
class BrowserTab(
    val webView: WebView,
    val id: String = newId(),
    val isPrivate: Boolean = false,
    val groupId: String? = null
) {
    var title by mutableStateOf("New Tab")
    var url by mutableStateOf("")
    var progress by mutableStateOf(0)
    var isLoading by mutableStateOf(false)
    var canGoBack by mutableStateOf(false)
    var canGoForward by mutableStateOf(false)
    var favicon by mutableStateOf<Bitmap?>(null)
    var isSecure by mutableStateOf(false)
    var isStartPage by mutableStateOf(true)
    var errorText by mutableStateOf<String?>(null)
    var errorCode by mutableStateOf<Int?>(null)
    var preview by mutableStateOf<Bitmap?>(null)
    var isReaderMode by mutableStateOf(false)
    var findQuery by mutableStateOf("")
    var findMatches by mutableStateOf(0)
    var pendingPermission by mutableStateOf<PagePermission?>(null)
    var pendingGeo by mutableStateOf<GeoPermission?>(null)
    var jsDialog by mutableStateOf<JsDialog?>(null)
    var fullscreenView by mutableStateOf<android.view.View?>(null)
    var blockedTrackers by mutableStateOf(0)
    var desktopMode by mutableStateOf(false)
    var lastActiveAt: Long = System.currentTimeMillis()

    fun domain(): String = UrlTools.host(url)
    fun displayUrl(): String = UrlTools.displayUrl(url)

    fun showStartPage() {
        isStartPage = true
        isReaderMode = false
        errorText = null
        title = "New Tab"
        url = ""
        progress = 0
        isLoading = false
        canGoBack = false
        canGoForward = false
        webView.stopLoading()
        webView.loadUrl("about:blank")
    }

    fun load(target: String) {
        isStartPage = false
        errorText = null
        errorCode = null
        isReaderMode = false
        url = target
        with(webView) {
            stopLoading()
            if (target.startsWith("http", true)) {
                var extra = mutableMapOf<String, String>()
                if (NovaFlags.globalDnt) extra["DNT"] = "1"
                loadUrl(target, extra)
            } else {
                loadUrl(target)
            }
        }
    }

    fun goBack() { if (webView.canGoBack()) webView.goBack() }
    fun goForward() { if (webView.canGoForward()) webView.goForward() }
    fun reload() { errorText = null; webView.reload() }
    fun stop() = webView.stopLoading()

    fun destroy() {
        runCatching {
            webView.stopLoading()
            webView.loadUrl("about:blank")
            (webView.parent as? android.view.ViewGroup)?.removeView(webView)
            webView.destroy()
        }
    }

    fun capturePreview(): Bitmap? {
        val view = webView
        if (view.width <= 0 || view.height <= 0) return preview
        return runCatching {
            val w = 360
            val h = (view.height.toFloat() / view.width.toFloat() * w).toInt().coerceIn(120, 640)
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bmp)
            val scale = w.toFloat() / view.width.toFloat()
            canvas.scale(scale, scale)
            view.draw(canvas)
            preview = bmp
            bmp
        }.getOrNull()
    }

    /** Kept for the chrome client to answer prompts when the dialog is dismissed. */
    fun cancelDialogs() {
        pendingPermission?.request?.deny()
        pendingPermission = null
        pendingGeo = null
        jsDialog?.result?.cancel()
        jsDialog = null
    }
}

/** Process-wide flags that WebView clients read without holding a context. */
object NovaFlags {
    @Volatile var globalDnt: Boolean = true
    @Volatile var safeBrowsing: Boolean = true
    @Volatile var trackingProtection: Boolean = true
    @Volatile var desktopMode: Boolean = false
    @Volatile var javaScript: Boolean = true
}
