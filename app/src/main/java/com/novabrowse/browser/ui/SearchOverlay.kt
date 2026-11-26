package com.novabrowse.browser.ui

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novabrowse.browser.browser.UrlTools
import com.novabrowse.browser.ui.components.FaviconImage
import com.novabrowse.browser.ui.components.GlassIconButton
import com.novabrowse.browser.ui.components.GlassPanel
import com.novabrowse.browser.ui.components.NovaChip
import com.novabrowse.browser.ui.theme.LocalNovaStyle
import kotlinx.coroutines.delay

@Composable
fun SearchOverlay(vm: NovaViewModel) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val engines = vm.engines
    val suggestions = vm.suggestions
    val style = LocalNovaStyle.current

    LaunchedEffect(Unit) {
        delay(120)
        focusRequester.requestFocus()
        keyboard?.show()
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                vm.updateSearchText(spoken)
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .padding(horizontal = 14.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            GlassIconButton(onClick = { vm.closeSearch() }, size = 40.dp) {
                Icon(Icons.Rounded.ArrowBack, "Back", modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(8.dp))
            GlassPanel(
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(25.dp),
                strong = true,
                elevation = 12.dp
            ) {
                Row(
                    Modifier.fillMaxSize().padding(start = 14.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    BasicTextField(
                        value = vm.searchText,
                        onValueChange = { vm.updateSearchText(it) },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { vm.submitSearch() }),
                        modifier = Modifier.weight(1f).focusRequester(focusRequester),
                        decorationBox = { inner ->
                            if (vm.searchText.isEmpty()) {
                                Text("Search or enter website",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f),
                                    fontSize = 16.sp)
                            }
                            inner()
                        }
                    )
                    if (vm.searchText.isNotEmpty()) {
                        IconButton(onClick = { vm.updateSearchText("") }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Rounded.Close, "Clear", modifier = Modifier.size(16.dp))
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
                        modifier = Modifier.size(36.dp)
                    ) { Icon(Icons.Rounded.Mic, "Voice search", modifier = Modifier.size(18.dp)) }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(engines, key = { it.id }) { engine ->
                NovaChip(
                    text = engine.name,
                    selected = engine.id == vm.currentEngine.id,
                    onClick = { vm.setSearchEngine(engine.id); vm.updateSearchText(vm.searchText) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        if (suggestions.isNotEmpty()) {
            LazyColumn(Modifier.fillMaxWidth().weight(1f, fill = false)) {
                items(suggestions, key = { it.url + it.title }) { suggestion ->
                    SuggestionRow(suggestion) {
                        vm.closeSearch()
                        vm.navigate(suggestion.url)
                    }
                }
            }
        } else {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.TopCenter) {
                Text(
                    "Search the web, or type an address like novabrowse.app",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
        }

        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GlassPanel(
                modifier = Modifier.clip(CircleShape).clickable {
                    val clip = vm.readClipboard()
                    if (clip.isNotBlank()) vm.updateSearchText(clip) else vm.toast("Clipboard is empty")
                },
                shape = CircleShape,
                elevation = 4.dp
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.ContentPaste, null, modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(6.dp))
                    Text("Paste", style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(Modifier.weight(1f))
            GlassPanel(
                modifier = Modifier.clip(CircleShape).clickable { vm.submitSearch() },
                shape = CircleShape,
                elevation = 6.dp,
                tint = MaterialTheme.colorScheme.primary
            ) {
                Row(Modifier.padding(horizontal = 18.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Search, null, tint = Color.White, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Go", style = MaterialTheme.typography.labelLarge, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun SuggestionRow(suggestion: Suggestion, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (suggestion.kind) {
            SuggestionKind.SEARCH -> GlassPanel(Modifier.size(30.dp), shape = RoundedCornerShape(10.dp), elevation = 0.dp) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Search, null, modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.primary)
                }
            }
            SuggestionKind.HISTORY -> GlassPanel(Modifier.size(30.dp), shape = RoundedCornerShape(10.dp), elevation = 0.dp) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.History, null, modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
            SuggestionKind.BOOKMARK -> GlassPanel(Modifier.size(30.dp), shape = RoundedCornerShape(10.dp), elevation = 0.dp) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Bookmark, null, modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.secondary)
                }
            }
            SuggestionKind.SHORTCUT -> FaviconImage(suggestion.url, size = 30.dp, corner = 9.dp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                suggestion.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
            )
            Text(
                suggestion.subtitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            )
        }
        Icon(Icons.Rounded.Link, null, modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
    }
}
