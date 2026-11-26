package com.novabrowse.browser.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddLink
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.FindInPage
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novabrowse.browser.ui.components.HairLine

private data class MenuEntry(
    val icon: ImageVector,
    val label: String,
    val destructive: Boolean = false,
    val checked: Boolean = false,
    val onClick: () -> Unit
)

/**
 * Chrome's overflow menu: a compact vertical list anchored to the toolbar's
 * three-dot button, opening with a short scale + fade from the top-right corner.
 */
@Composable
fun BrowserMenuSheet(vm: NovaViewModel) {
    val tab = vm.activeTab
    val bookmarked = tab?.url?.let { vm.isBookmarked(it) } == true
    val extensionItems = vm.extensionMenuItems
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.32f))
                .clickable { vm.closeMenu() }
        )
        AnimatedVisibility(
            visible = shown,
            enter = fadeIn(tween(120)) + scaleIn(
                initialScale = 0.92f,
                transformOrigin = TransformOrigin(1f, 0f),
                animationSpec = tween(160, easing = FastOutSlowInEasing)
            ),
            exit = fadeOut(tween(90)),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Column(
                Modifier
                    .statusBarsPadding()
                    .padding(top = 6.dp, end = 8.dp)
                    .width(300.dp)
                    .fillMaxHeight(0.82f)
                    .shadow(8.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                MenuItem(MenuEntry(Icons.Rounded.Add, "New tab") { vm.newTab() })
                MenuItem(MenuEntry(Icons.Rounded.VisibilityOff, "New private tab") { vm.newPrivateTab() })
                MenuDivider()
                MenuItem(MenuEntry(Icons.Rounded.History, "History") { vm.openScreen(NovaScreen.HISTORY) })
                MenuItem(MenuEntry(Icons.Rounded.DeleteSweep, "Delete browsing data") {
                    vm.closeMenu()
                    vm.openSettings(SettingsPage.PRIVACY)
                })
                MenuItem(MenuEntry(Icons.Rounded.Download, "Downloads") { vm.openScreen(NovaScreen.DOWNLOADS) })
                MenuItem(MenuEntry(Icons.Rounded.Bookmark, "Bookmarks") { vm.openScreen(NovaScreen.BOOKMARKS) })
                MenuItem(MenuEntry(Icons.Rounded.MenuBook, "Reading list") { vm.openScreen(NovaScreen.READING_LIST) })
                MenuItem(MenuEntry(Icons.Rounded.Extension, "Extensions") { vm.openScreen(NovaScreen.EXTENSIONS) })
                MenuDivider()
                MenuItem(MenuEntry(Icons.Rounded.FindInPage, "Find in page") {
                    vm.closeMenu(); vm.openFind()
                })
                MenuItem(MenuEntry(Icons.Rounded.DesktopWindows, "Desktop site", checked = tab?.desktopMode == true) {
                    vm.toggleDesktopMode()
                })
                MenuItem(MenuEntry(Icons.Rounded.Article, "Reader mode", checked = tab?.isReaderMode == true) {
                    vm.toggleReaderMode(); vm.closeMenu()
                })
                MenuDivider()
                MenuItem(MenuEntry(
                    if (bookmarked) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    if (bookmarked) "Edit bookmark" else "Bookmark this page"
                ) { vm.toggleBookmark() })
                MenuItem(MenuEntry(Icons.Rounded.AddLink, "Add shortcut") {
                    vm.openShortcutDraft(null); vm.closeMenu()
                })
                MenuItem(MenuEntry(Icons.Rounded.MenuBook, "Add to reading list") {
                    vm.addToReadingList(); vm.closeMenu()
                })
                MenuItem(MenuEntry(Icons.Rounded.Share, "Share page") {
                    vm.shareCurrentPage(); vm.closeMenu()
                })
                MenuItem(MenuEntry(Icons.Rounded.ContentCopy, "Copy link") {
                    vm.copyCurrentUrl(); vm.closeMenu()
                })
                MenuItem(MenuEntry(Icons.Rounded.PhotoCamera, "Screenshot") {
                    vm.captureScreenshot(); vm.closeMenu()
                })
                MenuItem(MenuEntry(Icons.Rounded.Translate, "Translate page") {
                    vm.translatePage(); vm.closeMenu()
                })
                MenuItem(MenuEntry(Icons.Rounded.QrCodeScanner, "Scan QR code") { vm.scanQr() })
                MenuDivider()
                MenuItem(MenuEntry(Icons.Rounded.Settings, "Settings") { vm.openSettings() })
                MenuItem(MenuEntry(Icons.Rounded.Close, "Close tab", destructive = true) {
                    vm.closeActiveTab()
                    vm.closeMenu()
                })

                if (extensionItems.isNotEmpty()) {
                    MenuDivider()
                    Text(
                        "EXTENSIONS",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, letterSpacing = 0.8.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 4.dp)
                    )
                    extensionItems.forEach { item ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { vm.clickExtensionMenuItem(item) }
                                .padding(horizontal = 16.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Extension, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    item.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    item.extensionName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuItem(entry: MenuEntry) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = entry.onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            entry.icon,
            contentDescription = null,
            tint = if (entry.destructive) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            entry.label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (entry.destructive) FontWeight.Medium else FontWeight.Normal
            ),
            color = if (entry.destructive) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (entry.checked) {
            Icon(
                Icons.Rounded.Check, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun MenuDivider() {
    Box(Modifier.padding(vertical = 6.dp)) { HairLine() }
}
