package com.scanfill.app.ocr

import android.content.Context
import android.graphics.Bitmap
import com.equationl.paddleocr4android.CpuPowerMode
import com.equationl.paddleocr4android.OCR
import com.equationl.paddleocr4android.OcrConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 引擎二：PaddleOCR PP-OCRv4（离线，模型内置 assets/models/ch_PP-OCRv4） */
class PaddleEngine(private val context: Context) : OcrEngine {

    override val id = "paddle"
    override val displayName = "Paddle"

    private val ocr = OCR(context)
    private var initialized = false

    @Synchronized
    private fun ensureInit(): Result<Boolean> {
        if (initialized) return Result.success(true)
        val config = OcrConfig().apply {
            modelPath = "models/ch_PP-OCRv4" // assets 内路径
            clsModelFilename = "cls.nb"
            detModelFilename = "det.nb"
            recModelFilename = "rec.nb"
            isRunDet = true
            isRunCls = true
            isRunRec = true
            cpuPowerMode = CpuPowerMode.LITE_POWER_FULL
        }
        return ocr.initModelSync(config).also {
            initialized = it.getOrDefault(false)
        }
    }

    override suspend fun recognize(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        ensureInit().fold(
            onSuccess = { ok ->
                if (!ok) throw IllegalStateException("Paddle 模型初始化失败")
            },
            onFailure = { throw it }
        )
        ocr.runSync(bitmap).fold(
            onSuccess = { it.simpleText },
            onFailure = { throw it }
        )
    }

    override fun release() {
        if (initialized) {
            try {
                ocr.releaseModel()
            } catch (e: Exception) {
                // ignore
            }
        }
        initialized = false
    }
}
