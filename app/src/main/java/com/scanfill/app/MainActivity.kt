package com.scanfill.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.scanfill.app.prefs.Prefs
import com.scanfill.app.scan.ScanActivity
import com.scanfill.app.service.FloatingBallService
import com.scanfill.app.service.PasteAccessibilityService
import com.scanfill.app.ui.settings.SettingsBallActivity
import com.scanfill.app.ui.settings.SettingsFormatActivity
import com.scanfill.app.ui.settings.SettingsOcrActivity
import com.scanfill.app.ui.settings.SettingsThemeActivity
import com.scanfill.app.ui.settings.AboutActivity
import com.scanfill.app.ui.settings.Ui

class MainActivity : AppCompatActivity() {

    private lateinit var ui: Ui
    private lateinit var overlayStatus: TextView
    private lateinit var a11yStatus: TextView
    private lateinit var cameraStatus: TextView
    private lateinit var notifStatus: TextView

    private val requestCamera =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { refreshStatuses() }

    private val requestNotif =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { refreshStatuses() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = Ui(this)
        setContentView(ui.screen(getString(R.string.app_name), showBack = false) { content ->
            buildContent(content)
        })
    }

    override fun onResume() {
        super.onResume()
        refreshStatuses()
    }

    private fun buildContent(content: LinearLayout) {
        // 使用状态总览
        ui.group(content, getString(R.string.status_card_title)).let { g ->
            overlayStatus = statusLabel()
            ui.row(g, getString(R.string.ball_overlay_perm), null, overlayStatus) {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            }
            ui.divider(g)
            a11yStatus = statusLabel()
            ui.row(g, getString(R.string.accessibility_label), getString(R.string.accessibility_off), a11yStatus) {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            ui.divider(g)
            cameraStatus = statusLabel()
            ui.row(g, getString(R.string.camera_perm), null, cameraStatus) {
                requestCamera.launch(Manifest.permission.CAMERA)
            }
            if (Build.VERSION.SDK_INT >= 33) {
                ui.divider(g)
                notifStatus = statusLabel()
                ui.row(g, getString(R.string.notif_perm), null, notifStatus) {
                    requestNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        // 快捷操作
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
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:$packageName")
                                )
                            )
                        }
                    } else {
                        Prefs.ballEnabled = false
                        FloatingBallService.stop(this)
                    }
                }
            )
            ui.divider(g)
            ui.row(g, getString(R.string.try_scan), "打开相机试试扫描效果", null) {
                startActivity(
                    Intent(this, ScanActivity::class.java)
                        .putExtra(ScanActivity.EXTRA_SOURCE, ScanActivity.SOURCE_MAIN)
                )
            }
        }

        // 分级设置入口
        ui.group(content, getString(R.string.settings_title)).let { g ->
            ui.row(g, getString(R.string.cat_theme), getString(R.string.cat_theme_sub)) {
                startActivity(Intent(this, SettingsThemeActivity::class.java))
            }
            ui.divider(g)
            ui.row(g, getString(R.string.cat_ocr), getString(R.string.cat_ocr_sub)) {
                startActivity(Intent(this, SettingsOcrActivity::class.java))
            }
            ui.divider(g)
            ui.row(g, getString(R.string.cat_format), getString(R.string.cat_format_sub)) {
                startActivity(Intent(this, SettingsFormatActivity::class.java))
            }
            ui.divider(g)
            ui.row(g, getString(R.string.cat_ball), getString(R.string.cat_ball_sub)) {
                startActivity(Intent(this, SettingsBallActivity::class.java))
            }
            ui.divider(g)
            ui.row(g, getString(R.string.cat_about), getString(R.string.cat_about_sub)) {
                startActivity(Intent(this, AboutActivity::class.java))
            }
        }
    }

    private fun statusLabel(): TextView = TextView(this).apply { text = "" }

    private fun refreshStatuses() {
        fun set(tv: TextView, ok: Boolean) {
            tv.text = if (ok) getString(R.string.granted) else getString(R.string.not_granted)
            tv.setTextColor(
                ContextCompat.getColor(
                    this,
                    if (ok) R.color.accent else R.color.on_bg_secondary
                )
            )
        }
        if (::overlayStatus.isInitialized) {
            set(overlayStatus, Settings.canDrawOverlays(this))
            set(a11yStatus, PasteAccessibilityService.isEnabled(this))
            set(
                cameraStatus,
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED
            )
            if (Build.VERSION.SDK_INT >= 33 && ::notifStatus.isInitialized) {
                set(
                    notifStatus,
                    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                            PackageManager.PERMISSION_GRANTED
                )
            }
        }
    }
}
