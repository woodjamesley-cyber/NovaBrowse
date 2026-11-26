package com.novabrowse.browser.ui

import android.content.Context
import android.net.Uri
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShortText
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novabrowse.browser.BuildConfig
import com.novabrowse.browser.Nova
import com.novabrowse.browser.data.AppSettings
import com.novabrowse.browser.data.SearchEngine
import com.novabrowse.browser.ui.components.GlassIconButton
import com.novabrowse.browser.ui.components.GlassPanel
import com.novabrowse.browser.ui.components.HairLine
import com.novabrowse.browser.ui.components.NovaChip
import com.novabrowse.browser.ui.components.NovaDialog
import com.novabrowse.browser.ui.components.NovaScreen
import com.novabrowse.browser.ui.components.SettingRow
import com.novabrowse.browser.ui.components.ToggleRow
import com.novabrowse.browser.ui.theme.ElectricBlue
import com.novabrowse.browser.ui.theme.NovaCyan
import com.novabrowse.browser.ui.theme.NovaViolet
import com.novabrowse.browser.ui.theme.SoftLavender
import com.novabrowse.browser.ui.theme.Wallpapers
import java.io.File

private fun update(block: (AppSettings) -> AppSettings) = Nova.settings.update(block)

@Composable
fun SettingsScreen(vm: NovaViewModel) {
    val settings = vm.settings
    if (vm.settingsPage != SettingsPage.ROOT) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconButton(onClick = { vm.settingsPage = SettingsPage.ROOT }, size = 40.dp) {
                    Icon(Icons.Rounded.ArrowBack, "Back", modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(pageTitle(vm.settingsPage), style = MaterialTheme.typography.headlineMedium,
                    fontSize = 22.sp)
            }
            Box(Modifier.fillMaxSize()) {
                when (vm.settingsPage) {
                    SettingsPage.GENERAL -> GeneralSettings(vm)
                    SettingsPage.APPEARANCE -> AppearanceSettings(vm)
                    SettingsPage.PRIVACY -> PrivacySettings(vm)
                    SettingsPage.DOWNLOADS -> DownloadSettings(vm)
                    SettingsPage.EXTENSIONS -> ExtensionSettings(vm)
                    SettingsPage.ADVANCED -> AdvancedSettings(vm)
                    SettingsPage.ABOUT -> AboutContent(vm)
                    SettingsPage.ROOT -> Unit
                }
            }
        }
        return
    }

    NovaScreen(title = "Settings", onClose = { vm.closeScreen() }) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp)
        ) {
            SettingsCategory(Icons.Rounded.Settings, "General",
                "Homepage, search engine, startup behaviour, language") {
                vm.settingsPage = SettingsPage.GENERAL
            }
            SettingsCategory(Icons.Rounded.Palette, "Appearance",
                "Theme, accent colour, Liquid Glass, toolbar, wallpaper") {
                vm.settingsPage = SettingsPage.APPEARANCE
            }
            SettingsCategory(Icons.Rounded.Security, "Privacy",
                "History, cookies, tracking protection, permissions, clear data") {
                vm.settingsPage = SettingsPage.PRIVACY
            }
            SettingsCategory(Icons.Rounded.Download, "Downloads",
                "Download folder, notifications, history") {
                vm.settingsPage = SettingsPage.DOWNLOADS
            }
            SettingsCategory(Icons.Rounded.Extension, "Extensions",
                "Installed extensions, permissions, installation") {
                vm.settingsPage = SettingsPage.EXTENSIONS
            }
            SettingsCategory(Icons.Rounded.BugReport, "Advanced",
                "Desktop site, JavaScript, storage, developer options") {
                vm.settingsPage = SettingsPage.ADVANCED
            }
            SettingsCategory(Icons.Rounded.Info, "About NovaBrowse",
                "Version ${BuildConfig.VERSION_NAME} · BROWSE BEYOND.") {
                vm.settingsPage = SettingsPage.ABOUT
            }
            Spacer(Modifier.height(30.dp))
            Text(
                "NovaBrowse ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(40.dp))
        }
    }
}

