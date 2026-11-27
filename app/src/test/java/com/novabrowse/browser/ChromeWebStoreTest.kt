package com.novabrowse.browser

import com.novabrowse.browser.extensions.ChromeWebStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChromeWebStoreTest {

    @Test
    fun acceptsBareExtensionId() {
        assertEquals(
            "eimadpbcbfnmbkopoojfekhnkhdbieeh",
            ChromeWebStore.resolveId("eimadpbcbfnmbkopoojfekhnkhdbieeh")
        )
        assertEquals(
            "eimadpbcbfnmbkopoojfekhnkhdbieeh",
            ChromeWebStore.resolveId("  eimadpbcbfnmbkopoojfekhnkhdbieeh  ")
        )
    }

    @Test
    fun extractsIdFromListingUrl() {
        assertEquals(
            "eimadpbcbfnmbkopoojfekhnkhdbieeh",
            ChromeWebStore.resolveId(
                "https://chromewebstore.google.com/detail/dark-reader/eimadpbcbfnmbkopoojfekhnkhdbieeh?hl=en"
            )
        )
    }

    @Test
    fun extractsIdFromQueryUrl() {
        assertEquals(
            "mnjggcdmjocbbbhaepdhchncahnbgone",
            ChromeWebStore.resolveId(
                "https://chromewebstore.google.com/webstore/detail?hl=en&id=mnjggcdmjocbbbhaepdhchncahnbgone"
            )
        )
    }

    @Test
    fun rejectsInputWithoutAnId() {
        assertNull(ChromeWebStore.resolveId("https://example.com/extension.zip"))
        assertNull(ChromeWebStore.resolveId("zzzznotanid"))
    }

    @Test
    fun crxUrlTargetsGooglesUpdateService() {
        val url = ChromeWebStore.crxUrl("eimadpbcbfnmbkopoojfekhnkhdbieeh")
        assertTrue(url.startsWith("https://clients2.google.com/service/update2/crx"))
        assertTrue(url.contains("x=id%3Deimadpbcbfnmbkopoojfekhnkhdbieeh%26installsource%3Dondemand%26uc"))
        assertTrue(url.contains("acceptformat=crx2,crx3"))
    }

    @Test
    fun parsesSearchResultCards() {
        val html = """
            <div class="QAB7uc">
              <div class="Cb7Kte" data-item-id="eimadpbcbfnmbkopoojfekhnkhdbieeh">
                <a class="q6LNgd" href="./detail/dark-reader/eimadpbcbfnmbkopoojfekhnkhdbieeh"></a>
                <div class="R6nElb">
                  <div class="KB5Wr"><img src="https://lh3.googleusercontent.com/icon=s60"></div>
                  <div class="feN2Qe"><h2 class="CiI2if">Dark Reader</h2>
                    <p class="rQHEi"><span class="cJI8ee HtRvfe">darkreader.org/</span></p>
                  </div>
                </div>
              </div>
              <div class="Cb7Kte" data-item-id="mnjggcdmjocbbbhaepdhchncahnbgone">
                <a class="q6LNgd" href="./detail/sponsorblock/mnjggcdmjocbbbhaepdhchncahnbgone"></a>
                <div class="R6nElb">
                  <div class="KB5Wr"><img src="https://lh3.googleusercontent.com/icon2=s60"></div>
                  <div class="feN2Qe"><h2 class="CiI2if">SponsorBlock &amp; more</h2></div>
                </div>
              </div>
            </div>
        """.trimIndent()

        val results = ChromeWebStore.parseSearchHtml(html)
        assertEquals(2, results.size)
        assertEquals("eimadpbcbfnmbkopoojfekhnkhdbieeh", results[0].id)
        assertEquals("Dark Reader", results[0].name)
        assertEquals("darkreader.org/", results[0].publisher)
        assertEquals("https://lh3.googleusercontent.com/icon=s60", results[0].iconUrl)
        assertEquals("dark-reader", results[0].slug)
        assertEquals("SponsorBlock & more", results[1].name)
    }

    @Test
    fun searchParsingSkipsRowsWithoutARealId() {
        val html = """
            <div data-item-id="not-an-extension">
              <h2>Nope</h2>
            </div>
            <div data-item-id="eimadpbcbfnmbkopoojfekhnkhdbieeh">
              <h2>Dark Reader</h2>
            </div>
        """.trimIndent()

        val results = ChromeWebStore.parseSearchHtml(html)
        assertEquals(1, results.size)
        assertEquals("Dark Reader", results.single().name)
    }
}
