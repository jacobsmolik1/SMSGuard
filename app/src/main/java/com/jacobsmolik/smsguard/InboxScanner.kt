package com.jacobsmolik.smsguard

import android.content.Context
import android.net.Uri
import android.util.Log

object InboxScanner {

    fun scan(context: Context): Int {
        val cursor = context.contentResolver.query(
            Uri.parse("content://sms/inbox"),
            arrayOf("_id", "address", "body"),
            null, null, null
        ) ?: return 0

        var deleted = 0
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(0)
                val address = it.getString(1) ?: continue
                val body = it.getString(2) ?: ""

                if (ContactsHelper.isKnownContact(context, address)) continue
                if (!FilterEngine.isMatch(context, body)) continue

                Log.i("SMSGuard", "Scan-deleting id=$id from $address")
                ShizukuHelper.deleteSmsById(id)
                FilterLog.record(context, address, body)
                deleted++
            }
        }
        return deleted
    }
}
