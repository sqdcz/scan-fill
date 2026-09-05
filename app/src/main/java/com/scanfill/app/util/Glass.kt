package com.scanfill.app.util

import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.View
import android.widget.ImageView
import com.scanfill.app.prefs.Prefs

/**
 * 玻璃面板样式：高斯模糊 / 液态玻璃。
 * API 31+ 用 RenderEffect 做背景实时模糊；低版本降级为半透明磨砂。
 */
object Glass {

    val liquid: Boolean get() = Prefs.glassStyle == Prefs.GLASS_LIQUID

    /** 面板圆角（px） */
    fun radius(context: Context): Float = dp(context, if (liquid) 28 else 20).toFloat()

    /** 给承载"模糊背景帧"的 ImageView 应用效果 */
    fun applyBackdrop(context: Context, view: ImageView) {
        if (Build.VERSION.SDK_INT >= 31) {
            val r = dp(context, if (liquid) 44 else 26).toFloat()
            view.setRenderEffect(
                RenderEffect.createBlurEffect(r, r, Shader.TileMode.CLAMP)
            )
            if (liquid) {
                val cm = ColorMatrix().apply { setSaturation(1.4f) }
                view.colorFilter = ColorMatrixColorFilter(cm)
            } else {
                view.colorFilter = null
            }
        }
    }

    /** 给面板本身画玻璃底色和描边 */
    fun panelBackground(context: Context, dark: Boolean): GradientDrawable {
        val bg = GradientDrawable()
        bg.cornerRadius = radius(context)
        val alpha = if (Build.VERSION.SDK_INT >= 31) {
            // 有实时模糊时底色更透
            if (dark) 0xB3 else 0xCC
        } else {
            // 无模糊时用浓一点的磨砂
            if (dark) 0xE0 else 0xF2
        }
        val base = if (dark) 0x161616 else 0xFFFFFF
        bg.setColor((alpha shl 24) or base)
        if (liquid) {
            bg.setStroke(
                dp(context, 1),
                (if (dark) 0x40FFFFFFL else 0x80FFFFFFL).toInt()
            )
        }
        return bg
    }

    fun dp(context: Context, v: Int): Int =
        (v * context.resources.displayMetrics.density + 0.5f).toInt()
}
