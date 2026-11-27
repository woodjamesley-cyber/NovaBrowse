package com.novabrowse.browser.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.novabrowse.browser.Nova
import com.novabrowse.browser.browser.BrowserTab
import com.novabrowse.browser.browser.ReaderMode
import com.novabrowse.browser.browser.UrlTools
import com.novabrowse.browser.data.AppSettings
import com.novabrowse.browser.data.Bookmark
import com.novabrowse.browser.data.ClosedTabEntry
import com.novabrowse.browser.data.HistoryEntry
import com.novabrowse.browser.data.ReadingItem
import com.novabrowse.browser.data.SearchEngine
import com.novabrowse.browser.data.Shortcut
import com.novabrowse.browser.data.newId
import com.novabrowse.browser.extensions.ChromeWebStore
import com.novabrowse.browser.extensions.ExtensionRecord
import com.novabrowse.browser.extensions.StoreListing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

enum class NovaScreen {
    NONE, SETTINGS, BOOKMARKS, DOWNLOADS, HISTORY, EXTENSIONS, READING_LIST, ABOUT, QR, EXTENSION_STORE, LICENSES
}

enum class SettingsPage { ROOT, GENERAL, APPEARANCE, PRIVACY, DOWNLOADS, EXTENSIONS, ADVANCED, ABOUT }

enum class SuggestionKind { SEARCH, HISTORY, BOOKMARK, SHORTCUT }

data class Suggestion(
    val title: String,
    val url: String,
    val subtitle: String,
    val kind: SuggestionKind
)

data class BookmarkDraft(val id: String?, val title: String, val url: String, val folder: String)

/**
 * Single source of truth for NovaBrowse's UI. Browser state lives in the engines
 * (TabManager / ExtensionManager); this class exposes it to Compose and owns
 * user-level actions such as navigation, bookmarks, downloads and settings.
 */
class NovaViewModel(app: Application) : AndroidViewModel(app) {

    private val context: Context get() = getApplication()

    val settingsFlow: StateFlow<AppSettings> = Nova.settings.state
    val bookmarksFlow = Nova.bookmarks.items
    val historyFlow = Nova.history.items
    val shortcutsFlow = Nova.shortcuts.items
    val closedFlow = Nova.closedTabs.items
    val readingFlow = Nova.readingList.items
    val downloadsFlow = Nova.downloads.items
    val downloadProgress = Nova.downloadsCenter.progress
    val extensionsFlow = Nova.extensionStore.installed
    val tabsVersion = mutableStateOf(0)

    var screen by mutableStateOf(NovaScreen.NONE)
        private set
    var settingsPage by mutableStateOf(SettingsPage.ROOT)
    var tabSwitcherOpen by mutableStateOf(false)
        private set
    var menuOpen by mutableStateOf(false)
        private set
    var searchOpen by mutableStateOf(false)
        private set
    var searchText by mutableStateOf("")
        private set
    var suggestions by mutableStateOf<List<Suggestion>>(emptyList())
        private set
    var toast by mutableStateOf<String?>(null)
    var pendingExtension by mutableStateOf<ExtensionRecord?>(null)
        private set
    var extensionPopup by mutableStateOf<ExtensionRecord?>(null)
    var storeQuery by mutableStateOf("")
        private set
    var storeResults by mutableStateOf<List<StoreListing>>(emptyList())
        private set
    var storeSearching by mutableStateOf(false)
        private set
    var storeMessage by mutableStateOf<String?>(null)
        private set
    var storeLinkDialog by mutableStateOf(false)
        private set
    var findVisible by mutableStateOf(false)
        private set
    var findQuery by mutableStateOf("")
        private set
    var findMatches by mutableStateOf(0)
        private set
    var bookmarkDraft by mutableStateOf<BookmarkDraft?>(null)
    var shortcutDraft by mutableStateOf<Shortcut?>(null)
    var customEngineDraft by mutableStateOf(false)
    var installUrlDialog by mutableStateOf(false)
        private set
    var busyMessage by mutableStateOf<String?>(null)
        private set

    private var searchJob: Job? = null
    private var storeSearchJob: Job? = null
    private val suggestionJob = Job()

