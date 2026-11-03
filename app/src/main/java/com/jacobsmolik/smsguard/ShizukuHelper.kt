package com.jacobsmolik.smsguard

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import rikka.shizuku.Shizuku
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object ShizukuHelper {

    @Volatile private var deleter: ISmsDeleter? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            deleter = ISmsDeleter.Stub.asInterface(binder)
            Log.d("SMSGuard", "SmsDeleterService connected")
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            deleter = null
            Log.w("SMSGuard", "SmsDeleterService disconnected")
        }
    }

    fun isAvailable() = try { Shizuku.pingBinder() } catch (e: Exception) { false }

    fun hasPermission() = try {
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (e: Exception) { false }

    fun requestPermission(code: Int) = Shizuku.requestPermission(code)

    fun bindService(context: android.content.Context) {
        if (!hasPermission()) return
        try {
            Shizuku.bindUserService(serviceArgs(context), connection)
        } catch (e: Exception) {
            Log.e("SMSGuard", "bindService failed", e)
        }
    }

    fun unbindService(context: android.content.Context) {
        try {
            Shizuku.unbindUserService(serviceArgs(context), connection, true)
        } catch (e: Exception) { /* ignore */ }
    }

    fun deleteSmsById(id: Long) = delete { it.deleteById(id) }

    fun deleteSmsFromNumber(address: String) = delete { it.deleteByAddress(address) }

    private fun delete(op: (ISmsDeleter) -> Int) {
        val d = deleter
        if (d != null) {
            try { op(d) } catch (e: Exception) { Log.e("SMSGuard", "delete failed", e) }
            return
        }
        // Service not yet bound — this can happen when the receiver fires before MainActivity opens.
        // Block briefly using a temp connection.
        Log.w("SMSGuard", "Deleter not bound; attempting sync bind")
        val latch = CountDownLatch(1)
        var tempDeleter: ISmsDeleter? = null
        val temp = object : ServiceConnection {
            override fun onServiceConnected(n: ComponentName?, b: IBinder?) {
                tempDeleter = ISmsDeleter.Stub.asInterface(b)
                latch.countDown()
            }
            override fun onServiceDisconnected(n: ComponentName?) = latch.countDown()
        }
        try {
            // We need a context; use ApplicationContext via the Shizuku provider trick
            val ctx = Shizuku::class.java
                .getDeclaredMethod("requireContext")
                .also { it.isAccessible = true }
                .invoke(null) as? android.content.Context
            if (ctx != null) {
                Shizuku.bindUserService(serviceArgs(ctx), temp)
                latch.await(5, TimeUnit.SECONDS)
                tempDeleter?.let { td ->
                    try { op(td) } catch (e: Exception) { Log.e("SMSGuard", "delete (temp) failed", e) }
                }
                Shizuku.unbindUserService(serviceArgs(ctx), temp, false)
            }
        } catch (e: Exception) {
            Log.e("SMSGuard", "sync bind failed", e)
        }
    }

    private fun serviceArgs(context: android.content.Context) =
        Shizuku.UserServiceArgs(ComponentName(context, SmsDeleterService::class.java))
            .daemon(false)
            .processNameSuffix("sms_deleter")
            .version(BuildConfig.VERSION_CODE)
}
