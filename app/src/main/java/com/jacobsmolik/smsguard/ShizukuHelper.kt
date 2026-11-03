package com.jacobsmolik.smsguard

import android.content.pm.PackageManager
import android.util.Log
import rikka.shizuku.Shizuku
import java.lang.reflect.Method

object ShizukuHelper {

    private val newProcessMethod: Method? by lazy {
        try {
            Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            ).also { it.isAccessible = true }
        } catch (e: Exception) {
            Log.e("SMSGuard", "Shizuku newProcess not found", e)
            null
        }
    }

    fun isAvailable(): Boolean = try { Shizuku.pingBinder() } catch (e: Exception) { false }

    fun hasPermission(): Boolean = try {
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (e: Exception) { false }

    fun requestPermission(code: Int) = Shizuku.requestPermission(code)

    fun deleteSmsById(id: Long) = runShell("content", "delete", "--uri", "content://sms", "--where", "_id=$id")

    fun deleteSmsFromNumber(number: String) {
        val safe = number.replace("'", "\\'")
        runShell("content", "delete", "--uri", "content://sms", "--where", "address='$safe'")
    }

    private fun runShell(vararg cmd: String) {
        if (!hasPermission()) {
            Log.w("SMSGuard", "Shizuku permission not granted")
            return
        }
        val method = newProcessMethod ?: return
        try {
            val process = method.invoke(null, cmd, null, null) as Process
            val exit = process.waitFor()
            Log.d("SMSGuard", "Shell ${cmd.joinToString(" ")} → exit $exit")
        } catch (e: Exception) {
            Log.e("SMSGuard", "Shell command failed: ${cmd.joinToString(" ")}", e)
        }
    }
}
