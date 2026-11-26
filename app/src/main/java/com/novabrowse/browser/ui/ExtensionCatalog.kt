package com.novabrowse.browser.ui

data class CatalogEntry(
    val name: String,
    val summary: String,
    val category: String,
    val status: String,
    val level: Compatibility,
    val storeSearch: String
) {
    enum class Compatibility { WORKS, LIMITED, NOT_SUPPORTED }
}

/**
 * Honest compatibility guidance for popular Chrome extensions. NovaBrowse's runtime
 * implements a subset of chrome.* over Android WebView, so this list says what to expect
 * instead of promising that every desktop extension runs.
 */
object ExtensionCatalog {
    val entries = listOf(
        CatalogEntry(
            "Dark Reader", "Dark mode for every website using injected stylesheets.",
            "Appearance", "Content scripts and storage are supported; scripting API runs in the main frame.",
            CatalogEntry.Compatibility.WORKS, "dark reader"
        ),
        CatalogEntry(
            "SponsorBlock", "Skip sponsor segments in YouTube videos.",
            "Video", "Uses content scripts, storage and runtime messaging — all implemented.",
            CatalogEntry.Compatibility.WORKS, "sponsorblock"
        ),
        CatalogEntry(
            "Refined GitHub", "Small usability improvements for GitHub.",
            "Productivity", "Content scripts work. Some menus depend on desktop-only APIs.",
            CatalogEntry.Compatibility.WORKS, "refined github"
        ),
        CatalogEntry(
            "Video Speed Controller", "Keyboard shortcuts for playback speed.",
            "Video", "Pure content script — usually works without changes.",
            CatalogEntry.Compatibility.WORKS, "video speed controller"
        ),
        CatalogEntry(
            "JSON Viewer", "Pretty-print JSON responses in the browser.",
            "Developer", "Works when it uses content scripts; response rewriting is not available.",
            CatalogEntry.Compatibility.LIMITED, "json viewer"
        ),
        CatalogEntry(
            "uBlock Origin", "Content and network blocker.",
            "Privacy", "Needs blocking webRequest, which Android WebView does not expose. " +
                "NovaBrowse's built-in on-device tracking protection covers common trackers instead.",
            CatalogEntry.Compatibility.NOT_SUPPORTED, "ublock origin"
        ),
        CatalogEntry(
            "Privacy Badger", "Learns and blocks tracking domains.",
            "Privacy", "Depends on blocking webRequest; use NovaBrowse's built-in tracker filter.",
            CatalogEntry.Compatibility.NOT_SUPPORTED, "privacy badger"
        ),
        CatalogEntry(
            "Bitwarden", "Password manager extension.",
            "Security", "Requires native messaging and WebAuthn surfaces that Android WebView " +
                "does not provide to extensions. Use the Bitwarden Android app's autofill instead.",
            CatalogEntry.Compatibility.NOT_SUPPORTED, "bitwarden"
        ),
        CatalogEntry(
            "Tampermonkey", "Userscript manager.",
            "Developer", "Needs unrestricted scripting and background service workers with " +
                "long life; only the basic scripting API is emulated.",
            CatalogEntry.Compatibility.LIMITED, "tampermonkey"
        ),
        CatalogEntry(
            "Web Archives", "View archived copies of the current page.",
            "Research", "Uses tabs and context menus; some archive providers need host permissions.",
            CatalogEntry.Compatibility.LIMITED, "web archives"
        )
    )
}
