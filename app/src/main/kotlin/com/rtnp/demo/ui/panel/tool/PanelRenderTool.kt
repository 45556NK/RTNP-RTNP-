// app/src/main/kotlin/com/rtnp/demo/ui/panel/tool/PanelRenderTool.kt
package com.rtnp.demo.ui.panel.tool

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

object PanelRenderTool {

    private val BG_COLOR = Color.argb(64, 200, 200, 200)
    private val BORDER_COLOR = Color.WHITE
    private const val BORDER_WIDTH = 2f
    private const val CORNER_RADIUS = 24f

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = BG_COLOR
        style = Paint.Style.FILL
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = BORDER_COLOR
        style = Paint.Style.STROKE
        strokeWidth = BORDER_WIDTH
    }

    fun drawPanelBackground(canvas: Canvas) {
        val rect = PanelBox.rect()
        canvas.drawRoundRect(rect, CORNER_RADIUS, CORNER_RADIUS, bgPaint)
        canvas.drawRoundRect(rect, CORNER_RADIUS, CORNER_RADIUS, borderPaint)
    }
}