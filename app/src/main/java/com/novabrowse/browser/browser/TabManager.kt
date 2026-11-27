package com.novabrowse.browser.browser

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.novabrowse.browser.data.AppSettings
import com.novabrowse.browser.data.ClosedTabEntry
import com.novabrowse.browser.data.HistoryEntry
import com.novabrowse.browser.data.PermissionStore
import com.novabrowse.browser.data.PersistedList
import com.novabrowse.browser.data.SessionTab
import com.novabrowse.browser.data.SettingsStore
import com.novabrowse.browser.data.TabGroup
import com.novabrowse.browser.data.newId
import com.novabrowse.browser.extensions.ExtensionBridge
import org.json.JSONArray
import java.io.File

class TabManager(
    private val context: Context,
    override val settings: SettingsStore,
    override val history: PersistedList<HistoryEntry>,
    private val closedStore: PersistedList<ClosedTabEntry>,
    override val permissions: PermissionStore,
    private val groupsStore: PersistedList<TabGroup>
) : BrowserDeps {

    val tabs = mutableStateListOf<BrowserTab>()
    val privateTabs = mutableStateListOf<BrowserTab>()
    var activeId by mutableStateOf<String?>(null)
        private set
    var privateMode by mutableStateOf(false)
        private set
    var extensionBridge: ExtensionBridge? = null
    var downloadHandler: ((DownloadRequest) -> Unit)? = null
    var onFullscreen: ((View, WebChromeClient.CustomViewCallback) -> Unit)? = null
    var onFullscreenExit: (() -> Unit)? = null
    var onTabsChanged: (() -> Unit)? = null

    val groups: List<TabGroup> get() = groupsStore.value
    val closedTabs: List<ClosedTabEntry> get() = closedStore.value

    private var incognitoProfileName = "nova_private"

    @SuppressLint("SetJavaScriptEnabled")
    private fun buildWebView(isPrivate: Boolean): WebView {
        val webView = WebView(context)
        if (isPrivate && WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
            runCatching {
                ProfileStore.getInstance().getOrCreateProfile(incognitoProfileName)
                WebViewCompat.setProfile(webView, incognitoProfileName)
            }
        }
        webView.setBackgroundColor(0xFF070B1E.toInt())
        webView.isVerticalScrollBarEnabled = true
        webView.overScrollMode = WebView.OVER_SCROLL_IF_CONTENT_SCROLLS
        applySettingsTo(webView)
        return webView
    }

    fun applySettingsTo(webView: WebView) {
        val s = settings.value
        with(webView.settings) {
            javaScriptEnabled = s.javaScriptEnabled
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = false
            allowContentAccess = true
            setGeolocationEnabled(true)
            mediaPlaybackRequiresUserGesture = true
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            builtInZoomControls = false
            displayZoomControls = false
            useWideViewPort = true
            loadWithOverviewMode = true
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(true)
            textZoom = s.textZoom
            userAgentString = if (s.desktopMode) DESKTOP_UA else WebSettings.getDefaultUserAgent(context)
            blockNetworkImage = false
            safeBrowsingEnabled = s.safeBrowsing
            cacheMode = WebSettings.LOAD_DEFAULT
        }
        NovaFlags.javaScript = s.javaScriptEnabled
        NovaFlags.trackingProtection = s.trackingProtection
        NovaFlags.safeBrowsing = s.safeBrowsing
        NovaFlags.desktopMode = s.desktopMode
        NovaFlags.globalDnt = s.doNotTrack
        // Let the system autofill service (Google Password Manager, Bitwarden, ...) fill logins.
        runCatching { webView.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES }
        CookieManager.getInstance().setAcceptCookie(s.cookiesEnabled)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, s.thirdPartyCookies)
    }

    fun applyGlobalSettings() {
        (tabs + privateTabs).forEach { tab ->
            applySettingsTo(tab.webView)
            tab.desktopMode = settings.value.desktopMode
            tab.webView.reload()
        }
    }

    @Synchronized
    fun newTab(url: String? = null, isPrivate: Boolean = false, activate: Boolean = true): BrowserTab {
        val webView = buildWebView(isPrivate)
        val tab = BrowserTab(webView = webView, isPrivate = isPrivate)
        val client = NovaWebViewClient(tab, this)
        val chrome = NovaWebChromeClient(tab, this)
        webView.webViewClient = client
        webView.webChromeClient = chrome
        webView.setDownloadListener { downloadUrl, userAgent, contentDisposition, mimeType, contentLength ->
            requestDownload(
                DownloadRequest(
                    url = downloadUrl,
                    userAgent = userAgent,
                    contentDisposition = contentDisposition,
                    mimeType = mimeType,
                    contentLength = contentLength,
                    cookie = cookieFor(downloadUrl),
                    pageUrl = tab.url
                )
            )
        }
        if (isPrivate) {
            privateTabs.add(tab)
            if (activate) privateMode = true
        } else {
            tabs.add(tab)
            if (activate) privateMode = false
        }
        if (activate) activeId = tab.id
        if (!isPrivate) extensionBridge?.prepareTab(tab)
        if (url != null) tab.load(url) else tab.showStartPage()
        persistSession()
        onTabsChanged?.invoke()
        return tab
    }

    fun activeTab(): BrowserTab? = (if (privateMode) privateTabs else tabs).firstOrNull { it.id == activeId }

    fun findTab(id: String): BrowserTab? = (tabs + privateTabs).firstOrNull { it.id == id }

    fun selectTab(tab: BrowserTab) {
        activeTab()?.capturePreview()
        activeId = tab.id
        privateMode = tab.isPrivate
        if (!tab.isPrivate) {
            extensionBridge?.onTabSelected(tab)
        }
        persistSession()
    }

    fun currentList(): List<BrowserTab> = if (privateMode) privateTabs else tabs

    fun closeTab(tab: BrowserTab) {
        val list = if (tab.isPrivate) privateTabs else tabs
        val index = list.indexOf(tab)
        if (index < 0) return
        if (!tab.isPrivate && tab.url.startsWith("http")) {
            closedStore.add(ClosedTabEntry(title = tab.title, url = tab.url))
        }
        list.removeAt(index)
        extensionBridge?.onTabClosed(tab)
        tab.destroy()
        if (activeId == tab.id) {
            val next = list.getOrNull(index.coerceAtMost(list.size - 1))
            activeId = next?.id
            if (next == null && tab.isPrivate) {
                // All private tabs are gone: drop the private profile and leave private mode.
                clearPrivateProfile()
                privateMode = false
                activeId = tabs.lastOrNull()?.id
            }
        }
        persistSession()
        onTabsChanged?.invoke()
    }

    fun closeAll(private: Boolean) {
        val list = if (private) privateTabs else tabs
        val copy = list.toList()
        copy.forEach { tab ->
            tab.destroy()
            extensionBridge?.onTabClosed(tab)
        }
        list.clear()
        if (private) {
            clearPrivateProfile()
            privateMode = false
            activeId = tabs.lastOrNull()?.id
        } else {
            activeId = privateTabs.lastOrNull()?.id?.also { privateMode = true }
        }
        persistSession()
        onTabsChanged?.invoke()
    }

    fun clearPrivateProfile() {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) return
        runCatching {
            ProfileStore.getInstance().deleteProfile(incognitoProfileName)
            incognitoProfileName = "nova_private_${System.currentTimeMillis()}"
        }
    }

    fun duplicateTab(tab: BrowserTab): BrowserTab = newTab(
        url = tab.url.takeIf { it.startsWith("http") }, isPrivate = tab.isPrivate, activate = true
    )

    fun moveTab(from: Int, to: Int, private: Boolean) {
        val list = if (private) privateTabs else tabs
        if (from in list.indices && to in list.indices) {
            val tab = list.removeAt(from)
            list.add(to, tab)
        }
    }

    fun restoreClosed(entry: ClosedTabEntry) {
        closedStore.remove { it.id == entry.id }
        newTab(entry.url, isPrivate = false, activate = true)
    }

    fun togglePrivateMode(on: Boolean) {
        if (on) {
            if (privateTabs.isEmpty()) newTab(isPrivate = true) else {
                privateMode = true
                activeId = privateTabs.last().id
            }
        } else {
            privateMode = false
            activeId = tabs.lastOrNull()?.id
        }
    }

    fun createGroup(name: String, tabIds: List<String>, colorIndex: Int = 0) {
        val group = TabGroup(name = name, colorIndex = colorIndex)
        groupsStore.add(group)
        (tabs + privateTabs).forEach { tab ->
            if (tab.id in tabIds) {
                // Tab identity stays stable; group membership is stored on the session.
                tab.setGroup(group.id)
            }
        }
    }

    private val groupOf = mutableMapOf<String, String>()
    private fun BrowserTab.setGroup(id: String?) {
        if (id == null) groupOf.remove(this.id) else groupOf[this.id] = id
    }
    fun groupIdFor(tab: BrowserTab): String? = groupOf[tab.id]

    fun addTabToGroup(tab: BrowserTab, groupId: String?) {
        if (groupId == null) groupOf.remove(tab.id) else groupOf[tab.id] = groupId
    }

    fun removeGroup(group: TabGroup) {
        groupsStore.remove { it.id == group.id }
        groupOf.entries.removeAll { it.value == group.id }
    }

    // ----- BrowserDeps -----

    override fun openExternal(uri: Uri): Boolean {
        return runCatching {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
                true
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, "No app can open ${uri.scheme} links", Toast.LENGTH_SHORT).show()
                false
            }
        }.getOrDefault(false)
    }

    override fun openNewTab(url: String, isPrivate: Boolean) {
        newTab(url = url, isPrivate = isPrivate, activate = true)
    }

    override fun requestDownload(request: DownloadRequest) {
        downloadHandler?.invoke(request)
    }

    override fun onFullscreenEntered(view: View, callback: WebChromeClient.CustomViewCallback) {
        activeTab()?.fullscreenView = view
        onFullscreen?.invoke(view, callback)
    }

    override fun onFullscreenExited() {
        (activeTab() ?: tabs.firstOrNull { it.fullscreenView != null })?.fullscreenView = null
        onFullscreenExit?.invoke()
    }

    override fun onPageFinished(tab: BrowserTab) {
        extensionBridge?.onPageFinished(tab)
    }

    override fun onTabTitle(tab: BrowserTab, title: String) {
        extensionBridge?.onTabUpdated(tab)
        if (!tab.isPrivate && settings.value.historyEnabled && tab.url.startsWith("http")) {
            history.update({ it.url == tab.url }) { it.copy(title = title) }
        }
        persistSession()
    }

    override fun onUrlUpdated(tab: BrowserTab, url: String) {
        extensionBridge?.onTabUpdated(tab)
        if (!tab.isPrivate) extensionBridge?.onPageStarted(tab, url)
    }

    // ----- Session persistence -----

    private val sessionFile: File get() = File(context.filesDir, "session.json")

    fun persistSession() {
        if (!settings.value.restoreSession) return
        runCatching {
            val arr = JSONArray()
            (tabs + privateTabs.filter { false }).forEach { tab ->
                if (tab.url.startsWith("http")) {
                    arr.put(
                        SessionTab(
                            url = tab.url,
                            title = tab.title,
                            groupId = groupOf[tab.id],
                            isPrivate = tab.isPrivate
                        ).toJson()
                    )
                }
            }
            sessionFile.writeText(arr.toString())
        }
    }

    fun restoreSession(): Boolean {
        if (!settings.value.restoreSession) return false
        if (tabs.isNotEmpty()) return true
        val saved = runCatching {
            if (!sessionFile.exists()) emptyList() else {
                val arr = JSONArray(sessionFile.readText())
                (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.let(SessionTab::fromJson) }
            }
        }.getOrDefault(emptyList())
        if (saved.isEmpty()) return false
        val active = saved.last()
        saved.forEach { st ->
            val tab = newTab(url = st.url, isPrivate = false, activate = false)
            tab.title = st.title.ifBlank { UrlTools.displayUrl(st.url) }
            if (st.groupId != null) groupOf[tab.id] = st.groupId
        }
        activeId = tabs.lastOrNull()?.id
        privateMode = false
        return tabs.isNotEmpty()
    }

    fun rememberGroup(tabId: String, groupId: String?) {
        if (groupId == null) groupOf.remove(tabId) else groupOf[tabId] = groupId
    }

    fun applySnapshot(bitmap: Bitmap?) {}

    companion object {
        val DESKTOP_UA: String get() =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    }
}
