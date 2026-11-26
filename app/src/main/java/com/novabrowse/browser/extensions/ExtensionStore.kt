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
 * Installs unpacked extension packages (.zip, or the ZIP payload of a .crx) with
 * zip-slip protection and explicit manifest validation.
 *
 * NovaBrowse does not install directly from the Chrome Web Store: the store's install
 * flow requires desktop Chrome and Google's CRX signing service. Users can export a
 * package from the store page's source project or GitHub release and import it here.
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

    fun installFromUrl(url: String): Result<ExtensionRecord> = runCatching {
        val fileName = url.substringAfterLast('/').ifBlank { "extension.zip" }
        val temp = File(context.cacheDir, "ext_dl_${System.currentTimeMillis()}")
        temp.mkdirs()
        try {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 30_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "NovaBrowse/1.0 (Android)")
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

    private fun finalise(unpacked: File, source: String): ExtensionRecord {
        val packageRoot = locateRoot(unpacked) ?: error("No manifest.json found in the package")
        val parsed = ExtensionManifestParser.parse(packageRoot, source).getOrThrow()
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
