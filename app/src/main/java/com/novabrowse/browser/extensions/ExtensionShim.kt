package com.novabrowse.browser.extensions

/**
 * The JavaScript half of NovaBrowse's extension runtime. It gives extension code the
 * familiar `chrome.*` surface and forwards every call to the native runtime, which
 * enforces permissions and answers using NovaBrowse's own tab, storage and bookmark APIs.
 */
object ExtensionShim {

    fun bootstrap(extId: String, extensionName: String, manifestJson: String = "{}"): String = """
    (function () {
      var EXT_ID = ${js(extId)};
      window.__novaExtShims = window.__novaExtShims || {};
      if (window.__novaExtShims[EXT_ID]) return;
      var EXT_NAME = ${js(extensionName)};
      var MANIFEST = {};
      try { MANIFEST = JSON.parse(${js(manifestJson)}); } catch (e) { MANIFEST = {}; }
      var pending = new Map();
      var seq = 0;
      var listeners = { message: [], connect: [], actionClicked: [], installed: [], startup: [] };

      function call(ns, method, args) {
        return new Promise(function (resolve, reject) {
          var id = ++seq;
          pending.set(id, { resolve: resolve, reject: reject });
          try {
            window.novaExt.postMessage(JSON.stringify({
              id: id, ns: ns, method: method, args: args || [], extId: EXT_ID
            }));
          } catch (e) {
            pending.delete(id);
            reject(new Error('NovaBrowse extension channel unavailable'));
          }
        });
      }
      window.__novaExtResolve = function (payload) {
        var msg;
        try { msg = typeof payload === 'string' ? JSON.parse(payload) : payload; } catch (e) { return; }
        var p = pending.get(msg.id);
        if (!p) return;
        pending.delete(msg.id);
        if (msg.error) p.reject(new Error(msg.error)); else p.resolve(msg.result);
      };
      function fire(kind, arg) {
        (listeners[kind] || []).forEach(function (fn) {
          try { fn(arg); } catch (e) { console.error('[NovaBrowse]', e); }
        });
      }
      window.__novaExtDispatch = function (kind, payload) {
        if (kind === 'message') {
          var respond = null;
          var sender = payload && payload.sender ? payload.sender : {};
          var msg = payload && payload.message;
          for (var i = 0; i < listeners.message.length; i++) {
            var fn = listeners.message[i];
            try {
              var res = fn(msg, sender, function (response) {
                if (respond) return;
                respond = true;
                window.novaExt.postMessage(JSON.stringify({
                  id: payload.callbackId, reply: response === undefined ? null : response, extId: EXT_ID
                }));
              });
              if (res === true) return true;
            } catch (e) { console.error('[NovaBrowse]', e); }
          }
          return false;
        }
        if (kind === 'actionClicked') fire('actionClicked', payload);
        if (kind === 'installed' || kind === 'startup') fire(kind, payload || {});
        return false;
      };

      var storageArea = function (area) {
        return {
          get: function (keys, cb) {
            var p = call('storage', 'get', [area, keys]);
            if (typeof keys === 'function') { p.then(keys); return; }
            if (cb) p.then(cb);
            return p;
          },
          set: function (items, cb) {
            var p = call('storage', 'set', [area, items]);
            if (cb) p.then(function () { cb(); });
            return p;
          },
          remove: function (keys, cb) {
            var p = call('storage', 'remove', [area, keys]);
            if (cb) p.then(function () { cb(); });
            return p;
          },
          clear: function (cb) {
            var p = call('storage', 'clear', [area]);
            if (cb) p.then(function () { cb(); });
            return p;
          }
        };
      };

      var chromeObj = {
        runtime: {
          id: EXT_ID,
          getManifest: function () { return MANIFEST; },
          getURL: function (path) { return 'https://extensions.novabrowse.local/' + EXT_ID + '/' + String(path || '').replace(/^\//, ''); },
          sendMessage: function () {
            var args = Array.prototype.slice.call(arguments);
            var cb = typeof args[args.length - 1] === 'function' ? args.pop() : null;
            var msg = args.length === 1 ? args[0] : args;
            var p = call('runtime', 'sendMessage', [msg]);
            if (cb) p.then(cb, function () { cb(undefined); });
            return p;
          },
          onMessage: {
            addListener: function (fn) { listeners.message.push(fn); },
            removeListener: function (fn) {
              var i = listeners.message.indexOf(fn);
              if (i >= 0) listeners.message.splice(i, 1);
            },
            hasListener: function (fn) { return listeners.message.indexOf(fn) >= 0; }
          },
          onInstalled: { addListener: function (fn) { listeners.installed.push(fn); } },
          onStartup: { addListener: function (fn) { listeners.startup.push(fn); } },
          lastError: null
        },
        storage: {
          local: storageArea('local'),
          sync: storageArea('sync'),
          session: storageArea('session'),
          onChanged: { addListener: function () {} }
        },
        tabs: {
          query: function (info, cb) {
            var p = call('tabs', 'query', [info || {}]);
            if (typeof info === 'function') p.then(info);
            if (cb) p.then(cb);
            return p;
          },
          get: function (id, cb) { var p = call('tabs', 'get', [id]); if (cb) p.then(cb); return p; },
          create: function (info, cb) { var p = call('tabs', 'create', [info || {}]); if (cb) p.then(cb); return p; },
          update: function (id, info, cb) {
            if (typeof info === 'function') { cb = info; info = {}; }
            var p = call('tabs', 'update', [id, info || {}]);
            if (cb) p.then(cb);
            return p;
          },
          remove: function (id, cb) { var p = call('tabs', 'remove', [id]); if (cb) p.then(cb); return p; },
          captureVisibleTab: function () {
            return Promise.reject(new Error('captureVisibleTab is not supported on NovaBrowse'));
          },
          sendMessage: function (id, msg, cb) {
            var p = call('tabs', 'sendMessage', [id, msg]);
            if (cb) p.then(cb);
            return p;
          },
          onUpdated: { addListener: function () {} },
          onActivated: { addListener: function () {} },
          onRemoved: { addListener: function () {} }
        },
        bookmarks: {
          search: function (q, cb) {
            var query = typeof q === 'string' ? { query: q } : (q || {});
            var p = call('bookmarks', 'search', [query]);
            if (cb) p.then(cb);
            return p;
          },
          getTree: function (cb) { var p = call('bookmarks', 'getTree', []); if (cb) p.then(cb); return p; },
          create: function (b, cb) { var p = call('bookmarks', 'create', [b || {}]); if (cb) p.then(cb); return p; },
          remove: function (id, cb) { var p = call('bookmarks', 'remove', [id]); if (cb) p.then(cb); return p; }
        },
        notifications: {
          create: function (id, options, cb) {
            if (typeof id === 'object') { cb = options; options = id; id = 'nova'; }
            var p = call('notifications', 'create', [id, options || {}]);
            if (cb) p.then(cb);
            return p;
          },
          clear: function (id, cb) { var p = call('notifications', 'clear', [id]); if (cb) p.then(cb); return p; }
        },
        contextMenus: {
          create: function (props, cb) { var p = call('contextMenus', 'create', [props || {}]); if (cb) p.then(cb); return p; },
          remove: function (id, cb) { var p = call('contextMenus', 'remove', [id]); if (cb) p.then(cb); return p; },
          removeAll: function (cb) { var p = call('contextMenus', 'removeAll', []); if (cb) p.then(cb); return p; },
          onClicked: { addListener: function (fn) { window.__novaExtMenuHandler = fn; } }
        },
        scripting: {
          executeScript: function (details) { return call('scripting', 'executeScript', [details || {}]); },
          insertCSS: function (details) { return call('scripting', 'insertCSS', [details || {}]); }
        },
        i18n: {
          getMessage: function (key) { return key; },
          getUILanguage: function () { return navigator.language || 'en'; }
        },
        permissions: {
          contains: function (perms, cb) { if (cb) cb(true); return Promise.resolve(true); },
          request: function (perms, cb) { if (cb) cb(true); return Promise.resolve(true); }
        },
        action: {
          setTitle: function (details) { return call('runtime', 'setTitle', [details || {}]); },
          setBadgeText: function (details) { return call('runtime', 'setBadgeText', [details || {}]); },
          onClicked: { addListener: function (fn) { listeners.actionClicked.push(fn); } }
        },
        windows: {
          getCurrent: function (cb) {
            var p = Promise.resolve({ id: 1, focused: true });
            if (cb) p.then(cb);
            return p;
          }
        }
      };
      chromeObj.browserAction = chromeObj.action;
      window.__novaExtShims[EXT_ID] = chromeObj;
      if (!window.chrome || !window.chrome.runtime) {
        window.chrome = chromeObj;
        window.browser = chromeObj;
      }
      window.NOVA_EXTENSION = { id: EXT_ID, name: EXT_NAME };
    })();
    """.trimIndent()

