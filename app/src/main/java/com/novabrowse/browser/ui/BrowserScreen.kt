package com.novabrowse.browser.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.view.View
import android.view.ViewGroup
import android.webkit.PermissionRequest
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FindInPage
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.novabrowse.browser.browser.BrowserTab
import com.novabrowse.browser.browser.JsDialog
import com.novabrowse.browser.browser.UrlTools
import com.novabrowse.browser.ui.components.FaviconImage
import com.novabrowse.browser.ui.components.GlassIconButton
import com.novabrowse.browser.ui.components.GlassPanel
import com.novabrowse.browser.ui.components.NovaDialog

@Composable
fun BrowserScreen(vm: NovaViewModel) {
    val settings = vm.settings
    val tab = vm.activeTab
    vm.tabsVersion.value
    val showChrome = tab?.fullscreenView == null

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (showChrome && !settings.toolbarBottom) {
            ChromeHeader(vm)
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            WebViewHost(tab, visible = tab != null && !tab.isStartPage)
            if (tab == null || tab.isStartPage) {
                HomeScreen(vm)
            }
            if (tab != null && !tab.isStartPage) {
                LoadingBar(tab)
                if (tab.errorText != null) {
                    PageErrorOverlay(tab) { tab.reload() }
                }
                if (vm.findVisible) {
                    FindBar(vm)
                }
                if (tab.blockedTrackers > 0 && settings.trackingProtection) {
                    TrackerBadge(tab.blockedTrackers)
                }
            }
            if (tab?.fullscreenView != null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
                    GlassIconButton(
                        onClick = {
                            tab.webView.evaluateJavascript("document.exitFullscreen && document.exitFullscreen()", null)
                        },
                        modifier = Modifier.statusBarsPadding().padding(12.dp),
                        size = 40.dp,
                        tint = MaterialTheme.colorScheme.onSurface
                    ) { Icon(Icons.Rounded.Close, "Exit fullscreen", modifier = Modifier.size(20.dp)) }
                }
            }
        }

        if (showChrome && settings.toolbarBottom) {
            ChromeToolbar(vm, bottom = true)
        }
    }

    tab?.pendingPermission?.let { PermissionDialog(vm, tab) }
    tab?.pendingGeo?.let { geo ->
        NovaDialog(onDismiss = {
            geo.callback.invoke(geo.origin, false, false)
            tab.pendingGeo = null
        }) {
            Text("Allow location?", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                "${geo.origin.ifBlank { "This website" }} wants to know your location.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))
            DialogActions(
                confirmLabel = "Allow",
                onConfirm = {
                    geo.callback.invoke(geo.origin, true, true)
                    vm.toast("Location shared with ${geo.origin}")
                    tab.pendingGeo = null
                },
                onCancel = {
                    geo.callback.invoke(geo.origin, false, true)
                    tab.pendingGeo = null
                }
            )
        }
    }
    tab?.jsDialog?.let { dialog ->
        JsDialogHost(dialog) { tab.jsDialog = null }
    }
}

@Composable
private fun ChromeHeader(vm: NovaViewModel) {
    Column(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        ChromeTabStrip(vm)
        ChromeToolbar(vm, bottom = false)
    }
}

