package com.scanfill.app.ui.settings

import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.scanfill.app.R
import com.scanfill.app.prefs.Prefs
import com.scanfill.app.theme.ThemeEngine

class SettingsThemeActivity : BaseSettingsActivity() {

    override val titleRes = R.string.cat_theme

    private lateinit var startLabel: TextView
    private lateinit var endLabel: TextView

    override fun build(ui: Ui, content: LinearLayout) {
        // 外观模式
        ui.group(content, getString(R.string.theme_mode)).let { g ->
            ui.radioList(
                g,
                listOf(
                    getString(R.string.theme_light),
                    getString(R.string.theme_dark),
                    getString(R.string.theme_system),
                    getString(R.string.theme_schedule)
                ),
                Prefs.themeMode
            ) { idx ->
                Prefs.themeMode = idx
                ThemeEngine.apply()
                recreate()
            }
        }

        // 定时切换
        ui.group(content, getString(R.string.theme_schedule)).let { g ->
            startLabel = ui.valueLabel(Prefs.scheduleStart)
            ui.row(g, getString(R.string.schedule_start), null, startLabel) {
                pickTime(true)
            }
            ui.divider(g)
            endLabel = ui.valueLabel(Prefs.scheduleEnd)
            ui.row(g, getString(R.string.schedule_end), null, endLabel) {
                pickTime(false)
            }
            ui.sectionText(g, "时间到达后自动在亮暗色之间切换，例如 19:00 开启暗色，07:00 回到亮色。")
        }

        // 模糊风格
        ui.group(content, getString(R.string.glass_style)).let { g ->
            ui.radioList(
                g,
                listOf(
                    getString(R.string.glass_gaussian),
                    getString(R.string.glass_liquid)
                ),
                Prefs.glassStyle
            ) { idx ->
                Prefs.glassStyle = idx
                Toast.makeText(this, "已切换", Toast.LENGTH_SHORT).show()
            }
            ui.sectionText(g, "影响扫描页底部面板和结果卡片的玻璃质感。Android 12 以下自动降级为半透明磨砂。")
        }

        // 强调色
        ui.group(content, getString(R.string.accent_color)).let { g ->
            ui.accentColorPicker(g, Prefs.accent) { c ->
                Prefs.accent = c
                recreate()
            }
        }
    }

    private fun pickTime(isStart: Boolean) {
        val current = if (isStart) Prefs.scheduleStart else Prefs.scheduleEnd
        val parts = current.split(":")
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(parts[0].toInt())
            .setMinute(parts[1].toInt())
            .build()
        picker.addOnPositiveButtonClickListener {
            val text = String.format("%02d:%02d", picker.hour, picker.minute)
            if (isStart) {
                Prefs.scheduleStart = text
                startLabel.text = text
            } else {
                Prefs.scheduleEnd = text
                endLabel.text = text
            }
            ThemeEngine.apply()
        }
        picker.show(supportFragmentManager, "time")
    }
}
