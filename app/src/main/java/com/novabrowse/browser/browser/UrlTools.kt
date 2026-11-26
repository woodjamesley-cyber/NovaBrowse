package com.novabrowse.browser.browser

import com.novabrowse.browser.data.SearchEngine
import java.net.URLEncoder

/**
 * Pure on purpose: address-bar parsing rules are unit tested on the JVM.
 */
object UrlTools {

    // Scheme names are matched without dots so that "example.com:8080" is not read as a scheme.
    private val schemeRegex = Regex("^[a-zA-Z][a-zA-Z0-9+\\-]*:")
    private val hostRegex = Regex("^(localhost|([a-zA-Z0-9]([a-zA-Z0-9\\-]*[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,})(:\\d{1,5})?(/.*)?$")
    private val localhostRegex = Regex("^localhost(:\\d{1,5})?(/.*)?$", RegexOption.IGNORE_CASE)
    private val ipv4Regex = Regex("^\\d{1,3}(\\.\\d{1,3}){3}(:\\d{1,5})?(/.*)?$")
    private val ipv6Regex = Regex("^\\[([0-9a-fA-F:]+)](:\\d{1,5})?(/.*)?$")
    private val whitespace = Regex("\\s")

    private val supportedSchemes = setOf(
        "http", "https", "file", "about", "data", "blob", "content", "javascript", "mailto", "tel", "sms", "intent", "market"
    )

    fun schemeOf(input: String): String? {
        val m = schemeRegex.find(input.trim()) ?: return null
        return m.value.dropLast(1).lowercase()
    }

    fun isSupportedScheme(scheme: String): Boolean = scheme in supportedSchemes

    /** True when the typed text should be treated as a URL rather than a search query. */
    fun isLikelyUrl(raw: String): Boolean {
        val input = raw.trim()
        if (input.isEmpty()) return false
        if (input.any { it == ' ' }) return false
        if (localhostRegex.matches(input)) return true
        if (schemeRegex.containsMatchIn(input)) {
            val scheme = schemeOf(input) ?: return false
            return scheme in supportedSchemes
        }
        if (input.contains("://")) return false
        if (hostRegex.matches(input)) return true
        if (ipv4Regex.matches(input)) return true
        if (ipv6Regex.matches(input)) return true
        return false
    }

    fun toUrl(raw: String, engine: SearchEngine): String {
        val input = raw.trim()
        if (input.isEmpty()) return engine.buildSearchUrl("")
        if (!isLikelyUrl(input)) return engine.buildSearchUrl(input)
        if (localhostRegex.matches(input)) return "http://$input"
        if (schemeRegex.containsMatchIn(input)) {
            val scheme = schemeOf(input)
            if (scheme == "http" || scheme == "https" || scheme == "file" || scheme == "about" ||
                scheme == "data" || scheme == "blob" || scheme == "content"
            ) return input
            return input // mailto:, tel:, market: are handed to the OS by the tab
        }
        return "https://$input"
    }

    fun searchUrl(query: String, engine: SearchEngine): String =
        engine.queryUrl.replace("{query}", URLEncoder.encode(query, "UTF-8"))

    fun suggestUrl(query: String, engine: SearchEngine): String? =
        engine.suggestUrl?.replace("{query}", URLEncoder.encode(query, "UTF-8"))

    fun displayUrl(url: String): String {
        if (url.isBlank() || url == "about:blank") return ""
        return url
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .trimEnd('/')
    }

    fun host(url: String): String {
        return runCatching { java.net.URI(url).host ?: "" }.getOrDefault("")
    }

    fun isHttps(url: String): Boolean = url.startsWith("https://", ignoreCase = true)

    fun faviconService(url: String, size: Int = 64): String? {
        val host = host(url)
        if (host.isBlank()) return null
        return "https://www.google.com/s2/favicons?domain=${URLEncoder.encode(host, "UTF-8")}&sz=$size"
    }

    fun fileExtension(name: String): String =
        name.substringAfterLast('.', "").lowercase()

    fun looksLikeExtensionRequest(url: String): Boolean {
        val u = url.lowercase()
        return u.endsWith(".crx") || u.endsWith(".zip")
    }

    fun decodeIfNeeded(text: String): String =
        runCatching { java.net.URLDecoder.decode(text, "UTF-8") }.getOrDefault(text)

    fun trimQuery(text: String): String = text.replace(whitespace, " ").trim()
}
