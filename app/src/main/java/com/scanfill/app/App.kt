package com.scanfill.app

import android.app.Application
import com.scanfill.app.prefs.Prefs
import com.scanfill.app.theme.ThemeEngine

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        ThemeEngine.init(this)
    }
}
