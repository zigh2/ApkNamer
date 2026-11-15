package io.github.apknamer.data.rename

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class RenameRecord(val from: String, val to: String)

/** Persists the most recent rename batches so they can be undone, even after the app restarts. */
class RenameHistory(private val store: File, private val maxBatches: Int = 20) {
    private val mutex = Mutex()

    suspend fun push(records: List<RenameRecord>) = mutex.withLock {
        withContext(Dispatchers.IO) {
            val all = load()
            all.add(records)
            save(all.takeLast(maxBatches))
        }
    }

    suspend fun pop(): List<RenameRecord>? = mutex.withLock {
        withContext(Dispatchers.IO) {
            val all = load()
            val last = all.removeLastOrNull()
            save(all)
            last
        }
    }

    suspend fun hasBatches(): Boolean = mutex.withLock {
        withContext(Dispatchers.IO) { load().isNotEmpty() }
    }

    private fun load(): MutableList<List<RenameRecord>> {
        if (!store.exists()) return mutableListOf()
        return try {
            val array = JSONArray(store.readText())
            MutableList(array.length()) { i ->
                val batch = array.getJSONArray(i)
                List(batch.length()) { j ->
                    val o = batch.getJSONObject(j)
                    RenameRecord(o.getString("from"), o.getString("to"))
                }
            }
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    private fun save(batches: List<List<RenameRecord>>) {
        val array = JSONArray()
        batches.forEach { batch ->
            val inner = JSONArray()
            batch.forEach { inner.put(JSONObject().put("from", it.from).put("to", it.to)) }
            array.put(inner)
        }
        store.writeText(array.toString())
    }
}
