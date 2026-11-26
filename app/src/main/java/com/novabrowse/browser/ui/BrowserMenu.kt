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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddLink
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.Bookmark
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novabrowse.browser.ui.components.GlassPanel
import com.novabrowse.browser.ui.components.HairLine
import com.novabrowse.browser.ui.theme.LocalNovaStyle

private data class MenuAction(
    val icon: ImageVector,
    val label: String,
    val destructive: Boolean = false,
    val onClick: () -> Unit
)

@Composable
fun BrowserMenuSheet(vm: NovaViewModel) {
    val tab = vm.activeTab
    val style = LocalNovaStyle.current
    val bookmarked = tab?.url?.let { vm.isBookmarked(it) } == true
    val extensionItems = vm.extensionMenuItems

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable { vm.closeMenu() }
        )
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(10.dp)
        ) {
            GlassPanel(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(30.dp),
                strong = true,
                elevation = 28.dp
            ) {
                Column(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (tab?.isStartPage != false) "NovaBrowse" else tab.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (tab != null && !tab.isStartPage) {
                                Text(
                                    "${tab.domain()} · ${if (tab.isSecure) "secure" else "not secure"}",
                                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                        Text(
                            "BROWSE BEYOND.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    val actions = buildList {
                        add(MenuAction(Icons.Rounded.Add, "New tab") { vm.newTab() })
                        add(MenuAction(Icons.Rounded.VisibilityOff, "Private tab") { vm.newPrivateTab() })
                        add(MenuAction(Icons.Rounded.Bookmark, "Bookmarks") {
                            vm.openScreen(NovaScreen.BOOKMARKS)
                        })
                        add(MenuAction(Icons.Rounded.History, "History") { vm.openScreen(NovaScreen.HISTORY) })
                        add(MenuAction(Icons.Rounded.Download, "Downloads") { vm.openScreen(NovaScreen.DOWNLOADS) })
                        add(MenuAction(Icons.Rounded.MenuBook, "Reading list") {
                            vm.openScreen(NovaScreen.READING_LIST)
                        })
                        add(MenuAction(Icons.Rounded.Extension, "Extensions") {
                            vm.openScreen(NovaScreen.EXTENSIONS)
                        })
                        add(MenuAction(Icons.Rounded.FindInPage, "Find on page") {
                            vm.closeMenu(); vm.openFind()
                        })
                        add(MenuAction(Icons.Rounded.DesktopWindows, "Desktop site") {
                            vm.toggleDesktopMode()
                        })
                        add(MenuAction(Icons.Rounded.Article, "Reader mode") {
                            vm.toggleReaderMode(); vm.closeMenu()
                        })
                        add(MenuAction(
                            if (bookmarked) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                            if (bookmarked) "Remove bookmark" else "Add bookmark"
                        ) { vm.toggleBookmark() })
                        add(MenuAction(Icons.Rounded.AddLink, "Add shortcut") {
                            vm.openShortcutDraft(null); vm.closeMenu()
                        })
                        add(MenuAction(Icons.Rounded.MenuBook, "Save for later") {
                            vm.addToReadingList(); vm.closeMenu()
                        })
                        add(MenuAction(Icons.Rounded.Share, "Share page") {
                            vm.shareCurrentPage(); vm.closeMenu()
                        })
                        add(MenuAction(Icons.Rounded.ContentCopy, "Copy link") {
                            vm.copyCurrentUrl(); vm.closeMenu()
                        })
                        add(MenuAction(Icons.Rounded.PhotoCamera, "Screenshot") {
                            vm.captureScreenshot(); vm.closeMenu()
                        })
                        add(MenuAction(Icons.Rounded.Translate, "Translate page") {
                            vm.translatePage(); vm.closeMenu()
                        })
                        add(MenuAction(Icons.Rounded.QrCodeScanner, "Scan QR code") {
                            vm.scanQr()
                        })
                        add(MenuAction(Icons.Rounded.Settings, "Settings") { vm.openSettings() })
                        add(MenuAction(Icons.Rounded.Close, "Close tab", destructive = true) {
                            vm.closeActiveTab()
                            vm.closeMenu()
                        })
                    }
                    val rows = actions.chunked(4)
                    Column(Modifier.fillMaxWidth()) {
                        rows.forEach { row ->
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                row.forEach { action ->
                                    MenuTile(action, Modifier.weight(1f))
                                }
                                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }

                    if (extensionItems.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        HairLine(Modifier.padding(horizontal = 18.dp))
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "EXTENSION ACTIONS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        extensionItems.forEach { item ->
                            Row(
                                Modifier.fillMaxWidth()
                                    .clickable { vm.clickExtensionMenuItem(item) }
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier.size(26.dp).clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Extension, null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(item.title, style = MaterialTheme.typography.bodyLarge)
                                    Text(item.extensionName, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuTile(action: MenuAction, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = action.onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                action.icon,
                contentDescription = action.label,
                tint = if (action.destructive) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(
            action.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
}
