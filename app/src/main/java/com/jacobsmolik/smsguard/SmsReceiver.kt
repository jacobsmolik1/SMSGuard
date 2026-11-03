package com.jacobsmolik.smsguard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Telephony
import android.util.Log

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val sender = messages[0].displayOriginatingAddress ?: return
        val body = messages.joinToString("") { it.messageBody ?: "" }

        if (ContactsHelper.isKnownContact(context, sender)) return
        if (!FilterEngine.isMatch(context, body)) return

        Log.i("SMSGuard", "Intercepted political SMS from $sender")
        FilterLog.record(context, sender, body)

        // Query DB for the actual _id (more reliable than deleting by address)
        val id = findSmsId(context, sender, body)
        if (id != null) {
            ShizukuHelper.deleteSmsById(id)
        } else {
            // Fallback: delete all from this sender
            ShizukuHelper.deleteSmsFromNumber(sender)
        }
    }

    private fun findSmsId(context: Context, address: String, body: String): Long? {
        val cursor = context.contentResolver.query(
            Uri.parse("content://sms/inbox"),
            arrayOf("_id", "address", "body"),
            "address=? AND body=?",
            arrayOf(address, body),
            "date DESC"
        ) ?: return null
        return cursor.use {
            if (it.moveToFirst()) it.getLong(0) else null
        }
    }
}
