package com.novabrowse.browser.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novabrowse.browser.browser.UrlTools
import com.novabrowse.browser.ui.components.GlassPanel
import com.novabrowse.browser.ui.components.NovaDialog
import com.novabrowse.browser.ui.theme.LocalNovaStyle
import kotlinx.coroutines.delay

@Composable
fun NovaRoot(vm: NovaViewModel, onExit: () -> Unit) {
    BackHandler {
        if (!vm.onBack()) onExit()
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        BrowserScreen(vm)

        AnimatedVisibility(
            visible = vm.tabSwitcherOpen,
            enter = fadeIn(tween(140)) + slideInVertically(
                animationSpec = tween(200, easing = FastOutSlowInEasing)
            ) { it / 18 },
            exit = fadeOut(tween(120)) + slideOutVertically(
                animationSpec = tween(160, easing = FastOutSlowInEasing)
            ) { it / 24 }
        ) {
            OverlaySurface { TabSwitcher(vm) }
        }

        AnimatedVisibility(
            visible = vm.searchOpen,
            enter = fadeIn(tween(120)) + slideInVertically(
                animationSpec = tween(180, easing = FastOutSlowInEasing)
            ) { -it / 22 },
            exit = fadeOut(tween(110))
        ) {
            OverlaySurface { SearchOverlay(vm) }
        }

        if (vm.menuOpen) BrowserMenuSheet(vm)

        AnimatedVisibility(
            visible = vm.screen != NovaScreen.NONE,
            enter = fadeIn(tween(140)) + slideInHorizontally(
                animationSpec = tween(220, easing = FastOutSlowInEasing)
            ) { it / 5 },
            exit = fadeOut(tween(120)) + slideOutHorizontally(
                animationSpec = tween(180, easing = FastOutSlowInEasing)
            ) { it / 8 }
        ) {
            OverlaySurface {
                when (vm.screen) {
                    NovaScreen.SETTINGS -> SettingsScreen(vm)
                    NovaScreen.BOOKMARKS -> BookmarksScreen(vm)
                    NovaScreen.DOWNLOADS -> DownloadsScreen(vm)
                    NovaScreen.HISTORY -> HistoryScreen(vm)
                    NovaScreen.READING_LIST -> ReadingListScreen(vm)
                    NovaScreen.EXTENSIONS -> ExtensionsScreen(vm)
                    NovaScreen.EXTENSION_STORE -> ExtensionStoreScreen(vm)
                    NovaScreen.ABOUT -> com.novabrowse.browser.ui.components.NovaScreen(
                        title = "About NovaBrowse",
                        onClose = { vm.closeScreen() }
                    ) { AboutContent(vm) }
                    NovaScreen.LICENSES -> LicensesScreen(vm)
                    NovaScreen.ACCOUNT -> AccountScreen(vm)
                    NovaScreen.QR -> QrScannerScreen(vm)
                    NovaScreen.NONE -> Unit
                }
            }
        }

        if (vm.bookmarkDraft != null) BookmarkEditorDialog(vm)
        if (vm.shortcutDraft != null) ShortcutEditorDialog(vm)
        if (vm.customEngineDraft) CustomEngineDialog(vm)
        if (vm.installUrlDialog) InstallFromUrlDialog(vm)
        if (vm.storeLinkDialog) StoreLinkDialog(vm)
        if (vm.pendingExtension != null) ExtensionInstallDialog(vm)
        if (vm.extensionPopup != null) ExtensionPopupDialog(vm)

        ToastHost(vm)
        BusyHost(vm)
    }
}

@Composable
private fun OverlaySurface(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        OverlayBackdrop()
        content()
    }
}

@Composable
private fun OverlayBackdrop() {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
}

