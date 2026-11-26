package com.novabrowse.browser.browser

/**
 * Light-weight, local tracker/ad host filter. NovaBrowse never sends the browsing
 * URL anywhere to make this decision: matching happens on device.
 */
object TrackingProtection {

    private val trackerHosts = setOf(
        "doubleclick.net",
        "google-analytics.com",
        "analytics.google.com",
        "googletagmanager.com",
        "googletagservices.com",
        "adservice.google.com",
        "pagead2.googlesyndication.com",
        "connect.facebook.net",
        "graph.facebook.com",
        "scorecardresearch.com",
        "quantserve.com",
        "adsrvr.org",
        "adnxs.com",
        "rubiconproject.com",
        "pubmatic.com",
        "openx.net",
        "casalemedia.com",
        "criteo.com",
        "criteo.net",
        "taboola.com",
        "outbrain.com",
        "moatads.com",
        "amazon-adsystem.com",
        "smartadserver.com",
        "hotjar.com",
        "mixpanel.com",
        "segment.io",
        "segment.com",
        "amplitude.com",
        "branch.io",
        "appsflyer.com",
        "adjust.com",
        "newrelic.com",
        "bugsnag.com",
        "sentry.io",
        "fullstory.com",
        "mouseflow.com",
        "crazyegg.com",
        "optimizely.com",
        "vwo.com",
        "matomo.cloud",
        "yandex.ru/metrika",
        "mc.yandex.ru",
        "bat.bing.com",
        "clarity.ms",
        "ads.linkedin.com",
        "px.ads.linkedin.com",
        "analytics.tiktok.com",
        "analytics.twitter.com",
        "static.ads-twitter.com",
        "tr.snapchat.com"
    )

    /** Ad-server path fragments that are blocked even on first-party CDNs. */
    private val adPaths = listOf(
        "/pagead/", "/adsbygoogle", "/adserver/", "/advert/", "/banner_ad", "/popunder"
    )

    fun hostOf(url: String): String = runCatching {
        java.net.URI(url).host?.lowercase() ?: ""
    }.getOrDefault("")

    fun isTrackerHost(host: String): Boolean {
        val h = host.lowercase().trim('.')
        if (h.isEmpty()) return false
        return trackerHosts.any { blocked -> h == blocked || h.endsWith(".$blocked") }
    }

    fun isTracker(url: String): Boolean {
        if (!url.startsWith("http", ignoreCase = true)) return false
        if (isTrackerHost(hostOf(url))) return true
        val path = runCatching { java.net.URI(url).path?.lowercase() ?: "" }.getOrDefault("")
        return adPaths.any { path.contains(it) } && !path.endsWith(".html") && !path.endsWith(".css")
    }

    fun count(): Int = trackerHosts.size
}
