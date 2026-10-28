package com.blinkguard.eyecare

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
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
    private lateinit var permissionCard: LinearLayout
    private lateinit var setupActiveRow: LinearLayout
    private lateinit var reminderStatus: TextView
    private lateinit var currentMinutes: TextView
    private lateinit var reminderSwitch: Switch
    private lateinit var dimValue: TextView
    private lateinit var warmValue: TextView
    private lateinit var dimSeekBar: SeekBar
    private lateinit var warmSeekBar: SeekBar
    private lateinit var previewImage: ImageView
    private lateinit var homeScroll: ScrollView
    private lateinit var settingsScroll: ScrollView
    private lateinit var aboutScroll: ScrollView
    private lateinit var aboutPermissionText: TextView
    private lateinit var statusDot: View
    private lateinit var statusText: TextView
    private lateinit var setupMode: TextView
    private lateinit var setupDim: TextView
    private lateinit var setupWarm: TextView
    private lateinit var setupRemind: TextView
    private lateinit var aboutSetupMode: TextView
    private lateinit var aboutSetupDim: TextView
    private lateinit var aboutSetupWarm: TextView
    private lateinit var aboutSetupRemind: TextView
    private var pulseAnimator: ObjectAnimator? = null
    private val navIcons = mutableListOf<IconView>()
    private val navLabels = mutableListOf<TextView>()
    private val tileLayouts = mutableMapOf<String, LinearLayout>()
    private val tileLabels = mutableMapOf<String, TextView>()
    private val intervalButtons = mutableMapOf<Int, LinearLayout>()
    private val intervalNumbers = mutableMapOf<Int, TextView>()

    private val bg = Color.rgb(11, 14, 18)
    private val card = Color.rgb(21, 26, 32)
    private val panel = Color.rgb(13, 17, 22)
    private val border = Color.rgb(48, 55, 65)
    private val accent = Color.rgb(117, 224, 160)
    private val accentDark = Color.rgb(18, 59, 40)
    private val warm = Color.rgb(215, 183, 121)
    private val warning = Color.rgb(231, 182, 92)
    private val orange = Color.rgb(240, 130, 30)
    private val blue = Color.rgb(59, 130, 246)
    private val blueText = Color.rgb(147, 197, 253)
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

        // Bottom navigation with icons
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = rounded(panel, 0, border, dp(1))
            setPadding(dp(8), dp(8), dp(8), dp(12))
        }
        listOf(Triple("home", "Home", 0), Triple("gear", "Settings", 1), Triple("info", "About", 2)).forEach { (icon, name, tab) ->
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setPadding(0, dp(4), 0, dp(4))
            }
            val iconView = IconView(this, icon, grey).apply {
                layoutParams = LinearLayout.LayoutParams(dp(24), dp(24))
            }
            val labelView = TextView(this).apply {
                text = name
                textSize = 12f
                gravity = Gravity.CENTER
                setTextColor(grey)
                setPadding(0, dp(2), 0, 0)
            }
            item.addView(iconView)
            item.addView(labelView)
            item.setOnClickListener { switchTab(tab) }
            navIcons.add(iconView)
            navLabels.add(labelView)
            nav.addView(item)
        }
        root.addView(nav)

        setContentView(root)
        switchTab(0)
        refreshAll()
    }

    override fun onResume() {
        super.onResume()
        refreshAll()
    }

    private fun switchTab(tab: Int) {
        homeScroll.visibility = if (tab == 0) View.VISIBLE else View.GONE
        settingsScroll.visibility = if (tab == 1) View.VISIBLE else View.GONE
        aboutScroll.visibility = if (tab == 2) View.VISIBLE else View.GONE
        navIcons.forEachIndexed { i, v -> v.setIconColor(if (i == tab) accent else grey) }
        navLabels.forEachIndexed { i, v ->
            v.setTextColor(if (i == tab) accent else grey)
            v.setTypeface(Typeface.DEFAULT, if (i == tab) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    // ---------- Home page (image screen 1 / 4) ----------

    private fun buildHomePage(): ScrollView {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(28))
        }

        // Logo + wordmark, centred — minimal
        val logo = ImageView(this).apply {
            setImageResource(R.drawable.logo)
            layoutParams = LinearLayout.LayoutParams(dp(84), dp(84)).apply { gravity = Gravity.CENTER_HORIZONTAL }
        }
        root.addView(logo)
        val wordmark = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dp(8), 0, 0)
        }
        wordmark.addView(label("Blink", 30f, white, bold = true))
        wordmark.addView(label("Guard", 30f, accent, bold = true))
        root.addView(wordmark)
        root.addView(label("System-Wide Eye Filter &\nScreen-Time Reminder", 14f, grey).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, 0)
        })
        // Live status pill
        val statusPill = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = rounded(panel, dp(16), border, dp(1))
            setPadding(dp(14), dp(7), dp(14), dp(7))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(14)
            }
        }
        statusDot = View(this).apply {
            background = rounded(grey, dp(6))
            layoutParams = LinearLayout.LayoutParams(dp(10), dp(10)).apply { marginEnd = dp(8) }
        }
        statusPill.addView(statusDot)
        statusText = label("Shield is off", 13f, lightGrey, bold = true)
        statusPill.addView(statusText)
        root.addView(statusPill)
        root.addView(Space(this).apply { minimumHeight = dp(16) })

        // Permission card (only when permission is missing)
        permissionCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(panel, dp(14), warning, dp(1))
            setPadding(dp(14), dp(12), dp(14), dp(12))
            visibility = View.GONE
        }
        permissionCard.addView(label("One step left", 15f, warning, bold = true))
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
        root.addView(permissionCard)

        // Enable / Turn Off button
        toggleBtn = Button(this).apply {
            text = "▶  Enable System-Wide\nEye Shield (All Apps)"
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            isAllCaps = false
            setTextColor(darkText)
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(16), dp(16), dp(16))
            stateListAnimator = null
            background = rounded(accent, dp(14))
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
        val toggleWrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(16), 0, 0)
        }
        toggleWrap.addView(toggleBtn)
        root.addView(toggleWrap)
        root.addView(Space(this).apply { minimumHeight = dp(16) })

        // Single card on Home: setup, with the Active state folded in
        val setupCard = cardLayout()
        setupActiveRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(10))
            visibility = View.GONE
        }
        setupActiveRow.addView(IconView(this, "check", accent).apply {
            layoutParams = LinearLayout.LayoutParams(dp(22), dp(22)).apply { marginEnd = dp(8) }
        })
        val activeTexts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        activeTexts.addView(label("Shield is Active", 15f, accent, bold = true))
        activeTexts.addView(label("Protecting your eyes across apps", 12f, grey))
        setupActiveRow.addView(activeTexts)
        setupCard.addView(setupActiveRow)
        setupCard.addView(label("YOUR SETUP", 11f, accent, bold = true).apply { letterSpacing = 0.08f })
        setupMode = summaryRow(setupCard, "sun", "Colour Mode", "Warm")
        setupDim = summaryRow(setupCard, "half", "Brightness Dimmer", "20%")
        setupWarm = summaryRow(setupCard, "sun", "Warmth Intensity", "35%")
        setupRemind = summaryRow(setupCard, "clock", "Rest Reminder", "Every 60 mins")
        setupCard.addView(label("Change it anytime in Settings.", 12f, grey).apply {
            setPadding(0, dp(6), 0, 0)
        })
        root.addView(setupCard)

        // Plain line under the card — no second card
        root.addView(label("Works across WhatsApp, YouTube and every app.", 13f, grey).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(16), 0, 0)
        })

        scroll.addView(root)
        return scroll
    }

    private fun summaryRow(parent: LinearLayout, icon: String, title: String, value: String): TextView {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(8))
        }
        row.addView(IconView(this, icon, warm).apply {
            layoutParams = LinearLayout.LayoutParams(dp(22), dp(22)).apply { marginEnd = dp(6) }
        })
        row.addView(label(title, 14f, lightGrey).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        val valueView = label(value, 14f, warm, bold = true)
        row.addView(valueView)
        parent.addView(row)
        return valueView
    }

    private fun stepRow(number: String, title: String, subtitle: String): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(9), 0, dp(9))
        }
        val badge = TextView(this).apply {
            text = number
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(darkText)
            gravity = Gravity.CENTER
            background = rounded(accent, dp(22))
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
        }
        row.addView(badge)
        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }
        texts.addView(label(title, 14f, lightGrey, bold = true))
        texts.addView(label(subtitle, 12f, grey).apply { setPadding(0, dp(2), 0, 0) })
        row.addView(texts)
        return row
    }

    private fun benefitRow(icon: String, iconColor: Int, title: String, subtitle: String): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(9), 0, dp(9))
        }
        val box = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            background = rounded(panel, dp(12), border, dp(1))
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
        }
        box.addView(IconView(this, icon, iconColor).apply {
            layoutParams = LinearLayout.LayoutParams(dp(24), dp(24))
        })
        row.addView(box)
        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }
        texts.addView(label(title, 14f, lightGrey, bold = true))
        texts.addView(label(subtitle, 12f, grey).apply { setPadding(0, dp(2), 0, 0) })
        row.addView(texts)
        return row
    }

    // ---------- Settings page (image screens 2 + 3) ----------

    private fun buildSettingsPage(): ScrollView {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(28))
        }

        root.addView(label("Eye-Safe Screen Colour Mode", 19f, white, bold = true))
        root.addView(label("(Works on WhatsApp, YouTube, etc.)", 13f, grey).apply {
            setPadding(0, dp(4), 0, dp(14))
        })

        // Colour mode tiles in one row
        val tileRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf(
            Triple("NATURAL", "diamond", grey),
            Triple("WARM", "sun", warm),
            Triple("AMBER", "sun", Color.rgb(232, 145, 45)),
            Triple("RED", "moon", Color.rgb(229, 72, 77))
        ).forEach { (mode, icon, iconColor) ->
            tileRow.addView(makeModeTile(mode, icon, iconColor))
        }
        root.addView(tileRow)
        root.addView(Space(this).apply { minimumHeight = dp(16) })

        // Sliders
        root.addView(label("Extra Screen Brightness Dimmer", 14f, lightGrey))
        val dimRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        dimRow.addView(IconView(this, "half", grey).apply {
            layoutParams = LinearLayout.LayoutParams(dp(22), dp(22)).apply { marginEnd = dp(8) }
        })
        dimValue = valuePill("$dimLevel%")
        val dimCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        dimSeekBar = styledSeekBar(max = 75, initial = dimLevel, barColor = accent) { progress ->
            dimLevel = progress
            dimValue.text = "$progress%"
            savePrefs()
            updatePreview()
            if (EyeShieldService.isRunning) startShieldService("UPDATE_FILTER")
        }
        dimCol.addView(dimSeekBar)
        dimRow.addView(dimCol)
        dimRow.addView(dimValue)
        root.addView(dimRow)
        root.addView(label("Blue-Light Warmth Intensity", 14f, lightGrey).apply {
            setPadding(0, dp(14), 0, 0)
        })
        val warmRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        warmRow.addView(IconView(this, "sun", Color.rgb(232, 145, 45)).apply {
            layoutParams = LinearLayout.LayoutParams(dp(22), dp(22)).apply { marginEnd = dp(8) }
        })
        warmValue = valuePill("$warmthLevel%")
        val warmCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        warmSeekBar = styledSeekBar(max = 80, initial = warmthLevel, barColor = Color.rgb(232, 145, 45)) { progress ->
            warmthLevel = progress
            warmValue.text = "$progress%"
            savePrefs()
            updatePreview()
            if (EyeShieldService.isRunning) startShieldService("UPDATE_FILTER")
        }
        warmCol.addView(warmSeekBar)
        warmRow.addView(warmCol)
        warmRow.addView(warmValue)
        root.addView(warmRow)
        root.addView(Space(this).apply { minimumHeight = dp(18) })

        // Live Preview: 4 panels
        root.addView(label("Live Preview (Approximate)", 13f, grey).apply {
            setPadding(0, 0, 0, dp(8))
        })
        previewImage = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(140)
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = rounded(panel, dp(14), border, dp(1))
        }
        root.addView(previewImage)
        root.addView(Space(this).apply { minimumHeight = dp(22) })

        // Time Limit card (image screen 3)
        root.addView(label("When should the Rest & Water\nPop-Up appear?", 19f, white, bold = true))
        root.addView(Space(this).apply { minimumHeight = dp(14) })
        val timeRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf(20, 45, 60, 90).forEach { mins ->
            timeRow.addView(makeIntervalTile(mins))
        }
        root.addView(timeRow)
        root.addView(Space(this).apply { minimumHeight = dp(14) })
        val currentCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(card, dp(18), border, dp(1))
            setPadding(dp(18), dp(16), dp(18), dp(16))
        }
        currentCard.addView(label("Current Setting", 13f, lightGrey).apply { gravity = Gravity.CENTER })
        currentMinutes = label("$selectedLimit Minutes", 20f, white, bold = true).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(6))
        }
        currentCard.addView(currentMinutes)
        reminderStatus = label("", 13f, grey).apply { gravity = Gravity.CENTER }
        currentCard.addView(reminderStatus)
        root.addView(currentCard)
        root.addView(Space(this).apply { minimumHeight = dp(14) })
        val testBtn = Button(this).apply {
            text = "Test Pop-Up Right Now"
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            isAllCaps = false
            setTextColor(white)
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(14), dp(16), dp(14))
            stateListAnimator = null
            background = rounded(panel, dp(14), blue, dp(1))
            setOnClickListener {
                if (!hasOverlayPermission()) {
                    showPermissionHelp()
                    return@setOnClickListener
                }
                startShieldService("TEST_POPUP")
            }
        }
        root.addView(testBtn)

        scroll.addView(root)
        return scroll
    }

    private fun makeModeTile(mode: String, icon: String, iconColor: Int): LinearLayout {
        val tile = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = rounded(panel, dp(14), border, dp(1))
            setPadding(dp(6), dp(14), dp(6), dp(14))
            layoutParams = LinearLayout.LayoutParams(0, dp(84), 1f).apply { marginEnd = dp(8) }
            isClickable = true
            isFocusable = true
        }
        val iconView = IconView(this, icon, iconColor).apply {
            layoutParams = LinearLayout.LayoutParams(dp(26), dp(26))
        }
        tile.addView(iconView)
        val labelView = label(mode, 12f, white, bold = true).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, 0)
        }
        tile.addView(labelView)
        tile.setOnClickListener {
            selectedMode = mode
            savePrefs()
            refreshTiles()
            updatePreview()
            if (EyeShieldService.isRunning) startShieldService("UPDATE_FILTER")
        }
        tileLayouts[mode] = tile
        tileLabels[mode] = labelView
        return tile
    }

    private fun makeIntervalTile(mins: Int): LinearLayout {
        val tile = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = rounded(panel, dp(14), border, dp(1))
            setPadding(dp(6), dp(14), dp(6), dp(14))
            layoutParams = LinearLayout.LayoutParams(0, dp(76), 1f).apply { marginEnd = dp(8) }
            isClickable = true
            isFocusable = true
        }
        val number = TextView(this).apply {
            text = "$mins"
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(white)
            gravity = Gravity.CENTER
        }
        tile.addView(number)
        tile.addView(label("mins", 12f, grey).apply { gravity = Gravity.CENTER })
        tile.setOnClickListener {
            selectedLimit = mins
            savePrefs()
            refreshIntervalChips()
            updateReminderStatus()
            if (EyeShieldService.isRunning) startShieldService("UPDATE_TIMER")
        }
        intervalButtons[mins] = tile
        intervalNumbers[mins] = number
        return tile
    }

    private fun refreshTiles() {
        tileLayouts.forEach { (mode, tile) ->
            val selected = mode == selectedMode
            tile.background = if (selected) rounded(panel, dp(14), warm, dp(2)) else rounded(panel, dp(14), border, dp(1))
            tileLabels[mode]?.setTextColor(if (selected) warm else white)
        }
    }

    private fun refreshIntervalChips() {
        intervalButtons.forEach { (mins, tile) ->
            val selected = mins == selectedLimit
            tile.background = if (selected) rounded(panel, dp(14), accent, dp(2)) else rounded(panel, dp(14), border, dp(1))
            intervalNumbers[mins]?.setTextColor(if (selected) accent else white)
        }
    }

    // ---------- About page ----------

    private fun buildAboutPage(): ScrollView {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(28))
        }

        // Logo header, matching the Home page — minimal
        root.addView(ImageView(this).apply {
            setImageResource(R.drawable.logo)
            layoutParams = LinearLayout.LayoutParams(dp(72), dp(72)).apply { gravity = Gravity.CENTER_HORIZONTAL }
        })
        val aboutWordmark = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dp(8), 0, 0)
        }
        aboutWordmark.addView(label("Blink", 26f, white, bold = true))
        aboutWordmark.addView(label("Guard", 26f, accent, bold = true))
        root.addView(aboutWordmark)
        root.addView(label("Your eyes deserve a break.", 13f, grey).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, 0)
        })
        root.addView(TextView(this).apply {
            text = "Version 1.7"
            textSize = 12f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(accent)
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(5), dp(12), dp(5))
            background = rounded(accentDark, dp(14), accent, dp(1))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(10)
                bottomMargin = dp(16)
            }
        })

        val aboutCard = cardLayout()
        aboutCard.addView(label("ABOUT", 11f, accent, bold = true).apply { letterSpacing = 0.08f })
        aboutCard.addView(label("System-wide eye comfort — screen tint and rest reminders.", 14f, white, bold = true).apply {
            setPadding(0, dp(6), 0, dp(8))
        })
        aboutPermissionText = label("", 13f, grey).apply { setPadding(0, dp(4), 0, 0) }
        aboutCard.addView(aboutPermissionText)
        root.addView(aboutCard)
        root.addView(Space(this).apply { minimumHeight = dp(16) })

        // Your setup — live values, same as Home
        val aboutSetupCard = cardLayout()
        aboutSetupCard.addView(label("YOUR SETUP", 11f, accent, bold = true).apply { letterSpacing = 0.08f })
        aboutSetupMode = summaryRow(aboutSetupCard, "sun", "Colour Mode", "Warm")
        aboutSetupDim = summaryRow(aboutSetupCard, "half", "Brightness Dimmer", "20%")
        aboutSetupWarm = summaryRow(aboutSetupCard, "sun", "Warmth Intensity", "35%")
        aboutSetupRemind = summaryRow(aboutSetupCard, "clock", "Rest Reminder", "Every 60 mins")
        aboutSetupCard.addView(label("Tap Settings to change your tint and reminders.", 12f, grey).apply {
            setPadding(0, dp(6), 0, 0)
        })
        root.addView(aboutSetupCard)
        root.addView(Space(this).apply { minimumHeight = dp(16) })

        // How it works — 3 steps
        val howCard = cardLayout()
        howCard.addView(label("HOW IT WORKS", 11f, accent, bold = true).apply { letterSpacing = 0.08f })
        howCard.addView(stepRow("1", "Allow overlay access", "One time, in Android settings"))
        howCard.addView(stepRow("2", "Tap Enable on Home", "The tint covers every app"))
        howCard.addView(stepRow("3", "Rest when reminded", "Look away, blink, drink water"))
        root.addView(howCard)
        root.addView(Space(this).apply { minimumHeight = dp(16) })

        // Why it is safe — scannable features
        val featuresCard = cardLayout()
        featuresCard.addView(label("BUILT FOR PRIVACY", 11f, accent, bold = true).apply { letterSpacing = 0.08f })
        featuresCard.addView(benefitRow("check", accent, "No account needed", "Open the app and use it"))
        featuresCard.addView(benefitRow("bolt", warm, "Works offline", "No internet, no ads"))
        featuresCard.addView(benefitRow("heart", Color.rgb(244, 114, 182), "Nothing leaves your phone", "No tracking, no data collection"))
        root.addView(featuresCard)
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

        val reminderCard = cardLayout()
        reminderCard.addView(label("REMINDERS", 11f, accent, bold = true).apply { letterSpacing = 0.08f })
        reminderSwitch = Switch(this).apply {
            text = "Rest reminders on"
            textSize = 14f
            setTextColor(lightGrey)
            isChecked = remindersOn
            thumbTintList = ColorStateList.valueOf(accent)
            trackTintList = ColorStateList.valueOf(border)
            setPadding(0, dp(8), 0, dp(4))
            setOnCheckedChangeListener { _, checked ->
                remindersOn = checked
                savePrefs()
                updateReminderStatus()
                if (EyeShieldService.isRunning) startShieldService("UPDATE_TIMER")
            }
        }
        reminderCard.addView(reminderSwitch)
        val resetBtn = Button(this).apply {
            text = "Reset to defaults"
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            isAllCaps = false
            setTextColor(lightGrey)
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(12), dp(16), dp(12))
            stateListAnimator = null
            background = rounded(panel, dp(15), border, dp(1))
            setOnClickListener { resetDefaults() }
        }
        reminderCard.addView(resetBtn)
        root.addView(reminderCard)
        root.addView(Space(this).apply { minimumHeight = dp(16) })

        val privacyCard = cardLayout()
        privacyCard.addView(label("PRIVACY", 11f, accent, bold = true).apply { letterSpacing = 0.08f })
        privacyCard.addView(label("No data leaves your phone.", 18f, white, bold = true).apply {
            setPadding(0, dp(6), 0, dp(4))
        })
        privacyCard.addView(label("BlinkGuard has no account, no internet use and no ads. Your tint and reminder settings stay on this device only. The overlay draws a tint on top of the screen — it does not read, record or change what other apps show, and it does not change hardware brightness.", 13f, grey))
        root.addView(privacyCard)
        root.addView(label("Made for healthier screen time", 13f, lightGrey, bold = true).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(20), 0, 0)
        })
        root.addView(label("BlinkGuard • Version 1.7", 12f, grey).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, 0)
        })

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

    // ---------- Live preview: 4 panels, one per mode ----------

    private fun tintForMode(mode: String): Int {
        val dimAlpha = (dimLevel * 1.8f).toInt().coerceIn(0, 135)
        val tintAlpha = (warmthLevel * 1.25f).toInt().coerceIn(0, 100)
        return when (mode) {
            "NATURAL" -> Color.argb(dimAlpha, 0, 0, 0)
            "WARM" -> Color.argb((dimAlpha + tintAlpha).coerceAtMost(190), 255, 170, 80)
            "AMBER" -> Color.argb((dimAlpha + tintAlpha).coerceAtMost(190), 255, 125, 20)
            "RED" -> Color.argb((dimAlpha + tintAlpha).coerceAtMost(190), 220, 35, 35)
            else -> Color.argb((dimAlpha + tintAlpha).coerceAtMost(190), 255, 170, 80)
        }
    }

    private fun updatePreview() {
        if (!::previewImage.isInitialized) return
        val panelW = 240
        val h = 300
        val w = panelW * 4
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val modes = listOf("NATURAL", "WARM", "AMBER", "RED")
        modes.forEachIndexed { i, mode ->
            val left = (i * panelW).toFloat()
            drawScenePanel(canvas, left, panelW.toFloat(), h.toFloat(), tintForMode(mode), mode)
        }
        previewImage.setImageDrawable(BitmapDrawable(resources, bitmap))
    }

    private fun drawScenePanel(canvas: Canvas, left: Float, pw: Float, ph: Float, tint: Int, modeLabel: String) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        for (y in 0 until ph.toInt()) {
            val f = y / ph
            paint.color = Color.rgb(
                (170 + (245 - 170) * f).toInt(),
                (200 + (220 - 200) * f).toInt(),
                (230 + (200 - 230) * f).toInt()
            )
            canvas.drawRect(left, y.toFloat(), left + pw, (y + 1).toFloat(), paint)
        }
        paint.color = Color.rgb(255, 220, 120)
        canvas.drawCircle(left + pw * 0.74f, ph * 0.26f, ph * 0.11f, paint)
        paint.color = Color.rgb(90, 120, 110)
        val back = Path().apply {
            moveTo(left, ph * 0.62f)
            lineTo(left + pw * 0.22f, ph * 0.30f)
            lineTo(left + pw * 0.45f, ph * 0.62f)
            lineTo(left + pw * 0.68f, ph * 0.24f)
            lineTo(left + pw, ph * 0.62f)
            lineTo(left + pw, ph)
            lineTo(left, ph)
            close()
        }
        canvas.drawPath(back, paint)
        paint.color = Color.rgb(60, 85, 75)
        val front = Path().apply {
            moveTo(left, ph * 0.78f)
            lineTo(left + pw * 0.30f, ph * 0.48f)
            lineTo(left + pw * 0.58f, ph * 0.78f)
            lineTo(left + pw * 0.85f, ph * 0.52f)
            lineTo(left + pw, ph * 0.78f)
            lineTo(left + pw, ph)
            lineTo(left, ph)
            close()
        }
        canvas.drawPath(front, paint)
        paint.color = tint
        canvas.drawRect(left, 0f, left + pw, ph, paint)
        // Label chip
        paint.color = Color.argb(150, 0, 0, 0)
        canvas.drawRect(left + 8f, ph - 44f, left + pw - 8f, ph - 8f, paint)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = white
            textSize = 26f
            textAlign = Paint.Align.CENTER
            setTypeface(Typeface.DEFAULT_BOLD)
        }
        canvas.drawText(modeLabel, left + pw / 2, ph - 16f, textPaint)
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
        updateReminderStatus()
        refreshPermissionStatus()
        updateToggle(EyeShieldService.isRunning)
        updatePreview()
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

    private fun modeDisplayName(): String {
        return when (selectedMode) {
            "NATURAL" -> "Natural"
            "WARM" -> "Warm"
            "AMBER" -> "Amber"
            "RED" -> "Red"
            else -> "Warm"
        }
    }

    private fun updateStatusPill(active: Boolean) {
        if (!::statusDot.isInitialized) return
        pulseAnimator?.cancel()
        pulseAnimator = null
        if (active) {
            statusDot.background = rounded(accent, dp(6))
            statusDot.alpha = 1f
            statusText.text = "Shield is ON — protecting your eyes"
            statusText.setTextColor(accent)
            pulseAnimator = ObjectAnimator.ofFloat(statusDot, "alpha", 1f, 0.35f).apply {
                duration = 900
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                start()
            }
        } else {
            statusDot.background = rounded(grey, dp(6))
            statusDot.alpha = 1f
            statusText.text = if (hasOverlayPermission()) {
                "Shield is off — tap Enable below"
            } else {
                "Shield is off — allow access first"
            }
            statusText.setTextColor(lightGrey)
        }
    }

    private fun updateSummary(active: Boolean) {
        if (::setupActiveRow.isInitialized) {
            setupActiveRow.visibility = if (active) View.VISIBLE else View.GONE
        }
        if (::setupMode.isInitialized) {
            setupMode.text = modeDisplayName()
            setupDim.text = "$dimLevel%"
            setupWarm.text = "$warmthLevel%"
            setupRemind.text = if (remindersOn) "Every $selectedLimit mins" else "Off"
        }
        if (::aboutSetupMode.isInitialized) {
            aboutSetupMode.text = modeDisplayName()
            aboutSetupDim.text = "$dimLevel%"
            aboutSetupWarm.text = "$warmthLevel%"
            aboutSetupRemind.text = if (remindersOn) "Every $selectedLimit mins" else "Off"
        }
        updateStatusPill(active)
    }

    private fun updateToggle(running: Boolean) {
        val active = running && hasOverlayPermission()
        if (::toggleBtn.isInitialized) {
            if (active) {
                toggleBtn.text = "⏸  Turn Off System-Wide\nShield (Running)"
                toggleBtn.background = rounded(orange, dp(14))
                toggleBtn.setTextColor(darkText)
            } else {
                toggleBtn.text = "▶  Enable System-Wide\nEye Shield (All Apps)"
                toggleBtn.background = rounded(accent, dp(14))
                toggleBtn.setTextColor(darkText)
            }
        }
        updateReminderStatus()
        refreshPermissionStatus()
        updateSummary(active)
    }

    // ---------- Shared helpers ----------

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
            background = rounded(card, dp(20), border, dp(1))
            setPadding(dp(18), dp(18), dp(18), dp(18))
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
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = dp(8) }
        }
    }

    private fun styledSeekBar(max: Int, initial: Int, barColor: Int, onChange: (Int) -> Unit): SeekBar {
        return SeekBar(this).apply {
            this.max = max
            progress = initial
            progressTintList = ColorStateList.valueOf(barColor)
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

    private fun updateReminderStatus() {
        if (::reminderStatus.isInitialized) {
            reminderStatus.text = when {
                !remindersOn -> "Reminders are off. Turn them on in About."
                EyeShieldService.isRunning -> "You will get a rest reminder after $selectedLimit minutes while the shield is on."
                else -> "You will get a rest reminder after $selectedLimit minutes while the shield is on."
            }
        }
        if (::currentMinutes.isInitialized) {
            currentMinutes.text = "$selectedLimit Minutes"
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