/** Chrome's horizontal tab strip: rounded chips, close buttons, trailing "+". */
@Composable
private fun ChromeTabStrip(vm: NovaViewModel) {
    val tabs = vm.tabs
    if (tabs.size < 2) return
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().height(42.dp).padding(start = 6.dp, end = 4.dp, top = 4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(tabs, key = { it.id }) { tab ->
                val selected = tab.id == vm.activeTab?.id
                Row(
                    Modifier
                        .width(172.dp)
                        .height(38.dp)
                        .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                        .background(if (selected) scheme.surfaceVariant else Color.Transparent)
                        .clickable { vm.selectTab(tab) }
                        .padding(start = 10.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FaviconImage(tab.url.ifBlank { "https://novabrowse.app" }, size = 16.dp, corner = 4.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (tab.isStartPage) "New tab" else tab.title.ifBlank { UrlTools.displayUrl(tab.url) },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                        ),
                        color = if (selected) scheme.onSurface else scheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { vm.closeTab(tab) }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Rounded.Close, "Close tab",
                            tint = scheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
        IconButton(onClick = { vm.newTab() }, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Rounded.Add, "New tab", tint = scheme.onSurface, modifier = Modifier.size(20.dp))
        }
    }
}

/** Chrome's navigation row: home, back, forward, reload, omnibox, tab count, overflow. */
@Composable
private fun ChromeToolbar(vm: NovaViewModel, bottom: Boolean) {
    val tab = vm.activeTab
    val scheme = MaterialTheme.colorScheme
    val modifier = if (bottom) {
        Modifier.fillMaxWidth().navigationBarsPadding().background(scheme.surface)
    } else {
        Modifier.fillMaxWidth().background(scheme.surface)
    }
    Column(modifier) {
        if (bottom) Box(Modifier.fillMaxWidth().height(1.dp).background(scheme.outlineVariant))
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(start = 4.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { vm.showStartPage() }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Rounded.Home, "Home", tint = scheme.onSurface, modifier = Modifier.size(21.dp))
            }
            IconButton(
                onClick = { tab?.goBack() },
                enabled = tab?.canGoBack == true && tab.isStartPage.not(),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Rounded.ArrowBack, "Back",
                    tint = if (tab?.canGoBack == true && tab.isStartPage.not()) scheme.onSurface
                    else scheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(21.dp)
                )
            }
            IconButton(
                onClick = { tab?.goForward() },
                enabled = tab?.canGoForward == true,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Rounded.ArrowForward, "Forward",
                    tint = if (tab?.canGoForward == true) scheme.onSurface
                    else scheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(21.dp)
                )
            }
            IconButton(
                onClick = { if (tab?.isLoading == true) tab.stop() else tab?.reload() },
                enabled = tab != null && !tab.isStartPage,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Rounded.Refresh, "Reload",
                    tint = if (tab != null && !tab.isStartPage) scheme.onSurface
                    else scheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }

            AddressPill(vm, tab, Modifier.weight(1f))

            Spacer(Modifier.width(6.dp))
            Box(
                Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.5.dp, scheme.onSurfaceVariant, RoundedCornerShape(6.dp))
                    .clickable { vm.openTabSwitcher() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    vm.tabs.size.toString(),
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                    color = scheme.onSurface
                )
            }
            IconButton(onClick = { vm.openMenu() }, modifier = Modifier.size(42.dp)) {
                Icon(Icons.Rounded.MoreVert, "Menu", tint = scheme.onSurface, modifier = Modifier.size(21.dp))
            }
        }
        if (!bottom) Box(Modifier.fillMaxWidth().height(1.dp).background(scheme.outlineVariant))
    }
}

