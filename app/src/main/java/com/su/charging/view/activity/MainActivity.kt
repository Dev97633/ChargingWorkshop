package com.su.charging.view.activity

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
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

    private lateinit var tvStatus: TextView
    private lateinit var tvBattery: TextView
    private lateinit var fragmentContainer: View

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return

            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

            val percent = if (level >= 0 && scale > 0) {
                (level * 100) / scale
            } else {
                0
            }

            tvBattery.text = "$percent%"

            tvStatus.text = when (status) {
                BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
                BatteryManager.BATTERY_STATUS_FULL -> "Fully Charged"
                else -> "Not Charging"
            }
        }
    }

    /* ================= VIDEO PICKER ================= */

    private val videoPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri == null) return@registerForActivityResult

            // 🔒 Persist permission (VERY IMPORTANT)
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            // 💾 Save selected animation
            getSharedPreferences("charging_prefs", MODE_PRIVATE)
                .edit()
                .putString("charging_animation_uri", uri.toString())
                .apply()

            tip("Animation selected ✔")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 🔐 Overlay permission check (IMPORTANT)
        if (!PermissionUtils.INS.checkWindowPermission(this)) {
            PermissionBottomSheetFragment.open(this)
        }

        // 🎯 UI references
        val btnSelect = findViewById<Button>(R.id.btnSelect)
        val btnSettings = findViewById<Button>(R.id.btnSettings)

        tvStatus = findViewById(R.id.tvStatus)
        tvBattery = findViewById(R.id.tvBattery)
        fragmentContainer = findViewById(R.id.fragment_container)

        // 🎬 Select Animation (TEMP action)
        btnSelect.setOnClickListener {
            videoPicker.launch(arrayOf("video/*"))
        }

        // ⚙ Open Settings Fragment
        btnSettings.setOnClickListener {
            fragmentContainer.visibility = View.VISIBLE

            supportFragmentManager.beginTransaction()
            .setCustomAnimations(
            android.R.anim.fade_in,
            android.R.anim.fade_out,
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )
                .replace(R.id.fragment_container, SettingsFragmentCompat())
                .addToBackStack(null)
                .commit()
        }

        // 🔋 Start battery monitoring
        registerReceiver(
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
    }

    override fun onBackPressed() {
        // Close settings first
        if (fragmentContainer.visibility == View.VISIBLE) {
            fragmentContainer.visibility = View.GONE
            supportFragmentManager.popBackStack()
            return
        }

        // Preserve original charging behavior
        if (ChargingService.isOpen) {
            moveTaskToBack(true)
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(batteryReceiver)
        } catch (_: Exception) {
        }
    }

    // 🔔 Utility toast function
    private fun Activity.tip(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
