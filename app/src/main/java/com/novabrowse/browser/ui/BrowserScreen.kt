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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FindInPage
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tab
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novabrowse.browser.browser.BrowserTab
import com.novabrowse.browser.browser.JsDialog
import com.novabrowse.browser.browser.UrlTools
import com.novabrowse.browser.ui.components.GlassIconButton
import com.novabrowse.browser.ui.components.GlassPanel
import com.novabrowse.browser.ui.components.NovaDialog
import com.novabrowse.browser.ui.components.ToolbarIcon
import com.novabrowse.browser.ui.theme.LocalNovaStyle

@Composable
fun BrowserScreen(vm: NovaViewModel) {
    val settings = vm.settings
    val tab = vm.activeTab
    val tabsVersion = vm.tabsVersion.value
    val style = LocalNovaStyle.current
    val showChrome = tab?.fullscreenView == null

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (showChrome && !settings.toolbarBottom) {
            BrowserTopToolbar(vm)
            AddressHeader(vm, tab)
        } else if (showChrome) {
            AddressHeader(vm, tab)
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
                        size = 38.dp
                    ) { Icon(Icons.Rounded.Close, "Exit fullscreen", modifier = Modifier.size(18.dp)) }
                }
            }
        }

        if (showChrome && settings.toolbarBottom) {
            BrowserToolbar(vm)
        }
    }

    tab?.pendingPermission?.let { permission ->
        PermissionDialog(vm, tab)
    }
    tab?.pendingGeo?.let { geo ->
        NovaDialog(onDismiss = {
            geo.callback.invoke(geo.origin, false, false)
            tab.pendingGeo = null
        }) {
            Text("Allow location?", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(
                "${geo.origin.ifBlank { "This website" }} wants to know your location.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
            Spacer(Modifier.height(18.dp))
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
private fun WebViewHost(tab: BrowserTab?, visible: Boolean) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            FrameLayout(context).apply { setBackgroundColor(0xFF070B1E.toInt()) }
        },
        update = { container ->
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
            tab?.fullscreenView?.let { fullscreen ->
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
private fun AddressHeader(vm: NovaViewModel, tab: BrowserTab?) {
    val settings = vm.settings
    val isBookmarked = tab?.url?.let { vm.isBookmarked(it) } == true
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassPanel(
            modifier = Modifier
                .weight(1f)
                .height(46.dp)
                .clip(RoundedCornerShape(23.dp))
                .clickable { vm.openSearch() },
            shape = RoundedCornerShape(23.dp),
            elevation = 8.dp,
            strong = true,
            tint = if (tab?.isPrivate == true) Color(0xFF3A1D63) else null
        ) {
            Row(
                Modifier.fillMaxSize().padding(start = 14.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (tab?.isPrivate == true) {
                    Icon(Icons.Rounded.VisibilityOff, null, tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp))
                } else if (tab?.isStartPage == false) {
                    Icon(
                        if (tab.isSecure) Icons.Rounded.Lock else Icons.Rounded.Warning,
                        contentDescription = if (tab.isSecure) "Secure" else "Not secure",
                        tint = if (tab.isSecure) MaterialTheme.colorScheme.primary else Color(0xFFFFB74D),
                        modifier = Modifier.size(14.dp)
                    )
                } else {
                    Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = when {
                        tab == null || tab.isStartPage -> "Search or enter website"
                        else -> tab.title.ifBlank { UrlTools.displayUrl(tab.url) }
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (tab?.isStartPage != false) 0.55f else 0.9f),
                    modifier = Modifier.weight(1f)
                )
                if (tab != null && !tab.isStartPage) {
                    if (tab.isLoading) {
                        IconButton(onClick = { tab.stop() }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Rounded.Close, "Stop", modifier = Modifier.size(16.dp))
                        }
                    } else {
                        IconButton(onClick = { tab.reload() }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Rounded.Refresh, "Reload", modifier = Modifier.size(17.dp))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        GlassIconButton(onClick = { vm.openTabSwitcher() }, size = 46.dp) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Tab, "Tabs", modifier = Modifier.size(20.dp))
                if (vm.tabs.size > 1) {
                    Text(
                        vm.tabs.size.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp, fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.Center).padding(top = 22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BrowserTopToolbar(vm: NovaViewModel) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolbarButtons(vm)
    }
}

@Composable
private fun BrowserToolbar(vm: NovaViewModel) {
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        elevation = 22.dp,
        strong = true
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            ToolbarButtons(vm)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.ToolbarButtons(vm: NovaViewModel) {
    val tab = vm.activeTab
    ToolbarIcon(
        Icons.Rounded.ArrowBack, "Back",
        enabled = tab?.canGoBack == true && tab.isStartPage.not(),
        onClick = { tab?.goBack() }
    )
    ToolbarIcon(
        Icons.Rounded.ArrowForward, "Forward",
        enabled = tab?.canGoForward == true,
        onClick = { tab?.goForward() }
    )
    ToolbarIcon(Icons.Rounded.Home, "Home", enabled = true, onClick = { vm.showStartPage() })
    ToolbarIcon(Icons.Rounded.Add, "New tab", enabled = true, onClick = { vm.newTab() })
    ToolbarIcon(
        Icons.Rounded.Tab, "Tabs", enabled = true, onClick = { vm.openTabSwitcher() },
        badge = vm.tabs.size
    )
    ToolbarIcon(Icons.Rounded.Menu, "Menu", enabled = true, onClick = { vm.openMenu() })
}

@Composable
private fun LoadingBar(tab: BrowserTab) {
    val target = (tab.progress / 100f).coerceIn(0f, 1f)
    val animated by animateFloatAsState(targetValue = target, label = "progress")
    if (animated < 1f && tab.isLoading) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(animated)
                    .height(2.dp)
                    .background(Brush.horizontalGradient(
                        listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary)
                    ))
            )
        }
    }
}

@Composable
private fun TrackerBadge(count: Int) {
    Box(Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.BottomStart) {
        GlassPanel(
            shape = CircleShape,
            elevation = 6.dp,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Security, null, modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(5.dp))
                Text("$count blocked", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
            }
        }
    }
}

@Composable
private fun PageErrorOverlay(tab: BrowserTab, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        GlassPanel(Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), strong = true, elevation = 24.dp) {
            Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.Warning, null, tint = Color(0xFFFFB74D), modifier = Modifier.size(30.dp))
                Spacer(Modifier.height(10.dp))
                Text("Can't open this page", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text(
                    tab.errorText ?: "Something went wrong.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Row {
                    NovaPillButton("Try again", onRetry)
                    Spacer(Modifier.width(10.dp))
                    NovaPillButton("Home", { tab.showStartPage() }, secondary = true)
                }
            }
        }
    }
}

@Composable
private fun NovaPillButton(label: String, onClick: () -> Unit, secondary: Boolean = false) {
    Box(
        Modifier
            .clip(CircleShape)
            .background(
                if (secondary) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                else MaterialTheme.colorScheme.primary
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (secondary) MaterialTheme.colorScheme.onSurface else Color.White
        )
    }
}

@Composable
private fun FindBar(vm: NovaViewModel) {
    Row(
        Modifier.fillMaxWidth().padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassPanel(
            Modifier.weight(1f).height(44.dp),
            shape = RoundedCornerShape(22.dp),
            strong = true,
            elevation = 10.dp
        ) {
            Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.FindInPage, null, modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = vm.findQuery,
                    onValueChange = { vm.findInPage(it) },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (vm.findQuery.isEmpty()) {
                            Text("Find on page", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                fontSize = 15.sp)
                        }
                        inner()
                    }
                )
                Text(
                    if (vm.findMatches > 0) "${vm.findMatches}" else "",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
        }
        Spacer(Modifier.width(6.dp))
        GlassIconButton(onClick = { vm.closeFind() }, size = 40.dp) {
            Icon(Icons.Rounded.Close, "Close find", modifier = Modifier.size(18.dp))
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
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
        )
        Spacer(Modifier.height(18.dp))
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
            Modifier.clip(CircleShape).clickable(onClick = onCancel)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) { Text("Not now", style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) }
        Spacer(Modifier.width(6.dp))
        Box(
            Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primary).clickable(onClick = onConfirm)
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) { Text(confirmLabel, style = MaterialTheme.typography.labelLarge, color = Color.White) }
    }
}

