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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.novabrowse.browser.ui.components.ChromeCard
import com.novabrowse.browser.ui.components.FaviconImage
import com.novabrowse.browser.ui.components.NovaChip
import com.novabrowse.browser.ui.components.NovaDialog
import com.novabrowse.browser.ui.components.NovaWordmark
import com.novabrowse.browser.ui.components.SectionTitle
import com.novabrowse.browser.ui.theme.Wallpapers
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(vm: NovaViewModel, modifier: Modifier = Modifier) {
    val settings = vm.settings
    val shortcuts by vm.shortcutsFlow.collectAsStateWithLifecycle()
    val bookmarks by vm.bookmarksFlow.collectAsStateWithLifecycle()
    val recent by vm.historyFlow.collectAsStateWithLifecycle()
    val closed by vm.closedFlow.collectAsStateWithLifecycle()
    var actionShortcut by remember { mutableStateOf<Shortcut?>(null) }

    Box(modifier.fillMaxSize()) {
        HomeBackdrop(settings)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NovaWordmark(size = 22)
                    Spacer(Modifier.weight(1f))
                    if (vm.isPrivate) {
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.VisibilityOff, null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Private",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    IconButton(onClick = { vm.openSettings() }, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.Rounded.Settings, "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }

            item { HomeSearchPill(vm) }

            if (settings.showClock) {
                item { HomeClock() }
            }

            if (vm.isPrivate) {
                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f))
                            .padding(14.dp)
                    ) {
                        Text(
                            "You're browsing privately",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "History, cookies and site data from these tabs are kept apart and discarded when " +
                                "the last private tab closes. Websites, your network and your provider can still " +
                                "see your traffic.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (settings.showShortcuts) {
                item {
                    Column(Modifier.padding(top = 8.dp)) {
                        SectionTitle(
                            "Shortcuts",
                            action = "Add",
                            onAction = { vm.openShortcutDraft(null) }
                        )
                        ChromeCard {
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
            }

            if (settings.showFavourites && bookmarks.isNotEmpty()) {
                item {
                    Column {
                        SectionTitle("Favourites", action = "All", onAction = { vm.openScreen(NovaScreen.BOOKMARKS) })
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(bookmarks.take(12), key = { it.id }) { bm ->
                                FavouriteTile(bm) { vm.navigate(bm.url) }
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NovaChip("Bookmarks", onClick = { vm.openScreen(NovaScreen.BOOKMARKS) }, leading = Icons.Rounded.Bookmark)
                    NovaChip("History", onClick = { vm.openScreen(NovaScreen.HISTORY) }, leading = Icons.Rounded.History)
                    NovaChip("Reading", onClick = { vm.openScreen(NovaScreen.READING_LIST) }, leading = Icons.Rounded.MenuBook)
                    NovaChip("Downloads", onClick = { vm.openScreen(NovaScreen.DOWNLOADS) }, leading = Icons.Rounded.Download)
                }
            }

            if (settings.showRecent && recent.isNotEmpty()) {
                item {
                    Column {
                        SectionTitle("Recently visited", action = "History", onAction = { vm.openScreen(NovaScreen.HISTORY) })
                        ChromeCard {
                            recent.sortedByDescending { it.visitedAt }.take(6).forEachIndexed { index, entry ->
                                RecentRow(entry) { vm.navigate(entry.url) }
                                if (index < 5) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(start = 58.dp)
                                            .height(1.dp)
                                            .background(MaterialTheme.colorScheme.outlineVariant)
                                    )
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
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(closed.take(8), key = { it.id }) { entry ->
                                ClosedChip(entry, onOpen = { vm.restoreClosed(entry) })
                            }
                        }
                    }
                }
            }

            item {
                Box(Modifier.fillMaxWidth().padding(top = 18.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "BROWSE BEYOND.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }

    actionShortcut?.let { shortcut ->
        NovaDialog(onDismiss = { actionShortcut = null }) {
            Text(shortcut.title, style = MaterialTheme.typography.titleLarge)
            Text(
                UrlTools.displayUrl(shortcut.url),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
        style = MaterialTheme.typography.bodyLarge,
        color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(onClick = onClick)
            .padding(vertical = 12.dp)
    )
}

@Composable
fun HomeBackdrop(settings: AppSettings) {
    val custom = settings.wallpaper.startsWith("file:")
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (settings.wallpaper != "plain") {
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
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = settings.wallpaperDim * 0.55f))
            )
        }
    }
}

@Composable
private fun HomeSearchPill(vm: NovaViewModel) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(onClick = vm::openSearch)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Rounded.Search, null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            "Search ${vm.currentEngine.name} or type URL",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Rounded.Mic, "Voice search",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun HomeClock() {
    val now by produceState(initialValue = Date()) {
        while (true) {
            value = Date()
            delay(30_000)
        }
    }
    Text(
        SimpleDateFormat("EEEE, d MMMM · HH:mm", Locale.getDefault()).format(now),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 6.dp, top = 8.dp)
    )
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
    if (tiles.isEmpty() || tiles.size % cols != 0) tiles.add(null)
    val rows = tiles.chunked(cols)
    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(onClick = { onOpen(shortcut) }, onLongClick = { onLongPress(shortcut) })
            .padding(vertical = 8.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            FaviconImage(shortcut.url, size = 26.dp, corner = 6.dp)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            shortcut.title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AddShortcutTile(onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Add, "Add shortcut",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Add",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FavouriteTile(bookmark: Bookmark, onClick: () -> Unit) {
    Row(
        Modifier
            .width(168.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FaviconImage(bookmark.url, size = 22.dp, corner = 6.dp)
        Spacer(Modifier.width(10.dp))
        Text(
            bookmark.title.ifBlank { UrlTools.displayUrl(bookmark.url) },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun RecentRow(entry: HistoryEntry, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FaviconImage(entry.url, size = 28.dp, corner = 7.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                entry.title.ifBlank { UrlTools.displayUrl(entry.url) },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                UrlTools.displayUrl(entry.url),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ClosedChip(entry: ClosedTabEntry, onOpen: () -> Unit) {
    Row(
        Modifier
            .width(168.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(onClick = onOpen)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FaviconImage(entry.url, size = 20.dp, corner = 5.dp)
        Spacer(Modifier.width(8.dp))
        Text(
            entry.title.ifBlank { UrlTools.displayUrl(entry.url) },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
