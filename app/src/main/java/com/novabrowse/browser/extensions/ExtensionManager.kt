package com.novabrowse.browser.extensions

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import androidx.compose.runtime.mutableStateOf
import com.novabrowse.browser.browser.UrlTools
import androidx.core.app.NotificationCompat
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.ScriptHandler
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.novabrowse.browser.browser.BrowserTab
import com.novabrowse.browser.browser.TabManager
import com.novabrowse.browser.data.Bookmark
import com.novabrowse.browser.data.HistoryEntry
import com.novabrowse.browser.data.PersistedList
import com.novabrowse.browser.data.SettingsStore
import org.json.JSONArray
import org.json.JSONObject

interface ExtensionBridge {
    fun prepareTab(tab: BrowserTab)
    fun onTabSelected(tab: BrowserTab)
    fun onTabClosed(tab: BrowserTab)
    fun onTabUpdated(tab: BrowserTab)
    fun onPageStarted(tab: BrowserTab, url: String)
    fun onPageFinished(tab: BrowserTab)
}

/**
 * NovaBrowse's extension runtime.
 *
 * Real extension JavaScript runs inside WebViews (content scripts in the page, a hidden
 * background page per extension for runtime/storage/event logic) and talks to this class
 * through AndroidX WebKit's WebMessage channel. Permissions declared in the manifest are
 * enforced here: an extension only receives the APIs it asked for.
 */
