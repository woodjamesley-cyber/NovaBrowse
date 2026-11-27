# NovaBrowse — BROWSE BEYOND.

NovaBrowse is a native Android web browser with a Chrome-style interface, real
Chromium-powered page rendering, full tab/bookmark/download management, private browsing, and a
Chrome-style WebExtension runtime.

| | |
|---|---|
| **App name** | NovaBrowse |
| **Package** | `com.novabrowse.browser` |
| **Version** | 1.1.0 |
| **Min SDK** | 26 (Android 8.0) |
| **Target SDK** | 34 (Android 14) |
| **Engine** | Android System WebView (Chromium, updated by Google Play) |
| **UI** | Jetpack Compose + Material 3, Chrome-style top toolbar, tab strip and neutral surfaces |
| **Language** | Kotlin |
| **Licence** | MIT |

---

## Install the APK

1. Download `NovaBrowse-debug.apk` from the
   [latest GitHub Release](https://github.com/woodjamesley-cyber/NovaBrowse/releases/latest)
   (or from the **Actions → Build NovaBrowse → artifacts** page).
2. Allow installation from your browser/file manager when Android asks.
3. Open **NovaBrowse** — it starts on the Chrome-style new tab page.
4. Type in the address bar or tap a shortcut to browse.

The debug APK is signed with the standard Android debug key, so it installs on any device or
emulator without extra setup. It is a complete build: every feature listed below is compiled in.

Requirements: an Android device with a modern **Android System WebView**. NovaBrowse reports the
installed WebView version in **Settings → About NovaBrowse**.

---

## Feature tour

### Chrome-style interface
A single design system (`ui/theme`, `ui/components/Glass.kt`) drives every surface the way Chrome
does: one neutral surface tone, hairline dividers, 8–16 dp corners, one blue accent and no heavy
transparency, so pages stay readable and scrolling stays smooth. The layout follows Chrome Android:
a top tab strip with close buttons and a **+** button, then a navigation row with home, back,
forward, reload, the omnibox pill, the tab count and the three-dot menu. The overflow menu is a
compact vertical list, settings use Chrome's grouped list rows, and the new tab page uses a large
search pill with a shortcuts card. Light, dark and system themes, four accent colours, eight
wallpapers (including your own photo) and an optional bottom toolbar are available in Settings.

### Real browsing engine
- Android System WebView (Chromium) per tab, with JavaScript, HTML5, CSS, cookies, DOM storage,
  video/audio, file uploads, redirects, multiple windows, full-screen video and camera/mic.
- HTTPS enforced, mixed content blocked, SSL errors always cancelled (never bypassed), Safe
  Browsing enabled.
- Desktop-site mode, text zoom, reader mode, find-on-page, screenshot capture, translation.

### Smart address bar
Recognises URLs vs. searches (unit-tested), supports Google, Bing, DuckDuckGo, Brave, Startpage,
Ecosia, Qwant and unlimited custom `{query}` engines. Live suggestions combine history,
bookmarks, shortcuts and the engine's own suggestion endpoint (optional), with voice search,
paste, clear and copy.

### Tabs
One live WebView per tab (state survives switching). Chrome-style card switcher with captured
previews, grid/compact layouts, duplicate, reorder via selection actions, groups, private tabs,
recently-closed restore, close-all, and session persistence across launches.

### Extensions (chrome.* bridge)
A real extension runtime, not a mock: extensions install from the built-in Chrome Web Store or
from unpacked packages, are validated, stored and executed.

- **In-app Chrome Web Store**: search Google's store and install with one tap. NovaBrowse fetches
  the same signed CRX that desktop Chrome installs from Google's update service, keeps the store's
  32-character extension id (so `chrome.runtime.id` and extension URLs match Chrome), and also
  accepts a pasted listing link or id. No desktop browser or side-loading is involved.

- Content scripts run at `document_start` / `document_idle`, honouring `matches` and
  `exclude_matches`.
- Background scripts and MV3 service workers run in a dedicated hidden WebView per extension.
- A two-way `WebMessage` bridge implements `chrome.storage` (local/sync/session),
  `chrome.runtime` (messaging, manifest, URL, onMessage with `sendResponse`),
  `chrome.tabs` (query/get/create/update/remove/sendMessage), `chrome.bookmarks`,
  `chrome.notifications`, `chrome.contextMenus` (items appear in the NovaBrowse menu) and
  `chrome.scripting` (executeScript / insertCSS).
- Extension popups render in a sandboxed WebView with the same bridge.
- Every install shows a permission review before anything is enabled.

**What is honestly not possible** (and is reported in-app instead of faked):

| API / feature | Why |
|---|---|
| Blocking `webRequest` / `declarativeNetRequest` network rules | Android System WebView exposes no request-blocking extension API. NovaBrowse ships its own on-device tracker filter instead. |
| Extension pages on `chrome-extension://` URLs | Extension resources are served from a private https origin, so hard-coded `chrome-extension://` links inside an extension do not resolve. |
| `chrome.debugger`, `devtools_page`, `identity`, `sidePanel`, `offscreen`, `tabCapture`, `nativeMessaging` | Desktop-only Chrome surfaces. |
| MV3 service-worker cold starts and long sleeps | Background pages run while NovaBrowse is in memory. |
| `chrome.action` popup chrome UI, `chrome.commands` | Rendered as NovaBrowse dialogs / address-bar shortcuts. |
| Full CRX signature verification | CRX3 payloads can be imported, but the signature is not evaluated by a Google key chain. |

**Extensions → Chrome Web Store** searches the real store, lists featured extensions with a
compatibility verdict, and installs from a listing link or a 32-character id. The extension detail
sheet lists compatibility notes per installed package.

### Bookmarks, downloads, history, reading list
Bookmark editor with folders, search, HTML import/export (Netscape format). Downloads use Android's
`DownloadManager` (real transfers, notifications, open/share, public Downloads folder option);
Android has no pause API, so pause/resume restarts the transfer and the UI says so. History with
day grouping and deletion. Reading list for later.

### Private browsing
Separate WebView profile (`androidx.webkit` multi-profile) for private tabs, no history writes,
private session discarded when the last private tab closes, distinct violet accents and an
explicit note that private browsing does not hide traffic from sites, carriers or networks.

### Privacy & security controls
On-device tracker/ad filter (53 hosts) with a per-page "blocked" badge, third-party cookie control,
Do Not Track header, Safe Browsing, per-site camera/microphone/location prompts, permission manager,
and granular "clear browsing data" (history, cookies, cache, site storage, permissions, all).
NovaBrowse has no account, no telemetry, no analytics SDK and never uploads history.

### Premium extras
Reader mode (DOM scoring), desktop site, picture-in-picture for full-screen video, QR scanner
(CameraX + ML Kit), share, copy link, find in page, text size, screenshots, custom search engines,
tab groups, session restore, wallpaper upload, toolbar top/bottom placement.

---

## Project structure

```
app/src/main/java/com/novabrowse/browser/
├── MainActivity.kt              # edge-to-edge Compose host, intents, file chooser, PiP
├── NovaApp.kt / Nova.kt         # process-scoped services (tabs, extensions, downloads)
├── browser/                     # WebView tabs, clients, permissions, reader mode, filters
├── data/                        # settings, permission store, JSON persistence, models
├── downloads/                   # DownloadManager integration
├── extensions/                  # manifest parser, installer, chrome.* runtime, JS shim
└── ui/                          # Chrome-style design system, screens, view model
    ├── theme/                   # colours, typography, wallpapers, surface tokens
    ├── components/              # GlassPanel, favicons, rows, dialogs, shared chrome
    └── *Screen.kt               # home, browser, tabs, search, settings, bookmarks, …
app/src/test/java/…              # JVM unit tests for URL parsing and tracker filtering
.github/workflows/build-apk.yml  # CI: tests → debug + release APK → artifacts → release
tools/make_icons.py              # builds launcher icons from the brand artwork
```

---

## Build from source

### Prerequisites
- JDK 17
- Android SDK Platform 34 + Build-Tools 34.0.0
- Gradle wrapper is committed (`./gradlew`)

```bash
git clone https://github.com/woodjamesley-cyber/NovaBrowse.git
cd NovaBrowse
echo "sdk.dir=$HOME/Android/Sdk" > local.properties   # or set ANDROID_HOME
./gradlew testDebugUnitTest
./gradlew assembleDebug
# APK → app/build/outputs/apk/debug/app-debug.apk
```

### Release signing (optional)
Create `keystore.properties` in the repository root (git-ignored) or provide the four CI secrets:

```properties
storeFile=release.keystore
storePassword=…
keyAlias=…
keyPassword=…
```

For GitHub Actions add repository secrets: `KEYSTORE_BASE64` (base64 of the keystore),
`KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Never commit the keystore or passwords.

### CI
`.github/workflows/build-apk.yml` runs on push/PR/dispatch:

1. checkout → JDK 17 → Android SDK 34 → Gradle cache
2. `./gradlew testDebugUnitTest`
3. `./gradlew assembleDebug` (and `assembleRelease` when signing is configured)
4. uploads `NovaBrowse-debug.apk` as an artifact, plus the test report
5. publishes a GitHub Release (`continuous` tag for `main`, tag name for `v*` tags)

---

## Testing status

- `./gradlew testDebugUnitTest` covers URL/search detection, engine templating, URL display,
  localhost/IP handling and the tracker filter.
- The project is compiled with `assembleDebug` in CI for every push.
- A physical-device or emulator smoke test is not possible inside this build environment; see the
  release notes for the exact checks that were run.

## Limitations

- Page rendering quality and CSS support depend on the WebView version installed on the device;
  NovaBrowse does not bundle its own Chromium.
- Widevine L1/DRM playback, PDF printing and Chrome sync are not implemented.
- Translation opens Google Translate in a new tab rather than translating in place.
- Reader mode uses a heuristic extractor; very unusual page layouts may not extract cleanly.
- Extension support is a documented subset of the Chrome extension API (see table above).

## Branding

The NovaBrowse name, compass mark and "BROWSE BEYOND." tagline are original to this project.
Safari is a trademark of Apple Inc. and Chrome is a trademark of Google LLC; they are referenced
only to describe design inspiration and extension compatibility. No Apple or Google logo is used.

## Licence

MIT — see [LICENSE](LICENSE).
