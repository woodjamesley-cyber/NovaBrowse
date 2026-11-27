package com.novabrowse.browser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novabrowse.browser.ui.components.ChromeCard
import com.novabrowse.browser.ui.components.HairLine
import com.novabrowse.browser.ui.components.NovaScreen
import com.novabrowse.browser.ui.components.SectionTitle
import com.novabrowse.browser.ui.components.SettingRow

@Composable
fun AccountScreen(vm: NovaViewModel) {
    NovaScreen(title = "Account", onClose = { vm.closeScreen() }) {
        AccountContent(vm)
    }
}

/** Account page body, shown from the home avatar and from Settings → Account. */
@Composable
fun AccountContent(vm: NovaViewModel) {
    LaunchedEffect(Unit) { vm.refreshGoogleSession() }
    val signedIn = vm.googleSignedIn

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        if (signedIn) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.AccountCircle, null,
                    tint = if (signedIn) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(38.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (signedIn) "Signed in to Google" else "Not signed in",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal)
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    if (signedIn) {
                        "Your Google session is kept in this browser, so Gmail, YouTube and " +
                            "google.com stay signed in."
                    } else {
                        "Sign in with Google to use your Google account inside NovaBrowse."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionTitle("Google")
        ChromeCard {
            if (signedIn) {
                SettingRow(
                    Icons.Rounded.CheckCircle,
                    "Google session active",
                    "Passwords, passkeys and account security stay with Google"
                )
                HairLine()
                SettingRow(
                    Icons.Rounded.OpenInNew,
                    "Manage Google account",
                    "Opens myaccount.google.com in a new tab"
                ) { vm.openGoogleAccount() }
                HairLine()
                SettingRow(
                    Icons.Rounded.Logout,
                    "Sign out of Google",
                    "Clears Google's session cookies from this browser"
                ) { vm.signOutOfGoogle() }
            } else {
                SettingRow(
                    Icons.Rounded.Login,
                    "Sign in with Google",
                    "Opens Google's sign-in page in a new tab"
                ) { vm.signInWithGoogle() }
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            "The sign-in happens on Google's own page, inside a normal browser tab. NovaBrowse " +
                "never sees or stores your password, and it keeps the session cookie the way any " +
                "browser does. If you use a Google account on a shared device, sign out here when " +
                "you are done.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(22.dp))
        SectionTitle("Browser data")
        ChromeCard {
            SettingRow(
                Icons.Rounded.CloudOff,
                "No cloud sync",
                "Bookmarks, history, open tabs and settings are stored only on this device. " +
                    "NovaBrowse has no sync service and uploads nothing to your Google account."
            )
            HairLine()
            SettingRow(
                Icons.Rounded.AccountCircle,
                "Local profile",
                "${vm.bookmarksFlow.value.size} bookmarks · ${vm.historyFlow.value.size} history entries"
            )
        }

        Spacer(Modifier.height(26.dp))
        Text(
            "NovaBrowse is not affiliated with Google. Signing in here uses Google's standard " +
                "web sign-in; it does not create a Google account or grant NovaBrowse access to " +
                "your Google data.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
        Spacer(Modifier.height(40.dp))
    }
}

/** Small avatar used in the new tab header. */
@Composable
fun AccountAvatar(vm: NovaViewModel, size: Int = 30) {
    val signedIn = vm.googleSignedIn
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(
                if (signedIn) MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable { vm.openScreen(NovaScreen.ACCOUNT) },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Rounded.AccountCircle, "Account",
            tint = if (signedIn) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size((size - 6).dp)
                .padding(0.dp)
        )
    }
}
