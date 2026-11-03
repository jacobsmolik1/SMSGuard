package com.jacobsmolik.smsguard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.jacobsmolik.smsguard.databinding.ActivityMainBinding
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val RC_SHIZUKU = 1001
    private val RC_PERMISSIONS = 1002

    private val shizukuListener = Shizuku.OnRequestPermissionResultListener { _, result ->
        if (result == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Shizuku access granted", Toast.LENGTH_SHORT).show()
            ShizukuHelper.bindService(this)
        }
        refreshStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Shizuku.addRequestPermissionResultListener(shizukuListener)
        if (ShizukuHelper.hasPermission()) ShizukuHelper.bindService(this)

        binding.btnPermissions.setOnClickListener { requestAllPermissions() }
        binding.btnKeywords.setOnClickListener { showKeywordsDialog() }
        binding.btnLog.setOnClickListener { showLogDialog() }
        binding.btnScan.setOnClickListener { scanInbox() }

        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(shizukuListener)
        ShizukuHelper.unbindService(this)
    }

    private fun refreshStatus() {
        val hasSms = checkPerm(Manifest.permission.RECEIVE_SMS)
        val hasReadSms = checkPerm(Manifest.permission.READ_SMS)
        val hasContacts = checkPerm(Manifest.permission.READ_CONTACTS)
        val shizukuOk = ShizukuHelper.isAvailable() && ShizukuHelper.hasPermission()

        binding.statusSms.text = if (hasSms && hasReadSms) "✓ SMS permissions" else "✗ SMS permissions (tap Grant)"
        binding.statusContacts.text = if (hasContacts) "✓ Contacts permission" else "✗ Contacts permission (tap Grant)"
        binding.statusShizuku.text = if (shizukuOk) "✓ Shizuku connected" else "✗ Shizuku — start it first, then tap Grant"
        binding.filterCount.text = "Messages filtered: ${FilterLog.getCount(this)}"

        val allReady = hasSms && hasReadSms && hasContacts && shizukuOk
        binding.guardStatus.text = if (allReady) "ACTIVE" else "SETUP REQUIRED"
        binding.btnScan.isEnabled = hasReadSms && shizukuOk
    }

    private fun checkPerm(p: String) =
        ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED

    private fun requestAllPermissions() {
        val needed = listOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_CONTACTS
        ).filter { !checkPerm(it) }
        if (needed.isNotEmpty())
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), RC_PERMISSIONS)

        when {
            !ShizukuHelper.isAvailable() ->
                Toast.makeText(this, "Shizuku is not running — start it first", Toast.LENGTH_LONG).show()
            !ShizukuHelper.hasPermission() ->
                ShizukuHelper.requestPermission(RC_SHIZUKU)
        }
    }

    override fun onRequestPermissionsResult(rc: Int, perms: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(rc, perms, results)
        refreshStatus()
    }

    private fun scanInbox() {
        binding.btnScan.isEnabled = false
        binding.btnScan.text = "Scanning…"
        Thread {
            val count = InboxScanner.scan(this)
            runOnUiThread {
                refreshStatus()
                binding.btnScan.text = "Scan Inbox Now"
                Toast.makeText(this, "Deleted $count political message${if (count != 1) "s" else ""}", Toast.LENGTH_LONG).show()
            }
        }.start()
    }

    private fun showKeywordsDialog() {
        val keywords = FilterEngine.getKeywords(this).toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Keywords (${keywords.size}) — tap to remove")
            .setItems(keywords) { _, i ->
                AlertDialog.Builder(this)
                    .setMessage("Remove \"${keywords[i]}\"?")
                    .setPositiveButton("Remove") { _, _ ->
                        FilterEngine.removeKeyword(this, keywords[i])
                        Toast.makeText(this, "Removed", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .setPositiveButton("+ Add") { _, _ -> showAddKeyword() }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showAddKeyword() {
        val input = EditText(this).apply {
            hint = "e.g. donate now"
            setPadding(48, 24, 48, 24)
        }
        AlertDialog.Builder(this)
            .setTitle("Add keyword")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val kw = input.text.toString().trim()
                if (kw.isNotEmpty()) {
                    FilterEngine.addKeyword(this, kw)
                    Toast.makeText(this, "Added: $kw", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLogDialog() {
        val entries = FilterLog.getEntries(this)
        if (entries.isEmpty()) {
            Toast.makeText(this, "Nothing filtered yet", Toast.LENGTH_SHORT).show()
            return
        }
        val items = entries.map { "[${it.timestamp}] ${it.sender}\n${it.preview}" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Filtered messages")
            .setItems(items, null)
            .setNegativeButton("Close", null)
            .show()
    }
}
