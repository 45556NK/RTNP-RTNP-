// app/src/main/kotlin/com/rtnp/demo/strategy/StrategyRenderTools.kt
package com.rtnp.demo.strategy

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

/**
 * 策略渲染辅助工具
 */
object StrategyRenderTools {

    /**
     * 环形进度条
     * @param canvas 画布
     * @param centerX 圆心X
     * @param centerY 圆心Y
     * @param radius 半径
     * @param strokeWidth 边框厚度
     * @param color 颜色（会自动应用15%透明度）
     * @param progress 进度 0.0 ~ 1.0，从正上方顺时针
     */
    fun drawCircleProgress(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float,
        strokeWidth: Float,
        color: Int,
        progress: Float
    ) {
        if (progress <= 0f) return

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            alpha = (255 * 0.15f).toInt()
        }

        // 画完整圆（底色，15%透明度）
        canvas.drawCircle(centerX, centerY, radius, paint)

        // 画进度弧（从正上方-90度开始顺时针）
        if (progress > 0f && progress < 1f) {
            paint.alpha = 255  // 进度部分不透明
            val oval = RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
            canvas.drawArc(oval, -90f, 360f * progress, false, paint)
        } else if (progress >= 1f) {
            paint.alpha = 255
            canvas.drawCircle(centerX, centerY, radius, paint)
        }
    }
}