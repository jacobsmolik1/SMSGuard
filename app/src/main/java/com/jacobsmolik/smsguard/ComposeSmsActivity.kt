package com.jacobsmolik.smsguard

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

// Stub required for default SMS app eligibility.
// Forwards to the system SMS chooser so the user can still send messages.
class ComposeSmsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val forward = Intent(intent).apply { `package` = null }
        try {
            startActivity(Intent.createChooser(forward, "Send with…"))
        } catch (e: Exception) {
            Toast.makeText(this, "Use another messaging app to send messages", Toast.LENGTH_LONG).show()
        }
        finish()
    }
}
