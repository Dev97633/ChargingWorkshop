package com.su.charging.view.activity

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.su.charging.R
import com.su.charging.ChargingService
import com.su.charging.util.PermissionUtils
import com.su.charging.view.fragment.PermissionBottomSheetFragment
import com.su.charging.view.fragment.SettingsFragmentCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 🔐 Keep overlay permission check (IMPORTANT)
        if (!PermissionUtils.INS.checkWindowPermission(this)) {
            PermissionBottomSheetFragment.open(this)
        }

        // 🎯 UI references
        val btnSettings = findViewById<Button>(R.id.btnSettings)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val tvBattery = findViewById<TextView>(R.id.tvBattery)
        val fragmentContainer = findViewById<View>(R.id.fragment_container)

        // 🧪 Temporary values (will replace with real logic later)
        tvStatus.text = "Not Charging"
        tvBattery.text = "80%"

        // ⚙ Open Settings Fragment
        btnSettings.setOnClickListener {
            fragmentContainer.visibility = View.VISIBLE

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SettingsFragmentCompat())
                .addToBackStack(null)
                .commit()
        }
    }

    // 🔙 Handle back press properly
    override fun onBackPressed() {
        val fragmentContainer = findViewById<View>(R.id.fragment_container)

        // Close settings first
        if (fragmentContainer.visibility == View.VISIBLE) {
            fragmentContainer.visibility = View.GONE
            supportFragmentManager.popBackStack()
            return
        }

        // Keep original charging service behavior
        if (ChargingService.isOpen) {
            moveTaskToBack(true)
        } else {
            super.onBackPressed()
        }
    }

    // 🔔 Utility toast function (kept from original)
    fun Activity.tip(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}
