@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.novabrowse.browser.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Tab
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.VisibilityOff
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.novabrowse.browser.browser.BrowserTab
import com.novabrowse.browser.browser.UrlTools
import com.novabrowse.browser.ui.components.FaviconImage
import com.novabrowse.browser.ui.components.GlassIconButton
import com.novabrowse.browser.ui.components.GlassPanel
import com.novabrowse.browser.ui.components.NovaChip
import com.novabrowse.browser.ui.components.NovaDialog
import com.novabrowse.browser.ui.components.NovaScreen
import com.novabrowse.browser.ui.theme.LocalNovaStyle

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TabSwitcher(vm: NovaViewModel) {
    var compact by remember { mutableStateOf(false) }
    var selection by remember { mutableStateOf<Set<String>>(emptySet()) }
    var groupDialog by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }
    vm.tabsVersion.value
    val privateTabs = com.novabrowse.browser.Nova.tabs.privateTabs
    val list = if (vm.isPrivate) privateTabs else vm.tabs
    val closed by vm.closedFlow.collectAsStateWithLifecycle()
    val style = LocalNovaStyle.current

    NovaScreen(
        title = if (vm.isPrivate) "Private tabs" else "Tabs",
        onClose = { vm.closeTabSwitcher() },
        actions = {
            GlassIconButton(onClick = { compact = !compact }, size = 40.dp) {
                Icon(if (compact) Icons.Rounded.GridView else Icons.Rounded.ViewAgenda,
                    "Layout", modifier = Modifier.size(18.dp))
            }
        }
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NovaChip(
                    "Standard", selected = !vm.isPrivate,
                    onClick = { vm.togglePrivateBrowsing(false) }
                )
                NovaChip(
                    "Private", selected = vm.isPrivate,
                    onClick = { vm.togglePrivateBrowsing(true) },
                    leading = Icons.Rounded.VisibilityOff
                )
                Spacer(Modifier.weight(1f))
                if (selection.isNotEmpty()) {
                    Text("${selection.size} selected", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(10.dp))

            if (list.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.Tab, null, modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                        Spacer(Modifier.height(10.dp))
                        Text(if (vm.isPrivate) "No private tabs" else "No open tabs",
                            style = MaterialTheme.typography.titleMedium)
                        Text("Close private tabs to erase the private session.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
            } else if (compact) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    gridItems(list, key = { it.id }) { tab ->
                        CompactTabRow(
                            tab = tab,
                            selected = tab.id == vm.activeTab?.id,
                            isSelected = tab.id in selection,
                            onClick = {
                                if (selection.isEmpty()) vm.selectTab(tab)
                                else selection = toggle(selection, tab.id)
                            },
                            onClose = { vm.closeTab(tab) },
                            onLongPress = { selection = toggle(selection, tab.id) }
                        )
                    }
                    item { Spacer(Modifier.height(120.dp)) }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    gridItems(list, key = { it.id }) { tab ->
                        TabCard(
                            tab = tab,
                            active = tab.id == vm.activeTab?.id,
                            selected = tab.id in selection,
                            onClick = {
                                if (selection.isEmpty()) vm.selectTab(tab)
                                else selection = toggle(selection, tab.id)
                            },
                            onLongPress = { selection = toggle(selection, tab.id) },
                            onClose = { vm.closeTab(tab) },
                            onDuplicate = { vm.duplicateTab(tab) }
                        )
                    }
                    item { Spacer(Modifier.height(140.dp)) }
                }
            }

            if (closed.isNotEmpty()) {
                Column(Modifier.padding(horizontal = 18.dp)) {
                    Text("Recently closed", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(closed.take(8), key = { it.id }) { entry ->
                            GlassPanel(
                                modifier = Modifier.width(150.dp).clip(RoundedCornerShape(14.dp))
                                    .combinedClickable(onClick = { vm.restoreClosed(entry) }),
                                shape = RoundedCornerShape(14.dp),
                                elevation = 4.dp
                            ) {
                                Row(Modifier.padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
                                    FaviconImage(entry.url, size = 20.dp, corner = 6.dp)
                                    Spacer(Modifier.width(7.dp))
                                    Text(entry.title.ifBlank { UrlTools.displayUrl(entry.url) },
                                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TabActionButton(Icons.Rounded.Add, "New tab") {
                    if (vm.isPrivate) vm.newPrivateTab() else vm.newTab()
                }
                if (selection.size > 1) {
                    TabActionButton(Icons.Rounded.Folder, "Group") { groupDialog = true }
                }
                Spacer(Modifier.weight(1f))
                if (selection.isNotEmpty()) {
                    TabActionButton(Icons.Rounded.Check, "Done") { selection = emptySet() }
                }
                TabActionButton(Icons.Rounded.ContentCopy, "Duplicate") {
                    list.firstOrNull { it.id in selection }?.let {
                        vm.duplicateTab(it)
                        selection = emptySet()
                    }
                }
                TabActionButton(Icons.Rounded.DeleteSweep, "Close all") {
                    vm.closeAllTabs()
                    selection = emptySet()
                }
            }
        }
    }

    if (groupDialog) {
        NovaDialog(onDismiss = { groupDialog = false }) {
            Text("New tab group", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            GlassPanel(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), elevation = 2.dp) {
                BasicTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp),
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    decorationBox = { inner ->
                        if (groupName.isEmpty()) {
                            Text("Group name", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        }
                        inner()
                    }
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text("Cancel", style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.clickable { groupDialog = false }.padding(10.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Create",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .clickable {
                            if (groupName.isNotBlank()) {
                                vm.createGroup(groupName.trim(), selection.toList())
                                groupName = ""
                                selection = emptySet()
                                groupDialog = false
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}

private fun toggle(set: Set<String>, id: String): Set<String> =
    if (id in set) set - id else set + id

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TabCard(
    tab: BrowserTab,
    active: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onClose: () -> Unit,
    onDuplicate: () -> Unit
) {
    val style = LocalNovaStyle.current
    val borderColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, label = "tabBorder"
    )
    GlassPanel(
        modifier = Modifier
            .height(190.dp)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
        shape = RoundedCornerShape(12.dp),
        tint = if (active) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        strong = active
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                val preview = tab.preview
                if (preview != null) {
                    AsyncImage(
                        model = preview,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        FaviconImage(tab.url.ifBlank { "https://novabrowse.app" }, size = 46.dp, corner = 14.dp)
                    }
                }
                if (tab.isPrivate) {
                    Box(
                        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f))
                    )
                }
                GlassIconButton(
                    onClick = onClose,
                    size = 28.dp,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
                ) { Icon(Icons.Rounded.Close, "Close tab", modifier = Modifier.size(14.dp)) }
                if (selected) {
                    Box(
                        Modifier.align(Alignment.TopStart).padding(8.dp)
                            .size(22.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(13.dp)) }
                }
            }
            Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                FaviconImage(tab.url.ifBlank { "https://novabrowse.app" }, size = 22.dp, corner = 7.dp)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(tab.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                    Text(
                        if (tab.isStartPage) "Start page" else UrlTools.displayUrl(tab.url),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                }
                if (active) {
                    Box(Modifier.size(7.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompactTabRow(
    tab: BrowserTab,
    selected: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit,
    onLongPress: () -> Unit
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
        shape = RoundedCornerShape(10.dp),
        tint = if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        strong = selected
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            FaviconImage(tab.url.ifBlank { "https://novabrowse.app" }, size = 28.dp, corner = 9.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(tab.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge)
                Text(
                    if (tab.isStartPage) "Start page" else UrlTools.displayUrl(tab.url),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                )
            }
            if (isSelected) {
                Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
            }
            Icon(
                Icons.Rounded.Close, "Close tab",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp).clickable(onClick = onClose)
            )
        }
    }
}

@Composable
private fun TabActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            .combinedClickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, label, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}
