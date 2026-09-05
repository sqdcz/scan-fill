package com.scanfill.app.ocr

import android.content.Context
import android.graphics.Bitmap
import com.scanfill.app.prefs.Prefs

interface OcrEngine {
    val id: String
    val displayName: String

    /** 离线识别，返回原始文本（换行保留原样，格式化交给 TextFormatter） */
    suspend fun recognize(bitmap: Bitmap): String

    fun release()
}

object OcrManager {

    private var engine: OcrEngine? = null
    private var engineId: String? = null

    fun get(context: Context): OcrEngine {
        val id = Prefs.ocrEngine
        if (engine == null || engineId != id) {
            engine?.release()
            engine = if (id == Prefs.ENGINE_PADDLE) {
                PaddleEngine(context.applicationContext)
            } else {
                MlKitEngine()
            }
            engineId = id
        }
        return engine!!
    }

    suspend fun recognize(context: Context, bitmap: Bitmap): String {
        val primary = get(context)
        return try {
            primary.recognize(bitmap)
        } catch (e: Exception) {
            // 主引擎失败（如模型初始化异常）时自动切到另一个引擎重试，
            // 并记住切换结果，下次直接用可用引擎
            val fallbackId = if (primary.id == Prefs.ENGINE_PADDLE) Prefs.ENGINE_MLKIT else Prefs.ENGINE_PADDLE
            release()
            Prefs.ocrEngine = fallbackId
            get(context).recognize(bitmap)
        }
    }

    fun release() {
        engine?.release()
        engine = null
        engineId = null
    }
}
