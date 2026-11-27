# Changelog

## 1.3.0 — Samsung-style home, bigger store, account

- **One search field on the home page**: the toolbar no longer draws a second omnibox while the
  start page is open, so the new tab shows a single large search bar (Samsung Internet style)
  above the shortcut grid.
- **Bigger Chrome Web Store**: the store now loads live category sections (productivity, privacy,
  ad blockers, shopping, developer tools, music, photos, news) with horizontal rows of installable
  extensions, category chips for one-tap browsing, and up to 40 results per search instead of 20.
- **Account section**: new `Account` screen (home avatar, Settings → Account) with Google sign-in.
  Signing in uses Google's own sign-in page in a tab and keeps the session in the browser; the
  screen detects an active Google session and can sign out by clearing Google's cookies. There is
  no cloud sync — bookmarks, history and settings stay on the device, and the screen says so.
- Version bumped to 1.3.0 (versionCode 4) and published as the `continuous` release build.

## 1.2.0 — Chrome Web Store

- New **in-app Chrome Web Store** (`Extensions → Chrome Web Store`): search Google's store from
  inside the browser and install an extension with one tap, instead of exporting a package from
  another app or downloading a ZIP somewhere else.
- Store installs use Google's official CRX update service, so the package is the same signed build
  desktop Chrome receives. The store's 32-character id is kept, and any listing link or bare id can
  be pasted in to install an extension that is not in the featured list.
- Installed manifests now resolve `__MSG_*__` names and descriptions from `_locales`, so real store
  extensions show their proper name instead of a placeholder key.
- Extension engines now serve each extension's files at its own origin, which makes
  `chrome.runtime.getURL`, popup stylesheets/images and MV3 **module** service workers load;
  `chrome.runtime.getManifest()` is synchronous and `onInstalled` / `onStartup` fire.
- Featured store list rebuilt around extensions that actually install and run (Dark Reader,
  SponsorBlock, Video Speed Controller, JSON Formatter, Refined GitHub, Stylus), with the
  network-blocking extensions marked as unsupported rather than promised.
- Version bumped to 1.2.0 (versionCode 3) and published as the `continuous` release build.

## 1.1.0 — Chrome-style interface

- New **Chrome-style layout**: top tab strip with close buttons and a **+** button, then a
  navigation row with home, back, forward, reload, the omnibox pill, the tab counter and the
  three-dot overflow menu.
- New **Chrome overflow menu**: compact vertical list with grouped actions and dividers instead of
  the previous icon grid.
- New **Chrome new tab page**: large search pill, shortcuts inside a grouped card, and flat
  favourites, history and recently-closed sections.
- New **Chrome settings**: back-arrow app bar, working settings search, grouped category rows and
  Chrome-style section headers, switches and chips.
- **Smoother motion**: all overlays now use short (120–220 ms) fast-out-slow-in fades and slides
  anchored with `graphicsLayer`-friendly transforms; the perpetual sheen animation, full-screen
  gradient backdrops and heavy shadows were removed, so panel draws are flat and cheap.
- Flat material surfaces replace translucent glass panels for readability and frame-rate stability.
- Tab switcher cards, tab strip chips and quick-action pills restyled to match Chrome.
- Version bumped to 1.1.0 (versionCode 2).

## 1.0.0 — First release

- Initial NovaBrowse release: Android System WebView browsing engine, tabs, bookmarks, downloads,
  history, private browsing, tracker filter, reader mode, QR scanner and the chrome.* extension
  runtime.
