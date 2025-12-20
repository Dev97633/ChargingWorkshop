package com.su.charging.view.activity

import android.app.Activity
import android.content.*
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.su.charging.ChargingService
import com.su.charging.R
import com.su.charging.util.PermissionUtils
import com.su.charging.view.fragment.PermissionBottomSheetFragment
import com.su.charging.view.fragment.SettingsFragmentCompat

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQ_SELECT_VIDEO = 1001
    }

    private lateinit var tvStatus: TextView
    private lateinit var tvBattery: TextView
    private lateinit var fragmentContainer: View
    private lateinit var videoPreview: VideoView

    /* ================= BATTERY RECEIVER ================= */

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return

            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

            val percent =
                if (level >= 0 && scale > 0) (level * 100) / scale else 0

            tvBattery.text = "$percent%"

            tvStatus.text = when (status) {
                BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
                BatteryManager.BATTERY_STATUS_FULL -> "Fully Charged"
                else -> "Not Charging"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 🔐 Overlay permission
        if (!PermissionUtils.INS.checkWindowPermission(this)) {
            PermissionBottomSheetFragment.open(this)
        }

        // 🎯 UI
        val btnSelect = findViewById<Button>(R.id.btnSelect)
        val btnSettings = findViewById<Button>(R.id.btnSettings)

        tvStatus = findViewById(R.id.tvStatus)
        tvBattery = findViewById(R.id.tvBattery)
        fragmentContainer = findViewById(R.id.fragment_container)
        videoPreview = findViewById(R.id.videoPreview)

        // ▶ Load saved animation preview (if exists)
        loadSavedPreview()

        // 🎬 Select Animation
        btnSelect.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "video/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            }
            startActivityForResult(intent, REQ_SELECT_VIDEO)
        }

        // ⚙ Settings
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

        // 🔋 Battery listener
        registerReceiver(
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
    }

    /* ================= VIDEO PREVIEW ================= */

    private fun loadSavedPreview() {
        val uriString = getSharedPreferences("charging_prefs", MODE_PRIVATE)
            .getString("charging_animation_uri", null)

        uriString?.let {
            showPreview(Uri.parse(it))
        }
    }

    private fun showPreview(uri: Uri) {
        videoPreview.setVideoURI(uri)

        videoPreview.setOnPreparedListener { mp ->
            mp.isLooping = true
            mp.setVolume(0f, 0f)

            val videoRatio = mp.videoWidth / mp.videoHeight.toFloat()
            val viewRatio = videoPreview.width / videoPreview.height.toFloat()

            if (videoRatio > viewRatio) {
                videoPreview.scaleX = videoRatio / viewRatio
                videoPreview.scaleY = 1f
            } else {
                videoPreview.scaleY = viewRatio / videoRatio
                videoPreview.scaleX = 1f
            }

            videoPreview.alpha = 0f
            videoPreview.animate().alpha(1f).setDuration(300).start()

            videoPreview.start()
        }
    }

    /* ================= ACTIVITY RESULT ================= */

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQ_SELECT_VIDEO && resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return

            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            getSharedPreferences("charging_prefs", MODE_PRIVATE)
                .edit()
                .putString("charging_animation_uri", uri.toString())
                .apply()

            showPreview(uri)
            tip("Animation selected ✔")
        }
    }

    /* ================= BACK PRESS ================= */

    override fun onBackPressed() {
        if (fragmentContainer.visibility == View.VISIBLE) {
            fragmentContainer.visibility = View.GONE
            supportFragmentManager.popBackStack()
            return
        }

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
        } catch (_: Exception) {}
    }

    private fun Activity.tip(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
