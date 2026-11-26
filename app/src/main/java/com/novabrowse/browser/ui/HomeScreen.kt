@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.novabrowse.browser.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.novabrowse.browser.browser.UrlTools
import com.novabrowse.browser.data.AppSettings
import com.novabrowse.browser.data.Bookmark
import com.novabrowse.browser.data.ClosedTabEntry
import com.novabrowse.browser.data.HistoryEntry
import com.novabrowse.browser.data.Shortcut
import com.novabrowse.browser.ui.components.FaviconImage
import com.novabrowse.browser.ui.components.GlassIconButton
import com.novabrowse.browser.ui.components.GlassPanel
import com.novabrowse.browser.ui.components.GlassPill
import com.novabrowse.browser.ui.components.NovaChip
import com.novabrowse.browser.ui.components.NovaDialog
import com.novabrowse.browser.ui.components.NovaWordmark
import com.novabrowse.browser.ui.components.SectionTitle
import com.novabrowse.browser.ui.theme.LocalNovaStyle
import com.novabrowse.browser.ui.theme.Wallpapers
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(vm: NovaViewModel, modifier: Modifier = Modifier) {
    val settings = vm.settings
    val shortcuts by vm.shortcutsFlow.collectAsStateWithLifecycle()
    val bookmarks by vm.bookmarksFlow.collectAsStateWithLifecycle()
    val recent by vm.historyFlow.collectAsStateWithLifecycle()
    val closed by vm.closedFlow.collectAsStateWithLifecycle()
    var actionShortcut by remember { mutableStateOf<Shortcut?>(null) }
    val style = LocalNovaStyle.current

    Box(modifier.fillMaxSize()) {
        HomeBackdrop(settings)

        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 132.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        if (settings.showClock) {
                            val now by produceState(initialValue = Date()) {
                                while (true) { value = Date(); delay(15_000) }
                            }
                            Text(
                                SimpleDateFormat("HH:mm", Locale.getDefault()).format(now),
                                style = MaterialTheme.typography.displayLarge,
                                fontSize = 40.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                            )
                        }
                    }
                    if (vm.isPrivate) {
                        GlassPill {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.VisibilityOff, null, modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.tertiary)
                                Spacer(Modifier.width(6.dp))
                                Text("PRIVATE", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    GlassIconButton(onClick = { vm.openSettings() }, size = 44.dp) {
                        Icon(Icons.Rounded.Settings, "Settings", modifier = Modifier.size(20.dp))
                    }
                }
            }

            item {
                Column(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    NovaWordmark(size = 38, showTagline = true, light = !style.dark)
                }
            }

            item { HomeSearchPill(vm) }

            if (vm.isPrivate) {
                item {
                    GlassPanel(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        tint = Color(0xFF241046),
                        strong = true
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Private Browsing", style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFFE6D7FF))
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "History, cookies and site data from these tabs are kept apart and discarded " +
                                    "when the last private tab closes. Websites, your network and your provider can still see your traffic.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFCFC0F0)
                            )
                        }
                    }
                }
            }

            if (settings.showShortcuts) {
                item {
                    Column {
                        SectionTitle(
                            "Shortcuts",
                            action = "Add",
                            onAction = { vm.openShortcutDraft(null) }
                        )
                        Spacer(Modifier.height(10.dp))
                        ShortcutGrid(
                            shortcuts = shortcuts.sortedBy { it.order },
                            columns = settings.shortcutColumns,
                            onOpen = { vm.navigate(it.url) },
                            onLongPress = { actionShortcut = it },
                            onAdd = { vm.openShortcutDraft(null) }
                        )
                    }
                }
            }

            if (settings.showFavourites && bookmarks.isNotEmpty()) {
                item {
                    Column {
                        SectionTitle("Favourites", action = "All", onAction = { vm.openScreen(NovaScreen.BOOKMARKS) })
                        Spacer(Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(bookmarks.take(12), key = { it.id }) { bm ->
                                FavouriteCard(bm) { vm.navigate(bm.url) }
                            }
                        }
                    }
                }
            }

            item { QuickActionsRow(vm) }

            if (settings.showBookmarks && bookmarks.isEmpty()) {
                item {
                    GlassPanel(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Bookmark, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("No favourites yet", style = MaterialTheme.typography.titleMedium)
                                Text("Tap the bookmark icon while browsing to keep a page here.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            }

            if (settings.showRecent && recent.isNotEmpty()) {
                item {
                    Column {
                        SectionTitle("Recently visited", action = "History", onAction = { vm.openScreen(NovaScreen.HISTORY) })
                        Spacer(Modifier.height(8.dp))
                        GlassPanel(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                            Column {
                                recent.sortedByDescending { it.visitedAt }.take(6).forEachIndexed { index, entry ->
                                    RecentRow(entry) { vm.navigate(entry.url) }
                                    if (index < 5) {
                                        Box(
                                            Modifier.fillMaxWidth().height(1.dp)
                                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (settings.showRecentlyClosed && closed.isNotEmpty()) {
                item {
                    Column {
                        SectionTitle("Recently closed")
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(closed.take(8), key = { it.id }) { entry ->
                                ClosedChip(entry, onOpen = { vm.restoreClosed(entry) })
                            }
                        }
                    }
                }
            }

            item {
                Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "BROWSE BEYOND.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    )
                }
            }
        }
    }

    actionShortcut?.let { shortcut ->
        NovaDialog(onDismiss = { actionShortcut = null }) {
            Text(shortcut.title, style = MaterialTheme.typography.titleLarge)
            Text(UrlTools.displayUrl(shortcut.url), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(Modifier.height(16.dp))
            ActionRow("Edit shortcut") { actionShortcut = null; vm.openShortcutDraft(shortcut) }
            ActionRow("Move left") { vm.moveShortcut(shortcut, -1); actionShortcut = null }
            ActionRow("Move right") { vm.moveShortcut(shortcut, 1); actionShortcut = null }
            ActionRow("Remove", destructive = true) { vm.removeShortcut(shortcut); actionShortcut = null }
        }
    }
}

@Composable
private fun ActionRow(label: String, destructive: Boolean = false, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.titleMedium,
        color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 6.dp)
    )
}

@Composable
fun HomeBackdrop(settings: AppSettings) {
    val custom = settings.wallpaper.startsWith("file:")
    Box(Modifier.fillMaxSize()) {
        if (custom) {
            AsyncImage(
                model = File(settings.wallpaper.removePrefix("file:")),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(Modifier.fillMaxSize().background(Wallpapers.brush(settings.wallpaper)))
        }
        // Aurora glows for depth.
        Box(
            Modifier
                .size(320.dp)
                .align(Alignment.TopEnd)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF45E0FF).copy(alpha = 0.20f), Color.Transparent)
                    )
                )
        )
        Box(
            Modifier
                .size(360.dp)
                .align(Alignment.BottomStart)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF9B5CFF).copy(alpha = 0.20f), Color.Transparent)
                    )
                )
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = settings.wallpaperDim * 0.5f),
                            Color.Black.copy(alpha = settings.wallpaperDim * 0.15f),
                            Color.Black.copy(alpha = settings.wallpaperDim * 0.55f)
                        )
                    )
                )
        )
    }
}

