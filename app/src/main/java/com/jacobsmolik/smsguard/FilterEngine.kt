package com.jacobsmolik.smsguard

import android.content.Context

object FilterEngine {

    private val DEFAULT_KEYWORDS = listOf(
        "vote", "voting", "voter", "ballot", "election", "elect",
        "candidate", "campaign", "republican", "democrat",
        "gop", "dnc", "rnc", "congress", "senate", "senator",
        "governor", "mayor", "running for office",
        "donate", "donation", "contribution", "fundrais",
        "gotv", "get out the vote", "polling place", "poll worker",
        "re-elect", "reelect", "primary election", "runoff election",
        "fight back", "take action", "help us win", "support our candidate",
        "liberal", "conservative", "progressive", "maga",
        "swing state", "absentee ballot", "mail-in ballot", "voter registration",
        "text stop to stop", "paid for by", "authorized by"
    )

    private const val PREFS_NAME = "smsguard_prefs"
    private const val KEY_CUSTOM = "custom_keywords"
    private const val KEY_DISABLED = "disabled_keywords"

    fun getKeywords(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val custom = prefs.getStringSet(KEY_CUSTOM, emptySet()) ?: emptySet()
        val disabled = prefs.getStringSet(KEY_DISABLED, emptySet()) ?: emptySet()
        return (DEFAULT_KEYWORDS.filter { it !in disabled } + custom).distinct()
    }

    fun isMatch(context: Context, body: String): Boolean {
        val lower = body.lowercase()
        return getKeywords(context).any { lower.contains(it.lowercase()) }
    }

    fun addKeyword(context: Context, keyword: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_CUSTOM, mutableSetOf())!!.toMutableSet()
        current.add(keyword.lowercase().trim())
        prefs.edit().putStringSet(KEY_CUSTOM, current).apply()
    }

    fun removeKeyword(context: Context, keyword: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val custom = prefs.getStringSet(KEY_CUSTOM, mutableSetOf())!!.toMutableSet()
        if (custom.remove(keyword)) {
            prefs.edit().putStringSet(KEY_CUSTOM, custom).apply()
            return
        }
        if (DEFAULT_KEYWORDS.contains(keyword)) {
            val disabled = prefs.getStringSet(KEY_DISABLED, mutableSetOf())!!.toMutableSet()
            disabled.add(keyword)
            prefs.edit().putStringSet(KEY_DISABLED, disabled).apply()
        }
    }
}