    init {
        seedDefaultShortcuts()
        if (Nova.tabs.tabs.isEmpty() && Nova.tabs.privateTabs.isEmpty()) {
            val restored = Nova.settings.value.restoreSession &&
                Nova.settings.value.startupBehaviour == "restore" && Nova.tabs.restoreSession()
            if (!restored) Nova.tabs.newTab()
        } else {
            Nova.tabs.selectTab(Nova.tabs.currentList().last())
        }
        Nova.tabs.onTabsChanged = { tabsVersion.value++ }
        ensureTabAlive()
    }

    private fun seedDefaultShortcuts() {
        val prefs = context.getSharedPreferences("nova_bootstrap", Context.MODE_PRIVATE)
        if (prefs.getBoolean("shortcuts_seeded", false)) return
        prefs.edit().putBoolean("shortcuts_seeded", true).apply()
        if (Nova.shortcuts.value.isNotEmpty()) return
        listOf(
            Triple("Wikipedia", "https://www.wikipedia.org", 0),
            Triple("GitHub", "https://github.com", 1),
            Triple("YouTube", "https://m.youtube.com", 2),
            Triple("Reddit", "https://www.reddit.com", 3),
            Triple("Hacker News", "https://news.ycombinator.com", 4),
            Triple("DuckDuckGo", "https://duckduckgo.com", 5)
        ).forEach { (title, url, order) ->
            Nova.shortcuts.add(Shortcut(title = title, url = url, order = order))
        }
    }

    private fun ensureTabAlive() {
        if (Nova.tabs.currentList().isEmpty()) {
            Nova.tabs.newTab()
        }
    }

    val settings: AppSettings get() = Nova.settings.value
    val engines: List<SearchEngine> get() = Nova.settings.enabledEngines()
    val currentEngine: SearchEngine get() = Nova.settings.currentEngine()
    val activeTab: BrowserTab? get() = Nova.tabs.activeTab()
    val tabs: List<BrowserTab> get() = Nova.tabs.currentList()
    val isPrivate: Boolean get() = Nova.tabs.privateMode
    val extensionMenuItems get() = Nova.extensions.contextMenuItems.value

    // ------------------------------------------------------------------ navigation

    fun openSearch() {
        searchText = activeTab?.takeIf { !it.isStartPage }?.url ?: ""
        searchOpen = true
        updateSuggestions(searchText)
    }

    fun updateSearchText(text: String) {
        searchText = text
        updateSuggestions(text)
    }

    fun closeSearch() {
        searchOpen = false
        suggestions = emptyList()
    }

    fun submitSearch(text: String = searchText) {
        val input = text.trim()
        searchOpen = false
        suggestions = emptyList()
        if (input.isEmpty()) {
            activeTab?.showStartPage()
            return
        }
        navigate(UrlTools.toUrl(input, currentEngine))
    }

    fun navigate(url: String) {
        val tab = Nova.tabs.activeTab() ?: Nova.tabs.newTab(activate = true)
        tab.load(url)
        screen = NovaScreen.NONE
        tabSwitcherOpen = false
        menuOpen = false
    }

    fun openUrlInNewTab(url: String, private: Boolean = false) {
        Nova.tabs.newTab(url = url, isPrivate = private, activate = true)
        tabsVersion.value++
        closeAllOverlays()
    }

    fun newTab() {
        Nova.tabs.newTab(activate = true)
        tabSwitcherOpen = false
        menuOpen = false
        screen = NovaScreen.NONE
        tabsVersion.value++
    }

    fun newPrivateTab() {
        Nova.tabs.newTab(isPrivate = true, activate = true)
        tabSwitcherOpen = false
        menuOpen = false
        tabsVersion.value++
    }

    fun selectTab(tab: BrowserTab) {
        Nova.tabs.selectTab(tab)
        tabSwitcherOpen = false
        tabsVersion.value++
    }

    fun closeTab(tab: BrowserTab) {
        Nova.tabs.closeTab(tab)
        ensureTabAlive()
        tabsVersion.value++
    }

    fun closeActiveTab() {
        activeTab?.let { Nova.tabs.closeTab(it) }
        ensureTabAlive()
        tabsVersion.value++
    }

    fun duplicateTab(tab: BrowserTab) {
        Nova.tabs.duplicateTab(tab)
        tabsVersion.value++
    }

    fun closeAllTabs() {
        Nova.tabs.closeAll(private = Nova.tabs.privateMode)
        ensureTabAlive()
        tabsVersion.value++
    }

