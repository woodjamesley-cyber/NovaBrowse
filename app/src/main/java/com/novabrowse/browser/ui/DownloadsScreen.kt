package com.novabrowse.browser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novabrowse.browser.downloads.DownloadInfo
import com.novabrowse.browser.ui.components.EmptyState
import com.novabrowse.browser.ui.components.GlassIconButton
import com.novabrowse.browser.ui.components.GlassPanel
import com.novabrowse.browser.ui.components.NovaScreen
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DownloadsScreen(vm: NovaViewModel) {
    val progress by vm.downloadProgress.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        while (true) {
            vm.refreshDownloads()
            delay(1000)
        }
    }
    NovaScreen(
        title = "Downloads",
        onClose = { vm.closeScreen() },
        actions = {
            GlassIconButton(onClick = { vm.clearDownloads() }, size = 40.dp) {
                Icon(Icons.Rounded.DeleteSweep, "Clear history", modifier = Modifier.size(18.dp))
            }
        }
    ) {
        if (progress.isEmpty()) {
            EmptyState(
                Icons.Rounded.Download,
                "No downloads yet",
                "Files you download while browsing appear here with live progress",
                Modifier.fillMaxSize()
            )
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(progress, key = { it.record.id }) { info ->
                    DownloadCard(
                        info = info,
                        onOpen = { vm.openDownload(info.record.id) },
                        onShare = { vm.shareDownload(info.record.id) },
                        onPause = { vm.pauseDownload(info.record.id) },
                        onResume = { vm.resumeDownload(info.record.id) },
                        onCancel = { vm.cancelDownload(info.record.id) },
                        onRemove = { vm.removeDownload(info.record.id) }
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
                item {
                    Text(
                        "Pause stops the system transfer; resume restarts the download from the beginning " +
                            "because Android's DownloadManager does not support resuming by itself.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadCard(
    info: DownloadInfo,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRemove: () -> Unit
) {
    val record = info.record
    GlassPanel(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), elevation = 8.dp) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(38.dp).clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Download, null, tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(record.fileName, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium)
                    Text(
                        buildString {
                            append(formatBytes(if (info.totalBytes > 0) info.totalBytes else record.totalBytes))
                            append(" · ")
                            append(record.status.lowercase().replaceFirstChar { it.uppercase() })
                            append(" · ")
                            append(SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(record.createdAt)))
                        },
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                GlassIconButton(onClick = onRemove, size = 30.dp) {
                    Icon(Icons.Rounded.Close, "Remove", modifier = Modifier.size(14.dp))
                }
            }
            if (info.isRunning) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { info.progress },
                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${formatBytes(info.bytesDownloaded)} of ${formatBytes(info.totalBytes)}",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (info.isRunning) {
                    DownloadAction(Icons.Rounded.Pause, "Pause", onPause)
                } else if (info.isPausedLocal) {
                    DownloadAction(Icons.Rounded.PlayArrow, "Resume", onResume)
                } else if (record.status == "SUCCESSFUL") {
                    DownloadAction(Icons.Rounded.OpenInNew, "Open", onOpen)
                }
                DownloadAction(Icons.Rounded.Share, "Share", onShare)
                if (!info.isRunning && record.status != "SUCCESSFUL") {
                    DownloadAction(Icons.Rounded.Close, "Cancel", onCancel)
                }
            }
        }
    }
}

@Composable
private fun DownloadAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        Modifier.clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, fontSize = 12.sp)
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "Unknown size"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1 -> String.format(Locale.US, "%.2f GB", gb)
        mb >= 1 -> String.format(Locale.US, "%.1f MB", mb)
        kb >= 1 -> String.format(Locale.US, "%.0f KB", kb)
        else -> "$bytes B"
    }
}
