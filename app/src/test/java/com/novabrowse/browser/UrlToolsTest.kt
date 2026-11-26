package com.novabrowse.browser

import com.novabrowse.browser.browser.UrlTools
import com.novabrowse.browser.data.SearchEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlToolsTest {

    private val google = SearchEngine.BUILT_IN.first { it.id == "google" }
    private val duck = SearchEngine.BUILT_IN.first { it.id == "duckduckgo" }

    @Test
    fun `bare domain is a url`() {
        assertTrue(UrlTools.isLikelyUrl("example.com"))
        assertEquals("https://example.com", UrlTools.toUrl("example.com", google))
    }

    @Test
    fun `domain with path and port is a url`() {
        assertTrue(UrlTools.isLikelyUrl("example.com:8080/path?q=1"))
        assertEquals("https://example.com:8080/path?q=1", UrlTools.toUrl("example.com:8080/path?q=1", google))
    }

    @Test
    fun `words are a search query`() {
        assertFalse(UrlTools.isLikelyUrl("nova browse"))
        assertFalse(UrlTools.isLikelyUrl("how to build a browser"))
        assertEquals(
            "https://www.google.com/search?q=nova+browse",
            UrlTools.toUrl("nova browse", google)
        )
    }

    @Test
    fun `search engine templates are honoured`() {
        val url = UrlTools.toUrl("liquid glass", duck)
        assertTrue(url.startsWith("https://duckduckgo.com/"))
        assertTrue(url.contains("liquid+glass"))
    }

    @Test
    fun `explicit schemes are preserved`() {
        assertEquals("https://novabrowse.app/x", UrlTools.toUrl("https://novabrowse.app/x", google))
        assertEquals("http://novabrowse.app/x", UrlTools.toUrl("http://novabrowse.app/x", google))
        assertEquals("about:blank", UrlTools.toUrl("about:blank", google))
    }

    @Test
    fun `localhost and ipv4 are urls`() {
        assertTrue(UrlTools.isLikelyUrl("localhost:3000"))
        assertTrue(UrlTools.isLikelyUrl("192.168.1.1"))
        assertTrue(UrlTools.isLikelyUrl("127.0.0.1:8080/admin"))
    }

    @Test
    fun `single word is never treated as a host`() {
        assertFalse(UrlTools.isLikelyUrl("novabrowse"))
        assertFalse(UrlTools.isLikelyUrl("hello"))
    }

    @Test
    fun `display url strips scheme and www`() {
        assertEquals("example.com/path", UrlTools.displayUrl("https://www.example.com/path/"))
        assertEquals("", UrlTools.displayUrl(""))
    }

    @Test
    fun `host extraction and https detection`() {
        assertEquals("news.ycombinator.com", UrlTools.host("https://news.ycombinator.com/item?id=1"))
        assertTrue(UrlTools.isHttps("https://example.com"))
        assertFalse(UrlTools.isHttps("http://example.com"))
    }

    @Test
    fun `file extension helper`() {
        assertEquals("pdf", UrlTools.fileExtension("report.PDF"))
        assertEquals("", UrlTools.fileExtension("README"))
    }
}
