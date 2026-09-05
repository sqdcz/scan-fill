package com.scanfill.app.format

import com.scanfill.app.prefs.Prefs

/**
 * 识别结果的格式化处理：换行、空格、空行。
 */
object TextFormatter {

    fun format(raw: String): String {
        val text = raw.replace("\r\n", "\n").replace("\r", "\n")
        val lines = text.split("\n")

        // 1. 换行处理
        val merged: List<String> = when (Prefs.fmtLine) {
            Prefs.LINE_MERGE -> {
                val joiner = if (Prefs.fmtSpace == Prefs.SPACE_STRIP) "" else " "
                listOf(lines.filter { it.isNotBlank() }.joinToString(joiner) { it.trim() })
            }
            Prefs.LINE_SMART -> smartMerge(lines)
            else -> lines
        }

        // 2. 空行处理
        val noBlank = if (Prefs.fmtBlankLines) merged.filter { it.isNotBlank() } else merged

        // 3. 空格处理
        val spaced = noBlank.map { line ->
            when (Prefs.fmtSpace) {
                Prefs.SPACE_COLLAPSE -> line.trim().replace(Regex(" {2,}"), " ")
                Prefs.SPACE_STRIP -> line.replace(" ", "")
                Prefs.SPACE_CJK_LATIN -> line
                else -> line
            }
        }

        // 4. 中英文间加空格（最后处理，避免被其它规则破坏）
        val result = if (Prefs.fmtSpace == Prefs.SPACE_CJK_LATIN) {
            spaced.map { cjkLatinSpace(it.trim()) }
        } else spaced

        return result.joinToString("\n").trim()
    }

    /** 智能段落：短行合并，句末标点或段前符号处断段。 */
    private fun smartMerge(lines: List<String>): List<String> {
        val paras = mutableListOf(StringBuilder())
        fun closeParagraph() {
            if (paras.last().isNotEmpty()) paras.add(StringBuilder())
        }
        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty()) {
                closeParagraph()
                continue
            }
            val cur = paras.last()
            if (cur.isEmpty()) {
                cur.append(line)
            } else {
                val last = cur.last()
                if (last in "。！？；…」』）】" || line.first() in "「『（【·") {
                    closeParagraph()
                    paras.last().append(line)
                } else {
                    cur.append(line)
                }
            }
        }
        return paras.filter { it.isNotEmpty() }.map { it.toString() }
    }

    private val CJK = Regex("[\\u4e00-\\u9fff\\u3000-\\u303f，。！？；：、]")
    private val LAT = Regex("[A-Za-z0-9]")

    private fun cjkLatinSpace(s: String): String {
        val sb = StringBuilder(s.length + 8)
        var prev = ' '
        for (c in s) {
            if (sb.isNotEmpty()) {
                val a = prev.toString()
                val b = c.toString()
                if ((CJK.matches(a) && LAT.matches(b)) || (LAT.matches(a) && CJK.matches(b))) {
                    sb.append(' ')
                }
            }
            sb.append(c)
            prev = c
        }
        return sb.toString()
    }
}
