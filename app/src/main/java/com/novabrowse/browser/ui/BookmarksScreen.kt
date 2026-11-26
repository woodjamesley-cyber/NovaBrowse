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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novabrowse.browser.browser.UrlTools
import com.novabrowse.browser.data.Bookmark
import com.novabrowse.browser.ui.components.EmptyState
import com.novabrowse.browser.ui.components.FaviconImage
import com.novabrowse.browser.ui.components.GlassIconButton
import com.novabrowse.browser.ui.components.GlassPanel
import com.novabrowse.browser.ui.components.NovaChip
import com.novabrowse.browser.ui.components.NovaDialog
import com.novabrowse.browser.ui.components.NovaScreen

@Composable
fun BookmarksScreen(vm: NovaViewModel) {
    val bookmarks by vm.bookmarksFlow.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var folder by remember { mutableStateOf("All") }
    val folders = remember(bookmarks) { listOf("All") + bookmarks.map { it.folder }.distinct().sorted() }
    val filtered = bookmarks.filter { bm ->
        (folder == "All" || bm.folder == folder) &&
            (query.isBlank() || bm.title.contains(query, true) || bm.url.contains(query, true))
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(vm::importBookmarks)
    }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/html")
    ) { uri -> uri?.let(vm::exportBookmarks) }

    NovaScreen(
        title = "Bookmarks",
        onClose = { vm.closeScreen() },
        actions = {
            GlassIconButton(onClick = { vm.openBookmarkDraft(null) }, size = 40.dp) {
                Icon(Icons.Rounded.Add, "New bookmark", modifier = Modifier.size(18.dp))
            }
        }
    ) {
        Column(Modifier.fillMaxSize()) {
            GlassPanel(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp).height(46.dp),
                shape = RoundedCornerShape(23.dp),
                elevation = 8.dp
            ) {
                Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Search, null, modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (query.isEmpty()) {
                                Text("Search bookmarks", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f))
                            }
                            inner()
                        }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            LazyRow(
                Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(folders) { name ->
                    NovaChip(name, selected = folder == name, onClick = { folder = name })
                }
            }
            Spacer(Modifier.height(8.dp))
            if (filtered.isEmpty()) {
                EmptyState(
                    Icons.Rounded.Bookmark,
                    "Nothing here yet",
                    "Bookmark pages from the browser menu, or import a bookmarks file",
                    Modifier.weight(1f)
                )
            } else {
                LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
                    items(filtered, key = { it.id }) { bm ->
                        GlassPanel(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { vm.navigate(bm.url); vm.closeScreen() },
                            shape = RoundedCornerShape(18.dp),
                            elevation = 6.dp
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                FaviconImage(bm.url, size = 34.dp, corner = 10.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(bm.title.ifBlank { UrlTools.displayUrl(bm.url) },
                                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodyLarge)
                                    Text("${bm.folder} · ${UrlTools.displayUrl(bm.url)}",
                                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
                                }
                                Icon(Icons.Rounded.Edit, "Edit", modifier = Modifier.size(17.dp)
                                    .clickable { vm.openBookmarkDraft(bm) })
                                Spacer(Modifier.width(14.dp))
                                Icon(Icons.Rounded.Delete, "Delete",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(17.dp).clickable { vm.deleteBookmark(bm.id) })
                            }
                        }
                    }
                    item { Spacer(Modifier.height(100.dp)) }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassPanel(Modifier.clip(CircleShape).clickable {
                    importLauncher.launch(arrayOf("text/html", "text/plain", "application/octet-stream"))
                }, shape = CircleShape, elevation = 4.dp) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.FileUpload, null, modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text("Import HTML", style = MaterialTheme.typography.labelLarge)
                    }
                }
                GlassPanel(Modifier.clip(CircleShape).clickable {
                    exportLauncher.launch("novabrowse-bookmarks.html")
                }, shape = CircleShape, elevation = 4.dp) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.FileDownload, null, modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text("Export", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
fun BookmarkEditorDialog(vm: NovaViewModel) {
    val draft = vm.bookmarkDraft ?: return
    var title by remember(draft.id) { mutableStateOf(draft.title) }
    var url by remember(draft.id) { mutableStateOf(draft.url) }
    var folder by remember(draft.id) { mutableStateOf(draft.folder) }
    NovaDialog(onDismiss = { vm.bookmarkDraft = null }) {
        Text(if (draft.id == null) "New bookmark" else "Edit bookmark",
            style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Field("Title", title) { title = it }
        Spacer(Modifier.height(8.dp))
        Field("Web address", url) { url = it }
        Spacer(Modifier.height(8.dp))
        Field("Folder", folder) { folder = it }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text("Cancel", style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.clip(CircleShape).clickable { vm.bookmarkDraft = null }
                    .padding(horizontal = 14.dp, vertical = 10.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "Save",
                style = MaterialTheme.typography.labelLarge,
                color = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { vm.saveBookmark(BookmarkDraft(draft.id, title, url, folder)) }
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
fun Field(label: String, value: String, onValueChange: (String) -> Unit) {
    GlassPanel(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), elevation = 2.dp) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
