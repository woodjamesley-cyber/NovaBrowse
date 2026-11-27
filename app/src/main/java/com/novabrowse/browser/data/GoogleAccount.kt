package com.novabrowse.browser.data

import android.webkit.CookieManager

/**
 * Google account state for the browser profile.
 *
 * NovaBrowse deliberately does not implement Google OAuth or cloud sync: bookmarks, history and
 * settings stay on the device. What this supports is a real Google sign-in session inside the
 * browser, so google.com, YouTube and Gmail stay signed in, plus a clean way to sign out.
 */
object GoogleAccount {

    const val SIGN_IN_URL =
        "https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fwww.google.com%2F"

    private val HOSTS = listOf(
        "https://accounts.google.com",
        "https://www.google.com",
        "https://m.google.com",
        "https://mail.google.com",
        "https://drive.google.com",
        "https://www.youtube.com",
        "https://myaccount.google.com"
    )

    private val SESSION_COOKIES = listOf(
        "SID", "HSID", "SSID", "APISID", "SAPISID",
        "__Secure-1PSID", "__Secure-3PSID"
    )

    fun isSignedIn(): Boolean {
        val manager = CookieManager.getInstance()
        return HOSTS.take(2).any { host ->
            val cookies = manager.getCookie(host).orEmpty()
            cookies.isNotEmpty() && SESSION_COOKIES.any { name ->
                Regex("(^|;\\s*)" + Regex.escape(name) + "=").containsMatchIn(cookies)
            }
        }
    }

    /** Expires Google's session cookies for the browser profile. Other sites are untouched. */
    fun signOut() {
        val manager = CookieManager.getInstance()
        HOSTS.forEach { host ->
            val names = manager.getCookie(host).orEmpty()
                .split(';')
                .mapNotNull { part ->
                    part.trim().substringBefore('=', "").takeIf { it.isNotBlank() }
                }
                .distinct()
            names.forEach { name ->
                manager.setCookie(host, "$name=; Max-Age=0; Path=/")
                manager.setCookie(host, "$name=; Max-Age=0; Path=/; Domain=.google.com")
            }
        }
        manager.flush()
    }
}
