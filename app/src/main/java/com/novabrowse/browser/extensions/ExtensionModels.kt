package com.novabrowse.browser.extensions

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

data class ContentScript(
    val matches: List<String>,
    val excludeMatches: List<String> = emptyList(),
    val js: List<String> = emptyList(),
    val css: List<String> = emptyList(),
    val runAt: String = "document_idle",
    val allFrames: Boolean = false
) {
    fun toJson() = JSONObject().apply {
        put("matches", JSONArray(matches)); put("excludeMatches", JSONArray(excludeMatches))
        put("js", JSONArray(js)); put("css", JSONArray(css))
        put("runAt", runAt); put("allFrames", allFrames)
    }

    companion object {
        fun fromJson(o: JSONObject) = ContentScript(
            matches = o.optJSONArray("matches").toStringList(),
            excludeMatches = o.optJSONArray("excludeMatches").toStringList(),
            js = o.optJSONArray("js").toStringList(),
            css = o.optJSONArray("css").toStringList(),
            runAt = o.optString("runAt", "document_idle"),
            allFrames = o.optBoolean("allFrames", false)
        )
    }
}

data class UnsupportedFeature(val api: String, val reason: String) {
    fun toJson() = JSONObject().apply { put("api", api); put("reason", reason) }
    companion object {
        fun fromJson(o: JSONObject) = UnsupportedFeature(o.optString("api"), o.optString("reason"))
    }
}

data class ExtensionRecord(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val manifestVersion: Int,
    val permissions: List<String> = emptyList(),
    val optionalPermissions: List<String> = emptyList(),
    val hostPermissions: List<String> = emptyList(),
    val contentScripts: List<ContentScript> = emptyList(),
    val backgroundScripts: List<String> = emptyList(),
    val backgroundServiceWorker: String? = null,
    val enabled: Boolean = true,
    val installedAt: Long = System.currentTimeMillis(),
    val hasPopup: Boolean = false,
    val popupPath: String? = null,
    val actionTitle: String = "",
    val iconPath: String? = null,
    val source: String = "",
    val unsupported: List<UnsupportedFeature> = emptyList(),
    val manifestJson: String = "{}"
) {
    val isMv3: Boolean get() = manifestVersion >= 3
    val hostAccessSummary: String get() = when {
        hostPermissions.isEmpty() -> "No website access"
        hostPermissions.contains("<all_urls>") || hostPermissions.contains("*://*/*") -> "All websites"
        else -> hostPermissions.joinToString(", ").take(120)
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("name", name); put("version", version)
        put("description", description); put("manifestVersion", manifestVersion)
        put("permissions", JSONArray(permissions)); put("optionalPermissions", JSONArray(optionalPermissions))
        put("hostPermissions", JSONArray(hostPermissions))
        put("contentScripts", JSONArray().apply { contentScripts.forEach { put(it.toJson()) } })
        put("backgroundScripts", JSONArray(backgroundScripts))
        put("backgroundServiceWorker", backgroundServiceWorker ?: JSONObject.NULL)
        put("enabled", enabled); put("installedAt", installedAt)
        put("hasPopup", hasPopup); put("popupPath", popupPath ?: JSONObject.NULL)
        put("actionTitle", actionTitle)
        put("iconPath", iconPath ?: JSONObject.NULL)
        put("source", source)
        put("unsupported", JSONArray().apply { unsupported.forEach { put(it.toJson()) } })
        put("manifestJson", manifestJson)
    }

    companion object {
        fun fromJson(o: JSONObject): ExtensionRecord = ExtensionRecord(
            id = o.optString("id"),
            name = o.optString("name", "Extension"),
            version = o.optString("version", "0"),
            description = o.optString("description", ""),
            manifestVersion = o.optInt("manifestVersion", 3),
            permissions = o.optJSONArray("permissions").toStringList(),
            optionalPermissions = o.optJSONArray("optionalPermissions").toStringList(),
            hostPermissions = o.optJSONArray("hostPermissions").toStringList(),
            contentScripts = o.optJSONArray("contentScripts").let { arr ->
                if (arr == null) emptyList() else (0 until arr.length())
                    .mapNotNull { arr.optJSONObject(it)?.let(ContentScript::fromJson) }
            },
            backgroundScripts = o.optJSONArray("backgroundScripts").toStringList(),
            backgroundServiceWorker = if (o.isNull("backgroundServiceWorker")) null
                else o.optString("backgroundServiceWorker"),
            enabled = o.optBoolean("enabled", true),
            installedAt = o.optLong("installedAt", System.currentTimeMillis()),
            hasPopup = o.optBoolean("hasPopup", false),
            popupPath = if (o.isNull("popupPath")) null else o.optString("popupPath"),
            actionTitle = o.optString("actionTitle", ""),
            iconPath = if (o.isNull("iconPath")) null else o.optString("iconPath"),
            source = o.optString("source", ""),
            unsupported = o.optJSONArray("unsupported").let { arr ->
                if (arr == null) emptyList() else (0 until arr.length())
                    .mapNotNull { arr.optJSONObject(it)?.let(UnsupportedFeature::fromJson) }
            },
            manifestJson = o.optString("manifestJson", "{}")
        )
    }
}