@Composable
private fun ToastHost(vm: NovaViewModel) {
    val message = vm.toast
    LaunchedEffect(message) {
        if (message != null) {
            delay(2600)
            vm.consumeToast()
        }
    }
    AnimatedVisibility(
        visible = message != null,
        modifier = Modifier.fillMaxSize(),
        enter = fadeIn(tween(150)) + slideInVertically(tween(200)) { it },
        exit = fadeOut(tween(150))
    ) {
        Box(Modifier.fillMaxSize().navigationBarsPadding().padding(bottom = 96.dp), contentAlignment = Alignment.BottomCenter) {
            GlassPanel(shape = RoundedCornerShape(8.dp), strong = true, elevation = 18.dp, modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(
                    message.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 11.dp)
                )
            }
        }
    }
}

@Composable
private fun BusyHost(vm: NovaViewModel) {
    val busy = vm.busyMessage
    if (busy == null) return
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)).clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        GlassPanel(shape = RoundedCornerShape(22.dp), strong = true, elevation = 24.dp) {
            Row(Modifier.padding(horizontal = 22.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(14.dp))
                Text(busy, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun ShortcutEditorDialog(vm: NovaViewModel) {
    val shortcut = vm.shortcutDraft ?: return
    var title by remember(shortcut.id) { mutableStateOf(shortcut.title) }
    var url by remember(shortcut.id) { mutableStateOf(shortcut.url) }
    val existing = vm.shortcutsFlow.value.any { it.id == shortcut.id }
    NovaDialog(onDismiss = { vm.shortcutDraft = null }) {
        Text(if (existing) "Edit shortcut" else "Add shortcut", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Field("Name", title) { title = it }
        Spacer(Modifier.height(8.dp))
        Field("Web address", url) { url = it }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (existing) {
                Text("Remove", color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.clip(CircleShape)
                        .clickable { vm.removeShortcut(shortcut); vm.shortcutDraft = null }
                        .padding(horizontal = 12.dp, vertical = 10.dp))
            }
            Spacer(Modifier.weight(1f))
            Text("Cancel", style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.clip(CircleShape).clickable { vm.shortcutDraft = null }
                    .padding(horizontal = 12.dp, vertical = 10.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                "Save", color = Color.White, style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primary)
                    .clickable {
                        if (url.isNotBlank()) {
                            vm.saveShortcut(shortcut.copy(title = title, url = url))
                        } else {
                            vm.toast("Add a web address first")
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun CustomEngineDialog(vm: NovaViewModel) {
    var name by remember { mutableStateOf("") }
    var template by remember { mutableStateOf("") }
    NovaDialog(onDismiss = { vm.customEngineDraft = false }) {
        Text("Add search engine", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text("Use {query} where the search terms should go.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Spacer(Modifier.height(12.dp))
        Field("Name", name) { name = it }
        Spacer(Modifier.height(8.dp))
        Field("Search URL", template) { template = it }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text("Cancel", style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.clip(CircleShape).clickable { vm.customEngineDraft = false }
                    .padding(horizontal = 12.dp, vertical = 10.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                "Add", color = Color.White, style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primary)
                    .clickable {
                        if (name.isNotBlank() && template.contains("http")) {
                            vm.addCustomEngine(name.trim(), template.trim())
                        } else {
                            vm.toast("Name and an https URL are required")
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun InstallFromUrlDialog(vm: NovaViewModel) {
    var url by remember { mutableStateOf("") }
    NovaDialog(onDismiss = { vm.closeInstallUrlDialog() }) {
        Text("Install extension from URL", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            "Paste a direct link to an unpacked extension ZIP (for example a GitHub release artefact). " +
                "NovaBrowse downloads and validates the manifest before installing.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(12.dp))
        Field("https://…/extension.zip", url) { url = it }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text("Cancel", style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.clip(CircleShape).clickable { vm.closeInstallUrlDialog() }
                    .padding(horizontal = 12.dp, vertical = 10.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                "Download", color = Color.White, style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primary)
                    .clickable {
                        if (url.startsWith("http")) vm.installExtensionFromUrl(url.trim())
                        else vm.toast("Enter an https link to a ZIP package")
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    }
}
