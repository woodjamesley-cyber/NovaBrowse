package com.novabrowse.browser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.novabrowse.browser.extensions.StoreListing
import com.novabrowse.browser.ui.components.EmptyState
import com.novabrowse.browser.ui.components.NovaChip
import com.novabrowse.browser.ui.components.GlassIconButton
import com.novabrowse.browser.ui.components.NovaDialog
import com.novabrowse.browser.ui.components.NovaScreen
import com.novabrowse.browser.ui.components.SectionTitle
import java.io.File

/**
 * In-app Chrome Web Store: searches Google's store, installs the signed CRX directly and
 * lists featured extensions with one-tap installs.
 */
@Composable
fun ExtensionStoreScreen(vm: NovaViewModel) {
    val installed by vm.extensionsFlow.collectAsStateWithLifecycle()
    val installedIds = remember(installed) { installed.map { it.id }.toSet() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) { vm.loadStoreSections() }

    NovaScreen(
        title = "Chrome Web Store",
        onClose = { vm.closeScreen() },
        actions = {
            GlassIconButton(onClick = { vm.openStoreLinkDialog() }, size = 40.dp) {
                Icon(Icons.Rounded.Link, "Install from a link", modifier = Modifier.size(19.dp))
            }
        }
    ) {
        Column(Modifier.fillMaxSize()) {
            StoreSearchField(
                query = vm.storeQuery,
                onChange = vm::updateStoreQuery,
                onClear = vm::clearStoreSearch,
                onSubmit = { keyboard?.hide() }
            )
            Box(Modifier.fillMaxWidth().height(2.dp)) {
                if (vm.storeSearching) {
                    LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp))
                }
            }
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (vm.storeQuery.isBlank()) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(vm.storeCategories) { category ->
                                NovaChip(category.first, onClick = { vm.updateStoreQuery(category.second) })
                            }
                        }
                    }
                    item { SectionTitle("Featured extensions") }
                    items(ExtensionCatalog.entries, key = { it.id }) { entry ->
                        val catalogInstalled = installedIds.contains(entry.id)
                        StoreRow(
                            name = entry.name,
                            subtitle = entry.category + " · " + entry.summary,
                            iconUrl = null,
                            fallbackText = entry.name,
                            installed = catalogInstalled,
                            badge = entry.level,
                            onInstall = {
                                vm.installFromChromeWebStore(entry.id, entry.name)
                            }
                        )
                    }
                    if (vm.storeHomeLoading) {
                        item {
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "Loading more of the Chrome Web Store…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    vm.storeSections.forEach { section ->
                        item(key = "title-${section.query}") {
                            SectionTitle(
                                section.title,
                                action = "See all",
                                onAction = { vm.updateStoreQuery(section.query) }
                            )
                        }
                        item(key = "row-${section.query}") {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(section.listings, key = { it.id }) { listing ->
                                    StoreCard(
                                        listing = listing,
                                        installed = installedIds.contains(listing.id),
                                        onInstall = {
                                            vm.installFromChromeWebStore(listing.id, listing.name)
                                        }
                                    )
                                }
                            }
                        }
                    }
                    item { StoreNote(ExtensionCatalog.entries.firstOrNull { it.level != CatalogEntry.Compatibility.WORKS }?.status.orEmpty()) }
                } else {
                    val message = vm.storeMessage
                    if (!vm.storeSearching && vm.storeResults.isEmpty() && message != null) {
                        item {
                            EmptyState(
                                Icons.Rounded.Extension,
                                "Nothing found",
                                message,
                                Modifier.fillMaxWidth().padding(vertical = 24.dp)
                            )
                        }
                    }
                    if (vm.storeResults.isNotEmpty()) {
                        item { SectionTitle("Chrome Web Store results") }
                        items(vm.storeResults, key = { it.id }) { listing ->
                            StoreRow(
                                name = listing.name,
                                subtitle = listing.publisher.ifBlank { "chromewebstore.google.com" },
                                iconUrl = listing.iconUrl.takeIf { it.isNotBlank() },
                                fallbackText = listing.name,
                                installed = installedIds.contains(listing.id),
                                badge = ExtensionCatalog.find(listing.id)?.level,
                                onInstall = { vm.installFromChromeWebStore(listing.id, listing.name) }
                            )
                        }
                    }
                    item { StoreNote("") }
                }
            }
        }
    }
}

@Composable
private fun StoreNote(extra: String) {
    Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Text(
            "Packages come from Google's official Chrome Web Store download service, the same " +
                "signed CRX desktop Chrome installs. NovaBrowse runs them in its own chrome.* " +
                "runtime, so extensions that depend on desktop-only APIs are listed as " +
                "limited or unsupported instead of silently failing.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (extra.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                extra,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun StoreSearchField(
    query: String,
    onChange: (String) -> Unit,
    onClear: () -> Unit,
    onSubmit: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .height(48.dp)
            .padding(start = 16.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Rounded.Search, null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(19.dp)
        )
        Spacer(Modifier.width(12.dp))
        BasicTextField(
            value = query,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(
                        "Search the Chrome Web Store",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp
                    )
                }
                inner()
            }
        )
        if (query.isNotEmpty()) {
            IconButton(onClick = onClear, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Rounded.Close, "Clear search",
                    modifier = Modifier.size(17.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StoreRow(
    name: String,
    subtitle: String,
    iconUrl: String?,
    fallbackText: String,
    installed: Boolean,
    badge: CatalogEntry.Compatibility?,
    onInstall: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StoreIcon(iconUrl = iconUrl, fallbackText = fallbackText)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (badge != null) {
                Spacer(Modifier.height(6.dp))
                CompatibilityBadge(badge)
            }
        }
        Spacer(Modifier.width(10.dp))
        if (installed) {
            Text(
                "Installed",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
        } else {
            Text(
                "Install",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onInstall)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun StoreCard(
    listing: StoreListing,
    installed: Boolean,
    onInstall: () -> Unit
) {
    Column(
        Modifier
            .width(156.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        StoreIcon(
            iconUrl = listing.iconUrl.takeIf { it.isNotBlank() },
            fallbackText = listing.name
        )
        Spacer(Modifier.height(8.dp))
        Text(
            listing.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
            listing.publisher.ifBlank { "chromewebstore.google.com" },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(8.dp))
        if (installed) {
            Text(
                "Installed",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                "Install",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onInstall)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun StoreIcon(iconUrl: String?, fallbackText: String) {
    Box(
        Modifier.size(46.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF2F6BFF), Color(0xFF9B5CFF)))),
        contentAlignment = Alignment.Center
    ) {
        if (!iconUrl.isNullOrBlank()) {
            AsyncImage(
                model = iconUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                fallbackText.take(1).uppercase(),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

/** Installs an extension from a store listing URL or a bare 32-character id. */
@Composable
fun StoreLinkDialog(vm: NovaViewModel) {
    if (!vm.storeLinkDialog) return
    var value by remember { mutableStateOf("") }
    NovaDialog(onDismiss = { vm.closeStoreLinkDialog() }) {
        Text("Install from the Chrome Web Store", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            "Paste a Chrome Web Store link, or the 32-character id of any extension. " +
                "NovaBrowse downloads the signed package from Google, not from a third-party site.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .heightIn(min = 48.dp)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { vm.installFromStoreLink(value) }),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(
                            "chromewebstore.google.com/detail/…",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                    inner()
                }
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(
                "Cancel",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.clip(CircleShape)
                    .clickable { vm.closeStoreLinkDialog() }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Install",
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { vm.installFromStoreLink(value) }
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            )
        }
    }
}
