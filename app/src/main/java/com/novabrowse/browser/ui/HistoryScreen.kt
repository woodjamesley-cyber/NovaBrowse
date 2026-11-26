package com.novabrowse.browser.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MenuBook
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
import com.novabrowse.browser.ui.components.EmptyState
import com.novabrowse.browser.ui.components.FaviconImage
import com.novabrowse.browser.ui.components.GlassIconButton
import com.novabrowse.browser.ui.components.GlassPanel
import com.novabrowse.browser.ui.components.NovaChip
import com.novabrowse.browser.ui.components.NovaScreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(vm: NovaViewModel) {
    val history by vm.historyFlow.collectAsStateWithLifecycle()
    val closed by vm.closedFlow.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf(0) }

    val filtered = remember(history, query) {
        history.filter { query.isBlank() || it.title.contains(query, true) || it.url.contains(query, true) }
            .sortedByDescending { it.visitedAt }
    }
    val grouped = remember(filtered) {
        filtered.groupBy { dayLabel(it.visitedAt) }.toList()
    }

    NovaScreen(
        title = "History",
        onClose = { vm.closeScreen() },
        actions = {
            GlassIconButton(onClick = { vm.clearHistory() }, size = 40.dp) {
                Icon(Icons.Rounded.DeleteSweep, "Clear history", modifier = Modifier.size(18.dp))
            }
        }
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NovaChip("History", selected = tab == 0, onClick = { tab = 0 })
                NovaChip("Recently closed", selected = tab == 1, onClick = { tab = 1 })
            }
            Spacer(Modifier.height(10.dp))
            if (tab == 0) {
                GlassPanel(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp).height(44.dp),
                    shape = RoundedCornerShape(22.dp), elevation = 6.dp
                ) {
                    Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Search, null, modifier = Modifier.size(15.dp),
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
                                    Text("Search history", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f))
                                }
                                inner()
                            }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                if (filtered.isEmpty()) {
                    EmptyState(Icons.Rounded.History, "No history", "Pages you visit will appear here", Modifier.weight(1f))
                } else {
                    LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
                        grouped.forEach { (day, entries) ->
                            item(key = "header-$day") {
                                Text(day, style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                    modifier = Modifier.padding(vertical = 8.dp))
                            }
                            items(entries, key = { it.id }) { entry ->
                                Row(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                        .clickable { vm.navigate(entry.url); vm.closeScreen() }
                                        .padding(horizontal = 8.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FaviconImage(entry.url, size = 28.dp, corner = 8.dp)
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(entry.title.ifBlank { UrlTools.displayUrl(entry.url) },
                                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.bodyLarge)
                                        Text("${timeLabel(entry.visitedAt)} · ${UrlTools.displayUrl(entry.url)}",
                                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
                                    }
                                    Icon(Icons.Rounded.Delete, "Delete",
                                        modifier = Modifier.size(16.dp).clickable { vm.deleteHistory(entry.id) },
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                                }
                            }
                        }
                        item { Spacer(Modifier.height(40.dp)) }
                    }
                }
            } else if (closed.isEmpty()) {
                EmptyState(Icons.Rounded.History, "Nothing closed recently",
                    "Tabs you close can be reopened from here", Modifier.fillMaxSize())
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
                    items(closed, key = { it.id }) { entry ->
                        GlassPanel(Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { vm.restoreClosed(entry); vm.closeScreen() },
                            shape = RoundedCornerShape(16.dp), elevation = 4.dp) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                FaviconImage(entry.url, size = 28.dp, corner = 8.dp)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(entry.title.ifBlank { UrlTools.displayUrl(entry.url) },
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(UrlTools.displayUrl(entry.url), maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
                                }
                                Icon(Icons.Rounded.History, null, modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReadingListScreen(vm: NovaViewModel) {
    val items by vm.readingFlow.collectAsStateWithLifecycle()
    NovaScreen(title = "Reading list", onClose = { vm.closeScreen() }) {
        if (items.isEmpty()) {
            EmptyState(Icons.Rounded.MenuBook, "Your reading list is empty",
                "Use the browser menu to save an article for later", Modifier.fillMaxSize())
        } else {
            LazyColumn(Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
                items(items, key = { it.id }) { item ->
                    GlassPanel(Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        shape = RoundedCornerShape(18.dp), elevation = 6.dp) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            FaviconImage(item.url, size = 34.dp, corner = 10.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f).clickable { vm.navigate(item.url); vm.closeScreen() }) {
                                Text(item.title.ifBlank { UrlTools.displayUrl(item.url) },
                                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.titleMedium)
                                Text("${dayLabel(item.addedAt)} · ${UrlTools.displayUrl(item.url)}",
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
                            }
                            Icon(Icons.Rounded.Close, "Remove",
                                modifier = Modifier.size(16.dp).clickable { vm.removeReadingItem(item.id) },
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }
    }
}

private fun dayLabel(timestamp: Long): String {
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = timestamp }
    return when {
        now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR) -> "Today"
        now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) - then.get(Calendar.DAY_OF_YEAR) == 1 -> "Yesterday"
        else -> SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

private fun timeLabel(timestamp: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
