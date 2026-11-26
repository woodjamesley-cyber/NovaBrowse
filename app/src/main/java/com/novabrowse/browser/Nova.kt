package com.novabrowse.browser

import android.app.Application
import android.content.Context
import com.novabrowse.browser.browser.NovaFlags
import com.novabrowse.browser.browser.TabManager
import com.novabrowse.browser.data.Bookmark
import com.novabrowse.browser.data.ClosedTabEntry
import com.novabrowse.browser.data.DownloadRecord
import com.novabrowse.browser.data.HistoryEntry
import com.novabrowse.browser.data.PermissionStore
import com.novabrowse.browser.data.PersistedList
import com.novabrowse.browser.data.ReadingItem
import com.novabrowse.browser.data.SettingsStore
import com.novabrowse.browser.data.Shortcut
import com.novabrowse.browser.data.TabGroup
import com.novabrowse.browser.downloads.DownloadCenter
import com.novabrowse.browser.extensions.ExtensionManager
import com.novabrowse.browser.extensions.ExtensionRecord
import com.novabrowse.browser.extensions.ExtensionStore

class NovaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Nova.init(this)
    }
}

/**
 * NovaBrowse's service locator. Everything is process-scoped and initialised once from
 * [NovaApp] so that tabs, extensions and downloads share a single consistent state.
 */
object Nova {
    lateinit var settings: SettingsStore
        private set
    lateinit var permissions: PermissionStore
        private set
    lateinit var bookmarks: PersistedList<Bookmark>
        private set
    lateinit var history: PersistedList<HistoryEntry>
        private set
    lateinit var shortcuts: PersistedList<Shortcut>
        private set
    lateinit var closedTabs: PersistedList<ClosedTabEntry>
        private set
    lateinit var readingList: PersistedList<ReadingItem>
        private set
    lateinit var downloads: PersistedList<DownloadRecord>
        private set
    lateinit var groups: PersistedList<TabGroup>
        private set
    lateinit var extensionRecords: PersistedList<ExtensionRecord>
        private set
    lateinit var extensionStore: ExtensionStore
        private set
    lateinit var extensions: ExtensionManager
        private set
    lateinit var downloadsCenter: DownloadCenter
        private set
    lateinit var tabs: TabManager
        private set

    val isReady: Boolean get() = this::tabs.isInitialized

    fun init(context: Context) {
        if (isReady) return
        val app = context.applicationContext
        settings = SettingsStore(app)
        permissions = PermissionStore(app)
        bookmarks = PersistedList(app, "bookmarks", 2000, { it.toJson() }, { Bookmark.fromJson(it) })
        history = PersistedList(app, "history", 3000, { it.toJson() }, { HistoryEntry.fromJson(it) })
        shortcuts = PersistedList(app, "shortcuts", 200, { it.toJson() }, { Shortcut.fromJson(it) })
        closedTabs = PersistedList(app, "closed_tabs", 40, { it.toJson() }, { ClosedTabEntry.fromJson(it) })
        readingList = PersistedList(app, "reading_list", 500, { it.toJson() }, { ReadingItem.fromJson(it) })
        downloads = PersistedList(app, "downloads", 500, { it.toJson() }, { DownloadRecord.fromJson(it) })
        groups = PersistedList(app, "tab_groups", 100, { it.toJson() }, { TabGroup.fromJson(it) })
        extensionRecords = PersistedList(app, "extensions", 100, { it.toJson() }, { ExtensionRecord.fromJson(it) })

        downloadsCenter = DownloadCenter(app, downloads, settings)
        extensionStore = ExtensionStore(app, extensionRecords)
        extensions = ExtensionManager(app, extensionStore, settings, bookmarks, history)
        tabs = TabManager(app, settings, history, closedTabs, permissions, groups)

        tabs.downloadHandler = { request -> downloadsCenter.enqueue(request) }
        tabs.extensionBridge = extensions
        extensions.attach(tabs)

        NovaFlags.globalDnt = settings.value.doNotTrack
        NovaFlags.trackingProtection = settings.value.trackingProtection
        NovaFlags.safeBrowsing = settings.value.safeBrowsing
        NovaFlags.javaScript = settings.value.javaScriptEnabled
        NovaFlags.desktopMode = settings.value.desktopMode

        if (settings.value.extensionsEnabled) extensions.startEnabledBackgrounds()
    }
}
