package com.blinkguard.eyecare

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class EyeShieldService : Service() {
    private lateinit var windowManager: WindowManager
    private var filterView: android.view.View? = null
    private var popupView: android.view.View? = null
    private val handler = Handler(Looper.getMainLooper())
    private var limitMinutes = 60
    private var remindersOn = true
    private var screenOn = true
    private var remainingMs = 60 * 60_000L
    private var intervalStartedAt = 0L
    private var receiverRegistered = false

    companion object {
        @Volatile var isRunning = false
        @Volatile var currentDim = 20
        @Volatile var currentWarmth = 35
        @Volatile var currentMode = "WARM"
    }

    private val reminderRunnable = Runnable {
        if (isRunning && screenOn) {
            showSystemRestPopup()
            remainingMs = limitMinutes.coerceIn(1, 240) * 60_000L
            intervalStartedAt = android.os.SystemClock.elapsedRealtime()
            scheduleReminder()
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> pauseReminderClock()
                Intent.ACTION_SCREEN_ON -> resumeReminderClock()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForeground(101, buildNotification())
        isRunning = true
        screenOn = true
        registerScreenReceiver()
        createSystemFilterOverlay()
        resetReminderClock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_SHIELD" -> {
                currentDim = intent.getIntExtra("DIM", currentDim).coerceIn(0, 75)
                currentWarmth = intent.getIntExtra("WARMTH", currentWarmth).coerceIn(0, 80)
                currentMode = intent.getStringExtra("MODE") ?: currentMode
                limitMinutes = intent.getIntExtra("LIMIT_MINS", limitMinutes).coerceIn(1, 240)
                remindersOn = intent.getBooleanExtra("REMINDERS_ON", remindersOn)
                if (filterView == null) createSystemFilterOverlay()
                updateFilterColor()
                resetReminderClock()
                refreshNotification()
            }
            "UPDATE_FILTER" -> {
                currentDim = intent.getIntExtra("DIM", currentDim).coerceIn(0, 75)
                currentWarmth = intent.getIntExtra("WARMTH", currentWarmth).coerceIn(0, 80)
                currentMode = intent.getStringExtra("MODE") ?: currentMode
                if (filterView == null) createSystemFilterOverlay()
                updateFilterColor()
            }
            "UPDATE_TIMER" -> {
                limitMinutes = intent.getIntExtra("LIMIT_MINS", limitMinutes).coerceIn(1, 240)
                remindersOn = intent.getBooleanExtra("REMINDERS_ON", remindersOn)
                resetReminderClock()
                refreshNotification()
            }
            "TEST_POPUP" -> showSystemRestPopup()
            "RESET_TIMER" -> resetReminderClock()
            "SNOOZE" -> {
                removePopup()
                remainingMs = 10 * 60_000L
                intervalStartedAt = android.os.SystemClock.elapsedRealtime()
                scheduleReminder()
            }
            "STOP_SERVICE" -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }
        return START_STICKY
    }

    private fun registerScreenReceiver() {
        if (receiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerLegacyReceiver(filter)
        }
        receiverRegistered = true
    }

    @Suppress("DEPRECATION")
    private fun registerLegacyReceiver(filter: IntentFilter) {
        registerReceiver(screenReceiver, filter)
    }

    private fun pauseReminderClock() {
        if (!screenOn) return
        val elapsed = android.os.SystemClock.elapsedRealtime() - intervalStartedAt
        remainingMs = (remainingMs - elapsed).coerceAtLeast(0L)
        screenOn = false
        handler.removeCallbacks(reminderRunnable)
    }

    private fun resumeReminderClock() {
        if (screenOn) return
        screenOn = true
        intervalStartedAt = android.os.SystemClock.elapsedRealtime()
        scheduleReminder()
    }

    private fun resetReminderClock() {
        handler.removeCallbacks(reminderRunnable)
        remainingMs = limitMinutes.coerceIn(1, 240) * 60_000L
        intervalStartedAt = android.os.SystemClock.elapsedRealtime()
        if (screenOn && remindersOn) scheduleReminder()
    }

    private fun scheduleReminder() {
        handler.removeCallbacks(reminderRunnable)
        if (screenOn && isRunning && remindersOn) {
            intervalStartedAt = android.os.SystemClock.elapsedRealtime()
            handler.postDelayed(reminderRunnable, remainingMs.coerceAtLeast(1_000L))
        }
    }

    private fun createSystemFilterOverlay() {
        if (filterView != null || !Settings.canDrawOverlays(this)) return
        val view = android.view.View(this)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            // Cover the whole display, including under the status bar and cutout
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        try {
            windowManager.addView(view, params)
            filterView = view
            updateFilterColor()
        } catch (_: Exception) {
            filterView = null
        }
    }

    private fun updateFilterColor() {
        val view = filterView ?: return
        val dimAlpha = (currentDim * 1.8f).toInt().coerceIn(0, 135)
        val tintAlpha = (currentWarmth * 1.25f).toInt().coerceIn(0, 100)
        val color = when (currentMode) {
            "NATURAL" -> Color.argb(dimAlpha, 0, 0, 0)
            "WARM" -> Color.argb((dimAlpha + tintAlpha).coerceAtMost(190), 255, 170, 80)
            "AMBER" -> Color.argb((dimAlpha + tintAlpha).coerceAtMost(190), 255, 125, 20)
            "RED" -> Color.argb((dimAlpha + tintAlpha).coerceAtMost(190), 220, 35, 35)
            else -> Color.argb((dimAlpha + tintAlpha).coerceAtMost(190), 255, 170, 80)
        }
        view.setBackgroundColor(color)
    }

    private fun showSystemRestPopup() {
        if (popupView != null || !isRunning || !Settings.canDrawOverlays(this)) return

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(22), dp(22), dp(18))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#111827"))
                cornerRadius = dp(22).toFloat()
                setStroke(dp(2), Color.parseColor("#10B981"))
            }
        }
        container.addView(TextView(this).apply {
            text = "$limitMinutes MINS WHILE SHIELD IS ON"
            setTextColor(Color.parseColor("#34D399"))
            textSize = 12f
            gravity = Gravity.CENTER
        })
        container.addView(TextView(this).apply {
            text = "Rest Your Eyes &\nDrink Water"
            setTextColor(Color.WHITE)
            textSize = 22f
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(12))
        })
        container.addView(TextView(this).apply {
            text = "1. Look 20 feet away for 20 seconds\n2. Blink slowly 10 times to re-wet eyes\n3. Roll your shoulders\n4. Drink 1 glass of water"
            setTextColor(Color.parseColor("#D1D5DB"))
            textSize = 14f
            setPadding(0, 0, 0, dp(16))
        })
        container.addView(Button(this).apply {
            text = "✓  Done! Drank Water & Reset Timer"
            setOnClickListener {
                removePopup()
                resetReminderClock()
            }
        })
        container.addView(Button(this).apply {
            text = "Remind me later"
            setOnClickListener {
                removePopup()
                remainingMs = 10 * 60_000L
                intervalStartedAt = android.os.SystemClock.elapsedRealtime()
                scheduleReminder()
            }
        })

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            width = (resources.displayMetrics.widthPixels * 0.90f).toInt()
        }

        popupView = container
        try {
            windowManager.addView(container, params)
        } catch (_: Exception) {
            popupView = null
        }
    }

    private fun removePopup() {
        popupView?.let { view ->
            try { windowManager.removeView(view) } catch (_: Exception) {}
            popupView = null
        }
    }

    private fun buildNotification(): Notification {
        val channelId = "blinkguard_shield"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(channelId, "BlinkGuard Eye Shield", NotificationManager.IMPORTANCE_LOW)
            )
            return Notification.Builder(this, channelId)
                .setContentTitle("BlinkGuard is active")
                .setContentText("Screen tint enabled • Reminder every ${limitMinutes} min")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setOngoing(true)
                .build()
        }
        @Suppress("DEPRECATION")
        return Notification.Builder(this)
            .setContentTitle("BlinkGuard is active")
            .setContentText("Screen tint enabled")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()
    }

    private fun refreshNotification() {
        getSystemService(NotificationManager::class.java).notify(101, buildNotification())
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        if (receiverRegistered) {
            try { unregisterReceiver(screenReceiver) } catch (_: Exception) {}
            receiverRegistered = false
        }
        filterView?.let { try { windowManager.removeView(it) } catch (_: Exception) {} }
        filterView = null
        removePopup()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
