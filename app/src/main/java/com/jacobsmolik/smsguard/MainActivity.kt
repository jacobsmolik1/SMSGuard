package com.jacobsmolik.smsguard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Telephony
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.jacobsmolik.smsguard.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val RC_PERMISSIONS = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnPermissions.setOnClickListener { requestPermissions() }
        binding.btnSetDefault.setOnClickListener { setAsDefault() }
        binding.btnKeywords.setOnClickListener { showKeywordsDialog() }
        binding.btnLog.setOnClickListener { showLogDialog() }
        binding.btnScan.setOnClickListener { scanInbox() }

        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun isDefault() = Telephony.Sms.getDefaultSmsPackage(this) == packageName

    private fun refreshStatus() {
        val hasSms = checkPerm(Manifest.permission.RECEIVE_SMS)
        val hasReadSms = checkPerm(Manifest.permission.READ_SMS)
        val hasContacts = checkPerm(Manifest.permission.READ_CONTACTS)
        val isDefault = isDefault()

        binding.statusSms.text = if (hasSms && hasReadSms) "✓ SMS permissions" else "✗ SMS permissions (tap Grant)"
        binding.statusContacts.text = if (hasContacts) "✓ Contacts permission" else "✗ Contacts permission (tap Grant)"
        binding.statusDefault.text = if (isDefault) "✓ Default SMS app" else "✗ Not default SMS app — tap Set as Default"
        binding.filterCount.text = "Messages filtered: ${FilterLog.getCount(this)}"

        val active = hasSms && hasReadSms && hasContacts && isDefault
        binding.guardStatus.text = if (active) "ACTIVE" else "SETUP REQUIRED"
        binding.btnScan.isEnabled = isDefault && hasReadSms
        binding.btnSetDefault.isEnabled = !isDefault
    }

    private fun checkPerm(p: String) =
        ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED

    private fun requestPermissions() {
        val needed = listOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_CONTACTS
        ).filter { !checkPerm(it) }
        if (needed.isNotEmpty())
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), RC_PERMISSIONS)
    }

    override fun onRequestPermissionsResult(rc: Int, perms: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(rc, perms, results)
        refreshStatus()
    }

    private fun setAsDefault() {
        @Suppress("DEPRECATION")
        startActivity(Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
            putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
        })
    }

    private fun scanInbox() {
        binding.btnScan.isEnabled = false
        binding.btnScan.text = "Scanning…"
        Thread {
            val count = InboxScanner.scan(this)
            runOnUiThread {
                refreshStatus()
                binding.btnScan.isEnabled = isDefault()
                binding.btnScan.text = "Scan Inbox Now"
                Toast.makeText(
                    this,
                    if (count == 0) "No political messages found" else "Deleted $count political message${if (count != 1) "s" else ""}",
                    Toast.LENGTH_LONG
                ).show()
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
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .setPositiveButton("+ Add") { _, _ -> showAddKeyword() }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showAddKeyword() {
        val input = EditText(this).apply { hint = "e.g. donate now"; setPadding(48, 24, 48, 24) }
        AlertDialog.Builder(this)
            .setTitle("Add keyword")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val kw = input.text.toString().trim()
                if (kw.isNotEmpty()) FilterEngine.addKeyword(this, kw)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLogDialog() {
        val entries = FilterLog.getEntries(this)
        if (entries.isEmpty()) { Toast.makeText(this, "Nothing filtered yet", Toast.LENGTH_SHORT).show(); return }
        AlertDialog.Builder(this)
            .setTitle("Filtered messages")
            .setItems(entries.map { "[${it.timestamp}] ${it.sender}\n${it.preview}" }.toTypedArray(), null)
            .setNegativeButton("Close", null)
            .show()
    }
}