@Composable
private fun JsDialogHost(dialog: JsDialog, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(dialog.defaultValue.orEmpty()) }
    NovaDialog(onDismiss = { dialog.result.cancel(); onDismiss() }) {
        Text(dialog.message, style = MaterialTheme.typography.bodyLarge)
        if (dialog.type == JsDialog.Type.PROMPT) {
            Spacer(Modifier.height(12.dp))
            GlassPanel(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), elevation = 2.dp) {
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
            Box(Modifier.clip(CircleShape)
                .clickable { dialog.result.cancel(); onDismiss() }
                .padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(if (dialog.type == JsDialog.Type.ALERT) "Close" else "Cancel",
                    style = MaterialTheme.typography.labelLarge)
            }
            if (dialog.type != JsDialog.Type.ALERT) {
                Spacer(Modifier.width(6.dp))
                Box(Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primary)
                    .clickable {
                        when (dialog.type) {
                            JsDialog.Type.CONFIRM -> dialog.result.confirm()
                            JsDialog.Type.PROMPT -> (dialog.result as? android.webkit.JsPromptResult)?.confirm(text)
                                ?: dialog.result.confirm()
                            else -> dialog.result.confirm()
                        }
                        onDismiss()
                    }
                    .padding(horizontal = 18.dp, vertical = 10.dp)) {
                    Text("OK", style = MaterialTheme.typography.labelLarge, color = Color.White)
                }
            }
        }
    }
}
