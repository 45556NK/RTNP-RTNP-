package com.rtnp.demo.ui.panel.tool

import android.graphics.Paint

object BuildReserveLayoutTool {

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 28f
    }

    fun calculateHeight(): Float {
        val fm = textPaint.fontMetrics
        return (fm.bottom - fm.top) * 1.4f
    }
}