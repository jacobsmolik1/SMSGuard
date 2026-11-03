package com.jacobsmolik.smsguard

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION &&
            action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val sender = messages[0].displayOriginatingAddress ?: return
        val body = messages.joinToString("") { it.messageBody ?: "" }
        val timestamp = messages[0].timestampMillis
        val isDefault = isDefault(context)

        val isPolitical = !ContactsHelper.isKnownContact(context, sender) &&
                FilterEngine.isMatch(context, body)

        if (isPolitical) {
            Log.i("SMSGuard", "Blocking political SMS from $sender (isDefault=$isDefault)")
            FilterLog.record(context, sender, body)
            if (isDefault) {
                abortBroadcast()  // stop it reaching other receivers
                // Don't write to DB → message never exists
            } else {
                // Best-effort delete after storage (works if we ARE the default app)
                tryDeleteFromInbox(context, sender, body)
            }
            return
        }

        // Pass-through: if we're the default app we must write it to the DB ourselves
        if (isDefault) {
            writeToInbox(context, sender, body, timestamp)
        }
    }

    private fun isDefault(context: Context) =
        Telephony.Sms.getDefaultSmsPackage(context) == context.packageName

    private fun writeToInbox(context: Context, sender: String, body: String, timestamp: Long) {
        try {
            context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, ContentValues().apply {
                put(Telephony.Sms.ADDRESS, sender)
                put(Telephony.Sms.BODY, body)
                put(Telephony.Sms.DATE, timestamp)
                put(Telephony.Sms.DATE_SENT, timestamp)
                put(Telephony.Sms.READ, 0)
                put(Telephony.Sms.SEEN, 0)
                put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
            })
        } catch (e: Exception) {
            Log.e("SMSGuard", "writeToInbox failed", e)
        }
    }

    private fun tryDeleteFromInbox(context: Context, sender: String, body: String) {
        try {
            val cursor = context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf("_id"), "address=? AND body=?",
                arrayOf(sender, body), "date DESC"
            ) ?: return
            val id = cursor.use { if (it.moveToFirst()) it.getLong(0) else return }
            context.contentResolver.delete(Telephony.Sms.CONTENT_URI, "_id=?", arrayOf(id.toString()))
        } catch (e: Exception) {
            Log.e("SMSGuard", "tryDeleteFromInbox failed", e)
        }
    }
}
