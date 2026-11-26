package com.novabrowse.browser.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/**
 * Small JSON-backed persisted list. NovaBrowse deliberately keeps its own lightweight
 * storage instead of a database: the data sets (bookmarks, history, downloads,
 * shortcuts) are small and this keeps startup fast.
 */
class PersistedList<T : Any>(
    context: Context,
    private val fileName: String,
    private val maxItems: Int = 5000,
    private val toJson: (T) -> JSONObject,
    private val fromJson: (JSONObject) -> T
) {
    private val file = File(context.filesDir, "$fileName.json")
    private val _items = MutableStateFlow<List<T>>(emptyList())
    val items: StateFlow<List<T>> = _items
    val value: List<T> get() = _items.value

    init { load() }

    private fun load() {
        if (!file.exists()) return
        runCatching {
            val arr = JSONArray(file.readText())
            val parsed = ArrayList<T>(arr.length())
            for (i in 0 until arr.length()) {
                arr.optJSONObject(i)?.let { parsed.add(fromJson(it)) }
            }
            _items.value = parsed
        }
    }

    fun replace(list: List<T>) {
        _items.value = list.take(maxItems)
        persist()
    }

    fun add(item: T) = replace(listOf(item) + _items.value)

    fun addAll(newItems: List<T>) = replace(newItems + _items.value)

    fun update(predicate: (T) -> Boolean, transform: (T) -> T) {
        _items.value = _items.value.map { if (predicate(it)) transform(it) else it }
        persist()
    }

    fun remove(predicate: (T) -> Boolean) {
        _items.value = _items.value.filterNot(predicate)
        persist()
    }

    fun clear() {
        _items.value = emptyList()
        file.delete()
    }

    fun persist() {
        runCatching {
            val arr = JSONArray()
            _items.value.forEach { arr.put(toJson(it)) }
            val tmp = File(file.parentFile, "${file.name}.tmp")
            FileOutputStream(tmp).use { it.write(arr.toString().toByteArray()) }
            if (file.exists()) file.delete()
            tmp.renameTo(file)
        }
    }

    fun exportJson(): String {
        val arr = JSONArray()
        _items.value.forEach { arr.put(toJson(it)) }
        return arr.toString(2)
    }

    fun importJson(json: String): Int {
        val arr = JSONArray(json)
        val parsed = ArrayList<T>()
        for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { parsed.add(fromJson(it)) }
        if (parsed.isNotEmpty()) replace(parsed + _items.value)
        return parsed.size
    }
}
