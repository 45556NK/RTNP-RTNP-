// app/src/main/kotlin/com/rtnp/demo/ui/panel/tool/CooldownRenderTool.kt
package com.rtnp.demo.ui.panel.tool

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.rtnp.demo.ui.panel.data.InteractiveButtonComponent
import com.rtnp.demo.logger.Logger

object CooldownRenderTool {

    enum class Direction {
        BOTTOM_UP,
        TOP_DOWN
    }

    private val cooldownPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    fun draw(canvas: Canvas, comp: InteractiveButtonComponent, rect: RectF) {
        // CooldownRenderTool.draw 开头
        val progress = comp.cooldownProgress.coerceIn(0f, 1f)
        if (progress <= 0f) return

        cooldownPaint.color = comp.cooldownColor

        val fillHeight = rect.height() * progress
        val cooldownRect = when (comp.cooldownDirection) {
            Direction.BOTTOM_UP -> RectF(
                rect.left,
                rect.bottom - fillHeight,
                rect.right,
                rect.bottom
            )
            Direction.TOP_DOWN -> RectF(
                rect.left,
                rect.top,
                rect.right,
                rect.top + fillHeight
            )
            else -> RectF(
                rect.left,
                rect.bottom - fillHeight,
                rect.right,
                rect.bottom
            )
        }
        canvas.drawRoundRect(cooldownRect, InteractiveButtonTool.CORNER_RADIUS, InteractiveButtonTool.CORNER_RADIUS, cooldownPaint)
    }
}