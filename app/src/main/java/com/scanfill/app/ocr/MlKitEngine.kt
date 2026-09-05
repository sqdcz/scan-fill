package com.scanfill.app.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/** 引擎一：ML Kit 中文本地识别（离线） */
class MlKitEngine : OcrEngine {

    override val id = "mlkit"
    override val displayName = "ML Kit"

    private val recognizer by lazy {
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    }

    override suspend fun recognize(bitmap: Bitmap): String =
        suspendCancellableCoroutine { cont ->
            val task = recognizer.process(InputImage.fromBitmap(bitmap, 0))
            task.addOnSuccessListener { result ->
                if (cont.isActive) cont.resume(result.text)
            }
            task.addOnFailureListener { e ->
                if (cont.isActive) cont.resumeWithException(e)
            }
        }

    override fun release() {
        recognizer.close()
    }
}
