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
import android.view.View
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
    private lateinit var statusBadge: TextView
    private lateinit var profileTitle: TextView
    private lateinit var profileSub: TextView
    private lateinit var permissionRow: LinearLayout
    private lateinit var reminderStatus: TextView
    private lateinit var footerText: TextView
    private lateinit var dimValue: TextView
    private lateinit var warmValue: TextView
    private lateinit var dimSeekBar: SeekBar
    private lateinit var warmSeekBar: SeekBar
    private val tileLayouts = mutableMapOf<String, LinearLayout>()
    private val tileChecks = mutableMapOf<String, TextView>()
    private val intervalButtons = mutableMapOf<Int, Button>()

    private val bg = Color.rgb(11, 14, 18)
    private val card = Color.rgb(21, 26, 32)
    private val panel = Color.rgb(13, 17, 22)
    private val border = Color.rgb(48, 55, 65)
    private val accent = Color.rgb(117, 224, 160)
    private val accentDark = Color.rgb(18, 59, 40)
    private val warm = Color.rgb(215, 183, 121)
    private val white = Color.rgb(244, 245, 247)
    private val grey = Color.rgb(156, 166, 178)
    private val lightGrey = Color.rgb(203, 213, 225)
    private val darkText = Color.rgb(11, 14, 18)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadPrefs()
        requestNotificationPermissionIfNeeded()

        val scroll = ScrollView(this).apply { setBackgroundColor(bg) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(28))
        }

        // 1. Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val titleCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        titleCol.addView(label("BlinkGuard", 30f, white, bold = true))
        titleCol.addView(label("Eye comfort, simplified.", 13f, grey).apply {
            setPadding(0, dp(4), 0, 0)
        })
        header.addView(titleCol)
        val gear = TextView(this).apply {
            text = "⚙"
            textSize = 20f
            setTextColor(grey)
            gravity = Gravity.CENTER
            background = rounded(card, dp(12), border, dp(1))
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
            setOnClickListener { openOverlaySettings() }
        }
        header.addView(gear)
        root.addView(header)
        root.addView(Space(this).apply { minimumHeight = dp(18) })

        // 2. Eye Shield status card
        val shieldCard = cardLayout()
        val statusRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val shieldIcon = TextView(this).apply {
            text = "✓"
            textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(accent)
            gravity = Gravity.CENTER
            background = rounded(panel, dp(14), border, dp(1))
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
        }
        statusRow.addView(shieldIcon)
        val statusCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        statusCol.addView(label("Eye Shield", 18f, white, bold = true))
        statusCol.addView(label("System-wide screen tint", 13f, grey))
        statusRow.addView(statusCol)
        statusBadge = TextView(this).apply {
            text = "OFF"
            textSize = 11f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(grey)
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(6), dp(10), dp(6))
            background = rounded(panel, dp(20), border, dp(1))
        }
        statusRow.addView(statusBadge)
        shieldCard.addView(statusRow)

        val profilePanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(panel, dp(14), border, dp(1))
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        profilePanel.addView(label("CURRENT PROFILE", 10f, grey, bold = true).apply { letterSpacing = 0.08f })
        profileTitle = label("Warm Comfort", 16f, white, bold = true).apply {
            setPadding(0, dp(4), 0, 0)
        }
        profilePanel.addView(profileTitle)
        profileSub = label("Warm tint · Medium intensity", 13f, grey)
        profilePanel.addView(profileSub)
        val profileWrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(16), 0, dp(16))
        }
        profileWrap.addView(profilePanel)
        shieldCard.addView(profileWrap)

        permissionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(panel, dp(14), border, dp(1))
            setPadding(dp(14), dp(12), dp(14), dp(12))
            visibility = View.GONE
        }
        permissionRow.addView(label("Permission required — allow Display over other apps.", 13f, warm).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        val grantBtn = Button(this).apply {
            text = "Grant"
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            isAllCaps = false
            setTextColor(accent)
            stateListAnimator = null
            background = rounded(panel, dp(12), accent, dp(1))
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setOnClickListener { openOverlaySettings() }
        }
        permissionRow.addView(grantBtn)
        shieldCard.addView(permissionRow)
        val permissionSpace = Space(this).apply { minimumHeight = dp(16) }
        shieldCard.addView(permissionSpace)

        toggleBtn = Button(this).apply {
            text = "Enable Eye Shield"
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            isAllCaps = false
            setTextColor(darkText)
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
        shieldCard.addView(toggleBtn)
        root.addView(shieldCard)
        root.addView(Space(this).apply { minimumHeight = dp(16) })

        // 3. Tint Profile
        val tintCard = cardLayout()
        tintCard.addView(label("TINT PROFILE", 11f, accent, bold = true).apply { letterSpacing = 0.08f })
        tintCard.addView(label("Choose your tint", 18f, white, bold = true).apply {
            setPadding(0, dp(6), 0, dp(12))
        })
        tintCard.addView(label("Presets", 13f, grey).apply { setPadding(0, 0, 0, dp(8)) })
        val presetRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf(
            Triple("Reading", "NATURAL", Pair(10, 15)),
            Triple("Warm Comfort", "WARM", Pair(20, 35)),
            Triple("Night", "RED", Pair(35, 60))
        ).forEach { (name, mode, levels) ->
            val btn = chip(name)
            btn.layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginEnd = dp(6) }
            btn.setOnClickListener { applyPreset(name, mode, levels.first, levels.second) }
            presetRow.addView(btn)
        }
        tintCard.addView(presetRow)
        tintCard.addView(Space(this).apply { minimumHeight = dp(14) })
        val tileGrid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        listOf(
            Pair("NATURAL", grey),
            Pair("WARM", warm),
            Pair("AMBER", Color.rgb(232, 145, 45)),
            Pair("RED", Color.rgb(229, 72, 77))
        ).chunked(2).forEach { rowModes ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            rowModes.forEach { (mode, dotColor) ->
                row.addView(makeTile(mode, dotColor))
            }
            tileGrid.addView(row)
            tileGrid.addView(Space(this).apply { minimumHeight = dp(10) })
        }
        tintCard.addView(tileGrid)
        root.addView(tintCard)
        root.addView(Space(this).apply { minimumHeight = dp(16) })

        // 4. Fine-tune: both sliders in one card
        val tuneCard = cardLayout()
        tuneCard.addView(label("FINE-TUNE", 11f, accent, bold = true).apply { letterSpacing = 0.08f })
        tuneCard.addView(label("Adjust the tint", 18f, white, bold = true).apply {
            setPadding(0, dp(6), 0, dp(4))
        })
        dimValue = valuePill("$dimLevel%")
        tuneCard.addView(sliderHeader("Overlay darkness", dimValue))
        dimSeekBar = styledSeekBar(max = 75, initial = dimLevel) { progress ->
            dimLevel = progress
            dimValue.text = "$progress%"
            savePrefs()
            updateProfile()
            if (EyeShieldService.isRunning) startShieldService("UPDATE_FILTER")
        }
        tuneCard.addView(dimSeekBar)
        tuneCard.addView(minMaxRow("Light", "Dark"))
        warmValue = valuePill("$warmthLevel%")
        tuneCard.addView(sliderHeader("Tint intensity", warmValue))
        warmSeekBar = styledSeekBar(max = 80, initial = warmthLevel) { progress ->
            warmthLevel = progress
            warmValue.text = "$progress%"
            savePrefs()
            updateProfile()
            if (EyeShieldService.isRunning) startShieldService("UPDATE_FILTER")
        }
        tuneCard.addView(warmSeekBar)
        tuneCard.addView(minMaxRow("Light", "Strong"))
        root.addView(tuneCard)
        root.addView(Space(this).apply { minimumHeight = dp(16) })

        // 5. Rest Reminders
        val restCard = cardLayout()
        restCard.addView(label("REST REMINDERS", 11f, accent, bold = true).apply { letterSpacing = 0.08f })
        restCard.addView(label("Take a break", 18f, white, bold = true).apply {
            setPadding(0, dp(6), 0, dp(4))
        })
        restCard.addView(label("Take a short break at regular intervals.", 13f, grey).apply {
            setPadding(0, 0, 0, dp(14))
        })
        val timeRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf(20, 45, 60, 90).forEach { mins ->
            val btn = chip("${mins}m")
            btn.setOnClickListener {
                selectedLimit = mins
                savePrefs()
                refreshIntervalChips()
                updateReminderStatus()
                Toast.makeText(this@MainActivity, "Reminder interval: $mins minutes", Toast.LENGTH_SHORT).show()
                if (EyeShieldService.isRunning) startShieldService("UPDATE_TIMER")
            }
            intervalButtons[mins] = btn
            timeRow.addView(btn)
        }
        restCard.addView(timeRow)
        reminderStatus = label("", 13f, grey).apply { setPadding(0, dp(12), 0, 0) }
        restCard.addView(reminderStatus)
        restCard.addView(Space(this).apply { minimumHeight = dp(14) })
        val testBtn = Button(this).apply {
            text = "Test Rest Reminder Now"
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            isAllCaps = false
            setTextColor(white)
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(14), dp(16), dp(14))
            stateListAnimator = null
            background = rounded(panel, dp(15), border, dp(1))
            setOnClickListener {
                if (!checkOverlayPermission()) return@setOnClickListener
                startShieldService("TEST_POPUP")
            }
        }
        restCard.addView(testBtn)
        root.addView(restCard)
        root.addView(Space(this).apply { minimumHeight = dp(18) })

        // 6. Footer
        footerText = label("", 12f, grey).apply { gravity = Gravity.CENTER }
        root.addView(footerText)
        root.addView(label("Tint is an overlay only — it does not change hardware brightness.", 12f, grey).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, 0)
        })

        scroll.addView(root)
        setContentView(scroll)
        refreshTiles()
        refreshIntervalChips()
        updateProfile()
        updateReminderStatus()
        refreshPermissionStatus()
        updateToggle(EyeShieldService.isRunning)
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionStatus()
        if (::toggleBtn.isInitialized) updateToggle(EyeShieldService.isRunning)
    }

    private fun loadPrefs() {
        val prefs = getSharedPreferences("blinkguard", MODE_PRIVATE)
        selectedMode = prefs.getString("MODE", selectedMode) ?: selectedMode
        dimLevel = prefs.getInt("DIM", dimLevel)
        warmthLevel = prefs.getInt("WARMTH", warmthLevel)
        selectedLimit = prefs.getInt("LIMIT", selectedLimit)
    }

    private fun savePrefs() {
        getSharedPreferences("blinkguard", MODE_PRIVATE).edit()
            .putString("MODE", selectedMode)
            .putInt("DIM", dimLevel)
            .putInt("WARMTH", warmthLevel)
            .putInt("LIMIT", selectedLimit)
            .apply()
    }

    private fun rounded(color: Int, radius: Int, strokeColor: Int? = null, strokeWidth: Int = 0): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
            if (strokeColor != null) setStroke(strokeWidth, strokeColor)
        }
    }

    private fun oval(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }
    }

    private fun cardLayout(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(card, dp(20), border, dp(1))
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }
    }

    private fun chip(text: String): Button {
        return Button(this).apply {
            this.text = text
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            isAllCaps = false
            setTextColor(lightGrey)
            gravity = Gravity.CENTER
            minimumHeight = dp(48)
            stateListAnimator = null
            background = rounded(panel, dp(13), border, dp(1))
            layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginEnd = dp(6) }
            setPadding(0, 0, 0, 0)
        }
    }

    private fun makeTile(mode: String, dotColor: Int): LinearLayout {
        val tile = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(panel, dp(14), border, dp(1))
            setPadding(dp(14), dp(14), dp(14), dp(14))
            layoutParams = LinearLayout.LayoutParams(0, dp(52), 1f).apply {
                marginEnd = dp(8)
                bottomMargin = dp(2)
            }
            isClickable = true
            isFocusable = true
        }
        val dot = View(this).apply {
            background = oval(dotColor)
            layoutParams = LinearLayout.LayoutParams(dp(12), dp(12))
        }
        tile.addView(dot)
        tile.addView(label(mode, 14f, white, bold = true).apply { setPadding(dp(10), 0, 0, 0) })
        val check = TextView(this).apply {
            text = "✓"
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(accent)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            gravity = Gravity.END
            visibility = View.GONE
        }
        tile.addView(check)
        tile.setOnClickListener {
            selectedMode = mode
            savePrefs()
            refreshTiles()
            updateProfile()
            if (ensureShieldCanBeControlled()) startShieldService("UPDATE_FILTER")
        }
        tileLayouts[mode] = tile
        tileChecks[mode] = check
        return tile
    }

    private fun refreshTiles() {
        tileLayouts.forEach { (mode, tile) ->
            val selected = mode == selectedMode
            tile.background = if (selected) rounded(panel, dp(14), accent, dp(2)) else rounded(panel, dp(14), border, dp(1))
            tileChecks[mode]?.visibility = if (selected) View.VISIBLE else View.GONE
        }
    }

    private fun refreshIntervalChips() {
        intervalButtons.forEach { (mins, btn) ->
            val selected = mins == selectedLimit
            btn.background = if (selected) rounded(accent, dp(13)) else rounded(panel, dp(13), border, dp(1))
            btn.setTextColor(if (selected) darkText else lightGrey)
        }
    }

    private fun applyPreset(name: String, mode: String, dim: Int, warmthVal: Int) {
        selectedMode = mode
        dimLevel = dim
        warmthLevel = warmthVal
        if (::dimSeekBar.isInitialized) dimSeekBar.progress = dim
        if (::warmSeekBar.isInitialized) warmSeekBar.progress = warmthVal
        if (::dimValue.isInitialized) dimValue.text = "$dim%"
        if (::warmValue.isInitialized) warmValue.text = "$warmthVal%"
        savePrefs()
        refreshTiles()
        updateProfile()
        Toast.makeText(this, "Preset applied: $name", Toast.LENGTH_SHORT).show()
        if (EyeShieldService.isRunning) startShieldService("UPDATE_FILTER")
    }

    private fun updateProfile() {
        if (!::profileTitle.isInitialized) return
        profileTitle.text = when (selectedMode) {
            "NATURAL" -> "Natural Light"
            "WARM" -> "Warm Comfort"
            "AMBER" -> "Amber Glow"
            "RED" -> "Red Night"
            else -> "Warm Comfort"
        }
        val modeLabel = when (selectedMode) {
            "NATURAL" -> "Natural"
            "WARM" -> "Warm"
            "AMBER" -> "Amber"
            "RED" -> "Red"
            else -> "Warm"
        }
        val intensity = when {
            warmthLevel < 25 -> "Low"
            warmthLevel < 55 -> "Medium"
            else -> "High"
        }
        profileSub.text = "$modeLabel tint · $intensity intensity"
    }

    private fun updateReminderStatus() {
        if (!::reminderStatus.isInitialized) return
        reminderStatus.text = if (EyeShieldService.isRunning) {
            "Reminder every $selectedLimit minutes while the shield is on."
        } else {
            "Reminders start when the Eye Shield is on."
        }
    }

    private fun refreshPermissionStatus() {
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(this) else true
        if (::permissionRow.isInitialized) {
            permissionRow.visibility = if (granted) View.GONE else View.VISIBLE
        }
        if (::footerText.isInitialized) {
            footerText.text = if (granted) "Overlay permission granted · Version 1.2" else "Overlay permission required · Version 1.2"
        }
    }

    private fun valuePill(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(accent)
            setPadding(dp(10), dp(4), dp(10), dp(4))
            background = rounded(accentDark, dp(12))
        }
    }

    private fun sliderHeader(title: String, valueView: TextView): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(14), 0, dp(4))
            addView(label(title, 14f, lightGrey).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(valueView)
        }
    }

    private fun minMaxRow(left: String, right: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(6), 0, dp(2))
            addView(label(left, 11f, grey).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(label(right, 11f, grey))
        }
    }

    private fun styledSeekBar(max: Int, initial: Int, onChange: (Int) -> Unit): SeekBar {
        return SeekBar(this).apply {
            this.max = max
            progress = initial
            progressTintList = ColorStateList.valueOf(accent)
            progressBackgroundTintList = ColorStateList.valueOf(border)
            thumbTintList = ColorStateList.valueOf(white)
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
            toggleBtn.text = if (running) "Disable Eye Shield" else "Enable Eye Shield"
            toggleBtn.background = if (running) rounded(card, dp(15), accent, dp(1)) else rounded(accent, dp(15))
            toggleBtn.setTextColor(if (running) accent else darkText)
        }
        if (::statusBadge.isInitialized) {
            if (running) {
                statusBadge.text = "ACTIVE"
                statusBadge.setTextColor(accent)
                statusBadge.background = rounded(accentDark, dp(20), accent, dp(1))
            } else {
                statusBadge.text = "OFF"
                statusBadge.setTextColor(grey)
                statusBadge.background = rounded(panel, dp(20), border, dp(1))
            }
        }
        updateReminderStatus()
        refreshPermissionStatus()
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

    private fun openOverlaySettings() {
        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
    }

    private fun checkOverlayPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Allow 'Display over other apps' for BlinkGuard, then return.", Toast.LENGTH_LONG).show()
            openOverlaySettings()
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
