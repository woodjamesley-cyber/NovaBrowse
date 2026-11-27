package com.novabrowse.browser.extensions

import android.content.Context
import android.net.Uri
import com.novabrowse.browser.data.PersistedList
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * Installs extension packages: directly from the Chrome Web Store, from a .crx or .zip the
 * user picked, or from a URL. Every package is unpacked with zip-slip protection and its
 * manifest is validated before anything is written to the managed extension directory.
 *
 * Chrome Web Store installs download the same signed CRX (CRX3) that desktop Chrome
 * receives from Google's update service, so no desktop browser is involved.
 */
class ExtensionStore(
    private val context: Context,
    private val records: PersistedList<ExtensionRecord>
) {
    val installed: StateFlow<List<ExtensionRecord>> = records.items
    val all: List<ExtensionRecord> get() = records.value

    fun root(): File = File(context.filesDir, "extensions").apply { mkdirs() }
    fun dirFor(id: String): File = File(root(), id)
    fun fileFor(id: String, relative: String): File = File(dirFor(id), relative)

    fun find(id: String): ExtensionRecord? = records.value.firstOrNull { it.id == id }

    fun installFromUri(uri: Uri, displayName: String? = null): Result<ExtensionRecord> = runCatching {
        val temp = File(context.cacheDir, "ext_import_${System.currentTimeMillis()}")
        temp.mkdirs()
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                extractPackage(input, temp)
            } ?: error("Could not read the selected file")
            finalise(temp, displayName ?: uri.lastPathSegment ?: "Imported extension")
        } finally {
            temp.deleteRecursively()
        }
    }

    fun installFromUrl(url: String): Result<ExtensionRecord> {
        val storeId = ChromeWebStore.resolveId(url)
        if (storeId != null && (url.contains("chromewebstore", true) || ChromeWebStore.isExtensionId(url))) {
            return installFromChromeWebStore(storeId)
        }
        return runCatching {
            val fileName = url.substringAfterLast('/').ifBlank { "extension.zip" }
            val temp = File(context.cacheDir, "ext_dl_${System.currentTimeMillis()}")
            temp.mkdirs()
            try {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 20_000
                    readTimeout = 30_000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", ChromeWebStore.USER_AGENT)
                }
                connection.connect()
                require(connection.responseCode in 200..299) { "Download failed (HTTP ${connection.responseCode})" }
                connection.inputStream.use { input -> extractPackage(input, temp) }
                connection.disconnect()
                finalise(temp, fileName)
            } finally {
                temp.deleteRecursively()
            }
        }
    }

    /**
     * Downloads an extension from the Chrome Web Store by its 32-character id and installs it.
     * [fallbackName] (from the store listing) is used when a package's manifest stores its name
     * behind `_locales` strings that are missing from the download.
     */
    fun installFromChromeWebStore(
        extensionId: String,
        fallbackName: String? = null
    ): Result<ExtensionRecord> = runCatching {
        require(ChromeWebStore.isExtensionId(extensionId)) {
            "“$extensionId” is not a Chrome Web Store extension id"
        }
        val temp = File(context.cacheDir, "ext_cws_${System.currentTimeMillis()}")
        temp.mkdirs()
        try {
            val connection = (URL(ChromeWebStore.crxUrl(extensionId)).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 90_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", ChromeWebStore.USER_AGENT)
                setRequestProperty("Accept", "application/x-chrome-extension,*/*")
            }
            connection.connect()
            val code = connection.responseCode
            val contentType = connection.contentType.orEmpty()
            try {
                require(code == HttpURLConnection.HTTP_OK) {
                    if (code == 204 || code == 404) {
                        "The Chrome Web Store has no package for this extension (HTTP $code). " +
                            "It may have been unpublished or removed from the store."
                    } else {
                        "Chrome Web Store download failed (HTTP $code)"
                    }
                }
                require(!contentType.contains("text/html", true)) {
                    "The Chrome Web Store returned a web page instead of an extension package"
                }
                connection.inputStream.use { input -> extractPackage(input, temp) }
            } finally {
                connection.disconnect()
            }
            finalise(temp, "Chrome Web Store", extensionId, fallbackName)
        } finally {
            temp.deleteRecursively()
        }
    }

    /** Loads the ZIP payload out of a CRX3 file (the 12-byte magic + header are skipped). */
    private fun extractPackage(input: java.io.InputStream, target: File) {
        val buffered = java.io.BufferedInputStream(input)
        buffered.mark(16)
        val magic = ByteArray(4)
        val read = buffered.read(magic)
        if (read == 4 && magic[0] == 'C'.code.toByte() && magic[1] == 'r'.code.toByte() &&
            magic[2] == '2'.code.toByte() && magic[3] == '4'.code.toByte()
        ) {
            // "Cr24", version (4 bytes LE), header length (4 bytes LE), then signed header data.
            val version = ByteArray(4).also { buffered.read(it) }
            val headerLength = ByteArray(4).also { buffered.read(it) }
            val len = (headerLength[0].toInt() and 0xFF) or
                ((headerLength[1].toInt() and 0xFF) shl 8) or
                ((headerLength[2].toInt() and 0xFF) shl 16) or
                ((headerLength[3].toInt() and 0xFF) shl 24)
            require(len in 0..10_000_000) { "Invalid CRX header" }
            var skipped = 0L
            while (skipped < len) {
                val s = buffered.skip(len - skipped)
                if (s <= 0) break
                skipped += s
            }
            ZipInputStream(buffered).use { zip -> unzip(zip, target) }
        } else {
            buffered.reset()
            val signature = ByteArray(2)
            val read = buffered.read(signature)
            require(read == 2 && signature[0] == 'P'.code.toByte() && signature[1] == 'K'.code.toByte()) {
                "That file is not an extension package (expected a .crx or .zip)"
            }
            buffered.reset()
            ZipInputStream(buffered).use { zip -> unzip(zip, target) }
        }
    }

    private fun unzip(zip: ZipInputStream, target: File) {
        val canonicalTarget = target.canonicalFile
        var entries = 0
        var totalBytes = 0L
        while (true) {
            val entry = zip.nextEntry ?: break
            val name = entry.name
            if (name.contains("..") || name.startsWith("/")) {
                zip.closeEntry()
                continue
            }
            val outFile = File(target, name)
            if (!outFile.canonicalPath.startsWith(canonicalTarget.path + File.separator)) {
                zip.closeEntry()
                continue
            }
            if (entry.isDirectory) {
                outFile.mkdirs()
            } else {
                outFile.parentFile?.mkdirs()
                FileOutputStream(outFile).use { out ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val n = zip.read(buffer)
                        if (n <= 0) break
                        totalBytes += n
                        require(totalBytes < 48L * 1024 * 1024) { "Extension package is larger than 48 MB" }
                        out.write(buffer, 0, n)
                    }
                }
            }
            entries++
            require(entries < 4000) { "Extension package has too many files" }
            zip.closeEntry()
        }
    }

    private fun finalise(
        unpacked: File,
        source: String,
        idOverride: String? = null,
        fallbackName: String? = null
    ): ExtensionRecord {
        val packageRoot = locateRoot(unpacked) ?: error("No manifest.json found in the package")
        val parsed = ExtensionManifestParser.parse(packageRoot, source, idOverride).getOrThrow()
        val existing = find(parsed.id)
        val dest = dirFor(parsed.id)
        if (dest.exists()) dest.deleteRecursively()
        dest.mkdirs()
        copyTree(packageRoot, dest)

        // Persist the extension icon inside the managed directory so it survives cache clears.
        val iconRelative = parsed.iconPath?.let { path ->
            val inPackage = File(path)
            if (inPackage.exists() && inPackage.canonicalPath.startsWith(packageRoot.canonicalPath)) {
                inPackage.relativeTo(packageRoot).path
            } else null
        }
        val record = parsed.copy(
            name = if (parsed.name.startsWith("__MSG_")) fallbackName ?: parsed.name else parsed.name,
            iconPath = iconRelative?.let { File(dest, it).absolutePath },
            enabled = existing?.enabled ?: true,
            installedAt = existing?.installedAt ?: System.currentTimeMillis(),
            source = source
        )
        if (existing != null) {
            records.update({ it.id == record.id }) { record }
        } else {
            records.add(record)
        }
        return record
    }

    private fun locateRoot(dir: File): File? {
        if (File(dir, "manifest.json").exists()) return dir
        val candidates = dir.listFiles()?.filter { it.isDirectory } ?: return null
        return candidates.firstOrNull { File(it, "manifest.json").exists() }
    }

    private fun copyTree(from: File, to: File) {
        from.listFiles()?.forEach { f ->
            val dest = File(to, f.name)
            if (f.isDirectory) {
                dest.mkdirs()
                copyTree(f, dest)
            } else {
                f.copyTo(dest, overwrite = true)
            }
        }
    }

    fun setEnabled(id: String, enabled: Boolean) {
        records.update({ it.id == id }) { it.copy(enabled = enabled) }
    }

    fun uninstall(id: String) {
        records.remove { it.id == id }
        dirFor(id).deleteRecursively()
    }

    fun readText(id: String, relative: String): String? =
        runCatching { File(dirFor(id), relative).takeIf { it.exists() }?.readText() }.getOrNull()

    /** Resolves an extension-relative resource without escaping the extension directory. */
    fun resourceFile(id: String, relative: String): File? = runCatching {
        val root = dirFor(id).canonicalFile
        if (!root.exists()) return null
        val file = File(root, relative.replace('/', File.separatorChar)).canonicalFile
        if (!file.canonicalPath.startsWith(root.path + File.separator)) null
        else file.takeIf { it.isFile }
    }.getOrNull()

    /** MIME type for extension resources served to background pages and popups. */
    fun mimeType(relative: String): String = when (relative.substringAfterLast('.', "").lowercase()) {
        "html", "htm" -> "text/html"
        "js", "mjs" -> "application/javascript"
        "css" -> "text/css"
        "json", "map" -> "application/json"
        "svg" -> "image/svg+xml"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "ico" -> "image/x-icon"
        "woff" -> "font/woff"
        "woff2" -> "font/woff2"
        "ttf" -> "font/ttf"
        "wasm" -> "application/wasm"
        "mp3" -> "audio/mpeg"
        "mp4" -> "video/mp4"
        "txt" -> "text/plain"
        else -> "application/octet-stream"
    }

    fun readManifest(id: String): JSONObject? =
        runCatching { JSONObject(File(dirFor(id), "manifest.json").readText()) }.getOrNull()

    /** Inlines extension resources so popups render inside a sandboxed data document. */
    fun popupHtml(id: String, popupPath: String): String? {
        val root = dirFor(id)
        val html = readText(id, popupPath) ?: return null
        val baseDir = File(root, popupPath).parentFile ?: root
        val builder = StringBuilder(html)
        val inlineScript = Regex("<script[^>]*src=[\"']([^\"']+)[\"'][^>]*>\\s*</script>")
        inlineScript.findAll(html).forEach { match ->
            val src = match.groupValues[1]
            val code = runCatching {
                val f = File(baseDir, src).canonicalFile
                if (!f.canonicalPath.startsWith(root.canonicalPath)) "" else f.readText()
            }.getOrDefault("")
            builder.append("<script>").append(code.replace("</script>", "<\\/script>")).append("</script>")
        }
        val inlineStyle = Regex("<link[^>]*rel=[\"']stylesheet[\"'][^>]*href=[\"']([^\"']+)[\"'][^>]*>")
        inlineStyle.findAll(html).forEach { match ->
            val href = match.groupValues[1]
            val css = readText(id, File(baseDir.relativeTo(root).path, href).path.replace("\\", "/")).orEmpty()
            builder.append("<style>").append(css).append("</style>")
        }
        return builder.toString()
    }
}
