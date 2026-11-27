package com.novabrowse.browser.ui

data class CatalogEntry(
    val id: String,
    val name: String,
    val summary: String,
    val category: String,
    val status: String,
    val level: Compatibility
) {
    enum class Compatibility { WORKS, LIMITED, NOT_SUPPORTED }
}

/**
 * Featured Chrome Web Store extensions, each with the real store id so the store screen can
 * install it with one tap. Compatibility notes are honest about what NovaBrowse's chrome.*
 * runtime does and does not implement instead of promising that every desktop extension runs.
 */
object ExtensionCatalog {
    val entries = listOf(
        CatalogEntry(
            "eimadpbcbfnmbkopoojfekhnkhdbieeh",
            "Dark Reader", "Dark mode for every website using injected stylesheets.",
            "Appearance", "Injects its own stylesheets and keeps settings in chrome.storage, both supported.",
            CatalogEntry.Compatibility.WORKS
        ),
        CatalogEntry(
            "mnjggcdmjocbbbhaepdhchncahnbgone",
            "SponsorBlock", "Skip sponsor segments in YouTube videos.",
            "Video", "Content scripts, storage and runtime messaging all run in NovaBrowse.",
            CatalogEntry.Compatibility.WORKS
        ),
        CatalogEntry(
            "nffaoalbilbmmfgbnbgppjihopabppdk",
            "Video Speed Controller", "On-screen and keyboard playback speed control.",
            "Video", "A single content script with settings in storage — a good fit for this engine.",
            CatalogEntry.Compatibility.WORKS
        ),
        CatalogEntry(
            "bcjindcccaagfpapjjmafapmmgkkhgoa",
            "JSON Formatter", "Pretty-prints JSON pages in the browser.",
            "Developer", "Formats documents through content scripts; response rewriting is not available.",
            CatalogEntry.Compatibility.WORKS
        ),
        CatalogEntry(
            "hlepfoohegkhhmjieoechaddaejaokhf",
            "Refined GitHub", "Small usability improvements for GitHub.",
            "Productivity", "Content scripts and context menus work; a few desktop-only refinements are skipped.",
            CatalogEntry.Compatibility.WORKS
        ),
        CatalogEntry(
            "pkehgijcmpdhfbdbbnkijodmdjhbjlgp",
            "Privacy Badger", "Learns and blocks tracking domains.",
            "Privacy", "Blocking depends on declarativeNetRequest, which this engine does not enforce. " +
                "NovaBrowse's built-in tracking protection covers common trackers instead.",
            CatalogEntry.Compatibility.NOT_SUPPORTED
        ),
        CatalogEntry(
            "ddkjiahejlhfcafbddmgiahcphecmpfh",
            "uBlock Origin Lite", "Manifest V3 content blocker.",
            "Privacy", "Filter rules run through declarativeNetRequest, which is not enforced here, " +
                "so blocking has no effect. Use NovaBrowse's built-in tracker filter.",
            CatalogEntry.Compatibility.NOT_SUPPORTED
        ),
        CatalogEntry(
            "clngdbkpkpeebahjckkjfobafhncgmne",
            "Stylus", "Install and manage userstyles for websites.",
            "Appearance", "Style injection works; some editor pages depend on extension pages served " +
                "from chrome-extension:// origins.",
            CatalogEntry.Compatibility.LIMITED
        )
    )

    fun find(id: String): CatalogEntry? = entries.firstOrNull { it.id == id }
}
