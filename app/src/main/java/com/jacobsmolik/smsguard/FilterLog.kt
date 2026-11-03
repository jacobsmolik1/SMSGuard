package com.jacobsmolik.smsguard

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

object FilterLog {

    private const val PREFS_NAME = "smsguard_prefs"
    private const val KEY_LOG = "filter_log"
    private const val KEY_COUNT = "filter_count"
    private const val MAX_ENTRIES = 50
    private val DATE_FMT = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    data class Entry(val sender: String, val preview: String, val timestamp: String)

    fun record(context: Context, sender: String, body: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val count = prefs.getInt(KEY_COUNT, 0) + 1
        val existing = JSONArray(prefs.getString(KEY_LOG, "[]") ?: "[]")

        val entry = JSONObject().apply {
            put("sender", sender)
            put("preview", body.take(80))
            put("timestamp", DATE_FMT.format(Date()))
        }

        val updated = JSONArray().apply {
            put(entry)
            for (i in 0 until minOf(existing.length(), MAX_ENTRIES - 1)) put(existing.get(i))
        }

        prefs.edit()
            .putInt(KEY_COUNT, count)
            .putString(KEY_LOG, updated.toString())
            .apply()
    }

    fun getCount(context: Context): Int =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(KEY_COUNT, 0)

    fun getEntries(context: Context): List<Entry> {
        val log = JSONArray(
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LOG, "[]") ?: "[]"
        )
        return (0 until log.length()).map { i ->
            log.getJSONObject(i).let {
                Entry(it.getString("sender"), it.getString("preview"), it.getString("timestamp"))
            }
        }
    }
}
