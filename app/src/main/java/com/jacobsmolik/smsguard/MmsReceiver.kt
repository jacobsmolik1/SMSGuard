package com.jacobsmolik.smsguard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log

class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION) return
        val data = intent.getByteArrayExtra("data") ?: return
        val locationUrl = extractMmsUrl(data) ?: run {
            Log.w("SMSGuard", "Could not extract MMS URL from WAP_PUSH")
            return
        }
        try {
            @Suppress("DEPRECATION")
            SmsManager.getDefault().downloadMultimediaMessage(context, locationUrl, null, null, null)
        } catch (e: Exception) {
            Log.e("SMSGuard", "MMS download failed", e)
        }
    }

    // Extract the content-location URL from a WAP Binary-encoded MMS notification PDU
    private fun extractMmsUrl(pdu: ByteArray): String? {
        return try {
            val raw = String(pdu, Charsets.ISO_8859_1)
            val start = raw.indexOfFirst { it == 'h' && raw.startsWith("http", raw.indexOf(it)) }
                .takeIf { it >= 0 } ?: return null
            val httpIdx = raw.indexOf("http", start)
            if (httpIdx < 0) return null
            val end = raw.indexOf('\u0000', httpIdx).takeIf { it > httpIdx } ?: raw.length
            raw.substring(httpIdx, end)
        } catch (e: Exception) {
            null
        }
    }
}