internal fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    val out = ArrayList<String>(length())
    for (i in 0 until length()) optString(i).takeIf { it.isNotBlank() }?.let { out.add(it) }
    return out
}

object ExtensionManifestParser {

    fun parse(dir: File, source: String): Result<ExtensionRecord> = runCatching {
        val manifestFile = File(dir, "manifest.json")
        require(manifestFile.exists()) { "manifest.json is missing from the package" }
        val manifest = JSONObject(manifestFile.readText())
        val name = manifest.optString("name").ifBlank { "Unnamed extension" }
        val version = manifest.optString("version", "0")
        val description = manifest.optString("description", "")
        val mv = manifest.optInt("manifest_version", 2)
        require(mv == 2 || mv == 3) { "Unsupported manifest_version $mv (only 2 and 3 are supported)" }

        val permissions = manifest.optJSONArray("permissions").toStringList()
        val optional = manifest.optJSONArray("optional_permissions").toStringList()
        var hostPermissions = manifest.optJSONArray("host_permissions").toStringList()
        if (hostPermissions.isEmpty()) {
            hostPermissions = permissions.filter { it.contains("://") || it == "<all_urls>" }
        }

        val contentScripts = buildList {
            val arr = manifest.optJSONArray("content_scripts")
            if (arr != null) for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                add(
                    ContentScript(
                        matches = o.optJSONArray("matches").toStringList(),
                        excludeMatches = o.optJSONArray("exclude_matches").toStringList(),
                        js = o.optJSONArray("js").toStringList(),
                        css = o.optJSONArray("css").toStringList(),
                        runAt = o.optString("run_at", "document_idle"),
                        allFrames = o.optBoolean("all_frames", false)
                    )
                )
            }
        }

        val background = manifest.optJSONObject("background")
        val backgroundScripts = background?.optJSONArray("scripts").toStringList()
        val serviceWorker = background?.optString("service_worker")?.takeIf { it.isNotBlank() }
            ?: background?.optString("page")?.takeIf { it.isNotBlank() }

        val action = manifest.optJSONObject("action") ?: manifest.optJSONObject("browser_action")
        val popupPath = action?.optString("default_popup")?.takeIf { it.isNotBlank() }
        val actionTitle = action?.optString("default_title")?.takeIf { it.isNotBlank() } ?: name

        val icons = manifest.optJSONObject("icons")
        val iconCandidate = if (icons != null && icons.length() > 0) {
            val sizes = icons.keys().asSequence().mapNotNull { it.toIntOrNull() }.sorted()
            val best = sizes.lastOrNull()
            if (best != null) icons.optString(best.toString()) else icons.keys().asSequence().first()
        } else null

        val iconPath = iconCandidate?.let { rel ->
            val f = File(dir, rel)
            if (f.exists()) f.absolutePath else null
        }

        val unsupported = ExtensionCompatibility.analyse(manifest, permissions, hostPermissions, mv)
        val digest = MessageDigest.getInstance("SHA-1")
            .digest("$name:$version:${dir.absolutePath}".toByteArray())
            .joinToString("") { "%02x".format(it) }