private fun pageTitle(page: SettingsPage): String = when (page) {
    SettingsPage.GENERAL -> "General"
    SettingsPage.APPEARANCE -> "Appearance"
    SettingsPage.PRIVACY -> "Privacy"
    SettingsPage.DOWNLOADS -> "Downloads"
    SettingsPage.EXTENSIONS -> "Extensions"
    SettingsPage.ADVANCED -> "Advanced"
    SettingsPage.ABOUT -> "About NovaBrowse"
    SettingsPage.ROOT -> "Settings"
}

@Composable
private fun SettingsCategory(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    GlassPanel(
        Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = 8.dp
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(13.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Rounded.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    GlassPanel(Modifier.fillMaxWidth().padding(vertical = 6.dp), shape = RoundedCornerShape(22.dp), elevation = 8.dp) {
        Column(Modifier.fillMaxWidth()) { content() }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
        modifier = Modifier.padding(start = 18.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun GeneralSettings(vm: NovaViewModel) {
    val settings = vm.settings
    var engineDialog by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)) {
        SectionLabel("Search")
        SettingsCard {
            SettingRow(Icons.Rounded.Language, "Search engine", vm.currentEngine.name) { engineDialog = true }
            HairLine()
            ToggleRow(Icons.Rounded.Language, "Search suggestions", "Fetch live suggestions as you type",
                settings.searchSuggestions) { on -> update { it.copy(searchSuggestions = on) } }
            HairLine()
            SettingRow(Icons.Rounded.Search, "Custom search engines", vm.engines.count { it.isCustom }.toString() + " added") {
                vm.customEngineDraft = true
            }
        }
        SectionLabel("Startup")
        SettingsCard {
            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NovaChip("Start page", selected = settings.startupBehaviour == "home",
                    onClick = { update { it.copy(startupBehaviour = "home") } })
                NovaChip("Restore session", selected = settings.startupBehaviour == "restore",
                    onClick = { update { it.copy(startupBehaviour = "restore") } })
            }
            HairLine()
            ToggleRow(Icons.Rounded.RestartAlt, "Save tabs on exit",
                "Restore the tabs you had open the next time NovaBrowse starts",
                settings.restoreSession) { on -> update { it.copy(restoreSession = on) } }
        }
        SectionLabel("Homepage")
        SettingsCard {
            ToggleRow(Icons.Rounded.History, "Show clock", checked = settings.showClock) {
                on -> update { it.copy(showClock = on) }
            }
            HairLine()
            ToggleRow(Icons.Rounded.Settings, "Show shortcuts", checked = settings.showShortcuts) {
                on -> update { it.copy(showShortcuts = on) }
            }
            HairLine()
            ToggleRow(Icons.Rounded.Bookmark, "Show favourites", checked = settings.showFavourites) {
                on -> update { it.copy(showFavourites = on) }
            }
            HairLine()
            ToggleRow(Icons.Rounded.History, "Show recently visited", checked = settings.showRecent) {
                on -> update { it.copy(showRecent = on) }
            }
            HairLine()
            ToggleRow(Icons.Rounded.History, "Show recently closed", checked = settings.showRecentlyClosed) {
                on -> update { it.copy(showRecentlyClosed = on) }
            }
            HairLine()
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Shortcut columns", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                (3..5).forEach { count ->
                    NovaChip(count.toString(), selected = settings.shortcutColumns == count,
                        onClick = { update { it.copy(shortcutColumns = count) } })
                    Spacer(Modifier.width(6.dp))
                }
            }
        }
        SectionLabel("Language")
        SettingsCard {
            SettingRow(Icons.Rounded.Language, "Language", "NovaBrowse uses your Android language setting")
        }
        Spacer(Modifier.height(40.dp))
    }

    if (engineDialog) {
        NovaDialog(onDismiss = { engineDialog = false }) {
            Text("Search engine", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            vm.engines.forEach { engine ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .clickable { vm.setSearchEngine(engine.id); engineDialog = false }
                        .padding(vertical = 10.dp, horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(engine.name, modifier = Modifier.weight(1f))
                    if (engine.id == settings.searchEngineId) {
                        Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp))
                    }
                }
            }
            if (settings.customEngines.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text("Remove a custom engine", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                settings.customEngines.forEach { engine ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(engine.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text("Remove", color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.clickable { vm.removeCustomEngine(engine) })
                    }
                }
            }
        }
    }
}

