package com.novabrowse.browser.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

data class AppSettings(
    val theme: String = "system",            // system | light | dark
    val accent: String = "electric",         // electric | cyan | violet | aurora
    val glassIntensity: Float = 1.0f,
    val glassHighlights: Boolean = false,
    val toolbarBottom: Boolean = false,
    val searchEngineId: String = "google",
    val customEngines: List<SearchEngine> = emptyList(),
    val wallpaper: String = "plain",         // "plain", preset id or file:/// path
    val wallpaperDim: Float = 0.35f,
    val showClock: Boolean = true,
    val showFavourites: Boolean = true,
    val showShortcuts: Boolean = true,
    val showBookmarks: Boolean = true,
    val showRecent: Boolean = true,
    val showRecentlyClosed: Boolean = true,
    val shortcutColumns: Int = 4,
    val javaScriptEnabled: Boolean = true,
    val desktopMode: Boolean = false,
    val textZoom: Int = 100,
    val cookiesEnabled: Boolean = true,
    val thirdPartyCookies: Boolean = false,
    val doNotTrack: Boolean = true,
    val trackingProtection: Boolean = true,
    val safeBrowsing: Boolean = true,
    val historyEnabled: Boolean = true,
    val downloadNotifications: Boolean = true,
    val downloadToPublicFolder: Boolean = false,
    val startupBehaviour: String = "home",   // home | restore
    val restoreSession: Boolean = true,
    val extensionsEnabled: Boolean = true,
    val developerOptions: Boolean = false,
    val adBlockEnabled: Boolean = true,
    val searchSuggestions: Boolean = true
)

class SettingsStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("nova_settings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(read())
    val state: StateFlow<AppSettings> = _state
    val value: AppSettings get() = _state.value

    private fun read(): AppSettings {
        val raw = prefs.getString(KEY, null) ?: return AppSettings()
        return runCatching {
            val o = JSONObject(raw)
            val d = AppSettings()
            AppSettings(
                theme = o.optString("theme", d.theme),
                accent = o.optString("accent", d.accent),
                glassIntensity = o.optDouble("glassIntensity", d.glassIntensity.toDouble()).toFloat(),
                glassHighlights = o.optBoolean("glassHighlights", d.glassHighlights),
                toolbarBottom = o.optBoolean("toolbarBottom", d.toolbarBottom),
                searchEngineId = o.optString("searchEngineId", d.searchEngineId),
                customEngines = o.optJSONArray("customEngines")?.let { arr ->
                    (0 until arr.length()).mapNotNull { i ->
                        arr.optJSONObject(i)?.let { SearchEngine.fromJson(it) }
                    }
                } ?: emptyList(),
                wallpaper = o.optString("wallpaper", d.wallpaper),
                wallpaperDim = o.optDouble("wallpaperDim", d.wallpaperDim.toDouble()).toFloat(),
                showClock = o.optBoolean("showClock", d.showClock),
                showFavourites = o.optBoolean("showFavourites", d.showFavourites),
                showShortcuts = o.optBoolean("showShortcuts", d.showShortcuts),
                showBookmarks = o.optBoolean("showBookmarks", d.showBookmarks),
                showRecent = o.optBoolean("showRecent", d.showRecent),
                showRecentlyClosed = o.optBoolean("showRecentlyClosed", d.showRecentlyClosed),
                shortcutColumns = o.optInt("shortcutColumns", d.shortcutColumns),
                javaScriptEnabled = o.optBoolean("javaScriptEnabled", d.javaScriptEnabled),
                desktopMode = o.optBoolean("desktopMode", d.desktopMode),
                textZoom = o.optInt("textZoom", d.textZoom),
                cookiesEnabled = o.optBoolean("cookiesEnabled", d.cookiesEnabled),
                thirdPartyCookies = o.optBoolean("thirdPartyCookies", d.thirdPartyCookies),
                doNotTrack = o.optBoolean("doNotTrack", d.doNotTrack),
                trackingProtection = o.optBoolean("trackingProtection", d.trackingProtection),
                safeBrowsing = o.optBoolean("safeBrowsing", d.safeBrowsing),
                historyEnabled = o.optBoolean("historyEnabled", d.historyEnabled),
                downloadNotifications = o.optBoolean("downloadNotifications", d.downloadNotifications),
                downloadToPublicFolder = o.optBoolean("downloadToPublicFolder", d.downloadToPublicFolder),
                startupBehaviour = o.optString("startupBehaviour", d.startupBehaviour),
                restoreSession = o.optBoolean("restoreSession", d.restoreSession),
                extensionsEnabled = o.optBoolean("extensionsEnabled", d.extensionsEnabled),
                developerOptions = o.optBoolean("developerOptions", d.developerOptions),
                adBlockEnabled = o.optBoolean("adBlockEnabled", d.adBlockEnabled),
                searchSuggestions = o.optBoolean("searchSuggestions", d.searchSuggestions)
            )
        }.getOrElse { AppSettings() }
    }

    fun enabledEngines(): List<SearchEngine> = SearchEngine.all(value.customEngines)

    fun currentEngine(): SearchEngine =
        enabledEngines().firstOrNull { it.id == value.searchEngineId }
            ?: SearchEngine.BUILT_IN.first()

    fun update(block: (AppSettings) -> AppSettings) {
        val next = block(_state.value)
        _state.value = next
        prefs.edit().putString(KEY, encode(next).toString()).apply()
    }

    @Synchronized
    fun reset() {
        _state.value = AppSettings()
        prefs.edit().remove(KEY).apply()
    }

    private fun encode(s: AppSettings): JSONObject = JSONObject().apply {
        put("theme", s.theme); put("accent", s.accent)
        put("glassIntensity", s.glassIntensity.toDouble())
        put("glassHighlights", s.glassHighlights)
        put("toolbarBottom", s.toolbarBottom)
        put("searchEngineId", s.searchEngineId)
        put("customEngines", JSONArray().apply { s.customEngines.forEach { put(it.toJson()) } })
        put("wallpaper", s.wallpaper)
        put("wallpaperDim", s.wallpaperDim.toDouble())
        put("showClock", s.showClock); put("showFavourites", s.showFavourites)
        put("showShortcuts", s.showShortcuts); put("showBookmarks", s.showBookmarks)
        put("showRecent", s.showRecent); put("showRecentlyClosed", s.showRecentlyClosed)
        put("shortcutColumns", s.shortcutColumns)
        put("javaScriptEnabled", s.javaScriptEnabled); put("desktopMode", s.desktopMode)
        put("textZoom", s.textZoom)
        put("cookiesEnabled", s.cookiesEnabled); put("thirdPartyCookies", s.thirdPartyCookies)
        put("doNotTrack", s.doNotTrack); put("trackingProtection", s.trackingProtection)
        put("safeBrowsing", s.safeBrowsing); put("historyEnabled", s.historyEnabled)
        put("downloadNotifications", s.downloadNotifications)
        put("downloadToPublicFolder", s.downloadToPublicFolder)
        put("startupBehaviour", s.startupBehaviour); put("restoreSession", s.restoreSession)
        put("extensionsEnabled", s.extensionsEnabled)
        put("developerOptions", s.developerOptions)
        put("adBlockEnabled", s.adBlockEnabled); put("searchSuggestions", s.searchSuggestions)
    }

    companion object { private const val KEY = "settings_v1" }
}

