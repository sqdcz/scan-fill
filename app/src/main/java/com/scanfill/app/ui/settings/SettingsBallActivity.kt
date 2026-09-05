package com.scanfill.app.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.scanfill.app.R
import com.scanfill.app.prefs.Prefs
import com.scanfill.app.service.FloatingBallService

class SettingsBallActivity : BaseSettingsActivity() {

    override val titleRes = R.string.cat_ball

    private lateinit var sizeLabel: TextView
    private lateinit var alphaLabel: TextView

    override fun build(ui: Ui, content: LinearLayout) {
        // 开关
        ui.group(content).let { g ->
            ui.row(
                g,
                getString(R.string.ball_show),
                getString(R.string.cat_ball_sub),
                ui.switch(Prefs.ballEnabled) { on ->
                    if (on) {
                        if (Settings.canDrawOverlays(this)) {
                            Prefs.ballEnabled = true
                            FloatingBallService.start(this)
                        } else {
                            Prefs.ballEnabled = false
                            Toast.makeText(this, R.string.ball_overlay_perm, Toast.LENGTH_SHORT).show()
                            startActivity(
                                Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:$packageName"))
                            )
                        }
                    } else {
                        Prefs.ballEnabled = false
                        FloatingBallService.stop(this)
                    }
                }
            )
        }

        // 大小
        ui.group(content, getString(R.string.ball_size)).let { g ->
            sizeLabel = ui.valueLabel("${Prefs.ballSize} dp")
            ui.row(g, getString(R.string.ball_size), null, sizeLabel)
            g.addView(ui.slider(Prefs.ballSize, 40, 64) { v ->
                Prefs.ballSize = v
                sizeLabel.text = "$v dp"
                FloatingBallService.refresh()
            })
        }

        // 透明度
        ui.group(content, getString(R.string.ball_alpha)).let { g ->
            alphaLabel = ui.valueLabel("${Prefs.ballAlpha}%")
            ui.row(g, getString(R.string.ball_alpha), null, alphaLabel)
            g.addView(ui.slider(Prefs.ballAlpha, 30, 100) { v ->
                Prefs.ballAlpha = v
                alphaLabel.text = "$v%"
                FloatingBallService.refresh()
            })
        }

        // 贴边隐藏
        ui.group(content).let { g ->
            ui.row(
                g,
                getString(R.string.ball_hide_edge),
                "拖到屏幕边缘后缩进一半，不挡内容",
                ui.switch(Prefs.ballHideEdge) { Prefs.ballHideEdge = it }
            )
        }
    }
}
