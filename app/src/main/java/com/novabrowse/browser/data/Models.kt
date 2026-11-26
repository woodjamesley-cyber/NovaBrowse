package com.novabrowse.browser.data

import org.json.JSONObject
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()

private fun JSONObject.putSafe(key: String, value: String?) {
    put(key, value ?: JSONObject.NULL)
}

data class SearchEngine(
    val id: String,
    val name: String,
    val queryUrl: String,
    val suggestUrl: String? = null,
    val isCustom: Boolean = false
) {
    fun buildSearchUrl(query: String): String =
        queryUrl.replace("{query}", java.net.URLEncoder.encode(query, "UTF-8"))

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("queryUrl", queryUrl)
        putSafe("suggestUrl", suggestUrl)
        put("isCustom", isCustom)
    }

    companion object {
        fun fromJson(o: JSONObject): SearchEngine = SearchEngine(
            id = o.optString("id", newId()),
            name = o.optString("name", "Search"),
            queryUrl = o.optString("queryUrl", "https://www.google.com/search?q={query}"),
            suggestUrl = if (o.isNull("suggestUrl")) null else o.optString("suggestUrl").ifBlank { null },
            isCustom = o.optBoolean("isCustom", false)
        )

        val BUILT_IN: List<SearchEngine> = listOf(
            SearchEngine("google", "Google", "https://www.google.com/search?q={query}",
                "https://suggestqueries.google.com/complete/search?client=firefox&q={query}"),
            SearchEngine("bing", "Bing", "https://www.bing.com/search?q={query}",
                "https://api.bing.com/osjson.aspx?query={query}"),
            SearchEngine("duckduckgo", "DuckDuckGo", "https://duckduckgo.com/?q={query}",
                "https://duckduckgo.com/ac/?q={query}&type=list"),
            SearchEngine("brave", "Brave Search", "https://search.brave.com/search?q={query}"),
            SearchEngine("startpage", "Startpage", "https://www.startpage.com/sp/search?query={query}"),
            SearchEngine("ecosia", "Ecosia", "https://www.ecosia.org/search?q={query}"),
            SearchEngine("qwant", "Qwant", "https://www.qwant.com/?q={query}")
        )

        fun all(custom: List<SearchEngine>): List<SearchEngine> = BUILT_IN + custom
    }
}

data class Shortcut(
    val id: String = newId(),
    val title: String,
    val url: String,
    val iconUrl: String? = null,
    val order: Int = 0
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("title", title); put("url", url)
        putSafe("iconUrl", iconUrl); put("order", order)
    }

    companion object {
        fun fromJson(o: JSONObject) = Shortcut(
            id = o.optString("id", newId()),
            title = o.optString("title", "Shortcut"),
            url = o.optString("url", "https://example.com"),
            iconUrl = if (o.isNull("iconUrl")) null else o.optString("iconUrl"),
            order = o.optInt("order", 0)
        )
    }
}

data class Bookmark(
    val id: String = newId(),
    val title: String,
    val url: String,
    val folder: String = "Favourites",
    val createdAt: Long = System.currentTimeMillis(),
    val order: Int = 0
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("title", title); put("url", url); put("folder", folder)
        put("createdAt", createdAt); put("order", order)
    }

    companion object {
        const val ROOT_FOLDER = "Favourites"
        fun fromJson(o: JSONObject) = Bookmark(
            id = o.optString("id", newId()),
            title = o.optString("title", ""),
            url = o.optString("url", ""),
            folder = o.optString("folder", ROOT_FOLDER),
            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            order = o.optInt("order", 0)
        )
    }
}

data class HistoryEntry(
    val id: String = newId(),
    val title: String,
    val url: String,
    val visitedAt: Long = System.currentTimeMillis(),
    val visits: Int = 1
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("title", title); put("url", url)
        put("visitedAt", visitedAt); put("visits", visits)
    }

    companion object {
        fun fromJson(o: JSONObject) = HistoryEntry(
            id = o.optString("id", newId()),
            title = o.optString("title", ""),
            url = o.optString("url", ""),
            visitedAt = o.optLong("visitedAt", 0L),
            visits = o.optInt("visits", 1)
        )
    }
}

data class ClosedTabEntry(
    val id: String = newId(),
    val title: String,
    val url: String,
    val closedAt: Long = System.currentTimeMillis(),
    val wasPrivate: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("title", title); put("url", url)
        put("closedAt", closedAt); put("wasPrivate", wasPrivate)
    }

    companion object {
        fun fromJson(o: JSONObject) = ClosedTabEntry(
            id = o.optString("id", newId()),
            title = o.optString("title", ""),
            url = o.optString("url", ""),
            closedAt = o.optLong("closedAt", System.currentTimeMillis()),
            wasPrivate = o.optBoolean("wasPrivate", false)
        )
    }
}

data class ReadingItem(
    val id: String = newId(),
    val title: String,
    val url: String,
    val addedAt: Long = System.currentTimeMillis(),
    val excerpt: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("title", title); put("url", url)
        put("addedAt", addedAt); put("excerpt", excerpt)
    }

    companion object {
        fun fromJson(o: JSONObject) = ReadingItem(
            id = o.optString("id", newId()),
            title = o.optString("title", ""),
            url = o.optString("url", ""),
            addedAt = o.optLong("addedAt", 0L),
            excerpt = o.optString("excerpt", "")
        )
    }
}

data class DownloadRecord(
    val id: Long,
    val fileName: String,
    val url: String,
    val mimeType: String,
    val totalBytes: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "PENDING",
    val localPath: String? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("fileName", fileName); put("url", url); put("mimeType", mimeType)
        put("totalBytes", totalBytes); put("createdAt", createdAt); put("status", status)
        putSafe("localPath", localPath)
    }

    companion object {
        fun fromJson(o: JSONObject) = DownloadRecord(
            id = o.optLong("id", 0L),
            fileName = o.optString("fileName", "download"),
            url = o.optString("url", ""),
            mimeType = o.optString("mimeType", "application/octet-stream"),
            totalBytes = o.optLong("totalBytes", 0L),
            createdAt = o.optLong("createdAt", 0L),
            status = o.optString("status", "PENDING"),
            localPath = if (o.isNull("localPath")) null else o.optString("localPath")
        )
    }
}

data class TabGroup(
    val id: String = newId(),
    val name: String,
    val colorIndex: Int = 0
) {
    fun toJson(): JSONObject = JSONObject().apply { put("id", id); put("name", name); put("colorIndex", colorIndex) }
    companion object {
        fun fromJson(o: JSONObject) = TabGroup(
            o.optString("id", newId()), o.optString("name", "Group"), o.optInt("colorIndex", 0)
        )
    }
}

data class SessionTab(
    val url: String,
    val title: String,
    val groupId: String? = null,
    val isPrivate: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("url", url); put("title", title); putSafe("groupId", groupId); put("isPrivate", isPrivate)
    }
    companion object {
        fun fromJson(o: JSONObject) = SessionTab(
            o.optString("url", ""), o.optString("title", ""),
            if (o.isNull("groupId")) null else o.optString("groupId"),
            o.optBoolean("isPrivate", false)
        )
    }
}

fun JSONObject.optNullableString(key: String): String? =
    if (isNull(key)) null else optString(key).ifBlank { null }
