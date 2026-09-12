// app/src/main/kotlin/com/rtnp/demo/ui/panel/tool/EnergyRingRenderTool.kt
package com.rtnp.demo.ui.panel.tool

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.rtnp.demo.ui.panel.data.InteractiveButtonComponent

object EnergyRingRenderTool {

    enum class Direction {
        CLOCKWISE,       // 顺时针
        COUNTERCLOCKWISE // 逆时针（预留）
    }

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    /**
     * 绘制能量环
     * @param canvas 画布
     * @param comp 按钮组件
     * @param rect 按钮矩形
     */
    fun draw(canvas: Canvas, comp: InteractiveButtonComponent, rect: RectF) {
        val progress = comp.energyProgress.coerceIn(0f, 1f)
        if (progress <= 0f) return

        ringPaint.color = comp.energyColor
        val size = rect.width() // 正方形按钮
        val outerRadius = size * 1f / 3f   // 最远端：中心到2/3处
        val ringWidth = size / 9f           // 环宽
        val innerRadius = outerRadius - ringWidth
        ringPaint.strokeWidth = ringWidth

        val centerX = rect.centerX()
        val centerY = rect.centerY()

        // 圆弧外接矩形
        val oval = RectF(
            centerX - outerRadius,
            centerY - outerRadius,
            centerX + outerRadius,
            centerY + outerRadius
        )

        // 起始角度：从正上方（-90度）开始，顺时针绘制
        val startAngle = -90f
        val sweepAngle = when (comp.energyDirection) {
            Direction.CLOCKWISE -> 360f * progress
            Direction.COUNTERCLOCKWISE -> -360f * progress
            else -> 360f * progress
        }

        // 绘制圆环（使用内半径和外半径的平均值作为描边中心，直接用 outerRadius 描边即可）
        // 实际描边宽度已设置，绘制的弧线会在 outerRadius 上，描边向内延伸 ringWidth/2，向外延伸 ringWidth/2
        // 所以实际圆环覆盖范围是 [outerRadius - ringWidth/2, outerRadius + ringWidth/2]
        canvas.drawArc(oval, startAngle, sweepAngle, false, ringPaint)
    }
}