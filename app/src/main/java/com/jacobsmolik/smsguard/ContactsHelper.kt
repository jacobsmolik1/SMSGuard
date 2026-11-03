package com.jacobsmolik.smsguard

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

object ContactsHelper {
    fun isKnownContact(context: Context, number: String): Boolean {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(number)
        )
        val cursor = context.contentResolver.query(
            uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null
        ) ?: return false
        return cursor.use { it.count > 0 }
    }
}
