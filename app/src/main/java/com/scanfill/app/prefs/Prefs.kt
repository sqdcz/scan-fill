package com.scanfill.app.prefs

import android.content.Context
import android.content.SharedPreferences

object Prefs {

    // 主题
    const val THEME_LIGHT = 0
    const val THEME_DARK = 1
    const val THEME_SYSTEM = 2
    const val THEME_SCHEDULE = 3

    // 模糊风格
    const val GLASS_GAUSSIAN = 0
    const val GLASS_LIQUID = 1

    // 填入方式
    const val FILL_AUTO = "auto"
    const val FILL_CLIPBOARD = "clipboard"

    // 引擎
    const val ENGINE_MLKIT = "mlkit"
    const val ENGINE_PADDLE = "paddle"

    // 文字格式
    const val LINE_KEEP = 0
    const val LINE_MERGE = 1
    const val LINE_SMART = 2

    const val SPACE_AS_IS = 0
    const val SPACE_COLLAPSE = 1
    const val SPACE_CJK_LATIN = 2
    const val SPACE_STRIP = 3

    // 强调色预设
    val ACCENTS = intArrayOf(
        0xFF007AFF.toInt(),
        0xFF34C759.toInt(),
        0xFFAF52DE.toInt(),
        0xFFFF9500.toInt(),
        0xFFFF3B30.toInt(),
        0xFFFF2D55.toInt()
    )

    private lateinit var sp: SharedPreferences

    fun init(context: Context) {
        sp = context.applicationContext.getSharedPreferences("scanfill", Context.MODE_PRIVATE)
    }

    /** 供服务/页面监听设置变化，实现"改完立即生效" */
    fun registerListener(l: SharedPreferences.OnSharedPreferenceChangeListener) {
        sp.registerOnSharedPreferenceChangeListener(l)
    }

    fun unregisterListener(l: SharedPreferences.OnSharedPreferenceChangeListener) {
        sp.unregisterOnSharedPreferenceChangeListener(l)
    }

    var themeMode: Int
        get() = sp.getInt("theme_mode", THEME_SYSTEM)
        set(v) = sp.edit().putInt("theme_mode", v).apply()

    var scheduleStart: String
        get() = sp.getString("schedule_start", "19:00") ?: "19:00"
        set(v) = sp.edit().putString("schedule_start", v).apply()

    var scheduleEnd: String
        get() = sp.getString("schedule_end", "07:00") ?: "07:00"
        set(v) = sp.edit().putString("schedule_end", v).apply()

    var glassStyle: Int
        get() = sp.getInt("glass_style", GLASS_LIQUID)
        set(v) = sp.edit().putInt("glass_style", v).apply()

    var accent: Int
        get() = sp.getInt("accent", ACCENTS[0])
        set(v) = sp.edit().putInt("accent", v).apply()

    var ballEnabled: Boolean
        get() = sp.getBoolean("ball_enabled", false)
        set(v) = sp.edit().putBoolean("ball_enabled", v).apply()

    var ballSize: Int // dp
        get() = sp.getInt("ball_size", 48)
        set(v) = sp.edit().putInt("ball_size", v).apply()

    var ballAlpha: Int // 30..100
        get() = sp.getInt("ball_alpha", 80)
        set(v) = sp.edit().putInt("ball_alpha", v).apply()

    var ballHideEdge: Boolean
        get() = sp.getBoolean("ball_hide_edge", true)
        set(v) = sp.edit().putBoolean("ball_hide_edge", v).apply()

    var ocrEngine: String
        get() = sp.getString("ocr_engine", ENGINE_MLKIT) ?: ENGINE_MLKIT
        set(v) = sp.edit().putString("ocr_engine", v).apply()

    var fillMode: String
        get() = sp.getString("fill_mode", FILL_AUTO) ?: FILL_AUTO
        set(v) = sp.edit().putString("fill_mode", v).apply()

    var fmtLine: Int
        get() = sp.getInt("fmt_line", LINE_SMART)
        set(v) = sp.edit().putInt("fmt_line", v).apply()

    var fmtSpace: Int
        get() = sp.getInt("fmt_space", SPACE_COLLAPSE)
        set(v) = sp.edit().putInt("fmt_space", v).apply()

    var fmtBlankLines: Boolean
        get() = sp.getBoolean("fmt_blank", true)
        set(v) = sp.edit().putBoolean("fmt_blank", v).apply()
}
