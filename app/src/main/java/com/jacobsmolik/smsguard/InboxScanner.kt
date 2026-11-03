package com.jacobsmolik.smsguard

import android.content.Context
import android.provider.Telephony
import android.util.Log

object InboxScanner {

    fun scan(context: Context): Int {
        val cursor = context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
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

                val rows = context.contentResolver.delete(
                    Telephony.Sms.CONTENT_URI, "_id=?", arrayOf(id.toString())
                )
                Log.d("SMSGuard", "Scan delete id=$id: $rows row(s)")
                if (rows > 0) {
                    FilterLog.record(context, address, body)
                    deleted++
                }
            }
        }
        return deleted
    }
}
