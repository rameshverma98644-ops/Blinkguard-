package com.blinkguard.eyecare

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Space
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private var selectedLimit = 60
    private var selectedMode = "WARM"
    private var dimLevel = 20
    private var warmthLevel = 35
    private lateinit var toggleBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()

        val scroll = ScrollView(this).apply { setBackgroundColor(Color.rgb(11, 15, 23)) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(28), dp(24), dp(28))
        }

        root.addView(label("BlinkGuard", 26f, Color.WHITE))
        root.addView(label("System-wide screen tint & eye-rest reminders", 13f, Color.LTGRAY).apply {
            setPadding(0, dp(4), 0, dp(24))
        })

        toggleBtn = Button(this).apply {
            text = "Enable System-Wide Eye Shield"
            setOnClickListener {
                if (!checkOverlayPermission()) return@setOnClickListener
                if (!EyeShieldService.isRunning) {
                    startShieldService("START_SHIELD")
                } else {
                    stopService(Intent(this@MainActivity, EyeShieldService::class.java))
                    updateToggle(false)
                }
            }
        }
        root.addView(toggleBtn)

        root.addView(label("1. Screen tint mode", 16f, Color.WHITE).apply {
            setPadding(0, dp(24), 0, dp(8))
        })
        val modeRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("NATURAL", "WARM", "AMBER", "RED").forEach { mode ->
            modeRow.addView(Button(this).apply {
                text = mode
                textSize = 10f
                isAllCaps = false
                layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginEnd = dp(3) }
                setOnClickListener {
                    selectedMode = mode
                    if (ensureShieldCanBeControlled()) startShieldService("UPDATE_FILTER")
                }
            })
        }
        root.addView(modeRow)

        val dimText = label("Overlay darkness: $dimLevel%", 14f, Color.LTGRAY)
        root.addView(dimText)
        root.addView(SeekBar(this).apply {
            max = 75
            progress = dimLevel
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    dimLevel = progress
                    dimText.text = "Overlay darkness: $progress%"
                    if (fromUser && EyeShieldService.isRunning) startShieldService("UPDATE_FILTER")
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        })

        val warmText = label("Tint intensity: $warmthLevel%", 14f, Color.LTGRAY)
        root.addView(warmText)
        root.addView(SeekBar(this).apply {
            max = 80
            progress = warmthLevel
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    warmthLevel = progress
                    warmText.text = "Tint intensity: $progress%"
                    if (fromUser && EyeShieldService.isRunning) startShieldService("UPDATE_FILTER")
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        })

        root.addView(label("2. Rest reminder interval", 16f, Color.WHITE).apply {
            setPadding(0, dp(24), 0, dp(8))
        })
        val timeRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf(20, 45, 60, 90).forEach { mins ->
            timeRow.addView(Button(this).apply {
                text = "${mins}m"
                layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginEnd = dp(3) }
                setOnClickListener {
                    selectedLimit = mins
                    Toast.makeText(this@MainActivity, "Reminder interval: $mins minutes", Toast.LENGTH_SHORT).show()
                    if (EyeShieldService.isRunning) startShieldService("UPDATE_TIMER")
                }
            })
        }
        root.addView(timeRow)

        root.addView(Space(this).apply { minimumHeight = dp(20) })
        root.addView(Button(this).apply {
            text = "Test Rest Reminder Now"
            setOnClickListener {
                if (!checkOverlayPermission()) return@setOnClickListener
                if (!EyeShieldService.isRunning) startShieldService("TEST_POPUP")
                else startShieldService("TEST_POPUP")
            }
        })

        scroll.addView(root)
        setContentView(scroll)
        updateToggle(EyeShieldService.isRunning)
    }

    override fun onResume() {
        super.onResume()
        if (::toggleBtn.isInitialized) updateToggle(EyeShieldService.isRunning)
    }

    private fun ensureShieldCanBeControlled(): Boolean {
        if (!checkOverlayPermission()) return false
        if (!EyeShieldService.isRunning) {
            Toast.makeText(this, "Enable the Eye Shield first to apply the selected filter.", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun updateToggle(running: Boolean) {
        if (::toggleBtn.isInitialized) {
            toggleBtn.text = if (running) "Turn Off Eye Shield (Running)" else "Enable System-Wide Eye Shield"
        }
    }

    private fun label(value: String, size: Float, color: Int) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 42)
        }
    }

    private fun checkOverlayPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Allow 'Display over other apps' for BlinkGuard, then return.", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return false
        }
        return true
    }

    private fun startShieldService(actionType: String) {
        val intent = Intent(this, EyeShieldService::class.java).apply {
            action = actionType
            putExtra("DIM", dimLevel)
            putExtra("WARMTH", warmthLevel)
            putExtra("MODE", selectedMode)
            putExtra("LIMIT_MINS", selectedLimit)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
            updateToggle(true)
        } catch (e: Exception) {
            Toast.makeText(this, "Could not start service: ${e.message ?: "unknown error"}", Toast.LENGTH_LONG).show()
        }
    }
}
