package com.scanfill.app.ui.settings

import android.content.Intent
import android.provider.Settings
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.scanfill.app.R
import com.scanfill.app.prefs.Prefs
import com.scanfill.app.service.PasteAccessibilityService

class SettingsOcrActivity : BaseSettingsActivity() {

    override val titleRes = R.string.cat_ocr

    private lateinit var accessibilityStatus: TextView

    override fun build(ui: Ui, content: LinearLayout) {
        // 识别引擎
        ui.group(content, getString(R.string.ocr_engine)).let { g ->
            ui.radioList(
                g,
                listOf(
                    "ML Kit（Google，速度快）",
                    "PaddleOCR PP-OCRv4（复杂版面更强）"
                ),
                if (Prefs.ocrEngine == Prefs.ENGINE_PADDLE) 1 else 0
            ) { idx ->
                Prefs.ocrEngine = if (idx == 1) Prefs.ENGINE_PADDLE else Prefs.ENGINE_MLKIT
            }
            ui.sectionText(g, "两个引擎全部离线内置、双打包，可随时切换。扫描页右上角也能一键切换。")
        }

        // 填入方式
        ui.group(content, getString(R.string.ocr_fill_mode)).let { g ->
            ui.radioList(
                g,
                listOf(
                    getString(R.string.fill_auto),
                    getString(R.string.fill_clipboard)
                ),
                if (Prefs.fillMode == Prefs.FILL_CLIPBOARD) 1 else 0
            ) { idx ->
                Prefs.fillMode = if (idx == 1) Prefs.FILL_CLIPBOARD else Prefs.FILL_AUTO
            }
            ui.sectionText(g, "无障碍自动填入：识别完成后对当前输入框自动执行「全选 + 粘贴」，最接近 iOS 体验；剪贴板模式最稳，兼容所有应用。")
        }

        // 无障碍服务
        ui.group(content, getString(R.string.accessibility_status)).let { g ->
            accessibilityStatus = TextView(ui.ctx)
            ui.row(
                g,
                getString(R.string.accessibility_status),
                null,
                null
            ) {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            ui.divider(g)
            ui.row(g, getString(R.string.enable), getString(R.string.accessibility_off), null) {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            refreshStatus()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::accessibilityStatus.isInitialized) refreshStatus()
    }

    private fun refreshStatus() {
        val enabled = PasteAccessibilityService.isEnabled(this)
        accessibilityStatus.text = if (enabled) {
            getString(R.string.accessibility_on)
        } else {
            getString(R.string.accessibility_off)
        }
        accessibilityStatus.setTextColor(
            androidx.core.content.ContextCompat.getColor(
                this,
                if (enabled) R.color.accent else R.color.on_bg_secondary
            )
        )
        if (enabled) {
            accessibilityStatus.text = getString(R.string.accessibility_on)
        }
    }
}