@Composable
private fun AppearanceSettings(vm: NovaViewModel) {
    val settings = vm.settings
    val context = LocalContext.current
    val wallpaperPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { saveWallpaper(context, it) { path -> update { s -> s.copy(wallpaper = "file:$path") } } }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)) {
        SectionLabel("Theme")
        SettingsCard {
            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NovaChip("System", selected = settings.theme == "system",
                    onClick = { update { it.copy(theme = "system") } })
                NovaChip("Light", selected = settings.theme == "light",
                    onClick = { update { it.copy(theme = "light") } })
                NovaChip("Dark", selected = settings.theme == "dark",
                    onClick = { update { it.copy(theme = "dark") } })
            }
            HairLine()
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Accent colour", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                listOf("electric" to ElectricBlue, "cyan" to NovaCyan, "violet" to NovaViolet, "lavender" to SoftLavender)
                    .forEach { (id, color) ->
                        Box(
                            Modifier.size(26.dp).clip(CircleShape).background(color)
                                .clickable { update { it.copy(accent = id) } },
                            contentAlignment = Alignment.Center
                        ) {
                            if (settings.accent == id) {
                                Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                    }
            }
        }
        SectionLabel("Liquid Glass")
        SettingsCard {
            Column(Modifier.padding(14.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Glass intensity", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text("${(settings.glassIntensity * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = settings.glassIntensity,
                    onValueChange = { value -> update { it.copy(glassIntensity = value) } },
                    valueRange = 0.4f..1.4f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
                Text("Higher values make panels more opaque. Lower values show more of the page behind them.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
            HairLine()
            ToggleRow(Icons.Rounded.DarkMode, "Animated glass highlights",
                "Subtle specular sweep on hero surfaces", settings.glassHighlights) { on ->
                update { it.copy(glassHighlights = on) }
            }
            HairLine()
            ToggleRow(Icons.Rounded.Settings, "Toolbar at the bottom",
                "Turn off to place the navigation toolbar at the top", settings.toolbarBottom) { on ->
                update { it.copy(toolbarBottom = on) }
            }
        }
        SectionLabel("Wallpaper")
        SettingsCard {
            Column(Modifier.padding(14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Wallpapers.presets.forEach { preset ->
                        Box(
                            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(preset.colors))
                                .clickable { update { it.copy(wallpaper = preset.id) } }
                        )
                        Spacer(Modifier.width(2.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NovaChip("Upload wallpaper", onClick = { wallpaperPicker.launch("image/*") },
                        leading = Icons.Rounded.Wallpaper)
                    if (settings.wallpaper.startsWith("file:")) {
                        NovaChip("Use preset", onClick = { update { it.copy(wallpaper = "aurora") } })
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Dim", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Slider(
                    value = settings.wallpaperDim,
                    onValueChange = { value -> update { it.copy(wallpaperDim = value) } },
                    valueRange = 0f..0.85f
                )
            }
        }
        SectionLabel("Homepage layout")
        SettingsCard {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Shortcut layout", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                (3..5).forEach { count ->
                    NovaChip("$count×", selected = settings.shortcutColumns == count,
                        onClick = { update { it.copy(shortcutColumns = count) } })
                    Spacer(Modifier.width(6.dp))
                }
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

private fun saveWallpaper(context: Context, uri: Uri, onSaved: (String) -> Unit) {
    runCatching {
        val dir = File(context.filesDir, "wallpapers").apply { mkdirs() }
        val target = File(dir, "wallpaper-${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
        onSaved(target.absolutePath)
    }
}

@Composable
private fun PrivacySettings(vm: NovaViewModel) {
    val settings = vm.settings
    val permissions by Nova.permissions.state.collectAsStateWithLifecycle()
    var clearDialog by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)) {
        SectionLabel("Browsing data")
        SettingsCard {
            ToggleRow(Icons.Rounded.History, "Save browsing history",
                "Private tabs are never recorded", settings.historyEnabled) { on ->
                update { it.copy(historyEnabled = on) }
            }
            HairLine()
            SettingRow(Icons.Rounded.DeleteSweep, "Clear browsing data",
                "History, cookies, cache and site storage") { clearDialog = true }
        }
        SectionLabel("Cookies & tracking")
        SettingsCard {
            ToggleRow(Icons.Rounded.Lock, "Accept cookies", checked = settings.cookiesEnabled) { on ->
                update { it.copy(cookiesEnabled = on) }
                Nova.tabs.applyGlobalSettings()
            }
            HairLine()
            ToggleRow(Icons.Rounded.Security, "Third-party cookies",
                "Off by default. Some sign-in flows need this on.", settings.thirdPartyCookies) { on ->
                update { it.copy(thirdPartyCookies = on) }
                Nova.tabs.applyGlobalSettings()
            }
            HairLine()
            ToggleRow(Icons.Rounded.Security, "Tracking protection",
                "Block known trackers and ad hosts on device (${com.novabrowse.browser.browser.TrackingProtection.count()} hosts)",
                settings.trackingProtection) { on ->
                update { it.copy(trackingProtection = on) }
            }
            HairLine()
            ToggleRow(Icons.Rounded.Security, "Do Not Track header",
                "Ask websites not to track you", settings.doNotTrack) { on ->
                update { it.copy(doNotTrack = on) }
            }
            HairLine()
            ToggleRow(Icons.Rounded.Security, "Safe Browsing",
                "Warn about known malicious pages", settings.safeBrowsing) { on ->
                update { it.copy(safeBrowsing = on) }
            }
        }
        SectionLabel("Website permissions")
        SettingsCard {
            if (permissions.isEmpty()) {
                SettingRow(Icons.Rounded.Lock, "No site permissions saved",
                    "Camera, microphone and location prompts appear while browsing")
            } else {
                permissions.entries.forEach { (origin, perms) ->
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(origin, style = MaterialTheme.typography.titleMedium)
                            Text(
                                perms.joinToString(", ") { it.removePrefix("deny:") + if (it.startsWith("deny:")) " (blocked)" else "" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            )
                        }
                        Text("Reset", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.clickable { vm.revokePermissions(origin) })
                    }
                    HairLine()
                }
            }
        }
        Spacer(Modifier.height(40.dp))
    }

    if (clearDialog) {
        NovaDialog(onDismiss = { clearDialog = false }) {
            Text("Clear browsing data", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text("Choose what NovaBrowse should forget.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(Modifier.height(12.dp))
            listOf(
                "history" to "Browsing history",
                "cookies" to "Cookies and sign-ins",
                "cache" to "Cached pages and files",
                "storage" to "Website storage",
                "permissions" to "Website permissions",
                "all" to "Everything"
            ).forEach { (key, label) ->
                Row(Modifier.fillMaxWidth()
                    .clickable { vm.clearBrowsingData(key); clearDialog = false }
                    .padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge,
                        color = if (key == "all") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                    Icon(Icons.Rounded.DeleteSweep, null, modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }
        }
    }
}

@Composable
private fun DownloadSettings(vm: NovaViewModel) {
    val settings = vm.settings
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)) {
        SectionLabel("Downloads")
        SettingsCard {
            ToggleRow(Icons.Rounded.Download, "Save to public Downloads",
                "Off: files stay in NovaBrowse's private folder", settings.downloadToPublicFolder) { on ->
                update { it.copy(downloadToPublicFolder = on) }
            }
            HairLine()
            ToggleRow(Icons.Rounded.Download, "Download notifications",
                "Show Android notifications for completed downloads", settings.downloadNotifications) { on ->
                update { it.copy(downloadNotifications = on) }
            }
            HairLine()
            SettingRow(Icons.Rounded.Download, "Download history", "Manage files NovaBrowse has downloaded") {
                vm.openScreen(NovaScreen.DOWNLOADS)
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun ExtensionSettings(vm: NovaViewModel) {
    val settings = vm.settings
    val installed by vm.extensionsFlow.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)) {
        SectionLabel("Extensions")
        SettingsCard {
            ToggleRow(Icons.Rounded.Extension, "Enable extension runtime",
                "Runs content scripts and background pages for installed extensions",
                settings.extensionsEnabled) { on ->
                update { it.copy(extensionsEnabled = on) }
                Nova.extensions.restart()
            }
            HairLine()
            SettingRow(Icons.Rounded.Extension, "Manage extensions",
                "${installed.size} installed · ${installed.count { it.enabled }} enabled") {
                vm.openScreen(NovaScreen.EXTENSIONS)
            }
            HairLine()
            ToggleRow(Icons.Rounded.BugReport, "Developer options",
                "Show compatibility details and debug information", settings.developerOptions) { on ->
                update { it.copy(developerOptions = on) }
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun AdvancedSettings(vm: NovaViewModel) {
    val settings = vm.settings
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)) {
        SectionLabel("Page rendering")
        SettingsCard {
            ToggleRow(Icons.Rounded.Language, "JavaScript", "Disabling breaks most modern sites",
                settings.javaScriptEnabled) { on ->
                update { it.copy(javaScriptEnabled = on) }
                Nova.tabs.applyGlobalSettings()
            }
            HairLine()
            ToggleRow(Icons.Rounded.Language, "Desktop site by default",
                "Request desktop layouts for every page", settings.desktopMode) { on ->
                update { it.copy(desktopMode = on) }
                Nova.tabs.applyGlobalSettings()
            }
            HairLine()
            Column(Modifier.padding(14.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Text("Text size", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text("${settings.textZoom}%", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = settings.textZoom.toFloat(),
                    onValueChange = { value -> update { it.copy(textZoom = value.toInt()) } },
                    onValueChangeFinished = { Nova.tabs.applyGlobalSettings() },
                    valueRange = 70f..200f
                )
            }
        }
        SectionLabel("Storage")
        SettingsCard {
            SettingRow(Icons.Rounded.Storage, "Website storage", "Clear local storage for all sites") {
                vm.clearBrowsingData("storage")
            }
            HairLine()
            SettingRow(Icons.Rounded.Storage, "Cached pages", "Free up cached images and scripts") {
                vm.clearBrowsingData("cache")
            }
        }
        SectionLabel("Handbook")
        SettingsCard {
            SettingRow(Icons.Rounded.Info, "Open-source licenses", "AndroidX, Kotlin, Coil, CameraX, ML Kit") {
                vm.openScreen(NovaScreen.LICENSES)
            }
            HairLine()
            SettingRow(Icons.Rounded.Security, "Reset all settings", "Return NovaBrowse to its defaults") {
                Nova.settings.reset()
                vm.applyEngineSettings()
                vm.toast("Settings reset")
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
fun AboutContent(vm: NovaViewModel) {
    val context = LocalContext.current
    val engine = remember {
        runCatching {
            androidx.webkit.WebViewCompat.getCurrentWebViewPackage(context)?.let {
                "${it.packageName} ${it.versionName}"
            }
        }.getOrNull() ?: "Android System WebView"
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)) {
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            com.novabrowse.browser.ui.components.NovaBrandTile(size = 84.dp)
        }
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            com.novabrowse.browser.ui.components.NovaWordmark(size = 30, showTagline = true)
        }
        Spacer(Modifier.height(18.dp))
        SettingsCard {
            SettingRow(Icons.Rounded.Info, "Version", BuildConfig.VERSION_NAME, onClick = null)
            HairLine()
            SettingRow(Icons.Rounded.Info, "Package", "com.novabrowse.browser", onClick = null)
            HairLine()
            SettingRow(Icons.Rounded.Language, "Browser engine", engine, onClick = null)
            HairLine()
            SettingRow(Icons.Rounded.Extension, "Extension runtime",
                "NovaBrowse WebExtension bridge (chrome.* subset)", onClick = null)
        }
        Spacer(Modifier.height(8.dp))
        SettingsCard {
            SettingRow(Icons.Rounded.Lock, "Privacy policy", "Read how NovaBrowse handles your data") {
                vm.openScreen(NovaScreen.LICENSES)
            }
            HairLine()
            SettingRow(Icons.Rounded.Info, "Open-source licenses", "Third-party libraries and notices") {
                vm.openScreen(NovaScreen.LICENSES)
            }
        }
        Spacer(Modifier.height(8.dp))
        GlassPanel(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), elevation = 6.dp) {
            Column(Modifier.padding(16.dp)) {
                Text("BROWSE BEYOND.", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(6.dp))
                Text(
                    "NovaBrowse is built on Android's Chromium-powered WebView engine. " +
                        "It never uploads your browsing history, and tracking protection runs entirely on device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}
