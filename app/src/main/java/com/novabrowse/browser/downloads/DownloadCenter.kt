package com.novabrowse.browser.downloads

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import androidx.core.content.FileProvider
import com.novabrowse.browser.browser.DownloadRequest
import com.novabrowse.browser.data.DownloadRecord
import com.novabrowse.browser.data.PersistedList
import com.novabrowse.browser.data.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

data class DownloadInfo(
    val record: DownloadRecord,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val systemStatus: Int,
    val canOpen: Boolean
) {
    val progress: Float
        get() = if (totalBytes <= 0) 0f else (bytesDownloaded.toFloat() / totalBytes).coerceIn(0f, 1f)
    val isRunning: Boolean
        get() = systemStatus == DownloadManager.STATUS_RUNNING || systemStatus == DownloadManager.STATUS_PENDING
    val isPausedLocal: Boolean get() = record.status == "PAUSED"
}

/**
 * Downloads are handed to Android's DownloadManager (real HTTP transfers, system
 * notifications, Wi-Fi/mobile policies) and mirrored into NovaBrowse's own history.
 *
 * Android's DownloadManager has no pause API, so "Pause" stops the transfer and
 * "Resume" re-enqueues it from the start. This is stated in the UI.
 */
class DownloadCenter(
    private val context: Context,
    private val records: PersistedList<DownloadRecord>,
    private val settings: SettingsStore
) {
    private val manager: DownloadManager =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    private val _progress = MutableStateFlow<List<DownloadInfo>>(emptyList())
    val progress: StateFlow<List<DownloadInfo>> = _progress

    fun enqueue(request: DownloadRequest): Long? {
        if (request.url.startsWith("data:")) return saveDataUrl(request)
        val guessedName = URLUtil.guessFileName(request.url, request.contentDisposition, request.mimeType)
            .ifBlank { "download" }
        val fileName = sanitise(guessedName)
        val mime = request.mimeType?.takeIf { it.isNotBlank() && it != "application/octet-stream" }
            ?: MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(fileName.substringAfterLast('.', "").lowercase())
            ?: "application/octet-stream"

        return runCatching {
            val downloadRequest = DownloadManager.Request(Uri.parse(request.url)).apply {
                setTitle(fileName)
                setDescription("NovaBrowse download")
                setMimeType(mime)
                request.cookie?.let { addRequestHeader("Cookie", it) }
                request.userAgent?.let { addRequestHeader("User-Agent", it) }
                addRequestHeader("Referer", request.pageUrl ?: request.url)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
                setNotificationVisibility(
                    if (settings.value.downloadNotifications)
                        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                    else DownloadManager.Request.VISIBILITY_HIDDEN
                )
                if (settings.value.downloadToPublicFolder) {
                    setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS, "NovaBrowse/$fileName"
                    )
                } else {
                    setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "NovaBrowse/$fileName")
                }
            }
            val id = manager.enqueue(downloadRequest)
            records.add(
                DownloadRecord(
                    id = id,
                    fileName = fileName,
                    url = request.url,
                    mimeType = mime,
                    totalBytes = request.contentLength
                )
            )
            refresh()
            id
        }.getOrNull()
    }

    private fun saveDataUrl(request: DownloadRequest): Long? = runCatching {
        val comma = request.url.indexOf(',')
        require(comma > 0) { "Malformed data URL" }
        val meta = request.url.substring(5, comma)
        val payload = request.url.substring(comma + 1)
        val bytes = if (meta.contains("base64")) {
            android.util.Base64.decode(payload, android.util.Base64.DEFAULT)
        } else {
            java.net.URLDecoder.decode(payload, "UTF-8").toByteArray()
        }
        val name = "data-${System.currentTimeMillis()}.${if (request.mimeType?.contains("html") == true) "html" else "bin"}"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "NovaBrowse/$name")
        file.parentFile?.mkdirs()
        file.writeBytes(bytes)
        val id = -System.currentTimeMillis()
        records.add(
            DownloadRecord(
                id = id, fileName = name, url = "data:", mimeType = request.mimeType ?: "application/octet-stream",
                totalBytes = bytes.size.toLong(), status = "SUCCESSFUL", localPath = file.absolutePath
            )
        )
        refresh()
        id
    }.getOrNull()

    private fun sanitise(name: String): String =
        name.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(120).ifBlank { "download" }

    fun refresh() {
        val infos = records.value.map { record -> query(record) }
        _progress.value = infos
        // Keep stored status in sync with the system download manager.
        infos.forEach { info ->
            val newStatus = when (info.systemStatus) {
                DownloadManager.STATUS_SUCCESSFUL -> "SUCCESSFUL"
                DownloadManager.STATUS_FAILED -> "FAILED"
                DownloadManager.STATUS_RUNNING -> "RUNNING"
                DownloadManager.STATUS_PAUSED -> "PAUSED"
                else -> "PENDING"
            }
            if (info.record.status != newStatus && info.record.status != "PAUSED") {
                records.update({ it.id == info.record.id }) { it.copy(status = newStatus) }
            }
        }
    }

    private fun query(record: DownloadRecord): DownloadInfo {
        if (record.localPath != null) {
            return DownloadInfo(record, record.totalBytes, record.totalBytes, DownloadManager.STATUS_SUCCESSFUL, true)
        }
        val cursor = runCatching {
            manager.query(DownloadManager.Query().setFilterById(record.id))
        }.getOrNull()
        cursor?.use { c ->
            if (c.moveToFirst()) {
                val downloaded = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                var total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                if (total <= 0) total = record.totalBytes
                val status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                return DownloadInfo(record, downloaded, total, status, status == DownloadManager.STATUS_SUCCESSFUL)
            }
        }
        return DownloadInfo(record, 0, record.totalBytes, DownloadManager.STATUS_SUCCESSFUL, record.status == "SUCCESSFUL")
    }

    fun pause(record: DownloadRecord) {
        runCatching { manager.remove(record.id) }
        records.update({ it.id == record.id }) { it.copy(status = "PAUSED") }
        refresh()
    }

    fun resume(record: DownloadRecord) {
        records.remove { it.id == record.id }
        enqueue(
            DownloadRequest(record.url, null, null, record.mimeType, record.totalBytes, null, null)
        )
    }

    fun cancel(record: DownloadRecord) {
        runCatching { manager.remove(record.id) }
        records.remove { it.id == record.id }
        refresh()
    }

    fun removeFromHistory(record: DownloadRecord) {
        if (record.status != "RUNNING") records.remove { it.id == record.id }
        refresh()
    }

    fun clearHistory() {
        records.value.filterNot { it.status == "RUNNING" }.forEach { record ->
            records.remove { it.id == record.id }
        }
        refresh()
    }

    fun open(record: DownloadRecord): Boolean = runCatching {
        val uri = uriFor(record) ?: return false
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, record.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    }.getOrElse { false }

    fun share(record: DownloadRecord): Boolean = runCatching {
        val uri = uriFor(record) ?: return false
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = record.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share ${record.fileName}").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        true
    }.getOrElse { false }

    private fun uriFor(record: DownloadRecord): Uri? {
        val localPath = record.localPath
        if (localPath != null && File(localPath).exists()) {
            return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(localPath))
        }
        return runCatching { manager.getUriForDownloadedFile(record.id) }.getOrNull()
    }

    fun localFile(record: DownloadRecord): File? {
        val path = record.localPath ?: return null
        return File(path).takeIf { it.exists() }
    }
}