@SuppressLint("SetJavaScriptEnabled")
class ExtensionManager(
    private val context: Context,
    private val store: ExtensionStore,
    private val settings: SettingsStore,
    private val bookmarks: PersistedList<Bookmark>,
    private val history: PersistedList<HistoryEntry>
) : ExtensionBridge {

    private val main = Handler(Looper.getMainLooper())
    private var tabs: TabManager? = null
    private val backgrounds = mutableMapOf<String, WebView>()
    private val handlers = mutableMapOf<String, MutableList<ScriptHandler>>()
    private val listeners = mutableMapOf<String, MutableList<JavaScriptReplyProxy>>()
    private val messagingTabs = mutableMapOf<String, WebView>()
    private val tabInts = mutableMapOf<String, Int>()
    private var nextTabInt = 1
    private val storagePrefs get() = context.getSharedPreferences("nova_ext_storage", Context.MODE_PRIVATE)
    private val menuRegistry = mutableMapOf<String, MutableList<JSONObject>>()

    var contextMenuItems = mutableStateOf<List<ExtensionContextMenuItem>>(emptyList())
        private set
    var onRequestPopup = mutableStateOf<ExtensionRecord?>(null)

    val installed: List<ExtensionRecord> get() = store.all
    val enabled: List<ExtensionRecord> get() = store.all.filter { it.enabled }

    fun attach(tabManager: TabManager) { tabs = tabManager }

    fun installedCount(): Int = store.all.size

    // ---------------------------------------------------------------- lifecycle

    fun startEnabledBackgrounds() {
        if (!settings.value.extensionsEnabled) return
        enabled.forEach { startBackground(it) }
    }

    fun stopAll() {
        backgrounds.values.forEach { runCatching { it.destroy() } }
        backgrounds.clear()
        handlers.values.flatten().forEach { runCatching { it.remove() } }
        handlers.clear()
        messagingTabs.clear()
        contextMenuItems.value = emptyList()
    }

    fun restart() {
        stopAll()
        startEnabledBackgrounds()
        tabs?.let { manager ->
            (manager.tabs + manager.privateTabs).filterNot { it.isPrivate }.forEach { prepareTab(it) }
        }
    }

    fun setEnabled(record: ExtensionRecord, enabled: Boolean) {
        store.setEnabled(record.id, enabled)
        if (enabled) {
            store.find(record.id)?.let { startBackground(it) }
        }
        restart()
    }

    fun uninstall(record: ExtensionRecord) {
        backgrounds.remove(record.id)?.let { runCatching { it.destroy() } }
        handlers.remove(record.id)?.forEach { runCatching { it.remove() } }
        menuRegistry.remove(record.id)
        rebuildMenuState()
        store.uninstall(record.id)
        restart()
    }

    // ---------------------------------------------------------------- tab scripts

    override fun prepareTab(tab: BrowserTab) {
        if (tab.isPrivate || !settings.value.extensionsEnabled) return
        val webView = tab.webView
        attachMessageChannel(webView, tab.id)
        val scripts = enabled
        if (scripts.isEmpty()) return
        val registered = handlers.getOrPut(tab.id) { mutableListOf() }
        scripts.forEach { ext ->
            registerDocumentStart(webView, ext, registered, tab.id)
        }
    }

    private fun registerDocumentStart(
        webView: WebView,
        ext: ExtensionRecord,
        registered: MutableList<ScriptHandler>,
        tabId: String
    ) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) return
        val bootstrap = ExtensionShim.bootstrap(ext.id, ext.name)
        val startScripts = ext.contentScripts.filter { it.runAt == "document_start" }
        val combined = StringBuilder(bootstrap)
        startScripts.forEach { cs ->
            cs.css.forEach { cssFile ->
                val css = store.readText(ext.id, cssFile) ?: return@forEach
                combined.append("\n").append(
                    ExtensionShim.contentScript(injectCss(css), cs.matches, cs.excludeMatches)
                )
            }
            cs.js.forEach { jsFile ->
                val js = store.readText(ext.id, jsFile) ?: return@forEach
                combined.append("\n").append(
                    ExtensionShim.contentScript(js, cs.matches, cs.excludeMatches)
                )
            }
        }
        runCatching {
            val handler = WebViewCompat.addDocumentStartJavaScript(webView, combined.toString(), setOf("*"))
            registered.add(handler)
            messagingTabs[tabId] = webView
        }
    }

    private fun injectCss(css: String): String =
        "var s=document.createElement('style');s.textContent=${JSONObject.quote(css)};" +
            "(document.head||document.documentElement).appendChild(s);"

    override fun onPageStarted(tab: BrowserTab, url: String) {
        if (!settings.value.extensionsEnabled) return
        attachMessageChannel(tab.webView, tab.id)
    }

    override fun onTabSelected(tab: BrowserTab) {
        if (tab.isPrivate) return
        attachMessageChannel(tab.webView, tab.id)
        messagingTabs[tab.id] = tab.webView
    }

    override fun onTabClosed(tab: BrowserTab) {
        handlers.remove(tab.id)?.forEach { runCatching { it.remove() } }
        messagingTabs.remove(tab.id)
        tabInts.remove(tab.id)
    }

    override fun onTabUpdated(tab: BrowserTab) {}

    /** Injects document_idle / document_end scripts once the page has loaded. */
    override fun onPageFinished(tab: BrowserTab) {
        if (tab.isPrivate || !settings.value.extensionsEnabled) return
        val webView = tab.webView
        enabled.forEach { ext ->
            ext.contentScripts.filter { it.runAt != "document_start" }.forEach { cs ->
                cs.css.forEach { cssFile ->
                    store.readText(ext.id, cssFile)?.let {
                        webView.evaluateJavascript(
                            ExtensionShim.contentScript(injectCss(it), cs.matches, cs.excludeMatches), null
                        )
                    }
                }
                cs.js.forEach { jsFile ->
                    store.readText(ext.id, jsFile)?.let {
                        webView.evaluateJavascript(
                            ExtensionShim.contentScript(it, cs.matches, cs.excludeMatches), null
                        )
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- messaging

    private fun attachMessageChannel(webView: WebView, tabId: String) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) return
        if (webView.tag == KEY_CHANNEL) return
        runCatching {
            WebViewCompat.addWebMessageListener(
                webView, CHANNEL, setOf("*")
            ) { view, message: WebMessageCompat, origin: Uri, isMainFrame: Boolean, proxy: JavaScriptReplyProxy ->
                val data = message.data ?: return@addWebMessageListener
                handleMessage(data, proxy, tabId, origin.toString())
            }
            webView.tag = KEY_CHANNEL
        }
    }

    private fun handleMessage(raw: String, proxy: JavaScriptReplyProxy, tabId: String, origin: String) {
        main.post {
            val json = runCatching { JSONObject(raw) }.getOrNull() ?: return@post
            // Replies from background pages to a content-script sendMessage call.
            if (json.has("reply") && json.has("id")) {
                val key = json.optString("id")
                val numeric = key.substringAfterLast('|').toIntOrNull() ?: 0
                pendingReplies.remove(key)?.let { target ->
                    runCatching {
                        target.postMessage(
                            JSONObject().put("id", numeric).put("result", json.opt("reply")).toString()
                        )
                    }
                }
                return@post
            }
            val extId = json.optString("extId")
            val ext = store.find(extId) ?: return@post
            if (!ext.enabled || !settings.value.extensionsEnabled) return@post
            val id = json.optInt("id")
            val ns = json.optString("ns")
            val method = json.optString("method")
            val args = json.optJSONArray("args") ?: JSONArray()
            val result = runCatching {
                ExtensionApi(this, ext, tabId, origin, proxy, id.toString()).call(ns, method, args)
            }
            val response = JSONObject().put("id", id)
            result.onSuccess { response.put("result", it ?: JSONObject.NULL) }
            result.onFailure { response.put("error", it.message ?: "Extension API error") }
            runCatching { proxy.postMessage(response.toString()) }
        }
    }

    private val pendingReplies = mutableMapOf<String, JavaScriptReplyProxy>()

    // ---------------------------------------------------------------- background pages

    private fun startBackground(ext: ExtensionRecord) {
        if (backgrounds.containsKey(ext.id)) return
        if (ext.backgroundScripts.isEmpty() && ext.backgroundServiceWorker == null) return
        val webView = WebView(context)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = false
        webView.settings.blockNetworkImage = false
        webView.setBackgroundColor(0x00000000)
        attachMessageChannel(webView, "background:${ext.id}")
        val html = """
            <!doctype html><html><head><meta charset="utf-8">
            <script>${ExtensionShim.bootstrap(ext.id, ext.name)}</script>
            </head><body></body></html>
        """.trimIndent()
        webView.webViewClient = object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                val files = buildList {
                    if (ext.backgroundServiceWorker != null) add(ext.backgroundServiceWorker)
                    addAll(ext.backgroundScripts)
                }
                files.forEach { file ->
                    store.readText(ext.id, file)?.let { script ->
                        view.evaluateJavascript(script, null)
                    }
                }
            }
        }
        webView.loadDataWithBaseURL(BACKGROUND_ORIGIN, html, "text/html", "utf-8", null)
        backgrounds[ext.id] = webView
    }

    private fun evalInBackground(extId: String, js: String) {
        backgrounds[extId]?.evaluateJavascript(js, null)
    }

    private fun wrapForJson(value: Any?): Any = JSONObject.wrap(value) ?: JSONObject.NULL

    private fun currentTabJson(): JSONObject? {
        val manager = tabs ?: return null
        val tab = manager.activeTab() ?: return null
        val intId = tabInts.getOrPut(tab.id) { nextTabInt++ }
        return JSONObject()
            .put("id", intId)
            .put("url", tab.url)
            .put("title", tab.title)
            .put("active", true)
    }

    fun dispatchRuntimeMessage(
        ext: ExtensionRecord,
        message: Any?,
        sender: JSONObject,
        originProxy: JavaScriptReplyProxy?,
        callbackId: String?
    ) {
        if (originProxy != null && callbackId != null) pendingReplies[callbackId] = originProxy
        val payload = JSONObject()
            .put("message", wrapForJson(message))
            .put("sender", sender)
            .put("callbackId", callbackId ?: "")
        val js = "window.__novaExtDispatch && window.__novaExtDispatch('message', $payload);"
        evalInBackground(ext.id, js)
    }

    private fun dispatchToContentScripts(tab: BrowserTab, ext: ExtensionRecord, message: Any?) {
        val sender = JSONObject().put("id", ext.id).put("url", tab.url)
        currentTabJson()?.let { sender.put("tab", it) }
        val payload = JSONObject()
            .put("message", wrapForJson(message))
            .put("sender", sender)
        tab.webView.evaluateJavascript(
            "window.__novaExtDispatch && window.__novaExtDispatch('message', $payload);", null
        )
    }

    /** Builds a popup document that has the extension's chrome.* bridge injected. */
    fun popupDocument(ext: ExtensionRecord): String? {
        val path = ext.popupPath ?: return null
        val body = store.popupHtml(ext.id, path) ?: return null
        val shim = "<script>${ExtensionShim.bootstrap(ext.id, ext.name)}</script>"
        val headMatch = Regex("<head[^>]*>", RegexOption.IGNORE_CASE).find(body)
        return if (headMatch != null) {
            val insertAt = headMatch.range.last + 1
            body.substring(0, insertAt) + shim + body.substring(insertAt)
        } else {
            shim + body
        }
    }

    fun attachPopup(webView: WebView, ext: ExtensionRecord) {
        attachMessageChannel(webView, "popup:${ext.id}")
    }

    // ---------------------------------------------------------------- popups / menus

    fun popupHtml(ext: ExtensionRecord): String? {
        val path = ext.popupPath ?: return null
        return store.popupHtml(ext.id, path)
    }

    fun rebuildMenuState() {
        val items = mutableListOf<ExtensionContextMenuItem>()
        menuRegistry.forEach { (extId, list) ->
            val ext = store.find(extId) ?: return@forEach
            if (!ext.enabled) return@forEach
            list.forEach { item ->
                items.add(
                    ExtensionContextMenuItem(
                        ext.id, ext.name,
                        item.optString("id"), item.optString("title")
                    )
                )
            }
        }
        contextMenuItems.value = items
    }

    fun clickContextMenu(item: ExtensionContextMenuItem) {
        val tab = tabs?.activeTab() ?: return
        val payload = JSONObject()
            .put("menuItemId", item.id)
            .put("pageUrl", tab.url)
            .put("frameUrl", tab.url)
            .put("selectionText", "")
        evalInBackground(
            item.extensionId,
            "window.__novaExtMenuHandler && window.__novaExtMenuHandler($payload);"
        )
    }

    // ---------------------------------------------------------------- API surface

    inner class ExtensionApi(
        private val manager: ExtensionManager,
        private val ext: ExtensionRecord,
        private val tabId: String,
        private val origin: String,
        private val proxy: JavaScriptReplyProxy,
        private val requestId: String
    ) {
        fun call(ns: String, method: String, args: JSONArray): Any? {
            requireDeclared(ns, method)
            return when (ns) {
                "storage" -> storage(method, args)
                "runtime" -> runtime(method, args)
                "tabs" -> tabsApi(method, args)
                "bookmarks" -> bookmarksApi(method, args)
                "notifications" -> notificationsApi(method, args)
                "contextMenus" -> contextMenusApi(method, args)
                "scripting" -> scriptingApi(method, args)
                else -> throw IllegalArgumentException("chrome.$ns is not available on NovaBrowse")
            }
        }

        private fun requireDeclared(ns: String, method: String) {
            val needed = when (ns) {
                "tabs", "bookmarks", "notifications", "contextMenus", "scripting" -> ns
                else -> null
            }
            if (needed != null && !ext.permissions.contains(needed)) {
                throw SecurityException("Extension did not request the \"$needed\" permission")
            }
        }

        // storage
        private fun storage(method: String, args: JSONArray): Any? {
            val area = args.optString(0, "local").ifBlank { "local" }
            val prefs = storagePrefs
            val key = "${ext.id}:$area"
            val raw = prefs.getString(key, "{}") ?: "{}"
            val data = runCatching { JSONObject(raw) }.getOrElse { JSONObject() }
            return when (method) {
                "get" -> {
                    val keys = args.opt(1)
                    val out = JSONObject()
                    when (keys) {
                        is JSONArray -> for (i in 0 until keys.length()) {
                            val k = keys.optString(i)
                            if (data.has(k)) out.put(k, data.get(k))
                        }
                        is JSONObject -> {
                            val it = keys.keys()
                            while (it.hasNext()) {
                                val k = it.next()
                                out.put(k, if (data.has(k)) data.get(k) else keys.get(k))
                            }
                        }
                        is String -> if (data.has(keys)) out.put(keys, data.get(keys))
                        null, JSONObject.NULL -> data.keys().asSequence().forEach { k -> out.put(k, data.get(k)) }
                    }
                    out
                }
                "set" -> {
                    val items = args.optJSONObject(1) ?: JSONObject()
                    items.keys().asSequence().forEach { k -> data.put(k, items.get(k)) }
                    prefs.edit().putString(key, data.toString()).apply()
                    null
                }
                "remove" -> {
                    val keys = args.opt(1)
                    if (keys is JSONArray) for (i in 0 until keys.length()) data.remove(keys.optString(i))
                    else if (keys is String) data.remove(keys)
                    prefs.edit().putString(key, data.toString()).apply()
                    null
                }
                "clear" -> { prefs.edit().remove(key).apply(); null }
                else -> throw IllegalArgumentException("chrome.storage.$method is not supported")
            }
        }

        private fun runtime(method: String, args: JSONArray): Any? = when (method) {
            "getManifest" -> runCatching { JSONObject(ext.manifestJson) }.getOrElse { JSONObject() }
            "sendMessage" -> {
                val sender = JSONObject()
                    .put("id", ext.id)
                    .put("tab", activeTabJson())
                dispatchRuntimeMessage(ext, args.opt(0), sender, proxy, "$tabId|$requestId")
                null
            }
            "setTitle" -> null
            "setBadgeText" -> null
            else -> throw IllegalArgumentException("chrome.runtime.$method is not supported")
        }

        // tabs
        private fun tabsApi(method: String, args: JSONArray): Any? {
            val manager = tabs ?: throw IllegalStateException("Browser not ready")
            return when (method) {
                "query" -> {
                    val info = args.optJSONObject(0) ?: JSONObject()
                    val onlyActive = info.optBoolean("active", false)
                    val list = (manager.tabs + manager.privateTabs).filter { tab ->
                        (!onlyActive || tab.id == manager.activeId) && tab.url.startsWith("http")
                    }
                    JSONArray().apply { list.forEach { put(tabJson(it, manager)) } }
                }
                "get" -> {
                    val tab = manager.findTab(intToTabId(args.optInt(0)) ?: "") ?: throw IllegalArgumentException("No tab with that id")
                    tabJson(tab, manager)
                }
                "create" -> {
                    val info = args.optJSONObject(0) ?: JSONObject()
                    val url = info.optString("url").ifBlank { null }
                    val tab = manager.newTab(url = url, isPrivate = false, activate = info.optBoolean("active", true))
                    tabJson(tab, manager)
                }
                "update" -> {
                    val tab = manager.findTab(intToTabId(args.optInt(0)) ?: "") ?: throw IllegalArgumentException("No tab with that id")
                    val info = args.optJSONObject(1) ?: JSONObject()
                    if (info.has("url")) tab.load(info.optString("url"))
                    if (info.optBoolean("active", false)) manager.selectTab(tab)
                    tabJson(tab, manager)
                }
                "remove" -> {
                    manager.findTab(intToTabId(args.optInt(0)) ?: "")?.let { manager.closeTab(it) }
                    null
                }
                "sendMessage" -> {
                    val tab = manager.findTab(intToTabId(args.optInt(0)) ?: "")
                        ?: throw IllegalArgumentException("No tab with that id")
                    dispatchToContentScripts(tab, ext, args.opt(1))
                    null
                }
                "captureVisibleTab" -> throw UnsupportedOperationException("captureVisibleTab is not supported on NovaBrowse")
                else -> throw IllegalArgumentException("chrome.tabs.$method is not supported")
            }
        }

        private fun tabJson(tab: BrowserTab, manager: TabManager): JSONObject {
            val intId = tabInts.getOrPut(tab.id) { nextTabInt++ }
            return JSONObject()
                .put("id", intId)
                .put("index", (manager.tabs + manager.privateTabs).indexOf(tab))
                .put("active", tab.id == manager.activeId)
                .put("url", tab.url)
                .put("title", tab.title)
                .put("incognito", tab.isPrivate)
                .put("windowId", 1)
                .put("favIconUrl", UrlTools.faviconService(tab.url) ?: "")
        }

        private fun activeTabJson(): JSONObject? {
            val manager = tabs ?: return null
            val tab = manager.activeTab() ?: return null
            return tabJson(tab, manager)
        }

        private fun intToTabId(id: Int): String? = tabInts.entries.firstOrNull { it.value == id }?.key

        // bookmarks
        private fun bookmarksApi(method: String, args: JSONArray): Any? = when (method) {
            "search" -> {
                val query = args.optJSONObject(0)?.optString("query").orEmpty()
                JSONArray().apply {
                    bookmarks.value.filter {
                        query.isBlank() || it.title.contains(query, true) || it.url.contains(query, true)
                    }.take(50).forEach { bm ->
                        put(JSONObject().put("id", bm.id).put("title", bm.title)
                            .put("url", bm.url).put("parentId", "1"))
                    }
                }
            }
            "getTree" -> JSONArray().put(
                JSONObject().put("id", "0").put("title", "").put("children", JSONArray().put(
                    JSONObject().put("id", "1").put("title", "Favourites").put("children", JSONArray().apply {
                        bookmarks.value.forEach { bm ->
                            put(JSONObject().put("id", bm.id).put("title", bm.title).put("url", bm.url))
                        }
                    })
                ))
            )
            "create" -> {
                val o = args.optJSONObject(0) ?: JSONObject()
                val url = o.optString("url")
                if (url.isBlank()) throw IllegalArgumentException("Bookmarks created by extensions need a url")
                val bm = Bookmark(title = o.optString("title", url), url = url)
                bookmarks.add(bm)
                JSONObject().put("id", bm.id).put("title", bm.title).put("url", bm.url)
            }
            "remove" -> { bookmarks.remove { it.id == args.optString(0) }; null }
            else -> throw IllegalArgumentException("chrome.bookmarks.$method is not supported")
        }

        // notifications
        private fun notificationsApi(method: String, args: JSONArray): Any? = when (method) {
            "create" -> {
                val options = args.optJSONObject(1) ?: JSONObject()
                postNotification(
                    ext.name,
                    options.optString("title", ext.name),
                    options.optString("message", "")
                )
                "nova-${System.currentTimeMillis()}"
            }
            "clear" -> null
            else -> throw IllegalArgumentException("chrome.notifications.$method is not supported")
        }

        // context menus
        private fun contextMenusApi(method: String, args: JSONArray): Any? = when (method) {
            "create" -> {
                val props = args.optJSONObject(0) ?: JSONObject()
                val list = menuRegistry.getOrPut(ext.id) { mutableListOf() }
                list.add(
                    JSONObject()
                        .put("id", props.optString("id", "nova-${list.size}"))
                        .put("title", props.optString("title", "Extension action"))
                )
                manager.rebuildMenuState()
                null
            }
            "remove" -> {
                menuRegistry[ext.id]?.removeAll { it.optString("id") == args.optString(0) }
                manager.rebuildMenuState(); null
            }
            "removeAll" -> { menuRegistry.remove(ext.id); manager.rebuildMenuState(); null }
            else -> throw IllegalArgumentException("chrome.contextMenus.$method is not supported")
        }

        // scripting
        private fun scriptingApi(method: String, args: JSONArray): Any? {
            val details = args.optJSONObject(0) ?: JSONObject()
            val targetTabId = details.optJSONObject("target")?.optInt("tabId") ?: 0
            val manager = tabs ?: throw IllegalStateException("Browser not ready")
            val tab = manager.findTab(intToTabId(targetTabId) ?: "") ?: manager.activeTab()
                ?: throw IllegalArgumentException("No target tab")
            return when (method) {
                "executeScript" -> {
                    if (details.has("code")) {
                        tab.webView.evaluateJavascript(details.optString("code"), null)
                    } else {
                        details.optJSONArray("files")?.let { files ->
                            for (i in 0 until files.length()) {
                                store.readText(ext.id, files.optString(i))?.let { src ->
                                    tab.webView.evaluateJavascript(src, null)
                                }
                            }
                        }
                    }
                    JSONArray()
                }
                "insertCSS" -> {
                    val css = if (details.has("css")) details.optString("css") else ""
                    tab.webView.evaluateJavascript(injectCss(css), null)
                    null
                }
                else -> throw IllegalArgumentException("chrome.scripting.$method is not supported")
            }
        }
    }

    private fun postNotification(title: String, subject: String, body: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Extensions", NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(subject.ifBlank { title })
            .setContentText(body)
            .setAutoCancel(true)
            .build()
        runCatching { manager.notify((System.nanoTime() and 0xFFFFFF).toInt(), notification) }
    }

    companion object {
        const val CHANNEL = "novaExt"
        const val KEY_CHANNEL = "nova_ext_channel"
        const val CHANNEL_ID = "nova_extensions"
        const val BACKGROUND_ORIGIN = "https://extensions.novabrowse.local/"
    }
}
