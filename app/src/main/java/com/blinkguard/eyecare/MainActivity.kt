package com.blinkguard.eyecare

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Space
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private var selectedLimit = 60
    private var selectedMode = "WARM"
    private var dimLevel = 20
    private var warmthLevel = 35
    private var remindersOn = true
    private lateinit var toggleBtn: Button
    private lateinit var statusBadge: TextView
    private lateinit var profileTitle: TextView
    private lateinit var profileSub: TextView
    private lateinit var permissionCard: LinearLayout
    private lateinit var permissionStateText: TextView
    private lateinit var summaryCard: LinearLayout
    private lateinit var summaryMode: TextView
    private lateinit var summaryDim: TextView
    private lateinit var summaryWarm: TextView
    private lateinit var summaryRemind: TextView
    private lateinit var reminderStatus: TextView
    private lateinit var reminderSwitch: Switch
    private lateinit var dimValue: TextView
    private lateinit var warmValue: TextView
    private lateinit var dimSeekBar: SeekBar
    private lateinit var warmSeekBar: SeekBar
    private lateinit var previewImage: ImageView
    private lateinit var homeScroll: ScrollView
    private lateinit var settingsScroll: ScrollView
    private lateinit var aboutScroll: ScrollView
    private lateinit var navHome: TextView
    private lateinit var navSettings: TextView
    private lateinit var navAbout: TextView
    private lateinit var aboutPermissionText: TextView
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
    private val warning = Color.rgb(231, 182, 92)
    private val white = Color.rgb(244, 245, 247)
    private val grey = Color.rgb(156, 166, 178)
    private val lightGrey = Color.rgb(203, 213, 225)
    private val darkText = Color.rgb(11, 14, 18)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadPrefs()
        requestNotificationPermissionIfNeeded()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }

        // Header: shield mark + wordmark
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(12))
        }
        val mark = TextView(this).apply {
            text = "◉"
            textSize = 26f
            setTextColor(accent)
            gravity = Gravity.CENTER
            background = rounded(card, dp(14), border, dp(1))
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
        }
        header.addView(mark)
        val titleCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }
        titleCol.addView(label("BlinkGuard", 26f, white, bold = true))
        titleCol.addView(label("System-wide eye comfort", 13f, grey))
        header.addView(titleCol)
        root.addView(header)

        // Content: three tab pages
        homeScroll = buildHomePage()
        settingsScroll = buildSettingsPage()
        aboutScroll = buildAboutPage()
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }
        content.addView(homeScroll)
        content.addView(settingsScroll)
        content.addView(aboutScroll)
        root.addView(content)

        // Bottom navigation
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = rounded(panel, 0, border, dp(1))
            setPadding(dp(8), dp(10), dp(8), dp(14))
        }
        navHome = navItem("Home")
        navSettings = navItem("Settings")
        navAbout = navItem("About")
        navHome.setOnClickListener { switchTab(0) }
        navSettings.setOnClickListener { switchTab(1) }
        navAbout.setOnClickListener { switchTab(2) }
        nav.addView(navHome)
        nav.addView(navSettings)
        nav.addView(navAbout)
        root.addView(nav)

        setContentView(root)
        switchTab(0)
        refreshAll()
    }

    override fun onResume() {
        super.onResume()
        refreshAll()
    }

    private fun navItem(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, dp(10))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
    }

    private fun switchTab(tab: Int) {
        homeScroll.visibility = if (tab == 0) View.VISIBLE else View.GONE
        settingsScroll.visibility = if (tab == 1) View.VISIBLE else View.GONE
        aboutScroll.visibility = if (tab == 2) View.VISIBLE else View.GONE
        navHome.setTextColor(if (tab == 0) accent else grey)
        navSettings.setTextColor(if (tab == 1) accent else grey)
        navAbout.setTextColor(if (tab == 2) accent else grey)
        navHome.setTypeface(typeface, if (tab == 0) Typeface.BOLD else Typeface.NORMAL)
        navSettings.setTypeface(typeface, if (tab == 1) Typeface.BOLD else Typeface.NORMAL)
        navAbout.setTypeface(typeface, if (tab == 2) Typeface.BOLD else Typeface.NORMAL)
    }

    // ---------- Home page ----------

    private fun buildHomePage(): ScrollView {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(6), dp(20), dp(28))
        }

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
        profileTitle = label("Warm Comfort", 16f, white, bold = true).apply { setPadding(0, dp(4), 0, 0) }
        profilePanel.addView(profileTitle)
        profileSub = label("Warm tint · Medium intensity", 13f, grey)
        profilePanel.addView(profileSub)
        val profileWrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(16), 0, dp(16))
        }
        profileWrap.addView(profilePanel)
        shieldCard.addView(profileWrap)

        // Permission card: "One step left"
        permissionCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(panel, dp(14), warning, dp(1))
            setPadding(dp(14), dp(12), dp(14), dp(12))
            visibility = View.GONE
        }
        permissionCard.addView(label("⚠  One step left", 15f, warning, bold = true))
        permissionCard.addView(label("Allow overlay access to use Eye Shield across apps.", 13f, lightGrey).apply {
            setPadding(0, dp(4), 0, dp(10))
        })
        val permBtnRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val openSettingsBtn = Button(this).apply {
            text = "Open Settings"
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            isAllCaps = false
            setTextColor(darkText)
            stateListAnimator = null
            background = rounded(warning, dp(12))
            setPadding(dp(14), dp(8), dp(14), dp(8))
            setOnClickListener { openOverlaySettings() }
        }
        permBtnRow.addView(openSettingsBtn)
        val howTo = TextView(this).apply {
            text = "How to allow access"
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(warning)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), 0, 0, 0)
            setOnClickListener { showPermissionHelp() }
        }
        permBtnRow.addView(howTo)
        permissionCard.addView(permBtnRow)
        shieldCard.addView(permissionCard)
        shieldCard.addView(Space(this).apply { minimumHeight = dp(16) })

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
                if (!hasOverlayPermission()) {
                    showPermissionHelp()
                    return@setOnClickListener
                }
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

        // Summary when active
        summaryCard = cardLayout()
        summaryCard.addView(label("SHIELD IS ACTIVE", 11f, accent, bold = true).apply { letterSpacing = 0.08f })
        summaryMode = summaryRow(summaryCard, "Colour mode", "Warm")
        summaryDim = summaryRow(summaryCard, "Overlay darkness", "20%")
        summaryWarm = summaryRow(summaryCard, "Tint intensity", "35%")
        summaryRemind = summaryRow(summaryCard, "Rest reminder", "Every 60 mins")
        summaryCard.visibility = View.GONE
        root.addView(summaryCard)
        root.addView(Space(this).apply { minimumHeight = dp(16) })

        // Compact benefit rows
        val benefits = cardLayout()
        benefits.addView(label("WHY BLINKGUARD", 11f, accent, bold = true).apply { letterSpacing = 0.08f })
        benefits.addView(benefitRow("◐", "4 colour modes", "Natural, Warm, Amber and Red tints"))
        benefits.addView(benefitRow("▦", "Works across apps", "Tint stays on over WhatsApp, YouTube and more"))
        benefits.addView(benefitRow("◷", "Rest reminders", "A gentle break reminder while the shield is on"))
        root.addView(benefits)

        scroll.addView(root)
        return scroll
    }

    private fun summaryRow(parent: LinearLayout, title: String, value: String): TextView {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, dp(2))
        }
        row.addView(label(title, 14f, grey).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        val valueView = label(value, 14f, white, bold = true)
        row.addView(valueView)
        parent.addView(row)
        return valueView
    }

    private fun benefitRow(icon: String, title: String, sub: String): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(8))
        }
        row.addView(TextView(this).apply {
            text = icon
            textSize = 18f
            setTextColor(accent)
            gravity = Gravity.CENTER
            background = rounded(panel, dp(12), border, dp(1))
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
        })
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }
        col.addView(label(title, 14f, white, bold = true))
        col.addView(label(sub, 13f, grey))
        row.addView(col)
        return row
    }

    // ---------- Settings page ----------

    private fun buildSettingsPage(): ScrollView {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(6), dp(20), dp(28))
        }

        // Tint profile
        val tintCard = cardLayout()
        tintCard.addView(label("TINT PROFILE", 11f, accent, bold = true).apply { letterSpacing = 0.08f })
        tintCard.addView(label("Choose your tint", 18f, white, bold = true).apply { setPadding(0, dp(6), 0, dp(12)) })
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
            rowModes.forEach { (mode, dotColor) -> row.addView(makeTile(mode, dotColor)) }
            tileGrid.addView(row)
            tileGrid.addView(Space(this).apply { minimumHeight = dp(10) })
        }
        tintCard.addView(tileGrid)
        root.addView(tintCard)
        root.addView(Space(this).apply { minimumHeight = dp(16) })

        // Fine-tune + live preview
        val tuneCard = cardLayout()
        tuneCard.addView(label("FINE-TUNE", 11f, accent, bold = true).apply { letterSpacing = 0.08f })
        tuneCard.addView(label("Adjust the tint", 18f, white, bold = true).apply { setPadding(0, dp(6), 0, dp(4)) })
        dimValue = valuePill("$dimLevel%")
        tuneCard.addView(sliderHeader("Overlay darkness", dimValue))
        dimSeekBar = styledSeekBar(max = 75, initial = dimLevel) { progress ->
            dimLevel = progress
            dimValue.text = "$progress%"
            savePrefs()
            updateProfile()
            updatePreview()
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
            updatePreview()
            if (EyeShieldService.isRunning) startShieldService("UPDATE_FILTER")
        }
        tuneCard.addView(warmSeekBar)
        tuneCard.addView(minMaxRow("Light", "Strong"))
        tuneCard.addView(label("LIVE PREVIEW (APPROXIMATE)", 11f, accent, bold = true).apply {
            letterSpacing = 0.08f
            setPadding(0, dp(16), 0, dp(8))
        })
        previewImage = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(150)
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = rounded(panel, dp(14), border, dp(1))
        }
        tuneCard.addView(previewImage)
        tuneCard.addView(label("Approximate preview only — the real tint appears over other apps.", 12f, grey).apply {
            setPadding(0, dp(8), 0, 0)
        })
        root.addView(tuneCard)
        root.addView(Space(this).apply { minimumHeight = dp(16) })

        // Rest reminders
        val restCard = cardLayout()
        restCard.addView(label("REST REMINDERS", 11f, accent, bold = true).apply { letterSpacing = 0.08f })
        restCard.addView(label("When should your reminder appear?", 18f, white, bold = true).apply {
            setPadding(0, dp(6), 0, dp(4))
        })
        reminderSwitch = Switch(this).apply {
            text = "Reminders on"
            textSize = 14f
            setTextColor(lightGrey)
            isChecked = remindersOn
            setPadding(0, dp(4), 0, dp(10))
            setOnCheckedChangeListener { _, checked ->
                remindersOn = checked
                savePrefs()
                updateReminderStatus()
                if (EyeShieldService.isRunning) startShieldService("UPDATE_TIMER")
            }
        }
        restCard.addView(reminderSwitch)
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
            text = "Test Reminder Now"
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            isAllCaps = false
            setTextColor(white)
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(14), dp(16), dp(14))
            stateListAnimator = null
            background = rounded(panel, dp(15), border, dp(1))
            setOnClickListener {
                if (!hasOverlayPermission()) {
                    showPermissionHelp()
                    return@setOnClickListener
                }
                startShieldService("TEST_POPUP")
            }
        }
        restCard.addView(testBtn)
        root.addView(restCard)
        root.addView(Space(this).apply { minimumHeight = dp(16) })

        // Reset
        val resetBtn = Button(this).apply {
            text = "Reset to defaults"
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            isAllCaps = false
            setTextColor(lightGrey)
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(14), dp(16), dp(14))
            stateListAnimator = null
            background = rounded(panel, dp(15), border, dp(1))
            setOnClickListener { resetDefaults() }
        }
        root.addView(resetBtn)

        scroll.addView(root)
        return scroll
    }

    // ---------- About page ----------

    private fun buildAboutPage(): ScrollView {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(6), dp(20), dp(28))
        }

        val aboutCard = cardLayout()
        aboutCard.addView(label("ABOUT", 11f, accent, bold = true).apply { letterSpacing = 0.08f })
        aboutCard.addView(label("BlinkGuard", 22f, white, bold = true).apply { setPadding(0, dp(6), 0, 0) })
        aboutCard.addView(label("System-wide eye comfort — screen tint and rest reminders.", 13f, grey).apply {
            setPadding(0, dp(4), 0, dp(12))
        })
        aboutCard.addView(label("Version 1.3", 14f, white, bold = true))
        aboutPermissionText = label("", 13f, grey).apply { setPadding(0, dp(4), 0, 0) }
        aboutCard.addView(aboutPermissionText)
        root.addView(aboutCard)
        root.addView(Space(this).apply { minimumHeight = dp(16) })

        val helpCard = cardLayout()
        helpCard.addView(label("PERMISSION HELP", 11f, accent, bold = true).apply { letterSpacing = 0.08f })
        helpCard.addView(label("Display over other apps", 18f, white, bold = true).apply { setPadding(0, dp(6), 0, dp(4)) })
        helpCard.addView(label("Eye Shield needs this permission to show its tint over other apps. Android controls it — BlinkGuard cannot allow it by itself.", 13f, grey).apply {
            setPadding(0, 0, 0, dp(12))
        })
        val helpBtn = Button(this).apply {
            text = "How to allow access"
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            isAllCaps = false
            setTextColor(darkText)
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(14), dp(16), dp(14))
            stateListAnimator = null
            background = rounded(accent, dp(15))
            setOnClickListener { showPermissionHelp() }
        }
        helpCard.addView(helpBtn)
        helpCard.addView(Space(this).apply { minimumHeight = dp(10) })
        val openBtn = Button(this).apply {
            text = "Open Permission Settings"
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            isAllCaps = false
            setTextColor(white)
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(14), dp(16), dp(14))
            stateListAnimator = null
            background = rounded(panel, dp(15), border, dp(1))
            setOnClickListener { openOverlaySettings() }
        }
        helpCard.addView(openBtn)
        root.addView(helpCard)
        root.addView(Space(this).apply { minimumHeight = dp(16) })

        val privacyCard = cardLayout()
        privacyCard.addView(label("PRIVACY", 11f, accent, bold = true).apply { letterSpacing = 0.08f })
        privacyCard.addView(label("No data leaves your phone.", 18f, white, bold = true).apply {
            setPadding(0, dp(6), 0, dp(4))
        })
        privacyCard.addView(label("BlinkGuard has no account, no internet use and no ads. Your tint and reminder settings stay on this device only. The overlay draws a tint on top of the screen — it does not read, record or change what other apps show, and it does not change hardware brightness.", 13f, grey))
        root.addView(privacyCard)

        scroll.addView(root)
        return scroll
    }

    // ---------- Permission help ----------

    private fun showPermissionHelp() {
        AlertDialog.Builder(this)
            .setTitle("How to allow access")
            .setMessage(
                "1. Tap Open Settings below.\n" +
                "2. If Android shows a denied-access warning, go back and open Settings > Apps > BlinkGuard.\n" +
                "3. Tap the three-dot menu at the top right (if shown) and choose 'Allow restricted settings'.\n" +
                "4. Go back to 'Display over other apps' and turn the switch on.\n" +
                "5. Return to BlinkGuard — the permission is checked again automatically.\n\n" +
                "Menu names can differ slightly by phone brand and Android version. Only allow this if you trust BlinkGuard's source."
            )
            .setPositiveButton("Open Settings") { _, _ -> openOverlaySettings() }
            .setNeutralButton("Why is this needed?") { _, _ -> showWhyNeeded() }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showWhyNeeded() {
        AlertDialog.Builder(this)
            .setTitle("Why is this needed?")
            .setMessage(
                "Eye Shield is a tint layer drawn over the whole screen, so it can soften every app you use — WhatsApp, YouTube and more. " +
                "Android only lets an app draw over others with your explicit permission, and sideloaded apps get an extra 'restricted settings' step. " +
                "BlinkGuard cannot grant this permission itself. The overlay only draws a tint; it does not read or record other apps."
            )
            .setPositiveButton("Got it", null)
            .show()
    }

    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(this) else true
    }

    // ---------- Live preview ----------

    private fun updatePreview() {
        if (!::previewImage.isInitialized) return
        val w = 720
        val h = 360
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        // Sky gradient (bands)
        for (y in 0 until h) {
            val f = y.toFloat() / h
            val r = (170 + (245 - 170) * f).toInt()
            val g = (200 + (220 - 200) * f).toInt()
            val b = (230 + (200 - 230) * f).toInt()
            paint.color = Color.rgb(r, g, b)
            canvas.drawRect(0f, y.toFloat(), w.toFloat(), (y + 1).toFloat(), paint)
        }
        // Sun
        paint.color = Color.rgb(255, 220, 120)
        canvas.drawCircle(w * 0.78f, h * 0.26f, 34f, paint)
        // Mountains
        paint.color = Color.rgb(90, 120, 110)
        val back = Path().apply {
            moveTo(0f, h * 0.62f)
            lineTo(w * 0.22f, h * 0.30f)
            lineTo(w * 0.45f, h * 0.62f)
            lineTo(w * 0.68f, h * 0.24f)
            lineTo(w.toFloat(), h * 0.62f)
            lineTo(w.toFloat(), h.toFloat())
            lineTo(0f, h.toFloat())
            close()
        }
        canvas.drawPath(back, paint)
        paint.color = Color.rgb(60, 85, 75)
        val front = Path().apply {
            moveTo(0f, h * 0.78f)
            lineTo(w * 0.30f, h * 0.48f)
            lineTo(w * 0.58f, h * 0.78f)
            lineTo(w * 0.85f, h * 0.52f)
            lineTo(w.toFloat(), h * 0.78f)
            lineTo(w.toFloat(), h.toFloat())
            lineTo(0f, h.toFloat())
            close()
        }
        canvas.drawPath(front, paint)
        // Tint overlay, same formula as the service
        val dimAlpha = (dimLevel * 1.8f).toInt().coerceIn(0, 135)
        val tintAlpha = (warmthLevel * 1.25f).toInt().coerceIn(0, 100)
        val tint = when (selectedMode) {
            "NATURAL" -> Color.argb(dimAlpha, 0, 0, 0)
            "WARM" -> Color.argb((dimAlpha + tintAlpha).coerceAtMost(190), 255, 170, 80)
            "AMBER" -> Color.argb((dimAlpha + tintAlpha).coerceAtMost(190), 255, 125, 20)
            "RED" -> Color.argb((dimAlpha + tintAlpha).coerceAtMost(190), 220, 35, 35)
            else -> Color.argb((dimAlpha + tintAlpha).coerceAtMost(190), 255, 170, 80)
        }
        paint.color = tint
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        previewImage.setImageDrawable(BitmapDrawable(resources, bitmap))
    }

    // ---------- State / prefs ----------

    private fun loadPrefs() {
        val prefs = getSharedPreferences("blinkguard", MODE_PRIVATE)
        selectedMode = prefs.getString("MODE", selectedMode) ?: selectedMode
        dimLevel = prefs.getInt("DIM", dimLevel)
        warmthLevel = prefs.getInt("WARMTH", warmthLevel)
        selectedLimit = prefs.getInt("LIMIT", selectedLimit)
        remindersOn = prefs.getBoolean("REMINDERS_ON", remindersOn)
    }

    private fun savePrefs() {
        getSharedPreferences("blinkguard", MODE_PRIVATE).edit()
            .putString("MODE", selectedMode)
            .putInt("DIM", dimLevel)
            .putInt("WARMTH", warmthLevel)
            .putInt("LIMIT", selectedLimit)
            .putBoolean("REMINDERS_ON", remindersOn)
            .apply()
    }

    private fun resetDefaults() {
        selectedMode = "WARM"
        dimLevel = 20
        warmthLevel = 35
        selectedLimit = 60
        remindersOn = true
        if (::dimSeekBar.isInitialized) dimSeekBar.progress = dimLevel
        if (::warmSeekBar.isInitialized) warmSeekBar.progress = warmthLevel
        if (::dimValue.isInitialized) dimValue.text = "$dimLevel%"
        if (::warmValue.isInitialized) warmValue.text = "$warmthLevel%"
        if (::reminderSwitch.isInitialized) reminderSwitch.isChecked = true
        savePrefs()
        refreshAll()
        if (EyeShieldService.isRunning) startShieldService("UPDATE_FILTER")
        Toast.makeText(this, "Defaults restored", Toast.LENGTH_SHORT).show()
    }

    private fun refreshAll() {
        refreshTiles()
        refreshIntervalChips()
        updateProfile()
        updateReminderStatus()
        refreshPermissionStatus()
        updateToggle(EyeShieldService.isRunning)
        updatePreview()
        updateSummary()
    }

    private fun refreshPermissionStatus() {
        val granted = hasOverlayPermission()
        if (::permissionCard.isInitialized) {
            permissionCard.visibility = if (granted) View.GONE else View.VISIBLE
        }
        if (::aboutPermissionText.isInitialized) {
            aboutPermissionText.text = if (granted) {
                "Overlay permission: granted"
            } else {
                "Overlay permission: required — see Permission Help below"
            }
        }
    }

    private fun updateSummary() {
        if (!::summaryCard.isInitialized) return
        val running = EyeShieldService.isRunning && hasOverlayPermission()
        summaryCard.visibility = if (running) View.VISIBLE else View.GONE
        if (running) {
            summaryMode.text = when (selectedMode) {
                "NATURAL" -> "Natural"
                "WARM" -> "Warm"
                "AMBER" -> "Amber"
                "RED" -> "Red"
                else -> "Warm"
            }
            summaryDim.text = "$dimLevel%"
            summaryWarm.text = "$warmthLevel%"
            summaryRemind.text = if (remindersOn) "Every $selectedLimit mins" else "Off"
        }
    }

    private fun updateToggle(running: Boolean) {
        val active = running && hasOverlayPermission()
        if (::toggleBtn.isInitialized) {
            toggleBtn.text = if (active) "Turn Off Eye Shield" else "Enable Eye Shield"
            toggleBtn.background = if (active) rounded(card, dp(15), accent, dp(1)) else rounded(accent, dp(15))
            toggleBtn.setTextColor(if (active) accent else darkText)
        }
        if (::statusBadge.isInitialized) {
            if (active) {
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
        updateSummary()
    }

    // ---------- Shared helpers ----------

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
            updatePreview()
            if (EyeShieldService.isRunning) startShieldService("UPDATE_FILTER")
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
        updatePreview()
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
        reminderStatus.text = when {
            !remindersOn -> "Reminders are off."
            EyeShieldService.isRunning -> "Reminder every $selectedLimit minutes while the shield is on."
            else -> "Reminders start when the Eye Shield is on."
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

    private fun startShieldService(actionType: String) {
        val intent = Intent(this, EyeShieldService::class.java).apply {
            action = actionType
            putExtra("DIM", dimLevel)
            putExtra("WARMTH", warmthLevel)
            putExtra("MODE", selectedMode)
            putExtra("LIMIT_MINS", selectedLimit)
            putExtra("REMINDERS_ON", remindersOn)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
            updateToggle(true)
        } catch (e: Exception) {
            Toast.makeText(this, "Could not start service: ${e.message ?: "unknown error"}", Toast.LENGTH_LONG).show()
        }
    }
}
