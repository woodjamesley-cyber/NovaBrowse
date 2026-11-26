package com.novabrowse.browser.browser

/**
 * Reader mode implemented with on-device DOM scoring. No page content leaves the device.
 * The script replaces the document with a clean typographic reading view; reloading the
 * page exits reader mode.
 */
object ReaderMode {

    fun script(theme: String, fontSize: Int): String {
        val bg = if (theme == "light") "#F7F8FC" else "#0A0F24"
        val fg = if (theme == "light") "#101528" else "#EEF2FF"
        val sub = if (theme == "light") "#59617A" else "#9AA6C9"
        return """
        (function() {
          try {
            var paras = document.querySelectorAll('article p, .post p, .entry-content p, .article-body p, main p, p');
            var best = null, bestScore = 0;
            var map = new Map();
            for (var i = 0; i < paras.length; i++) {
              var p = paras[i];
              if (!p.innerText || p.innerText.trim().length < 40) continue;
              p.querySelectorAll('script,style').forEach(function(n){ n.remove(); });
              var parent = p.parentElement;
              if (!parent) continue;
              var score = map.get(parent) || 0;
              score += p.innerText.trim().length;
              map.set(parent, score);
              if (score > bestScore) { bestScore = score; best = parent; }
            }
            if (!best || bestScore < 200) return 'NOVA_READER_FAILED';
            var title = (document.querySelector('h1') || {}).innerText || document.title;
            var nodes = best.querySelectorAll('h1,h2,h3,p,li,blockquote,pre,img,figure,figcaption');
            var out = '';
            nodes.forEach(function(n) {
              var tag = n.tagName.toLowerCase();
              if (tag === 'img') {
                if (n.src) out += '<img src="' + n.src + '" alt="">';
              } else if (tag === 'pre') {
                out += '<pre>' + n.innerText.replace(/</g,'&lt;') + '</pre>';
              } else if (tag === 'h1' || tag === 'h2' || tag === 'h3') {
                out += '<h2>' + n.innerText + '</h2>';
              } else if (tag === 'li') {
                out += '<li>' + n.innerText + '</li>';
              } else if (tag === 'blockquote') {
                out += '<blockquote>' + n.innerText + '</blockquote>';
              } else {
                out += '<p>' + n.innerText + '</p>';
              }
            });
            var css = 'html,body{margin:0;padding:0;background:$bg;color:$fg;}' +
              'body{font-family:-apple-system,Roboto,system-ui,sans-serif;font-size:${fontSize}%;line-height:1.75;padding:56px 22px 96px;max-width:720px;margin:0 auto;}' +
              'h1{font-size:1.9em;line-height:1.25;margin:0 0 6px;letter-spacing:-0.02em;}' +
              '.meta{color:$sub;font-size:.8em;margin-bottom:28px;letter-spacing:.04em;text-transform:uppercase;}' +
              'h2{font-size:1.25em;margin:28px 0 8px;}p{margin:0 0 18px;}img{max-width:100%;border-radius:14px;margin:10px 0;}' +
              'blockquote{margin:20px 0;padding:12px 18px;border-left:3px solid #4C7DFF;color:$sub;}' +
              'pre{background:rgba(127,127,127,.14);padding:14px;border-radius:12px;overflow:auto;font-size:.85em;}' +
              'li{margin:0 0 8px .4em;}a{color:#5B8CFF;}';
            document.documentElement.innerHTML = '';
            var head = document.createElement('head');
            var meta = document.createElement('meta');
            meta.name = 'viewport';
            meta.content = 'width=device-width, initial-scale=1';
            var style = document.createElement('style');
            style.textContent = css;
            head.appendChild(meta); head.appendChild(style);
            document.documentElement.appendChild(head);
            var body = document.createElement('body');
            body.innerHTML = '<h1>' + (title || document.title) + '</h1>' +
              '<div class="meta">' + location.hostname + ' · NovaBrowse Reader</div>' + out;
            document.documentElement.appendChild(body);
            return 'NOVA_READER_OK';
          } catch (e) { return 'NOVA_READER_FAILED'; }
        })();
        """.trimIndent()
    }

    const val EXIT = "location.reload();"
}
