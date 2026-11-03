package com.jacobsmolik.smsguard

import android.content.pm.PackageManager
import android.util.Log
import rikka.shizuku.Shizuku

object ShizukuHelper {

    fun isAvailable(): Boolean = try {
        Shizuku.pingBinder()
    } catch (e: Exception) {
        false
    }

    fun hasPermission(): Boolean = try {
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (e: Exception) {
        false
    }

    fun requestPermission(code: Int) = Shizuku.requestPermission(code)

    fun deleteSmsFromNumber(number: String) {
        if (!hasPermission()) {
            Log.w("SMSGuard", "Shizuku not available, skipping delete for $number")
            return
        }
        try {
            val safe = number.replace("'", "\\'")
            val process = Shizuku.newProcess(
                arrayOf("content", "delete",
                    "--uri", "content://sms",
                    "--where", "address='$safe'"),
                null, null
            )
            val exit = process.waitFor()
            Log.d("SMSGuard", "Deleted SMS from $number (exit=$exit)")
        } catch (e: Exception) {
            Log.e("SMSGuard", "Delete failed for $number", e)
        }
    }
}
