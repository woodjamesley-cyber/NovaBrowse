package com.novabrowse.browser.browser

import android.net.Uri
import android.view.View
import android.webkit.WebChromeClient
import com.novabrowse.browser.data.HistoryEntry
import com.novabrowse.browser.data.PersistedList
import com.novabrowse.browser.data.PermissionStore
import com.novabrowse.browser.data.SettingsStore

data class DownloadRequest(
    val url: String,
    val userAgent: String?,
    val contentDisposition: String?,
    val mimeType: String?,
    val contentLength: Long,
    val cookie: String?,
    val pageUrl: String?
)

interface BrowserDeps {
    val settings: SettingsStore
    val history: PersistedList<HistoryEntry>
    val permissions: PermissionStore
    fun openExternal(uri: Uri): Boolean
    fun openNewTab(url: String, isPrivate: Boolean)
    fun requestDownload(request: DownloadRequest)
    fun onFullscreenEntered(view: View, callback: WebChromeClient.CustomViewCallback)
    fun onFullscreenExited()
    fun onTabTitle(tab: BrowserTab, title: String)
    fun onUrlUpdated(tab: BrowserTab, url: String)
    fun onPageFinished(tab: BrowserTab)
}
