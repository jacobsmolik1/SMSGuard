package com.jacobsmolik.smsguard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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

        Log.i("SMSGuard", "Blocking political SMS from unknown: $sender")
        FilterLog.record(context, sender, body)
        ShizukuHelper.deleteSmsFromNumber(sender)
    }
}
