// app/src/main/kotlin/com/rtnp/demo/ui/panel/tool/ProgressBarRenderTool.kt
package com.rtnp.demo.ui.panel.tool

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

object ProgressBarRenderTool {

    const val MARGIN_H = 5f
    const val CORNER_RADIUS = 6f

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(100, 200, 200, 200)
        style = Paint.Style.FILL
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 22f
        textAlign = Paint.Align.CENTER
    }

    fun draw(
        canvas: Canvas,
        panelRect: RectF,
        layoutY: Float,
        computedHeight: Float,
        progress: Float,
        progressColor: Int,
        text: String,
        textColor: Int = Color.WHITE
    ) {
        textPaint.color = textColor
        val left = panelRect.left + MARGIN_H
        val right = panelRect.right - MARGIN_H
        val top = panelRect.top + layoutY
        val bottom = top + computedHeight
        val barRect = RectF(left, top, right, bottom)

        // 背景
        canvas.drawRoundRect(barRect, CORNER_RADIUS, CORNER_RADIUS, bgPaint)

        // 进度
        if (progress > 0f) {
            val progressRight = left + (right - left) * progress.coerceIn(0f, 1f)
            val progressRect = RectF(left, top, progressRight, bottom)
            progressPaint.color = progressColor
            canvas.drawRoundRect(progressRect, CORNER_RADIUS, CORNER_RADIUS, progressPaint)
        }

        // 边框
        canvas.drawRoundRect(barRect, CORNER_RADIUS, CORNER_RADIUS, borderPaint)

        // 文字
        val textY = barRect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(text, barRect.centerX(), textY, textPaint)
    }
}