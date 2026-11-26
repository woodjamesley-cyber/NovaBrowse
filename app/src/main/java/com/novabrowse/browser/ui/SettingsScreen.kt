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
import androidx.compose.material.icons.rounded.Close
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
import androidx.compose.material3.IconButton
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
        Column(Modifier.fillMaxSize().statusBarsPadding().background(MaterialTheme.colorScheme.background)) {
            Row(
                Modifier.fillMaxWidth().height(60.dp).padding(start = 4.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { vm.settingsPage = SettingsPage.ROOT }, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Rounded.ArrowBack, "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    pageTitle(vm.settingsPage),
                    style = TextStyle(fontSize = 22.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
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

    var query by remember { mutableStateOf("") }
    val categories = listOf(
        SettingsEntry(Icons.Rounded.Search, "General",
            "Search engine (${vm.currentEngine.name}), homepage, startup, language",
            SettingsPage.GENERAL, "Basics"),
        SettingsEntry(Icons.Rounded.Palette, "Appearance",
            "Theme, accent colour, toolbar, wallpaper", SettingsPage.APPEARANCE, "Basics"),
        SettingsEntry(Icons.Rounded.Security, "Privacy and security",
            "Cookies, tracking protection, permissions, browsing data", SettingsPage.PRIVACY, "Basics"),
        SettingsEntry(Icons.Rounded.Download, "Downloads",
            "Download folder, notifications, history", SettingsPage.DOWNLOADS, "Basics"),
        SettingsEntry(Icons.Rounded.Extension, "Extensions",
            "Installed extensions and the chrome.* runtime", SettingsPage.EXTENSIONS, "Advanced"),
        SettingsEntry(Icons.Rounded.BugReport, "Advanced",
            "Desktop site, JavaScript, storage, developer options", SettingsPage.ADVANCED, "Advanced"),
        SettingsEntry(Icons.Rounded.Info, "About NovaBrowse",
            "Version ${BuildConfig.VERSION_NAME} · BROWSE BEYOND.", SettingsPage.ABOUT, "Advanced")
    )
    val filtered = categories.filter {
        query.isBlank() || it.title.contains(query, ignoreCase = true) ||
            it.subtitle.contains(query, ignoreCase = true)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().height(60.dp).padding(start = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { vm.closeScreen() }, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Rounded.ArrowBack, "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                "Settings",
                style = TextStyle(fontSize = 22.sp),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.Search, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp)
            )
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text(
                            "Search settings",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 15.sp
                        )
                    }
                    inner()
                }
            )
            if (query.isNotEmpty()) {
                IconButton(onClick = { query = "" }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Rounded.Close, "Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "No settings match \"$query\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            filtered.groupBy { it.section }.forEach { (section, entries) ->
                SectionLabel(section)
                entries.forEachIndexed { index, entry ->
                    SettingsEntryRow(entry) { vm.settingsPage = entry.page }
                    if (index < entries.lastIndex) {
                        HairLine(Modifier.padding(start = 56.dp))
                    }
                }
            }
            Spacer(Modifier.height(26.dp))
            Text(
                "NovaBrowse ${BuildConfig.VERSION_NAME} · BROWSE BEYOND.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
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

private data class SettingsEntry(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val subtitle: String,
    val page: SettingsPage,
    val section: String
)

@Composable
private fun SettingsEntryRow(entry: SettingsEntry, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            entry.icon, null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.title, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(2.dp))
            Text(
                entry.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    com.novabrowse.browser.ui.components.ChromeCard(
        Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(Modifier.fillMaxWidth()) { content() }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 18.dp, bottom = 6.dp)
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
        SectionLabel("Surfaces")
        SettingsCard {
            Column(Modifier.padding(14.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Surface transparency", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
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
                Text("Higher values keep panels more opaque over web pages and wallpapers.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HairLine()
            ToggleRow(Icons.Rounded.Settings, "Toolbar at the bottom",
                "Off keeps Chrome's top toolbar and tab strip", settings.toolbarBottom) { on ->
                update { it.copy(toolbarBottom = on) }
            }
        }
        SectionLabel("Wallpaper")
        SettingsCard {
            Column(Modifier.padding(14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Wallpapers.presets.forEach { preset ->
                        Box(
                            Modifier.size(36.dp).clip(RoundedCornerShape(9.dp))
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
