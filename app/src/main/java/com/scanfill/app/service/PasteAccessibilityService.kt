package com.scanfill.app.service

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.scanfill.app.R

/**
 * 无障碍填入服务：把剪贴板内容"全选 + 粘贴"进当前聚焦的输入框。
 */
class PasteAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        private var instance: PasteAccessibilityService? = null

        fun isEnabled(context: android.content.Context): Boolean {
            if (instance != null) return true
            val svc = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val expected = "${context.packageName}/PasteAccessibilityService"
            return svc.split(':').any { it.equals(expected, ignoreCase = true) }
        }

        /** 等扫描页关闭、焦点回到目标输入框后执行粘贴 */
        fun schedulePaste(delayMs: Long = 400) {
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({
                instance?.pasteIntoFocusedNode()
            }, delayMs)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {
        // 不需要处理事件，只用节点操作能力
    }

    override fun onInterrupt() {}

    fun pasteIntoFocusedNode() {
        val root = rootInActiveWindow ?: run {
            Toast.makeText(this, R.string.fill_fallback, Toast.LENGTH_LONG).show()
            return
        }
        val node = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)

        if (node == null || !node.isEditable) {
            // 有些 App 输入框不响应 findFocus，尝试在当前窗口找可编辑子节点
            val editable = findEditable(root)
            if (editable != null) {
                pasteNode(editable)
            } else {
                Toast.makeText(this, R.string.fill_fallback, Toast.LENGTH_LONG).show()
            }
            return
        }
        pasteNode(node)
    }

    private fun findEditable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                findEditable(child)?.let { return it }
            }
        }
        return null
    }

    private fun pasteNode(node: AccessibilityNodeInfo) {
        try {
            // 全选：把光标选区设为 [0, 文本末尾]
            val len = node.text?.length ?: node.hintText?.length ?: 0
            if (len > 0 && (node.actions and AccessibilityNodeInfo.ACTION_SET_SELECTION) != 0) {
                val args = Bundle().apply {
                    putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 0)
                    putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, len)
                }
                node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, args)
            }
            // 粘贴
            val ok = node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
            if (!ok) {
                Toast.makeText(this, R.string.fill_fallback, Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, R.string.fill_fallback, Toast.LENGTH_LONG).show()
        }
    }
}