/** Per-origin website permission decisions. */
class PermissionStore(context: Context) {
    private val prefs = context.getSharedPreferences("nova_permissions", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(readAll())
    val state: StateFlow<Map<String, Set<String>>> = _state

    private fun readAll(): Map<String, Set<String>> {
        val raw = prefs.getString("map", "{}") ?: "{}"
        return runCatching {
            val o = JSONObject(raw)
            o.keys().asSequence().associateWith { key ->
                val arr = o.optJSONArray(key) ?: JSONArray()
                (0 until arr.length()).mapNotNull { arr.optString(it).ifBlank { null } }.toSet()
            }
        }.getOrElse { emptyMap() }
    }

    private fun write(map: Map<String, Set<String>>) {
        val o = JSONObject()
        map.forEach { (origin, perms) -> o.put(origin, JSONArray(perms.toList())) }
        prefs.edit().putString("map", o.toString()).apply()
        _state.value = map
    }

    fun granted(origin: String, permission: String): Boolean =
        _state.value[origin]?.contains(permission) == true

    fun decision(origin: String, permission: String): Boolean? {
        val perms = _state.value[origin] ?: return null
        return when {
            perms.contains("deny:$permission") -> false
            perms.contains(permission) -> true
            else -> null
        }
    }

    fun set(origin: String, permission: String, allow: Boolean, remember: Boolean) {
        if (!remember) return
        val current = _state.value.toMutableMap()
        val perms = (current[origin] ?: emptySet()).toMutableSet()
        perms.remove(permission); perms.remove("deny:$permission")
        perms.add(if (allow) permission else "deny:$permission")
        current[origin] = perms
        write(current)
    }

    fun clear() = write(emptyMap())

    fun revokeAllFor(origin: String) {
        val current = _state.value.toMutableMap()
        current.remove(origin)
        write(current)
    }
}
