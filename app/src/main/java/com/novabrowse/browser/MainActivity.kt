package com.novabrowse.browser

import android.app.PictureInPictureParams
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.novabrowse.browser.browser.FileChooserBridge
import com.novabrowse.browser.browser.UrlTools
import com.novabrowse.browser.ui.NovaRoot
import com.novabrowse.browser.ui.NovaViewModel
import com.novabrowse.browser.ui.theme.NovaTheme

class MainActivity : ComponentActivity() {

    private val vm: NovaViewModel by viewModels()
    private var fileCallback: ValueCallback<Array<Uri>>? = null

    private val filePicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val callback = fileCallback
        fileCallback = null
        if (callback != null) {
            callback.onReceiveValue(
                WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Nova.init(applicationContext)

        FileChooserBridge.launcher = { callback, params ->
            fileCallback?.onReceiveValue(null)
            fileCallback = callback
            runCatching {
                filePicker.launch(params.createIntent())
                true
            }.getOrElse {
                fileCallback = null
                false
            }
        }

        setContent {
            val settings by vm.settingsFlow.collectAsState()
            NovaTheme(settings) {
                NovaRoot(vm, onExit = { moveTaskToBack(true) })
            }
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return
        val engine = Nova.settings.currentEngine()
        when (intent.action) {
            Intent.ACTION_VIEW -> intent.data?.toString()?.let { vm.navigate(it) }
            Intent.ACTION_SEND -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
                vm.navigate(
                    if (UrlTools.isLikelyUrl(text)) UrlTools.toUrl(text, engine)
                    else UrlTools.searchUrl(text, engine)
                )
            }
            Intent.ACTION_WEB_SEARCH -> {
                val query = intent.getStringExtra(android.app.SearchManager.QUERY) ?: return
                vm.navigate(UrlTools.toUrl(query, engine))
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val tab = Nova.tabs.activeTab()
            if (tab?.fullscreenView != null) {
                runCatching {
                    enterPictureInPictureMode(PictureInPictureParams.Builder().build())
                }
            }
        }
    }
}
