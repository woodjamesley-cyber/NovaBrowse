package com.novabrowse.browser.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.novabrowse.browser.Nova
import com.novabrowse.browser.extensions.ExtensionRecord
import com.novabrowse.browser.ui.components.EmptyState
import com.novabrowse.browser.ui.components.GlassIconButton
import com.novabrowse.browser.ui.components.GlassPanel
import com.novabrowse.browser.ui.components.NovaDialog
import com.novabrowse.browser.ui.components.NovaScreen
import java.io.File

@Composable
fun ExtensionsScreen(vm: NovaViewModel) {
    val installed by vm.extensionsFlow.collectAsStateWithLifecycle()
    var detail by remember { mutableStateOf<ExtensionRecord?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { vm.installExtension(it) }
    }

    NovaScreen(
        title = "Extensions",
        onClose = { vm.closeScreen() },
        actions = {
            GlassIconButton(onClick = { vm.openExtensionStore() }, size = 40.dp) {
                Icon(Icons.Rounded.Storefront, "Open Chrome Web Store", modifier = Modifier.size(19.dp))
            }
        }
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                GlassPanel(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), elevation = 8.dp) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Extensions from the Chrome Web Store",
                            style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Search Google's store and install with one tap. NovaBrowse downloads the " +
                                "same signed CRX that desktop Chrome installs and runs it in its own " +
                                "chrome.* runtime: content scripts, background pages, storage, tabs, " +
                                "scripting, messaging, bookmarks, notifications and context menus. " +
                                "Unpacked .zip and .crx packages can still be imported.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ActionPill(Icons.Rounded.Storefront, "Chrome Web Store") {
                                vm.openExtensionStore()
                            }
                            ActionPill(Icons.Rounded.FileUpload, "Import .zip / .crx") {
                                picker.launch("*/*")
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ActionPill(Icons.Rounded.Link, "Install from URL") {
                                vm.openInstallUrlDialog()
                            }
                        }
                    }
                }
            }
            if (installed.isEmpty()) {
                item {
                    EmptyState(Icons.Rounded.Extension, "No extensions installed",
                        "Open the Chrome Web Store to add browser features",
                        Modifier.fillMaxWidth().padding(vertical = 30.dp))
                }
            } else {
                items(installed, key = { it.id }) { record ->
                    GlassPanel(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                            .clickable { detail = record },
                        shape = RoundedCornerShape(18.dp),
                        elevation = 6.dp
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            ExtensionIcon(record, 44.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(record.name, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.titleMedium)
                                Text("v${record.version} · ${record.hostAccessSummary}",
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                if (record.unsupported.isNotEmpty()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Warning, null, modifier = Modifier.size(11.dp),
                                            tint = Color(0xFFFFB74D))
                                        Spacer(Modifier.width(4.dp))
                                        Text("${record.unsupported.size} compatibility notes",
                                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                                            color = Color(0xFFFFB74D))
                                    }
                                }
                            }
                            Switch(
                                checked = record.enabled,
                                onCheckedChange = { vm.toggleExtension(record, it) }
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(30.dp)) }
        }
    }

    detail?.let { record ->
        val live = installed.firstOrNull { it.id == record.id } ?: record
        NovaDialog(onDismiss = { detail = null }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ExtensionIcon(live, 46.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(live.name, style = MaterialTheme.typography.titleLarge)
                    Text("Version ${live.version} · Manifest V${live.manifestVersion}",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(live.description.ifBlank { "No description provided." },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Spacer(Modifier.height(12.dp))
            Text("Permissions", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
            Spacer(Modifier.height(6.dp))
            Text(
                (live.permissions + live.hostPermissions).joinToString(", ").ifBlank { "None requested" },
                style = MaterialTheme.typography.bodyMedium
            )
            if (live.unsupported.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("Compatibility", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
                live.unsupported.forEach { note ->
                    Row(Modifier.padding(vertical = 4.dp)) {
                        Icon(Icons.Rounded.Info, null, modifier = Modifier.size(13.dp),
                            tint = Color(0xFFFFB74D))
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text(note.api, style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFFFB74D))
                            Text(note.reason, style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }
            }
            if (live.hasPopup) {
                Spacer(Modifier.height(12.dp))
                ActionPill(Icons.Rounded.OpenInNew, "Open extension popup") {
                    detail = null
                    vm.openExtensionPopup(live)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text("Remove", color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.clip(CircleShape)
                        .clickable { vm.removeExtension(live); detail = null }
                        .padding(horizontal = 14.dp, vertical = 10.dp))
                Spacer(Modifier.width(8.dp))
                Text("Close", style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.clip(CircleShape).clickable { detail = null }
                        .padding(horizontal = 14.dp, vertical = 10.dp))
            }
        }
    }
}

@Composable
fun CompatibilityBadge(level: CatalogEntry.Compatibility) {
    val (label, color) = when (level) {
        CatalogEntry.Compatibility.WORKS -> "Likely works" to Color(0xFF4ADE80)
        CatalogEntry.Compatibility.LIMITED -> "Limited" to Color(0xFFFFB74D)
        CatalogEntry.Compatibility.NOT_SUPPORTED -> "Not supported" to Color(0xFFFF6B8B)
    }
    Row(
        Modifier.clip(CircleShape).background(color.copy(alpha = 0.14f))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (level == CatalogEntry.Compatibility.NOT_SUPPORTED) {
            Icon(Icons.Rounded.Warning, null, tint = color, modifier = Modifier.size(11.dp))
            Spacer(Modifier.width(4.dp))
        } else if (level == CatalogEntry.Compatibility.WORKS) {
            Icon(Icons.Rounded.Check, null, tint = color, modifier = Modifier.size(11.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(label, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp), color = color)
    }
}

@Composable
private fun ActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        Modifier.clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, fontSize = 12.sp)
    }
}

@Composable
fun ExtensionIcon(record: ExtensionRecord, size: androidx.compose.ui.unit.Dp) {
    val path = record.iconPath
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.28f))
            .background(
                Brush.linearGradient(listOf(Color(0xFF2F6BFF), Color(0xFF9B5CFF)))
            ),
        contentAlignment = Alignment.Center
    ) {
        if (path != null && File(path).exists()) {
            AsyncImage(
                model = File(path),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(size * 0.16f)
            )
        } else {
            Text(record.name.take(1).uppercase(), color = Color.White,
                style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun ExtensionInstallDialog(vm: NovaViewModel) {
    val record = vm.pendingExtension ?: return
    NovaDialog(onDismiss = { vm.cancelExtensionInstall(record) }) {
        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ExtensionIcon(record, 46.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(record.name, style = MaterialTheme.typography.titleLarge)
                    Text("v${record.version} · Manifest V${record.manifestVersion}",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
            Spacer(Modifier.height(10.dp))
            Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                Text(record.description.ifBlank { "No description provided." },
                    style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                Text("It wants to:", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
                permissionList(record).forEach { permission ->
                    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Check, null, modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text(permission, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (record.unsupported.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text("Compatibility notes", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
                    record.unsupported.forEach { note ->
                        Text("• ${note.reason}", style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFFFB74D), modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Extensions run in NovaBrowse's sandbox. They only get the APIs listed above, " +
                        "and you can disable or remove them at any time.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text("Cancel", style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.clip(CircleShape)
                        .clickable { vm.cancelExtensionInstall(record) }
                        .padding(horizontal = 14.dp, vertical = 10.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Install",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    modifier = Modifier.clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { vm.confirmExtensionInstall(record) }
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                )
            }
        }
    }
}

private fun permissionList(record: ExtensionRecord): List<String> {
    val labels = mapOf(
        "storage" to "Store data on this device",
        "tabs" to "Read and manage your tabs",
        "bookmarks" to "Read and change your bookmarks",
        "notifications" to "Show notifications",
        "contextMenus" to "Add items to the browser menu",
        "scripting" to "Run scripts on pages you allow",
        "activeTab" to "Act on the page when you tap the extension",
        "history" to "Read your browsing history"
    )
    val items = record.permissions.map { labels[it] ?: "Use chrome.$it" }.toMutableList()
    if (record.hostPermissions.isNotEmpty()) {
        items.add("Access ${record.hostAccessSummary.lowercase()}")
    }
    if (record.contentScripts.isNotEmpty()) {
        items.add("Automatically run on matching pages")
    }
    return items.distinct()
}

@Composable
fun ExtensionPopupDialog(vm: NovaViewModel) {
    val record = vm.extensionPopup ?: return
    val html = remember(record.id) { Nova.extensions.popupDocument(record) }
    NovaDialog(onDismiss = { vm.extensionPopup = null }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ExtensionIcon(record, 32.dp)
            Spacer(Modifier.width(10.dp))
            Text(record.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.Close, "Close", modifier = Modifier.size(18.dp)
                .clickable { vm.extensionPopup = null })
        }
        Spacer(Modifier.height(10.dp))
        if (html == null) {
            Text("This extension has no popup page.", style = MaterialTheme.typography.bodyMedium)
        } else {
            androidx.compose.ui.viewinterop.AndroidView(
                modifier = Modifier.fillMaxWidth().height(380.dp),
                factory = { context ->
                    android.webkit.WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        Nova.extensions.attachPopup(this, record)
                        loadDataWithBaseURL(
                            com.novabrowse.browser.extensions.ExtensionManager.BACKGROUND_ORIGIN,
                            html, "text/html", "utf-8", null
                        )
                    }
                }
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Popup content is rendered in a sandboxed WebView with the extension's chrome.* bridge.",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
        )
    }
}
