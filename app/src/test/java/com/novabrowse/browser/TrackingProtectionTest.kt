package com.novabrowse.browser

import com.novabrowse.browser.browser.TrackingProtection
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackingProtectionTest {

    @Test
    fun `known tracker hosts match including subdomains`() {
        assertTrue(TrackingProtection.isTrackerHost("doubleclick.net"))
        assertTrue(TrackingProtection.isTrackerHost("stats.doubleclick.net"))
        assertTrue(TrackingProtection.isTrackerHost("www.google-analytics.com"))
        assertTrue(TrackingProtection.isTracker("https://www.google-analytics.com/collect?v=1"))
    }

    @Test
    fun `normal sites are not blocked`() {
        assertFalse(TrackingProtection.isTrackerHost("example.com"))
        assertFalse(TrackingProtection.isTrackerHost("github.com"))
        assertFalse(TrackingProtection.isTracker("https://github.com/torvalds/linux"))
        assertFalse(TrackingProtection.isTracker("https://news.ycombinator.com/item?id=1"))
    }

    @Test
    fun `non http traffic is ignored`() {
        assertFalse(TrackingProtection.isTracker("file:///android_asset/index.html"))
        assertFalse(TrackingProtection.isTracker("data:text/html,hello"))
    }
}
