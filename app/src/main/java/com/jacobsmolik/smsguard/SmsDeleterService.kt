package com.jacobsmolik.smsguard

import android.util.Log

// Loaded by Shizuku and runs as shell user — Runtime.exec() here has shell privileges
class SmsDeleterService : ISmsDeleter.Stub() {

    override fun deleteById(id: Long): Int = exec("_id=$id")

    override fun deleteByAddress(address: String): Int {
        val safe = address.replace("'", "\\'")
        return exec("address='$safe'")
    }

    private fun exec(where: String): Int {
        return try {
            val process = Runtime.getRuntime().exec(
                arrayOf("/system/bin/content", "delete",
                    "--uri", "content://sms",
                    "--where", where)
            )
            val err = process.errorStream.bufferedReader().readText()
            val exit = process.waitFor()
            if (exit != 0) Log.e("SmsDeleter", "delete failed (exit=$exit): $err")
            else Log.d("SmsDeleter", "deleted where $where")
            exit
        } catch (e: Exception) {
            Log.e("SmsDeleter", "exec error", e)
            -1
        }
    }
}
