package com.scanfill.app.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.Image
import android.net.Uri
import androidx.camera.core.ImageProxy
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream

object Bitmaps {

    /** CameraX JPEG 内存拍照 -> 修正旋转后的 Bitmap */
    fun fromJpegImageProxy(proxy: ImageProxy): Bitmap? {
        val buffer = proxy.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        val rotation = proxy.imageInfo.rotationDegrees
        return if (rotation != 0) rotate(bmp, rotation.toFloat()) else bmp
    }

    /** 相册图片 -> 修正 EXIF 旋转后的 Bitmap */
    fun fromUri(context: Context, uri: Uri): Bitmap? {
        val bmp = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it)
        } ?: return null
        val rotation = try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).rotationDegrees
            } ?: 0
        } catch (e: Exception) {
            0
        }
        return if (rotation != 0) rotate(bmp, rotation.toFloat()) else bmp
    }

    fun rotate(bmp: Bitmap, degrees: Float): Bitmap {
        val m = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
    }

    /** YUV_420_888 -> NV21（用于底部面板的模糊背景帧） */
    fun imageProxyToNv21(image: ImageProxy): ByteArray {
        val width = image.width
        val height = image.height
        val ySize = width * height
        val nv21 = ByteArray(ySize + 2 * (width / 2) * (height / 2))

        val yPlane = image.planes[0]
        val yBuffer = yPlane.buffer
        val yRowStride = yPlane.rowStride
        if (yRowStride == width && yBuffer.remaining() == ySize) {
            yBuffer.get(nv21, 0, ySize)
        } else {
            var pos = 0
            if (yBuffer.remaining() == ySize) {
                for (row in 0 until height) {
                    yBuffer.position(row * yRowStride)
                    yBuffer.get(nv21, pos, width)
                    pos += width
                }
            } else {
                // 极少数设备 buffer 就是紧凑排布
                yBuffer.position(0)
                yBuffer.get(nv21, 0, yBuffer.remaining().coerceAtMost(ySize))
            }
        }

        val uPlane = image.planes[1]
        val vPlane = image.planes[2]
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer
        val chromaHeight = height / 2
        val chromaWidth = width / 2
        var pos = ySize
        for (row in 0 until chromaHeight) {
            for (col in 0 until chromaWidth) {
                val uIndex = row * uPlane.rowStride + col * uPlane.pixelStride
                val vIndex = row * vPlane.rowStride + col * vPlane.pixelStride
                nv21[pos++] = vBuffer.get(vIndex)
                nv21[pos++] = uBuffer.get(uIndex)
            }
        }
        return nv21
    }

    /** NV21 -> 小图 Bitmap（模糊背景用） */
    fun nv21ToBitmap(nv21: ByteArray, width: Int, height: Int, rotation: Int): Bitmap? {
        return try {
            val yuv = android.graphics.YuvImage(nv21, android.graphics.ImageFormat.NV21, width, height, null)
            val out = ByteArrayOutputStream()
            yuv.compressToJpeg(android.graphics.Rect(0, 0, width, height), 60, out)
            val bytes = out.toByteArray()
            var bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            if (rotation != 0) bmp = rotate(bmp, rotation.toFloat())
            bmp
        } catch (e: Exception) {
            null
        }
    }

    fun toJpegBytes(bmp: Bitmap, quality: Int = 90): ByteArray {
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, quality, out)
        return out.toByteArray()
    }

    fun setClipboardText(context: Context, text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("ScanFill", text))
    }
}