        ExtensionRecord(
            id = digest.take(16),
            name = name,
            version = version,
            description = description,
            manifestVersion = mv,
            permissions = permissions,
            optionalPermissions = optional,
            hostPermissions = hostPermissions,
            contentScripts = contentScripts,
            backgroundScripts = backgroundScripts,
            backgroundServiceWorker = serviceWorker,
            hasPopup = popupPath != null,
            popupPath = popupPath,
            actionTitle = actionTitle,
            iconPath = iconPath,
            source = source,
            unsupported = unsupported,
            manifestJson = manifest.toString(2)
        )
    }
}

object ExtensionCompatibility {

    private val unsupportedApis = mapOf(
        "webRequest" to "Blocking network requests from an extension is not available in the Android WebView engine.",
        "webRequestBlocking" to "Blocking network requests is not available in the Android WebView engine.",
        "debugger" to "The Chrome DevTools protocol is only available on desktop Chrome.",
        "devtools_page" to "Chrome DevTools extension pages require desktop Chrome.",
        "identity" to "Google account sign-in for extensions (chrome.identity) is not offered in NovaBrowse.",
        "sidePanel" to "Chrome's side panel UI is a desktop browser surface and has no mobile equivalent here.",
        "offscreen" to "Offscreen documents require a full Chromium extension host.",
        "declarativeNetRequest" to "Static declarative rule sets are not enforced by this engine. Content scripts and storage APIs still work.",
        "declarativeNetRequestWithHostAccess" to "Network rule enforcement is unavailable on this engine.",
        "desktopCapture" to "Screen capture APIs are desktop-only.",
        "tabCapture" to "Tab audio/video capture APIs are desktop-only.",
        "pageCapture" to "Page MHTML capture is desktop-only.",
        "system.cpu" to "System information APIs are not exposed to mobile extensions.",
        "nativeMessaging" to "Native messaging hosts cannot be installed or launched on Android.",
        "management" to "Chrome manages other extensions; NovaBrowse does not expose extension self-management.",
        "webNavigation" to "Full webNavigation event streams are not available on this engine."
    )

    private val partialApis = mapOf(
        "alarms" to "Timers run only while NovaBrowse is in memory.",
        "cookies" to "Cookie access is limited to clearing cookies, not reading arbitrary cookie jars.",
        "history" to "Read-only access to NovaBrowse's own history.",
        "tabs" to "Tab APIs work for NovaBrowse tabs; window arrangement is reported as a single window.",
        "contextMenus" to "Items appear in the NovaBrowse menu with the same click callbacks.",
        "scripting" to "executeScript and insertCSS are supported; frame targeting is main-frame only.",
        "notifications" to "Shown through Android notifications with the extension name."
    )

    fun analyse(
        manifest: JSONObject,
        permissions: List<String>,
        hostPermissions: List<String>,
        manifestVersion: Int
    ): List<UnsupportedFeature> {
        val out = mutableListOf<UnsupportedFeature>()
        val declared = (permissions + manifest.optJSONArray("optional_permissions").toStringList())
            .map { it.substringBefore('.') }
        (unsupportedApis.keys + partialApis.keys).forEach { key ->
            if (declared.contains(key) || manifest.has(key)) {
                unsupportedApis[key]?.let { out.add(UnsupportedFeature(key, "Not supported: $it")) }
                partialApis[key]?.let { out.add(UnsupportedFeature(key, "Limited: $it")) }
            }
        }
        if (manifest.has("chrome_url_overrides")) {
            out.add(UnsupportedFeature("chrome_url_overrides", "Not supported: NovaBrowse's start page cannot be replaced by an extension."))
        }
        if (manifest.has("commands")) {
            out.add(UnsupportedFeature("commands", "Limited: keyboard shortcuts only work while the address bar is focused."))
        }
        if (manifestVersion == 3 && manifest.optJSONObject("background")?.has("service_worker") == true) {
            out.add(
                UnsupportedFeature(
                    "service_worker lifecycle",
                    "Limited: MV3 service workers are hosted as a background page that runs while NovaBrowse is open, " +
                        "so long sleeps and cold-start wake-ups differ from desktop Chrome."
                )
            )
        }
        if (hostPermissions.any { it.contains("<all_urls>") }) {
            out.add(UnsupportedFeature("host access", "This extension requests access to every website. NovaBrowse asks you to approve that before installing."))
        }
        return out
    }
}

data class ExtensionContextMenuItem(
    val extensionId: String,
    val extensionName: String,
    val id: String,
    val title: String
)
