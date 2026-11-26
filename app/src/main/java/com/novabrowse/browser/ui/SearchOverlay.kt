package com.novabrowse.browser.ui

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowOutward
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novabrowse.browser.browser.UrlTools
import com.novabrowse.browser.ui.components.FaviconImage
import com.novabrowse.browser.ui.components.NovaChip
import kotlinx.coroutines.delay

/** Chrome's omnibox: a full-screen search surface with engine chips and suggestions. */
@Composable
fun SearchOverlay(vm: NovaViewModel) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val engines = vm.engines
    val suggestions = vm.suggestions
    val scheme = MaterialTheme.colorScheme

    LaunchedEffect(Unit) {
        delay(90)
        focusRequester.requestFocus()
        keyboard?.show()
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) vm.updateSearchText(spoken)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(scheme.background)
            .statusBarsPadding()
            .imePadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { vm.closeSearch() }, modifier = Modifier.size(44.dp)) {
                Icon(
                    Icons.Rounded.ArrowBack, "Close search",
                    tint = scheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
            Row(
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(scheme.surfaceVariant)
                    .padding(start = 14.dp, end = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Search, null,
                    tint = scheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    value = vm.searchText,
                    onValueChange = { vm.updateSearchText(it) },
                    singleLine = true,
                    textStyle = TextStyle(color = scheme.onSurface, fontSize = 16.sp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { vm.submitSearch() }),
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                    decorationBox = { inner ->
                        if (vm.searchText.isEmpty()) {
                            Text(
                                "Search ${vm.currentEngine.name} or type URL",
                                color = scheme.onSurfaceVariant,
                                fontSize = 15.sp
                            )
                        }
                        inner()
                    }
                )
                if (vm.searchText.isNotEmpty()) {
                    IconButton(onClick = { vm.updateSearchText("") }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.Close, "Clear", tint = scheme.onSurfaceVariant, modifier = Modifier.size(17.dp))
                    }
                }
                IconButton(
                    onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Search NovaBrowse")
                        }
                        runCatching { voiceLauncher.launch(intent) }
                            .onFailure { vm.toast("Voice search is not available on this device") }
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Rounded.Mic, "Voice search", tint = scheme.onSurfaceVariant, modifier = Modifier.size(19.dp))
                }
            }
        }

        LazyRow(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(engines, key = { it.id }) { engine ->
                NovaChip(
                    text = engine.name,
                    selected = engine.id == vm.currentEngine.id,
                    onClick = { vm.setSearchEngine(engine.id); vm.updateSearchText(vm.searchText) }
                )
            }
        }

        if (suggestions.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.TopCenter) {
                Text(
                    "Search the web, or type an address like novabrowse.app",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 28.dp)
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                items(suggestions, key = { it.url + it.title }) { suggestion ->
                    SuggestionRow(suggestion) {
                        vm.closeSearch()
                        vm.navigate(suggestion.url)
                    }
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, scheme.outline, RoundedCornerShape(20.dp))
                    .clickable {
                        val clip = vm.readClipboard()
                        if (clip.isNotBlank()) vm.updateSearchText(clip) else vm.toast("Clipboard is empty")
                    }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.ContentPaste, null,
                    modifier = Modifier.size(16.dp),
                    tint = scheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Text("Paste", style = MaterialTheme.typography.labelLarge, color = scheme.onSurface)
            }
            Spacer(Modifier.weight(1f))
            Row(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(scheme.primary)
                    .clickable { vm.submitSearch() }
                    .padding(horizontal = 20.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Search, null,
                    tint = scheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Go",
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun SuggestionRow(suggestion: Suggestion, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (suggestion.kind) {
            SuggestionKind.SHORTCUT -> FaviconImage(suggestion.url, size = 28.dp, corner = 7.dp)
            else -> Icon(
                when (suggestion.kind) {
                    SuggestionKind.HISTORY -> Icons.Rounded.History
                    SuggestionKind.BOOKMARK -> Icons.Rounded.Bookmark
                    else -> Icons.Rounded.Search
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                suggestion.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge
            )
            if (suggestion.subtitle.isNotBlank()) {
                Text(
                    suggestion.subtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            Icons.Rounded.ArrowOutward, null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}
