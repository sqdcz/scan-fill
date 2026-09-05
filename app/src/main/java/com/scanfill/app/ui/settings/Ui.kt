package com.scanfill.app.ui.settings

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Space
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import com.scanfill.app.R
import com.scanfill.app.prefs.Prefs

/**
 * 设置页 UI 构建器：纯白/纯黑 + 玻璃卡片，代码化构建，统一分级结构。
 */
class Ui(private val activity: AppCompatActivity) {

    val ctx: Context = activity
    val accent = Prefs.accent
    private val bgColor by lazy { ContextCompat.getColor(ctx, R.color.bg) }
    private val bgSecondary by lazy { ContextCompat.getColor(ctx, R.color.bg_secondary) }
    private val onBg by lazy { ContextCompat.getColor(ctx, R.color.on_bg) }
    private val onBgSecondary by lazy { ContextCompat.getColor(ctx, R.color.on_bg_secondary) }
    private val divider by lazy { ContextCompat.getColor(ctx, R.color.divider) }

    fun dp(v: Int): Int = (v * ctx.resources.displayMetrics.density + 0.5f).toInt()

    fun screen(title: String, showBack: Boolean = true, build: (LinearLayout) -> Unit): View {
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgColor)
            fitsSystemWindows = true
        }

        // 顶栏
        val header = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(16), dp(16), dp(8))
        }
        if (showBack) {
            val back = ImageView(ctx).apply {
                setImageResource(R.drawable.ic_back)
                setColorFilter(onBg)
                val pad = dp(12)
                setPadding(pad, pad, pad, pad)
                layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
                isClickable = true
                background = ripple()
                setOnClickListener { activity.finish() }
            }
            header.addView(back)
        }
        header.addView(TextView(ctx).apply {
            text = title
            setTextColor(onBg)
            textSize = 22f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = dp(if (showBack) 4 else 16) }
        })
        root.addView(header)

        val scroll = ScrollView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }
        val content = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(4), dp(16), dp(32))
        }
        build(content)
        scroll.addView(content)
        root.addView(scroll)
        return root
    }

    /** 分组卡片 */
    fun group(parent: LinearLayout, label: String? = null): LinearLayout {
        label?.let {
            parent.addView(TextView(ctx).apply {
                text = it
                setTextColor(onBgSecondary)
                textSize = 13f
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = dp(4)
                    topMargin = dp(20)
                    bottomMargin = dp(8)
                }
            })
        } ?: run {
            if (parent.childCount > 0) {
                parent.addView(Space(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(20)
                    )
                })
            }
        }
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dp(20).toFloat()
                setColor(bgSecondary)
            }
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        parent.addView(card, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        return card
    }

    fun row(
        parent: LinearLayout,
        title: String,
        sub: String? = null,
        widget: View? = null,
        onClick: (() -> Unit)? = null
    ): LinearLayout {
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            isClickable = onClick != null
            if (onClick != null) background = ripple()
            onClick?.let { fn -> setOnClickListener { fn() } }
        }

        val textCol = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        textCol.addView(TextView(ctx).apply {
            text = title
            setTextColor(onBg)
            textSize = 16f
        })
        sub?.let {
            textCol.addView(TextView(ctx).apply {
                text = it
                setTextColor(onBgSecondary)
                textSize = 13f
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(3) }
            })
        }
        row.addView(textCol)
        widget?.let {
            val wrap = FrameLayout(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { marginStart = dp(12) }
            }
            wrap.addView(it)
            row.addView(wrap)
        }
        parent.addView(row, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        return row
    }

    fun divider(parent: LinearLayout) {
        parent.addView(View(ctx).apply {
            setBackgroundColor(divider)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)).apply {
                marginStart = dp(16); marginEnd = dp(16)
            }
        })
    }

    fun switch(checked: Boolean, onChange: (Boolean) -> Unit): MaterialSwitch {
        return MaterialSwitch(ctx).apply {
            isChecked = checked
            trackTintList = accentState(accent)
            thumbTintList = accentState(accent)
            setOnCheckedChangeListener { _: CompoundButton, b: Boolean -> onChange(b) }
        }
    }

    fun radioList(
        parent: LinearLayout,
        options: List<String>,
        checkedIndex: Int,
        onChange: (Int) -> Unit
    ): RadioGroup {
        val rg = RadioGroup(ctx).apply {
            orientation = RadioGroup.VERTICAL
        }
        options.forEachIndexed { i, opt ->
            rg.addView(RadioButton(ctx).apply {
                text = opt
                setTextColor(onBg)
                textSize = 15f
                buttonTintList = accentState(accent)
                isChecked = i == checkedIndex
                setPadding(dp(16), dp(12), dp(16), dp(12))
                layoutParams = RadioGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                )
            })
        }
        rg.setOnCheckedChangeListener { _, id ->
            onChange(rg.indexOfChild(rg.findViewById(id)))
        }
        parent.addView(rg, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        return rg
    }

    fun slider(
        value: Int, from: Int, to: Int,
        onChange: (Int) -> Unit
    ): Slider {
        return Slider(ctx).apply {
            valueFrom = from.toFloat()
            valueTo = to.toFloat()
            this.value = value.toFloat().coerceIn(from.toFloat(), to.toFloat())
            stepSize = 1f
            trackActiveTintList = accentState(accent)
            thumbTintList = accentState(accent)
            addOnChangeListener { _, v, fromUser ->
                if (fromUser) onChange(v.toInt())
            }
        }
    }

    fun valueLabel(text: String): TextView {
        return TextView(ctx).apply {
            this.text = text
            setTextColor(accent)
            textSize = 15f
        }
    }

    fun statusDot(ok: Boolean): View {
        return View(ctx).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(if (ok) 0xFF34C759.toInt() else 0xFFFF9500.toInt())
            }
            layoutParams = LinearLayout.LayoutParams(dp(10), dp(10))
        }
    }

    fun accentColorPicker(
        parent: LinearLayout,
        current: Int,
        onPick: (Int) -> Unit
    ): LinearLayout {
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        Prefs.ACCENTS.forEach { c ->
            val circle = FrameLayout(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply { marginEnd = dp(10) }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(c)
                    if (c == current) {
                        setStroke(dp(3), 0xFFFFFFFF.toInt())
                    }
                }
                isClickable = true
                setOnClickListener {
                    onPick(c)
                }
            }
            row.addView(circle)
        }
        parent.addView(row, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        return row
    }

    fun sectionText(parent: LinearLayout, text: String, mono: Boolean = false): TextView {
        val tv = TextView(ctx).apply {
            this.text = text
            setTextColor(if (mono) onBg else onBgSecondary)
            textSize = if (mono) 14f else 13f
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        parent.addView(tv)
        return tv
    }

    private fun accentState(color: Int): ColorStateList {
        return ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(color, 0xFF9E9EA4.toInt())
        )
    }

    private fun ripple(): RippleDrawable {
        val content = GradientDrawable().apply { setColor(0x11000000) }
        return RippleDrawable(ColorStateList.valueOf(0x22000000), null, content)
    }
}
