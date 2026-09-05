package com.scanfill.app.theme

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import com.scanfill.app.prefs.Prefs
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 主题引擎：亮色 / 暗色 / 跟随系统 / 定时切换。
 * 设置页等 App 内页面通过 AppCompatDelegate 生效；
 * 扫描页和悬浮球直接读取 isDarkNow()。
 */
object ThemeEngine {

    private var app: Application? = null

    fun init(application: Application) {
        app = application
        application.registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(c: Context?, i: Intent?) {
                    // 每分钟检查定时切换
                    if (Prefs.themeMode == Prefs.THEME_SCHEDULE) apply()
                }
            },
            IntentFilter(Intent.ACTION_TIME_TICK)
        )
        apply()
    }

    fun apply() {
        AppCompatDelegate.setDefaultNightMode(
            when (Prefs.themeMode) {
                Prefs.THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                Prefs.THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
                Prefs.THEME_SCHEDULE ->
                    if (isDarkNow(app!!)) AppCompatDelegate.MODE_NIGHT_YES
                    else AppCompatDelegate.MODE_NIGHT_NO
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }

    fun isDarkNow(context: Context): Boolean = when (Prefs.themeMode) {
        Prefs.THEME_LIGHT -> false
        Prefs.THEME_DARK -> true
        Prefs.THEME_SCHEDULE -> {
            val fmt = DateTimeFormatter.ofPattern("H:mm")
            val now = LocalTime.now()
            val start = LocalTime.parse(Prefs.scheduleStart, fmt)
            val end = LocalTime.parse(Prefs.scheduleEnd, fmt)
            if (start <= end) {
                // 同一天内：19:00 -> 23:00
                !now.isBefore(start) && now.isBefore(end)
            } else {
                // 跨夜：19:00 -> 07:00
                !now.isBefore(start) || now.isBefore(end)
            }
        }
        else -> (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
    }
}
