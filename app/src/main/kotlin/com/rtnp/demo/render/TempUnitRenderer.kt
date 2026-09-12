// app/src/main/kotlin/com/rtnp/demo/render/TempUnitRenderer.kt
package com.rtnp.demo.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.rtnp.demo.core.InventoryItem
import com.rtnp.demo.core.TempUnitData
import com.rtnp.demo.logic.BuildManager

object TempUnitRenderer {

    private const val MIN_ALPHA = 64f
    private const val MAX_ALPHA = 211f
    private const val CYCLE_DURATION = 2500f

    private val shapePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val whiteRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(100, 100, 180, 255)  // 浅蓝色，半透明
        style = Paint.Style.STROKE
        strokeWidth = 10f  // 你调整后的值
    }
    private val yellowArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.YELLOW
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    @JvmStatic
    fun draw(canvas: Canvas, data: TempUnitData, zoom: Float) {
        val item = data.item
        val worldX = data.worldX
        val worldY = data.worldY

        val alpha = getPulsingAlpha()
        shapePaint.color = item.color
        shapePaint.alpha = alpha

        canvas.save()
        canvas.translate(worldX, worldY)
        drawShape(canvas, item)
        canvas.restore()
    }

    /** 绘制建造进程：单位形状 + 进度圆环 */
    @JvmStatic
    fun drawBuildProcess(canvas: Canvas, process: BuildManager.BuildProcess, zoom: Float) {
        val item = process.item
        val worldX = process.worldX
        val worldY = process.worldY
        val progress = (process.progress / item.buildAmount).coerceIn(0f, 1f)
        val radius = item.refVolumeRadius.takeIf { it > 0f } ?: 30f

        canvas.save()
        canvas.translate(worldX, worldY)

        // 绘制半透明单位形状
        shapePaint.color = item.color
        shapePaint.alpha = 128 // 半透明
        drawShape(canvas, item)

        // 绘制进度圆环
        // 白色底环
        canvas.drawCircle(0f, 0f, radius, whiteRingPaint)
        // 黄色进度弧（从-90度顺时针）
        if (progress > 0f) {
            val oval = RectF(-radius, -radius, radius, radius)
            canvas.drawArc(oval, -90f, 360f * progress, false, yellowArcPaint)
        }

        canvas.restore()
    }

    private fun drawShape(canvas: Canvas, item: InventoryItem) {
        when (item.shape) {
            "circle" -> canvas.drawCircle(0f, 0f, item.size / 2f, shapePaint)
            "square" -> {
                val half = item.size / 2f
                canvas.drawRect(-half, -half, half, half, shapePaint)
            }
            "rectangle" -> {
                val hw = item.width / 2f
                val hh = item.height / 2f
                canvas.drawRect(-hw, -hh, hw, hh, shapePaint)
            }
            else -> {
                val hw = item.width / 2f
                val hh = item.height / 2f
                canvas.drawRect(-hw, -hh, hw, hh, shapePaint)
            }
        }
    }

    private fun getPulsingAlpha(): Int {
        val now = System.currentTimeMillis()
        val phase = (now % CYCLE_DURATION.toLong()) / CYCLE_DURATION.toFloat()
        val progress = if (phase <= 0.5f) phase * 2f else (1f - phase) * 2f
        return (MIN_ALPHA + (MAX_ALPHA - MIN_ALPHA) * progress).toInt()
    }
}