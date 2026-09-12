// app/src/main/kotlin/com/rtnp/demo/ui/panel/tool/TextLayoutTool.kt
package com.rtnp.demo.ui.panel.tool

import android.graphics.Paint

object TextLayoutTool {

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun calculateHeight(textSize: Float): Float {
        textPaint.textSize = textSize
        val fm = textPaint.fontMetrics
        return (fm.bottom - fm.top) * 1.4f
    }
}