    /**
     * Wraps a content script so it only executes on pages matching the manifest's
     * match patterns (WebView's document-start API can only filter by origin).
     */
    fun contentScript(
        script: String,
        matches: List<String>,
        exclude: List<String>,
        extId: String = ""
    ): String {
        val matchesJson = matches.joinToString(",") { js(it) }
        val excludeJson = exclude.joinToString(",") { js(it) }
        // Pages can host several extensions, so each script rebinds chrome.* to its own shim.
        val bind = if (extId.isBlank()) "" else
            "if (window.__novaExtShims)" +
                "{var own=window.__novaExtShims[${js(extId)}];if(own)window.chrome=own;}"
        return """
        (function () {
          $bind
          function patternToRegex(p) {
            if (p === '<all_urls>') return /^https?:\\/\\/.*/;
            var out = '^';
            for (var i = 0; i < p.length; i++) {
              var c = p[i];
              if (p.substr(i, 3) === '://') { out += '://'; i += 2; continue; }
              if (c === '*') {
                if (p[i + 1] === '.') { out += '(?:[^/]+\\.)?'; i += 1; } else { out += '.*'; }
              } else if ('.+?^$()|[]{}\\'.indexOf(c) >= 0) { out += '\\' + c; }
              else { out += c; }
            }
            return new RegExp(out + '${'$'}');
          }
          var url = location.href;
          var inc = [$matchesJson].map(patternToRegex);
          var exc = [$excludeJson].map(patternToRegex);
          var allowed = inc.length === 0 || inc.some(function (r) { return r.test(url); });
          if (exc.some(function (r) { return r.test(url); })) allowed = false;
          if (!allowed) return;
          try {
            (function () {
              $script
            })();
          } catch (e) { console.error('[NovaBrowse extension]', e); }
        })();
        """.trimIndent()
    }

    fun cssInjection(css: String, matches: List<String>, exclude: List<String>): String = """
    (function () {
      var style = document.createElement('style');
      style.setAttribute('data-novabrowse-extension', 'true');
      style.textContent = ${js(css)};
      (document.head || document.documentElement).appendChild(style);
    })();
    """.trimIndent()

    fun eventDispatch(kind: String, payloadJson: String): String =
        "window.__novaExtDispatch && window.__novaExtDispatch(${js(kind)}, $payloadJson);"

    private fun js(value: String): String {
        val sb = StringBuilder("\"")
        value.forEach { c ->
            when (c) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (c.code < 32) sb.append("\\u%04x".format(c.code)) else sb.append(c)
            }
        }
        sb.append("\"")
        return sb.toString()
    }
}