    fun showStartPage() {
        activeTab?.showStartPage()
    }

    fun togglePrivateBrowsing(enabled: Boolean) {
        Nova.tabs.togglePrivateMode(enabled)
        tabsVersion.value++
    }

    fun restoreClosed(entry: ClosedTabEntry) {
        Nova.tabs.restoreClosed(entry)
        closeAllOverlays()
        tabsVersion.value++
    }

    fun createGroup(name: String, tabIds: List<String>) {
        Nova.tabs.createGroup(name, tabIds)
        toast("Group \"$name\" created")
    }

    fun onBack(): Boolean {
        if (extensionPopup != null) { extensionPopup = null; return true }
        if (searchOpen) { closeSearch(); return true }
        if (menuOpen) { menuOpen = false; return true }
        if (findVisible) { closeFind(); return true }
        if (tabSwitcherOpen) { tabSwitcherOpen = false; return true }
        if (screen != NovaScreen.NONE) {
            if (screen == NovaScreen.SETTINGS && settingsPage != SettingsPage.ROOT) {
                settingsPage = SettingsPage.ROOT
            } else {
                screen = NovaScreen.NONE
            }
            return true
        }
        val tab = activeTab
        if (tab != null && !tab.isStartPage && tab.webView.canGoBack()) {
            tab.goBack()
            return true
        }
        return false
    }

    fun openScreen(target: NovaScreen) {
        screen = target
        if (target == NovaScreen.SETTINGS) settingsPage = SettingsPage.ROOT
        menuOpen = false
        tabSwitcherOpen = false
    }

    fun openSettings(page: SettingsPage = SettingsPage.ROOT) {
        settingsPage = page
        screen = NovaScreen.SETTINGS
        menuOpen = false
    }

    fun closeScreen() {
        screen = NovaScreen.NONE
        settingsPage = SettingsPage.ROOT
    }

    fun closeAllOverlays() {
        screen = NovaScreen.NONE
        tabSwitcherOpen = false
        menuOpen = false
        findVisible = false
    }

    fun openTabSwitcher() { tabSwitcherOpen = true }
    fun closeTabSwitcher() { tabSwitcherOpen = false }
    fun openMenu() { menuOpen = true }
    fun closeMenu() { menuOpen = false }

    // ------------------------------------------------------------------ suggestions

