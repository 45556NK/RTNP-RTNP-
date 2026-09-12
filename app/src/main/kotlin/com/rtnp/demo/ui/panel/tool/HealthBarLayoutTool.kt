// app/src/main/kotlin/com/rtnp/demo/ui/panel/tool/HealthBarLayoutTool.kt
package com.rtnp.demo.ui.panel.tool

import android.graphics.Paint

object HealthBarLayoutTool {

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 28f
    }

    /**
     * 计算血量组件高度
     */
    fun calculateHeight(): Float {
        val fm = textPaint.fontMetrics
        return (fm.bottom - fm.top) * 1.4f
    }
}