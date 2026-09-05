package com.scanfill.app.service

import android.content.Intent
import android.service.quicksettings.TileService
import com.scanfill.app.scan.ScanActivity

/** 快捷设置磁贴：一键打开扫描 */
class ScanTileService : TileService() {

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, ScanActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(ScanActivity.EXTRA_SOURCE, ScanActivity.SOURCE_TILE)
        }
        startActivityAndCollapse(intent)
    }
}