@Composable
private fun AddressPill(vm: NovaViewModel, tab: BrowserTab?, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(scheme.surfaceVariant)
            .clickable { vm.openSearch() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon = when {
            tab?.isPrivate == true -> Icons.Rounded.VisibilityOff
            tab == null || tab.isStartPage -> Icons.Rounded.Search
            tab.isSecure -> Icons.Rounded.Lock
            else -> Icons.Rounded.Warning
        }
        Icon(
            icon,
            contentDescription = null,
            tint = when {
                tab == null || tab.isStartPage -> scheme.onSurfaceVariant
                tab.isPrivate -> scheme.tertiary
                tab.isSecure -> scheme.onSurfaceVariant
                else -> scheme.error
            },
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = when {
                tab == null || tab.isStartPage -> "Search ${vm.currentEngine.name} or type URL"
                else -> UrlTools.displayUrl(tab.url).ifBlank { tab.title }
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(fontSize = 14.sp),
            color = if (tab == null || tab.isStartPage) scheme.onSurfaceVariant else scheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun WebViewHost(tab: BrowserTab?, visible: Boolean) {
    val background = MaterialTheme.colorScheme.background
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            FrameLayout(context).apply { setBackgroundColor(background.toArgb()) }
        },
        update = { container ->
            container.setBackgroundColor(background.toArgb())
            container.visibility = if (visible) View.VISIBLE else View.GONE
            val webView = tab?.webView ?: return@AndroidView
            if (container.childCount == 0 || container.getChildAt(0) !== webView) {
                (webView.parent as? ViewGroup)?.removeView(webView)
                container.removeAllViews()
                container.addView(
                    webView,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
            }
            tab.fullscreenView?.let { fullscreen ->
                if (fullscreen.parent == null) {
                    container.addView(
                        fullscreen,
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    )
                }
            } ?: run {
                if (container.childCount > 1) {
                    container.removeViews(1, container.childCount - 1)
                }
            }
        }
    )
}

@Composable
private fun LoadingBar(tab: BrowserTab) {
    val target = (tab.progress / 100f).coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(180, easing = LinearEasing),
        label = "progress"
    )
    if (tab.isLoading && animated < 1f) {
        Box(Modifier.fillMaxWidth().height(2.dp)) {
            Box(
                Modifier
                    .fillMaxWidth(animated)
                    .height(2.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun TrackerBadge(count: Int) {
    Box(Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.BottomStart) {
        Row(
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.Security, null,
                modifier = Modifier.size(13.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "$count blocked",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PageErrorOverlay(tab: BrowserTab, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Rounded.Warning, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(44.dp)
            )
            Spacer(Modifier.height(14.dp))
            Text("Can't open this page", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(
                tab.errorText ?: "Something went wrong.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            Row {
                NovaPillButton("Try again", onRetry)
                Spacer(Modifier.width(10.dp))
                NovaPillButton("Home", { tab.showStartPage() }, secondary = true)
            }
        }
    }
}

@Composable
private fun NovaPillButton(label: String, onClick: () -> Unit, secondary: Boolean = false) {
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (secondary) Color.Transparent else MaterialTheme.colorScheme.primary
            )
            .then(
                if (secondary) Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 9.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (secondary) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
private fun FindBar(vm: NovaViewModel) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            Modifier
                .weight(1f)
                .height(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.FindInPage, null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = vm.findQuery,
                onValueChange = { vm.findInPage(it) },
                singleLine = true,
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (vm.findQuery.isEmpty()) {
                        Text(
                            "Find in page",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                    inner()
                }
            )
            if (vm.findMatches > 0) {
                Text(
                    vm.findMatches.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(onClick = { vm.closeFind() }, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Rounded.Close, "Close find", modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun PermissionDialog(vm: NovaViewModel, tab: BrowserTab) {
    val permission = tab.pendingPermission ?: return
    val context = LocalContext.current
    val needed = remember(permission) {
        buildList {
            if (permission.resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) add(Manifest.permission.CAMERA)
            if (permission.resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) add(Manifest.permission.RECORD_AUDIO)
        }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val granted = result.values.all { it } || result.isEmpty()
        if (granted) {
            permission.request.grant(permission.resources)
            vm.toast("Permission granted to ${permission.origin}")
        } else {
            permission.request.deny()
            vm.toast("Permission denied")
        }
        tab.pendingPermission = null
    }
    val label = when {
        permission.resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE) -> "camera"
        permission.resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE) -> "microphone"
        else -> "device"
    }
    NovaDialog(onDismiss = { permission.request.deny(); tab.pendingPermission = null }) {
        Text("Allow ${permission.origin.ifBlank { "this site" }} to use your $label?",
            style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "NovaBrowse only passes this on after Android also grants the permission. You can revoke it any time in Settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))
        DialogActions(
            confirmLabel = "Allow",
            onConfirm = {
                val missing = needed.filter {
                    ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                }
                if (missing.isEmpty()) {
                    permission.request.grant(permission.resources)
                    tab.pendingPermission = null
                    vm.toast("Permission granted")
                } else {
                    launcher.launch(missing.toTypedArray())
                }
            },
            onCancel = { permission.request.deny(); tab.pendingPermission = null }
        )
    }
}

@Composable
private fun DialogActions(confirmLabel: String, onConfirm: () -> Unit, onCancel: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Box(
            Modifier.clip(RoundedCornerShape(20.dp)).clickable(onClick = onCancel)
                .defaultMinSize(minWidth = 64.dp)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                "Not now",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onConfirm)
                .defaultMinSize(minWidth = 64.dp)
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Text(
                confirmLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun JsDialogHost(dialog: JsDialog, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(dialog.defaultValue.orEmpty()) }
    NovaDialog(onDismiss = { dialog.result.cancel(); onDismiss() }) {
        Text(dialog.message, style = MaterialTheme.typography.bodyLarge)
        if (dialog.type == JsDialog.Type.PROMPT) {
            Spacer(Modifier.height(12.dp))
            GlassPanel(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp),
                    modifier = Modifier.fillMaxWidth().padding(12.dp)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Box(
                Modifier.clip(RoundedCornerShape(20.dp))
                    .clickable { dialog.result.cancel(); onDismiss() }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    if (dialog.type == JsDialog.Type.ALERT) "Close" else "Cancel",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (dialog.type != JsDialog.Type.ALERT) {
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            when (dialog.type) {
                                JsDialog.Type.CONFIRM -> dialog.result.confirm()
                                JsDialog.Type.PROMPT -> (dialog.result as? android.webkit.JsPromptResult)?.confirm(text)
                                    ?: dialog.result.confirm()
                                else -> dialog.result.confirm()
                            }
                            onDismiss()
                        }
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text("OK", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}
