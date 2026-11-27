package com.novabrowse.browser.extensions

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class StoreListing(
    val id: String,
    val name: String,
    val publisher: String = "",
    val iconUrl: String = "",
    val slug: String = ""
)

/** A titled row of store results, used to browse the store without searching. */
data class StoreSection(
    val title: String,
    val query: String,
    val listings: List<StoreListing>
)

/**
 * Client for the Chrome Web Store.
 *
 * Two public endpoints are used, neither of which needs a Google account:
 *  - the store's server-rendered search page, parsed into result cards;
 *  - Google's CRX update service, which serves the same signed package Chrome installs.
 *
 * The update request advertises a very high product version because the service answers
 * 204 (no package) when the caller claims to be older than an extension's minimum Chrome.
 */
object ChromeWebStore {

    const val STORE_ORIGIN = "https://chromewebstore.google.com"
    const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    private val ID = Regex("^[a-p]{32}$")
    private val PATH_ID = Regex("/detail/(?:[^/?#]+/)?([a-p]{32})")
    private val QUERY_ID = Regex("[?&]id=([a-p]{32})")
    private val CARD_NAME = Regex("<h2[^>]*>([^<]*)</h2>")
    private val CARD_ICON = Regex("<div class=\"KB5Wr\">.*?<img[^>]+src=\"([^\"]+)\"", RegexOption.DOT_MATCHES_ALL)
    private val CARD_ANY_ICON = Regex("<img[^>]+src=\"([^\"]+)\"")
    private val CARD_PUBLISHER = Regex("<span class=\"[^\"]*HtRvfe[^\"]*\">([^<]*)</span>")
    private val CARD_SLUG = Regex("href=\"\\./detail/([^/\"]+)/[a-p]{32}\"")
    private val ENTITY = Regex("&#(x?)([0-9a-fA-F]+);")

    fun isExtensionId(value: String): Boolean = ID.matches(value.trim())

    fun listingUrl(id: String, slug: String = ""): String =
        if (slug.isBlank()) "$STORE_ORIGIN/detail/$id" else "$STORE_ORIGIN/detail/$slug/$id"

    /** Accepts a bare 32-character id, a store listing URL, or any URL carrying `?id=`. */
    fun resolveId(input: String): String? {
        val text = input.trim()
        if (isExtensionId(text)) return text
        PATH_ID.find(text)?.groupValues?.getOrNull(1)?.let { return it }
        QUERY_ID.find(text)?.groupValues?.getOrNull(1)?.let { return it }
        return null
    }

    fun crxUrl(id: String): String {
        val x = "id%3D$id%26installsource%3Dondemand%26uc"
        return "https://clients2.google.com/service/update2/crx?response=redirect" +
            "&os=win&arch=x64&os_arch=x86_64&nacl_arch=x86-64" +
            "&prod=chromecrx&prodchannel=unknown&prodversion=9999.0.9999.0" +
            "&acceptformat=crx2,crx3&x=$x"
    }

    fun search(query: String, limit: Int = 40): List<StoreListing> {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        if (encoded.isBlank()) return emptyList()
        return parseSearchHtml(get("$STORE_ORIGIN/search/$encoded?hl=en"), limit)
    }

    internal fun parseSearchHtml(html: String, limit: Int = 40): List<StoreListing> {
        val out = LinkedHashMap<String, StoreListing>()
        for (chunk in html.split("data-item-id=\"").drop(1)) {
            if (out.size >= limit) break
            val id = chunk.substringBefore('"')
            if (!isExtensionId(id) || out.containsKey(id)) continue
            val name = CARD_NAME.find(chunk)?.groupValues?.get(1)?.let(::decode)?.trim().orEmpty()
            val icon = CARD_ICON.find(chunk) ?: CARD_ANY_ICON.find(chunk)
            out[id] = StoreListing(
                id = id,
                name = name.ifBlank { id },
                publisher = CARD_PUBLISHER.find(chunk)?.groupValues?.get(1)?.let(::decode)?.trim().orEmpty(),
                iconUrl = icon?.groupValues?.get(1).orEmpty(),
                slug = CARD_SLUG.find(chunk)?.groupValues?.get(1).orEmpty()
            )
        }
        return out.values.toList()
    }

    internal fun decode(value: String): String {
        val named = value
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&nbsp;", " ")
        if (!named.contains("&#")) return named
        return ENTITY.replace(named) { match ->
            val hex = match.groupValues[1].isNotEmpty()
            val code = match.groupValues[2].toIntOrNull(if (hex) 16 else 10)
            if (code == null || code <= 0) match.value else runCatching {
                String(Character.toChars(code))
            }.getOrDefault(match.value)
        }
    }

    private fun get(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 20_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept-Language", "en-US,en;q=0.9")
        }
        try {
            connection.connect()
            val code = connection.responseCode
            require(code in 200..299) { "Chrome Web Store returned HTTP $code" }
            return connection.inputStream.readBytes().toString(Charsets.UTF_8)
        } finally {
            connection.disconnect()
        }
    }
}