@Composable
private fun HomeSearchPill(vm: NovaViewModel) {
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .combinedClickable(onClick = vm::openSearch),
        shape = RoundedCornerShape(26.dp),
        strong = true,
        elevation = 18.dp
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                "Search or enter website",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Rounded.Mic, "Voice search", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ShortcutGrid(
    shortcuts: List<Shortcut>,
    columns: Int,
    onOpen: (Shortcut) -> Unit,
    onLongPress: (Shortcut) -> Unit,
    onAdd: () -> Unit
) {
    val cols = columns.coerceIn(3, 5)
    val tiles = shortcuts.toMutableList<Shortcut?>()
    if (tiles.size < cols || tiles.size % cols != 0) tiles.add(null)
    val rows = tiles.chunked(cols)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { shortcut ->
                    Box(Modifier.weight(1f)) {
                        if (shortcut == null) {
                            AddShortcutTile(onAdd)
                        } else {
                            ShortcutTile(shortcut, onOpen, onLongPress)
                        }
                    }
                }
                repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShortcutTile(shortcut: Shortcut, onOpen: (Shortcut) -> Unit, onLongPress: (Shortcut) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(onClick = { onOpen(shortcut) }, onLongClick = { onLongPress(shortcut) })
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GlassPanel(Modifier.size(58.dp), shape = RoundedCornerShape(19.dp), elevation = 8.dp) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                FaviconImage(shortcut.url, size = 34.dp, corner = 9.dp)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            shortcut.title,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Medium),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
        )
    }
}

@Composable
private fun AddShortcutTile(onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).combinedClickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GlassPanel(Modifier.size(58.dp), shape = RoundedCornerShape(19.dp), elevation = 4.dp) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Add, "Add shortcut", tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("Add", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.5.sp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

@Composable
private fun FavouriteCard(bookmark: Bookmark, onClick: () -> Unit) {
    GlassPanel(
        modifier = Modifier.width(132.dp).clip(RoundedCornerShape(20.dp)).combinedClickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = 6.dp
    ) {
        Column(Modifier.padding(12.dp)) {
            FaviconImage(bookmark.url, size = 34.dp, corner = 10.dp)
            Spacer(Modifier.height(8.dp))
            Text(bookmark.title.ifBlank { UrlTools.displayUrl(bookmark.url) },
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
            Text(UrlTools.displayUrl(bookmark.url), maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
        }
    }
}

@Composable
private fun QuickActionsRow(vm: NovaViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        QuickActionPill(Icons.Rounded.Bookmark, "Bookmarks") { vm.openScreen(NovaScreen.BOOKMARKS) }
        QuickActionPill(Icons.Rounded.History, "History") { vm.openScreen(NovaScreen.HISTORY) }
        QuickActionPill(Icons.Rounded.MenuBook, "Reading") { vm.openScreen(NovaScreen.READING_LIST) }
        QuickActionPill(Icons.Rounded.Download, "Downloads") { vm.openScreen(NovaScreen.DOWNLOADS) }
    }
}

@Composable
private fun QuickActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    GlassPanel(
        modifier = Modifier.clip(CircleShape).combinedClickable(onClick = onClick),
        shape = CircleShape,
        elevation = 4.dp
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun RecentRow(entry: HistoryEntry, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().combinedClickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FaviconImage(entry.url, size = 30.dp, corner = 9.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.title.ifBlank { UrlTools.displayUrl(entry.url) }, maxLines = 1,
                overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
            Text(UrlTools.displayUrl(entry.url), maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
        }
        Icon(Icons.Rounded.ChevronRight, null, modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    }
}

@Composable
private fun ClosedChip(entry: ClosedTabEntry, onOpen: () -> Unit) {
    GlassPanel(
        modifier = Modifier.width(168.dp).clip(RoundedCornerShape(16.dp)).combinedClickable(onClick = onOpen),
        shape = RoundedCornerShape(16.dp),
        elevation = 4.dp
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            FaviconImage(entry.url, size = 22.dp, corner = 7.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                entry.title.ifBlank { UrlTools.displayUrl(entry.url) },
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
