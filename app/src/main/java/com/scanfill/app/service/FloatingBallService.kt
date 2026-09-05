package com.scanfill.app.service

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import com.scanfill.app.MainActivity
import com.scanfill.app.R
import com.scanfill.app.prefs.Prefs
import com.scanfill.app.scan.ScanActivity
import com.scanfill.app.theme.ThemeEngine
import com.scanfill.app.util.Glass
import kotlin.math.abs

/**
 * 悬浮球服务：可拖动、贴边吸附（可半隐藏）、点击启动扫描。
 */
class FloatingBallService : android.app.Service() {

    companion object {
        @Volatile
        private var instance: FloatingBallService? = null

        fun start(context: Context) {
            ContextCompat_startForegroundService(context)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingBallService::class.java))
        }

        fun isRunning(): Boolean = instance != null

        /** 设置页修改大小/透明度后刷新 */
        fun refresh() {
            instance?.applyParams()
        }

        private fun ContextCompat_startForegroundService(context: Context) {
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(Intent(context, FloatingBallService::class.java))
            } else {
                context.startService(Intent(context, FloatingBallService::class.java))
            }
        }
    }

    private var wm: WindowManager? = null
    private var ball: FrameLayout? = null
    private var params: WindowManager.LayoutParams? = null

    /** 设置变化（大小/透明度/主题/玻璃风格等）立即重绘悬浮球 */
    private val prefListener =
        android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            applyParams()
        }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        createChannel()
        startForeground(1, buildNotification())
        Prefs.registerListener(prefListener)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "exit") {
            Prefs.ballEnabled = false
            stopSelf()
            return START_NOT_STICKY
        }
        if (Settings.canDrawOverlays(this)) {
            showBall()
        } else {
            stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        instance = null
        Prefs.unregisterListener(prefListener)
        ball?.let { try { wm?.removeView(it) } catch (e: Exception) {} }
        ball = null
        super.onDestroy()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showBall() {
        if (ball != null) return
        wm = getSystemService(WINDOW_SERVICE) as WindowManager

        val size = Glass.dp(this, Prefs.ballSize)
        val view = LayoutInflater.from(this).inflate(R.layout.floating_ball, null) as FrameLayout
        val icon = view.findViewById<ImageView>(R.id.iv_ball_icon)

        val dark = ThemeEngine.isDarkNow(this)
        val bg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            val base = if (dark) 0x161616 else 0xFFFFFF
            setColor((0xE6 shl 24) or base)
            if (Prefs.glassStyle == Prefs.GLASS_LIQUID) {
                setStroke(Glass.dp(this@FloatingBallService, 1), if (dark) 0x40FFFFFF else 0x59FFFFFF)
            }
        }
        view.background = bg
        icon.setColorFilter(if (dark) 0xFFFFFFFF.toInt() else 0xFF161616.toInt())
        view.alpha = Prefs.ballAlpha / 100f

        val type = if (Build.VERSION.SDK_INT >= 26) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val p = WindowManager.LayoutParams(
            size, size, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = resources.displayMetrics.heightPixels / 3
        }
        params = p

        view.setOnTouchListener(object : View.OnTouchListener {
            var downX = 0f
            var downY = 0f
            var startX = 0
            var startY = 0
            var moved = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.rawX
                        downY = event.rawY
                        startX = p.x
                        startY = p.y
                        moved = false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - downX
                        val dy = event.rawY - downY
                        if (abs(dx) > 8 || abs(dy) > 8) moved = true
                        p.x = startX + dx.toInt()
                        p.y = startY + dy.toInt()
                        try { wm?.updateViewLayout(v, p) } catch (e: Exception) {}
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (!moved) {
                            launchScan()
                        } else {
                            snapToEdge(v, p)
                        }
                    }
                }
                return true
            }
        })

        wm?.addView(view, p)
        ball = view
    }

    private fun snapToEdge(v: View, p: WindowManager.LayoutParams) {
        val screenW = resources.displayMetrics.widthPixels
        val half = v.width / 2
        val target = if (p.x + half < screenW / 2) {
            if (Prefs.ballHideEdge) -half else 0
        } else {
            if (Prefs.ballHideEdge) screenW - v.width + half else screenW - v.width
        }
        val from = p.x
        ValueAnimator.ofInt(from, target).apply {
            duration = 180
            addUpdateListener {
                p.x = it.animatedValue as Int
                try { wm?.updateViewLayout(v, p) } catch (e: Exception) {}
            }
            start()
        }
    }

    private fun launchScan() {
        val intent = Intent(this, ScanActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(ScanActivity.EXTRA_SOURCE, ScanActivity.SOURCE_BALL)
        }
        startActivity(intent)
    }

    private fun applyParams() {
        ball?.let {
            try {
                wm?.removeView(it)
            } catch (e: Exception) {}
            ball = null
            if (Settings.canDrawOverlays(this)) showBall()
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                "ball",
                getString(R.string.notif_channel_ball),
                NotificationManager.IMPORTANCE_MIN
            )
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val scanPI = PendingIntent.getActivity(
            this, 1,
            Intent(this, ScanActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(ScanActivity.EXTRA_SOURCE, ScanActivity.SOURCE_BALL),
            pendingFlags()
        )
        val settingsPI = PendingIntent.getActivity(
            this, 2, Intent(this, MainActivity::class.java), pendingFlags()
        )
        val exitPI = PendingIntent.getService(
            this, 3,
            Intent(this, FloatingBallService::class.java).setAction("exit"),
            pendingFlags()
        )

        return NotificationCompat.Builder(this, "ball")
            .setSmallIcon(R.drawable.ic_scan)
            .setContentTitle(getString(R.string.notif_ball_title))
            .setContentText(getString(R.string.notif_ball_text))
            .setContentIntent(scanPI)
            .addAction(0, getString(R.string.notif_scan), scanPI)
            .addAction(0, getString(R.string.notif_settings), settingsPI)
            .addAction(0, getString(R.string.notif_exit), exitPI)
            .setOngoing(true)
            .build()
    }

    private fun pendingFlags(): Int =
        if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT
}
