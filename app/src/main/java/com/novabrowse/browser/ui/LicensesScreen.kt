package com.novabrowse.browser.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.novabrowse.browser.BuildConfig
import com.novabrowse.browser.ui.components.GlassPanel
import com.novabrowse.browser.ui.components.NovaScreen

@Composable
fun LicensesScreen(vm: NovaViewModel) {
    NovaScreen(title = "Licences & privacy", onClose = { vm.closeScreen() }) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)
        ) {
            GlassPanel(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), elevation = 8.dp) {
                Column(Modifier.padding(16.dp)) {
                    Text("NovaBrowse privacy policy", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "NovaBrowse stores bookmarks, history, shortcuts, downloads and settings only on this device. " +
                            "There is no NovaBrowse account, no telemetry and no analytics SDK.\n\n" +
                            "• Browsing data stays in the app's private storage and is only used to power history, " +
                            "suggestions and the start page.\n" +
                            "• Address-bar suggestions are fetched from your chosen search engine only while the " +
                            "suggestion list is open, and can be turned off in Settings → General.\n" +
                            "• Tracking protection matches requests against a local host list. The list never leaves the device.\n" +
                            "• Private tabs use a separate WebView profile and are not written to history.\n" +
                            "• Private browsing does not hide your traffic from websites, your carrier or your network.\n" +
                            "• Extension packages you install run locally with only the permissions shown at install time.\n\n" +
                            "Clearing browsing data removes history, cookies, cache, site storage and saved site permissions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            GlassPanel(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), elevation = 8.dp) {
                Column(Modifier.padding(16.dp)) {
                    Text("Open-source licences", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "NovaBrowse ${BuildConfig.VERSION_NAME} is released under the MIT License.\n\n" +
                            "AndroidX Core, Activity, Lifecycle, Compose, Material 3 — Apache License 2.0\n" +
                            "AndroidX WebKit — Apache License 2.0\n" +
                            "Kotlin & Kotlin Coroutines — Apache License 2.0\n" +
                            "Coil image loader — Apache License 2.0\n" +
                            "CameraX — Apache License 2.0\n" +
                            "ML Kit Barcode Scanning — Google APIs Terms of Service\n" +
                            "Android System WebView / Chromium — BSD-style licences\n\n" +
                            "NovaBrowse is not affiliated with Apple or Google. Safari and Chrome are trademarks " +
                            "of their respective owners and are referenced only to describe design inspiration and " +
                            "extension compatibility.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            GlassPanel(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), elevation = 6.dp) {
                Column(Modifier.padding(16.dp)) {
                    Text("Project", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "github.com/woodjamesley-cyber/NovaBrowse",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                            .then(Modifier)
                    )
                    Text(
                        "BROWSE BEYOND.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}
