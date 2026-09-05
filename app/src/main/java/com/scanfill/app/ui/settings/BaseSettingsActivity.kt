package com.scanfill.app.ui.settings

import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

abstract class BaseSettingsActivity : AppCompatActivity() {

    protected abstract val titleRes: Int

    /** 是否显示返回键（主页不显示） */
    protected open val showBack: Boolean = true

    protected abstract fun build(ui: Ui, content: LinearLayout)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ui = Ui(this)
        setContentView(ui.screen(getString(titleRes), showBack) { content ->
            build(ui, content)
        })
    }
}