    private fun updateSuggestions(query: String) {
        searchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            suggestions = historyFlow.value.take(6).map {
                Suggestion(it.title.ifBlank { it.url }, it.url, UrlTools.displayUrl(it.url), SuggestionKind.HISTORY)
            }
            return
        }
        val local = mutableListOf<Suggestion>()
        if (!UrlTools.isLikelyUrl(trimmed)) {
            local.add(
                Suggestion(
                    currentEngine.buildSearchUrl(trimmed), trimmed,
                    "Search with ${currentEngine.name}", SuggestionKind.SEARCH
                )
            )
        }
        bookmarksFlow.value.filter {
            it.title.contains(trimmed, true) || it.url.contains(trimmed, true)
        }.take(3).forEach {
            local.add(Suggestion(it.title.ifBlank { it.url }, it.url, "Bookmark · ${UrlTools.displayUrl(it.url)}", SuggestionKind.BOOKMARK))
        }
        historyFlow.value.filter {
            it.title.contains(trimmed, true) || it.url.contains(trimmed, true)
        }.take(4).forEach {
            local.add(Suggestion(it.title.ifBlank { it.url }, it.url, UrlTools.displayUrl(it.url), SuggestionKind.HISTORY))
        }
        shortcutsFlow.value.filter {
            it.title.contains(trimmed, true) || it.url.contains(trimmed, true)
        }.take(2).forEach {
            local.add(Suggestion(it.title, it.url, "Shortcut", SuggestionKind.SHORTCUT))
        }
        suggestions = local.distinctBy { it.url }
        if (settings.searchSuggestions && !UrlTools.isLikelyUrl(trimmed)) {
            searchJob = viewModelScope.launch {
                delay(280)
                val remote = fetchRemoteSuggestions(trimmed)
                if (remote.isNotEmpty() && searchOpen) {
                    val existing = suggestions.map { it.title }.toSet()
                    suggestions = suggestions + remote.filterNot { it.title in existing }.take(5)
                }
            }
        }
    }

    private suspend fun fetchRemoteSuggestions(query: String): List<Suggestion> = withContext(Dispatchers.IO) {
        val engine = currentEngine
        val template = engine.suggestUrl ?: return@withContext emptyList()
        val url = template.replace("{query}", URLEncoder.encode(query, "UTF-8"))
        runCatching {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 2500
                readTimeout = 2500
                setRequestProperty("User-Agent", "NovaBrowse/1.0")
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            parseSuggestions(body, query)
        }.getOrDefault(emptyList())
    }

    private fun parseSuggestions(body: String, query: String): List<Suggestion> {
        val out = mutableListOf<String>()
        runCatching {
            val trimmed = body.trim()
            val array = if (trimmed.startsWith("[")) JSONArray(trimmed) else return emptyList()
            val listNode = array.optJSONArray(1) ?: array
            for (i in 0 until listNode.length()) {
                val node = listNode.opt(i)
                val text = when (node) {
                    is String -> node
                    is JSONObject -> node.optString("phrase").ifBlank { node.optString("suggestion") }
                    else -> null
                }
                if (!text.isNullOrBlank()) out.add(text)
            }
        }
        return out.take(6).map {
            Suggestion(it, currentEngine.buildSearchUrl(it), "Search suggestion", SuggestionKind.SEARCH)
        }.filter { it.title.isNotBlank() }
    }

    // ------------------------------------------------------------------ bookmarks

    fun isBookmarked(url: String): Boolean =
        bookmarksFlow.value.any { it.url == url }

    fun toggleBookmark() {
        val tab = activeTab ?: return
        if (tab.url.isBlank() || !tab.url.startsWith("http")) {
            toast("Open a web page before bookmarking it")
            return
        }
        val existing = bookmarksFlow.value.firstOrNull { it.url == tab.url }
        if (existing != null) {
            Nova.bookmarks.remove { it.id == existing.id }
            toast("Bookmark removed")
        } else {
            Nova.bookmarks.add(Bookmark(title = tab.title.ifBlank { tab.displayUrl() }, url = tab.url))
            toast("Bookmarked ${tab.title.take(32)}")
        }
    }

    fun saveBookmark(draft: BookmarkDraft) {
        val url = draft.url.trim()
        if (url.isEmpty()) { toast("A bookmark needs a web address"); return }
        val normalised = if (UrlTools.isLikelyUrl(url)) UrlTools.toUrl(url, currentEngine) else url
        if (draft.id == null) {
            Nova.bookmarks.add(Bookmark(title = draft.title.ifBlank { UrlTools.displayUrl(normalised) },
                url = normalised, folder = draft.folder.ifBlank { Bookmark.ROOT_FOLDER }))
        } else {
            Nova.bookmarks.update({ it.id == draft.id }) {
                it.copy(title = draft.title, url = normalised, folder = draft.folder.ifBlank { Bookmark.ROOT_FOLDER })
            }
        }
        bookmarkDraft = null
        toast("Bookmark saved")
    }

    fun deleteBookmark(id: String) {
        Nova.bookmarks.remove { it.id == id }
    }

    fun bookmarkFolders(): List<String> =
        (listOf(Bookmark.ROOT_FOLDER) + bookmarksFlow.value.map { it.folder }).distinct()

    fun importBookmarks(uri: Uri) {
        viewModelScope.launch {
            busyMessage = "Importing bookmarks"
            val count = withContext(Dispatchers.IO) {
                runCatching {
                    val html = context.contentResolver.openInputStream(uri)?.use {
                        it.bufferedReader().readText()
                    } ?: return@runCatching 0
                    val regex = Regex("<A[^>]*HREF=\"([^\"]+)\"[^>]*>(.*?)</A>", RegexOption.IGNORE_CASE)
                    var added = 0
                    regex.findAll(html).forEach { match ->
                        val url = match.groupValues[1].trim()
                        val title = match.groupValues[2].replace(Regex("<[^>]+>"), "").trim()
                        if (url.startsWith("http")) {
                            Nova.bookmarks.add(Bookmark(title = title.ifBlank { url }, url = url, folder = "Imported"))
                            added++
                        }
                    }
                    added
                }.getOrDefault(0)
            }
            busyMessage = null
            toast(if (count > 0) "Imported $count bookmarks" else "No bookmarks found in that file")
        }
    }

    fun exportBookmarks(uri: Uri) {
        viewModelScope.launch {
            busyMessage = "Exporting bookmarks"
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val html = buildString {
                        appendLine("<!DOCTYPE NETSCAPE-Bookmark-file-1>")
                        appendLine("<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=UTF-8\">")
                        appendLine("<TITLE>Bookmarks</TITLE><H1>NovaBrowse Bookmarks</H1><DL><p>")
                        bookmarksFlow.value.forEach { bm ->
                            appendLine("    <DT><A HREF=\"${bm.url}\">${bm.title}</A>")
                        }
                        appendLine("</DL><p>")
                    }
                    context.contentResolver.openOutputStream(uri)?.use { it.write(html.toByteArray()) }
                    true
                }.getOrDefault(false)
            }
            busyMessage = null
            toast(if (ok) "Bookmarks exported" else "Could not write that file")
        }
    }

    // ------------------------------------------------------------------ shortcuts

    fun saveShortcut(shortcut: Shortcut) {
        val normalised = if (UrlTools.isLikelyUrl(shortcut.url)) UrlTools.toUrl(shortcut.url, currentEngine) else shortcut.url
        val existing = shortcutsFlow.value.firstOrNull { it.id == shortcut.id }
        if (existing == null) {
            Nova.shortcuts.add(
                shortcut.copy(
                    url = normalised,
                    title = shortcut.title.ifBlank { UrlTools.displayUrl(normalised) },
                    order = shortcutsFlow.value.size
                )
            )
        } else {
            Nova.shortcuts.update({ it.id == shortcut.id }) {
                it.copy(title = shortcut.title, url = normalised)
            }
        }
        shortcutDraft = null
    }

    fun removeShortcut(shortcut: Shortcut) {
        Nova.shortcuts.remove { it.id == shortcut.id }
    }

    fun moveShortcut(shortcut: Shortcut, delta: Int) {
        val list = shortcutsFlow.value.sortedBy { it.order }.toMutableList()
        val index = list.indexOfFirst { it.id == shortcut.id }
        val target = index + delta
        if (index < 0 || target !in list.indices) return
        val item = list.removeAt(index)
        list.add(target, item)
        Nova.shortcuts.replace(list.mapIndexed { i, s -> s.copy(order = i) })
    }

    // ------------------------------------------------------------------ history / reading

    fun deleteHistory(id: String) = Nova.history.remove { it.id == id }
    fun clearHistory() { Nova.history.clear(); toast("Browsing history cleared") }

    fun addToReadingList() {
        val tab = activeTab ?: return
        if (!tab.url.startsWith("http")) { toast("Open a page to save it for later"); return }
        Nova.readingList.add(ReadingItem(title = tab.title.ifBlank { tab.displayUrl() }, url = tab.url))
        toast("Added to reading list")
    }

    fun removeReadingItem(id: String) = Nova.readingList.remove { it.id == id }

    // ------------------------------------------------------------------ downloads

    fun refreshDownloads() = Nova.downloadsCenter.refresh()

    fun openDownload(id: Long) {
        val record = Nova.downloads.value.firstOrNull { it.id == id } ?: return
        if (!Nova.downloadsCenter.open(record)) toast("No app can open this file yet")
    }

    fun shareDownload(id: Long) {
        val record = Nova.downloads.value.firstOrNull { it.id == id } ?: return
        if (!Nova.downloadsCenter.share(record)) toast("File is not available on this device")
    }

    fun pauseDownload(id: Long) = Nova.downloads.value.firstOrNull { it.id == id }?.let {
        Nova.downloadsCenter.pause(it); toast("Download stopped — resume restarts the transfer")
    }

    fun resumeDownload(id: Long) = Nova.downloads.value.firstOrNull { it.id == id }?.let {
        Nova.downloadsCenter.resume(it); toast("Download restarted")
    }

    fun cancelDownload(id: Long) = Nova.downloads.value.firstOrNull { it.id == id }?.let {
        Nova.downloadsCenter.cancel(it); toast("Download cancelled")
    }

    fun removeDownload(id: Long) = Nova.downloads.value.firstOrNull { it.id == id }?.let {
        Nova.downloadsCenter.removeFromHistory(it)
    }

    fun clearDownloads() { Nova.downloadsCenter.clearHistory(); toast("Download history cleared") }

    // ------------------------------------------------------------------ page tools

    fun reloadPage() = activeTab?.reload()
    fun stopLoading() = activeTab?.stop()

    fun shareCurrentPage() {
        val tab = activeTab ?: return
        if (tab.url.isBlank()) return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, tab.title)
            putExtra(Intent.EXTRA_TEXT, "${tab.title}\n${tab.url}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share page").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun copyCurrentUrl() {
        val tab = activeTab ?: return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("NovaBrowse URL", tab.url))
        toast("Link copied")
    }

    fun readClipboard(): String {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        return clipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString().orEmpty()
    }

    fun toggleDesktopMode() {
        Nova.settings.update { it.copy(desktopMode = !it.desktopMode) }
        Nova.tabs.applyGlobalSettings()
        toast(if (settings.desktopMode) "Desktop site mode on" else "Desktop site mode off")
    }

    fun toggleReaderMode() {
        val tab = activeTab ?: return
        if (!tab.url.startsWith("http")) { toast("Reader mode works on articles"); return }
        val theme = if (settings.theme == "light") "light" else "dark"
        tab.webView.evaluateJavascript(ReaderMode.script(theme, settings.textZoom)) { result ->
            val ok = result?.contains("NOVA_READER_OK") == true
            tab.isReaderMode = ok
            toast(if (ok) "Reader mode on" else "This page is not an article")
        }
    }

    fun translatePage() {
        val tab = activeTab ?: return
        if (!tab.url.startsWith("http")) return
        val target = if (java.util.Locale.getDefault().language == "en") "es" else "en"
        val url = "https://translate.google.com/translate?sl=auto&tl=$target&u=" +
            URLEncoder.encode(tab.url, "UTF-8")
        Nova.tabs.newTab(url = url, activate = true)
        tabsVersion.value++
        toast("Opening translated page")
    }

    fun findInPage(query: String) {
        val tab = activeTab ?: return
        findQuery = query
        tab.webView.setFindListener { total, _, _ ->
            findMatches = total
            tab.findMatches = total
        }
        tab.webView.findAllAsync(query)
    }

    fun findNext(forward: Boolean = true) {
        val tab = activeTab ?: return
        if (findQuery.isBlank()) return
        tab.webView.findNext(forward)
    }

    fun openFind() { findVisible = true; findQuery = ""; findMatches = 0 }
    fun closeFind() {
        findVisible = false
        activeTab?.webView?.clearMatches()
        activeTab?.webView?.setFindListener(null)
    }

    fun toggleJavaScript() {
        Nova.settings.update { it.copy(javaScriptEnabled = !it.javaScriptEnabled) }
        applyEngineSettings()
        toast(if (settings.javaScriptEnabled) "JavaScript enabled" else "JavaScript disabled")
    }

    fun captureScreenshot() {
        val tab = activeTab ?: return
        if (tab.isStartPage) { toast("Open a page first"); return }
        viewModelScope.launch {
            val result = withContext(Dispatchers.Main) {
                runCatching {
                    val view = tab.webView
                    if (view.width == 0 || view.height == 0) return@runCatching null
                    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                    view.draw(Canvas(bitmap))
                    val dir = File(context.cacheDir, "screenshots").apply { mkdirs() }
                    val file = File(dir, "novabrowse-${System.currentTimeMillis()}.png")
                    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    file
                }.getOrNull()
            }
            if (result == null) { toast("Could not capture this page"); return@launch }
            runCatching {
                val uri = FileProvider.getUriForFile(
                    context, "${context.packageName}.fileprovider", result
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(intent, "Share screenshot").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }.onFailure { toast("Screenshot saved to cache") }
        }
    }

    // ------------------------------------------------------------------ settings

    fun mutateSettings(block: (AppSettings) -> AppSettings) {
        Nova.settings.update(block)
        applyEngineSettings()
    }

    fun applyEngineSettings() {
        val s = Nova.settings.value
        com.novabrowse.browser.browser.NovaFlags.javaScript = s.javaScriptEnabled
        com.novabrowse.browser.browser.NovaFlags.trackingProtection = s.trackingProtection
        com.novabrowse.browser.browser.NovaFlags.safeBrowsing = s.safeBrowsing
        com.novabrowse.browser.browser.NovaFlags.desktopMode = s.desktopMode
        com.novabrowse.browser.browser.NovaFlags.globalDnt = s.doNotTrack
        (Nova.tabs.tabs + Nova.tabs.privateTabs).forEach { Nova.tabs.applySettingsTo(it.webView) }
    }

    fun reloadAllTabs() {
        Nova.tabs.applyGlobalSettings()
    }

    fun setSearchEngine(id: String) {
        Nova.settings.update { it.copy(searchEngineId = id) }
    }

    fun addCustomEngine(name: String, queryUrl: String) {
        val id = "custom-${newId().take(6)}"
        val template = if (queryUrl.contains("{query}")) queryUrl else "$queryUrl{query}"
        Nova.settings.update {
            it.copy(customEngines = it.customEngines + SearchEngine(id, name, template, null, true))
        }
        customEngineDraft = false
        toast("$name added")
    }

    fun removeCustomEngine(engine: SearchEngine) {
        if (!engine.isCustom) return
        Nova.settings.update { s ->
            s.copy(
                customEngines = s.customEngines.filterNot { it.id == engine.id },
                searchEngineId = if (s.searchEngineId == engine.id) "google" else s.searchEngineId
            )
        }
    }

    fun clearBrowsingData(kind: String) {
        when (kind) {
            "history" -> Nova.history.clear()
            "cookies" -> {
                CookieManager.getInstance().removeAllCookies(null)
                CookieManager.getInstance().flush()
            }
            "cache" -> (Nova.tabs.tabs + Nova.tabs.privateTabs).forEach {
                it.webView.clearCache(true)
                it.webView.clearFormData()
            }
            "storage" -> WebStorage.getInstance().deleteAllData()
            "permissions" -> Nova.permissions.clear()
            "all" -> {
                Nova.history.clear()
                CookieManager.getInstance().removeAllCookies(null)
                CookieManager.getInstance().flush()
                WebStorage.getInstance().deleteAllData()
                Nova.permissions.clear()
                (Nova.tabs.tabs + Nova.tabs.privateTabs).forEach {
                    it.webView.clearCache(true)
                    it.webView.clearFormData()
                    it.webView.clearHistory()
                }
            }
        }
        toast("$kind cleared")
    }

    fun revokePermissions(origin: String) {
        Nova.permissions.revokeAllFor(origin)
        toast("Permissions reset for $origin")
    }

    fun clearPrivateSession() {
        Nova.tabs.privateTabs.toList().forEach { Nova.tabs.closeTab(it) }
        Nova.tabs.clearPrivateProfile()
        toast("Private session cleared")
    }

    // ------------------------------------------------------------------ extensions

    fun installExtension(uri: Uri, displayName: String? = null) {
        viewModelScope.launch {
            busyMessage = "Reading extension package"
            val result = withContext(Dispatchers.IO) {
                Nova.extensionStore.installFromUri(uri, displayName)
            }
            busyMessage = null
            result.onSuccess { pendingExtension = it }
                .onFailure { toast(it.message ?: "That package could not be installed") }
        }
    }

    fun installExtensionFromUrl(url: String) {
        viewModelScope.launch {
            busyMessage = "Downloading extension"
            val result = withContext(Dispatchers.IO) { Nova.extensionStore.installFromUrl(url) }
            busyMessage = null
            installUrlDialog = false
            result.onSuccess { pendingExtension = it }
                .onFailure { toast(it.message ?: "Download failed") }
        }
    }

    // ------------------------------------------------------------------ web store

    fun openExtensionStore() {
        openScreen(NovaScreen.EXTENSION_STORE)
    }

    fun openStoreLinkDialog() { storeLinkDialog = true }

    fun closeStoreLinkDialog() { storeLinkDialog = false }

    fun updateStoreQuery(value: String) {
        storeQuery = value
        storeSearchJob?.cancel()
        if (value.isBlank()) {
            storeResults = emptyList()
            storeMessage = null
            storeSearching = false
            return
        }
        storeSearchJob = viewModelScope.launch {
            delay(350)
            storeSearching = true
            storeMessage = null
            val result = withContext(Dispatchers.IO) {
                runCatching { ChromeWebStore.search(value) }
            }
            storeSearching = false
            result.onSuccess { listings ->
                storeResults = listings
                if (listings.isEmpty()) {
                    storeMessage = "No extensions on the Chrome Web Store match “$value”."
                }
            }.onFailure { error ->
                storeResults = emptyList()
                storeMessage = error.message
                    ?: "Could not reach the Chrome Web Store. Check your connection."
            }
        }
    }

    fun clearStoreSearch() {
        storeSearchJob?.cancel()
        storeQuery = ""
        storeResults = emptyList()
        storeMessage = null
        storeSearching = false
    }

    fun installFromChromeWebStore(id: String, name: String? = null) {
        if (!ChromeWebStore.isExtensionId(id)) {
            toast("That is not a Chrome Web Store extension id")
            return
        }
        if (Nova.extensionStore.find(id) != null) {
            toast("${name ?: "That extension"} is already installed")
            return
        }
        viewModelScope.launch {
            busyMessage = if (name != null) "Installing $name" else "Installing from Chrome Web Store"
            val result = withContext(Dispatchers.IO) {
                Nova.extensionStore.installFromChromeWebStore(id, name)
            }
            busyMessage = null
            result.onSuccess { pendingExtension = it }
                .onFailure { toast(it.message ?: "Chrome Web Store install failed") }
        }
    }

    fun installFromStoreLink(input: String) {
        val id = ChromeWebStore.resolveId(input)
        if (id == null) {
            toast("Paste a Chrome Web Store link or a 32-character extension id")
            return
        }
        storeLinkDialog = false
        installFromChromeWebStore(id)
    }

    fun confirmExtensionInstall(record: ExtensionRecord) {
        pendingExtension = null
        Nova.extensions.restart()
        if (record.backgroundScripts.isNotEmpty() || record.backgroundServiceWorker != null) {
            Nova.extensions.setEnabled(record, true)
        }
        toast("${record.name} installed")
    }

    fun cancelExtensionInstall(record: ExtensionRecord) {
        pendingExtension = null
        Nova.extensionStore.uninstall(record.id)
        toast("Installation cancelled")
    }

    fun toggleExtension(record: ExtensionRecord, enabled: Boolean) {
        Nova.extensions.setEnabled(record, enabled)
        toast("${record.name} ${if (enabled) "enabled" else "disabled"}")
    }

    fun removeExtension(record: ExtensionRecord) {
        Nova.extensions.uninstall(record)
        toast("${record.name} removed")
    }

    fun openExtensionPopup(record: ExtensionRecord) {
        extensionPopup = record
        menuOpen = false
    }

    fun clickExtensionMenuItem(item: com.novabrowse.browser.extensions.ExtensionContextMenuItem) {
        Nova.extensions.clickContextMenu(item)
        menuOpen = false
    }

    // ------------------------------------------------------------------ misc

    fun toast(message: String) {
        toast = message
    }

    fun consumeToast() {
        toast = null
    }

    fun setQrDialog(open: Boolean) {
        screen = if (open) NovaScreen.QR else NovaScreen.NONE
    }

    fun onQrResult(value: String) {
        screen = NovaScreen.NONE
        if (value.startsWith("http", true)) {
            navigate(value)
        } else {
            navigate(UrlTools.toUrl(value, currentEngine))
        }
    }

    fun openBookmarkDraft(bookmark: Bookmark?) {
        bookmarkDraft = BookmarkDraft(
            id = bookmark?.id,
            title = bookmark?.title ?: "",
            url = bookmark?.url ?: "",
            folder = bookmark?.folder ?: Bookmark.ROOT_FOLDER
        )
    }

    fun openShortcutDraft(shortcut: Shortcut?) {
        shortcutDraft = shortcut ?: Shortcut(title = "", url = "")
    }

    fun openInstallUrlDialog() { installUrlDialog = true }
    fun closeInstallUrlDialog() { installUrlDialog = false }

    fun scanQr() { screen = NovaScreen.QR }

    fun historyFor(url: String): HistoryEntry? = historyFlow.value.firstOrNull { it.url == url }

    fun dialogsSupported(): Boolean = true

    /** Used by the address bar to show a security label. */
    fun securityLabel(tab: BrowserTab?): String = when {
        tab == null -> ""
        tab.isStartPage -> ""
        tab.isSecure -> "Secure connection"
        tab.url.startsWith("http://") -> "Not secure"
        else -> "Local page"
    }

    fun closeFindIfNeeded(webView: WebView?) {
        if (findVisible) webView?.clearMatches()
    }

    fun extensionUnsupported(record: ExtensionRecord) = record.unsupported

    fun encodeUrlQuery(query: String): String = URLEncoder.encode(query, "UTF-8")
}
