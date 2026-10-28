package com.blinkguard.eyecare

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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
    private lateinit var statusPill: TextView
    private val modeButtons = mutableMapOf<String, Button>()
    private val intervalButtons = mutableMapOf<Int, Button>()

    private val bg = Color.rgb(7, 11, 20)
    private val card = Color.rgb(15, 23, 38)
    private val cardBorder = Color.rgb(34, 48, 74)
    private val chipBg = Color.rgb(27, 37, 58)
    private val emerald = Color.rgb(16, 185, 129)
    private val emeraldDark = Color.rgb(6, 78, 59)
    private val teal = Color.rgb(45, 212, 191)
    private val white = Color.WHITE
    private val grey = Color.rgb(148, 163, 184)
    private val lightGrey = Color.rgb(203, 213, 225)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()

        val scroll = ScrollView(this).apply { setBackgroundColor(bg) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(28), dp(20), dp(28))
        }

        // Header: badge + title
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val badge = TextView(this).apply {
            text = "◉"
            textSize = 26f
            setTextColor(white)
            gravity = Gravity.CENTER
            background = rounded(emerald, dp(28))
            layoutParams = LinearLayout.LayoutParams(dp(56), dp(56))
        }
        header.addView(badge)
        val titleCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), 0, 0, 0)
        }
        titleCol.addView(label("BlinkGuard", 28f, white, bold = true))
        titleCol.addView(label("System-wide screen tint & eye-rest reminders", 13f, grey).apply {
            setPadding(0, dp(4), 0, 0)
        })
        header.addView(titleCol)
        root.addView(header)

        root.addView(Space(this).apply { minimumHeight = dp(18) })

        statusPill = TextView(this).apply {
            text = "○  Shield off"
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(grey)
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = rounded(card, dp(20), cardBorder, dp(1))
        }
        root.addView(statusPill)
        root.addView(Space(this).apply { minimumHeight = dp(18) })

        toggleBtn = Button(this).apply {
            text = "Enable System-Wide Eye Shield"
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            isAllCaps = false
            setTextColor(white)
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(16), dp(16), dp(16))
            stateListAnimator = null
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
        root.addView(Space(this).apply { minimumHeight = dp(20) })

        // Card 1: screen tint
        val tintCard = cardLayout()
        tintCard.addView(label("SCREEN TINT", 11f, teal, bold = true).apply { letterSpacing = 0.08f })
        tintCard.addView(label("Screen tint mode", 18f, white, bold = true).apply {
            setPadding(0, dp(6), 0, dp(14))
        })
        val modeRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("NATURAL", "WARM", "AMBER", "RED").forEach { mode ->
            val btn = chip(mode)
            btn.setOnClickListener {
                selectedMode = mode
                refreshModeChips()
                if (ensureShieldCanBeControlled()) startShieldService("UPDATE_FILTER")
            }
            modeButtons[mode] = btn
            modeRow.addView(btn)
        }
        tintCard.addView(modeRow)

        val dimValue = TextView(this).apply {
            text = "$dimLevel%"
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(teal)
            setPadding(dp(10), dp(4), dp(10), dp(4))
            background = rounded(emeraldDark, dp(12))
        }
        tintCard.addView(sliderHeader("Overlay darkness", dimValue))
        tintCard.addView(styledSeekBar(max = 75, initial = dimLevel) { progress ->
            dimLevel = progress
            dimValue.text = "$progress%"
            if (EyeShieldService.isRunning) startShieldService("UPDATE_FILTER")
        })

        val warmValue = TextView(this).apply {
            text = "$warmthLevel%"
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(teal)
            setPadding(dp(10), dp(4), dp(10), dp(4))
            background = rounded(emeraldDark, dp(12))
        }
        tintCard.addView(sliderHeader("Tint intensity", warmValue))
        tintCard.addView(styledSeekBar(max = 80, initial = warmthLevel) { progress ->
            warmthLevel = progress
            warmValue.text = "$progress%"
            if (EyeShieldService.isRunning) startShieldService("UPDATE_FILTER")
        })
        root.addView(tintCard)
        root.addView(Space(this).apply { minimumHeight = dp(16) })

        // Card 2: rest reminders
        val restCard = cardLayout()
        restCard.addView(label("REST REMINDERS", 11f, teal, bold = true).apply { letterSpacing = 0.08f })
        restCard.addView(label("Rest reminder interval", 18f, white, bold = true).apply {
            setPadding(0, dp(6), 0, dp(14))
        })
        val timeRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf(20, 45, 60, 90).forEach { mins ->
            val btn = chip("${mins}m")
            btn.setOnClickListener {
                selectedLimit = mins
                refreshIntervalChips()
                Toast.makeText(this@MainActivity, "Reminder interval: $mins minutes", Toast.LENGTH_SHORT).show()
                if (EyeShieldService.isRunning) startShieldService("UPDATE_TIMER")
            }
            intervalButtons[mins] = btn
            timeRow.addView(btn)
        }
        restCard.addView(timeRow)
        restCard.addView(label("The reminder pauses when the screen is off.", 12f, grey).apply {
            setPadding(0, dp(12), 0, 0)
        })
        root.addView(restCard)
        root.addView(Space(this).apply { minimumHeight = dp(16) })

        val testBtn = Button(this).apply {
            text = "Test Rest Reminder Now"
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            isAllCaps = false
            setTextColor(white)
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(16), dp(16), dp(16))
            stateListAnimator = null
            background = rounded(Color.TRANSPARENT, dp(16), cardBorder, dp(1))
            setOnClickListener {
                if (!checkOverlayPermission()) return@setOnClickListener
                startShieldService("TEST_POPUP")
            }
        }
        root.addView(testBtn)
        root.addView(Space(this).apply { minimumHeight = dp(20) })
        root.addView(label("The overlay tints the screen. It does not change hardware brightness.", 12f, grey).apply {
            gravity = Gravity.CENTER
        })

        scroll.addView(root)
        setContentView(scroll)
        refreshModeChips()
        refreshIntervalChips()
        updateToggle(EyeShieldService.isRunning)
    }

    override fun onResume() {
        super.onResume()
        if (::toggleBtn.isInitialized) updateToggle(EyeShieldService.isRunning)
    }

    private fun rounded(color: Int, radius: Int, strokeColor: Int? = null, strokeWidth: Int = 0): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
            if (strokeColor != null) setStroke(strokeWidth, strokeColor)
        }
    }

    private fun cardLayout(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(card, dp(20), cardBorder, dp(1))
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }
    }

    private fun chip(text: String): Button {
        return Button(this).apply {
            this.text = text
            textSize = 12f
            setTypeface(typeface, Typeface.BOLD)
            isAllCaps = false
            setTextColor(lightGrey)
            gravity = Gravity.CENTER
            minimumHeight = dp(48)
            stateListAnimator = null
            background = rounded(chipBg, dp(14))
            layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                marginEnd = dp(6)
            }
            setPadding(0, 0, 0, 0)
        }
    }

    private fun refreshModeChips() {
        modeButtons.forEach { (mode, btn) ->
            val selected = mode == selectedMode
            btn.background = rounded(if (selected) emerald else chipBg, dp(14))
            btn.setTextColor(if (selected) white else lightGrey)
        }
    }

    private fun refreshIntervalChips() {
        intervalButtons.forEach { (mins, btn) ->
            val selected = mins == selectedLimit
            btn.background = rounded(if (selected) emerald else chipBg, dp(14))
            btn.setTextColor(if (selected) white else lightGrey)
        }
    }

    private fun sliderHeader(title: String, valueView: TextView): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(16), 0, dp(4))
            addView(label(title, 14f, lightGrey).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(valueView)
        }
    }

    private fun styledSeekBar(max: Int, initial: Int, onChange: (Int) -> Unit): SeekBar {
        return SeekBar(this).apply {
            this.max = max
            progress = initial
            progressTintList = ColorStateList.valueOf(teal)
            progressBackgroundTintList = ColorStateList.valueOf(chipBg)
            thumbTintList = ColorStateList.valueOf(teal)
            setPadding(dp(4), dp(8), dp(4), dp(8))
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) onChange(progress)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
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
            toggleBtn.text = if (running) "Turn Off Eye Shield" else "Enable System-Wide Eye Shield"
            toggleBtn.background = if (running) {
                rounded(card, dp(16), emerald, dp(1))
            } else {
                rounded(emerald, dp(16))
            }
            toggleBtn.setTextColor(if (running) teal else white)
        }
        if (::statusPill.isInitialized) {
            if (running) {
                statusPill.text = "●  Shield on"
                statusPill.setTextColor(Color.rgb(52, 211, 153))
                statusPill.background = rounded(emeraldDark, dp(20), emerald, dp(1))
            } else {
                statusPill.text = "○  Shield off"
                statusPill.setTextColor(grey)
                statusPill.background = rounded(card, dp(20), cardBorder, dp(1))
            }
        }
    }

    private fun label(value: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
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
