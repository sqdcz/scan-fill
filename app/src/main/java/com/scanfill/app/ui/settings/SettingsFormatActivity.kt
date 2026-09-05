package com.scanfill.app.ui.settings

import android.widget.LinearLayout
import android.widget.TextView
import com.scanfill.app.R
import com.scanfill.app.format.TextFormatter
import com.scanfill.app.prefs.Prefs

class SettingsFormatActivity : BaseSettingsActivity() {

    override val titleRes = R.string.cat_format

    private lateinit var preview: TextView

    override fun build(ui: Ui, content: LinearLayout) {
        // 换行处理
        ui.group(content, getString(R.string.fmt_line)).let { g ->
            ui.radioList(
                g,
                listOf(
                    getString(R.string.fmt_line_keep),
                    getString(R.string.fmt_line_merge),
                    getString(R.string.fmt_line_smart)
                ),
                Prefs.fmtLine
            ) { idx ->
                Prefs.fmtLine = idx
                updatePreview()
            }
        }

        // 空格处理
        ui.group(content, getString(R.string.fmt_space)).let { g ->
            ui.radioList(
                g,
                listOf(
                    getString(R.string.fmt_space_as_is),
                    getString(R.string.fmt_space_collapse),
                    getString(R.string.fmt_space_cjk),
                    getString(R.string.fmt_space_strip)
                ),
                Prefs.fmtSpace
            ) { idx ->
                Prefs.fmtSpace = idx
                updatePreview()
            }
        }

        // 空行
        ui.group(content).let { g ->
            ui.row(
                g,
                getString(R.string.fmt_blank),
                null,
                ui.switch(Prefs.fmtBlankLines) { checked ->
                    Prefs.fmtBlankLines = checked
                    updatePreview()
                }
            )
        }

        // 效果预览
        ui.group(content, getString(R.string.fmt_preview)).let { g ->
            preview = TextView(ui.ctx).apply {
                setPadding(ui.dp(16), ui.dp(12), ui.dp(16), ui.dp(12))
                textSize = 14f
            }
            g.addView(preview)
        }
        updatePreview()
    }

    private fun updatePreview() {
        if (::preview.isInitialized) {
            preview.text = TextFormatter.format(SAMPLE)
        }
    }

    companion object {
        private const val SAMPLE =
            "扫描填入 ScanFill\n" +
            "这是第二行的连续内容，没有标点\n" +
            "\n" +
            "第三段 支持 中 English 混排 content\n" +
            "  多余    空格   测试"
    }
}
