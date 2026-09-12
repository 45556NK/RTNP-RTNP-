// app/src/main/kotlin/com/rtnp/demo/ui/action/ActionButtonTool.kt
package com.rtnp.demo.ui.action

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

object ActionButtonTool {

    private const val SIZE_RATIO = 1f / 8f
    private const val BORDER_RATIO = 1f / 320f

    private val BG_COLOR = Color.argb(25, 173, 216, 230)
    private val BORDER_COLOR = Color.argb(179, 173, 216, 230)

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = BG_COLOR
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = BORDER_COLOR
        style = Paint.Style.STROKE
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
    }

    data class ButtonData(
        val id: String,
        val label: String,
        val cx: Float,
        val cy: Float,
        val radius: Float,
        val icon: Bitmap? = null
    )

    fun calculateSize(vw: Int): Float = vw * SIZE_RATIO

    fun calculateBorder(vw: Int): Float = vw * BORDER_RATIO

    fun createButton(id: String, label: String, cx: Float, cy: Float, vw: Int, icon: Bitmap? = null): ButtonData {
        val size = calculateSize(vw)
        return ButtonData(id = id, label = label, cx = cx, cy = cy, radius = size / 2f, icon = icon)
    }

    fun draw(canvas: Canvas, button: ButtonData, vw: Int, scale: Float = 1f, labelScale: Float = 0f, labelAlpha: Float = 0f) {
        borderPaint.strokeWidth = calculateBorder(vw) * scale

        val scaledRadius = button.radius * scale
        canvas.drawCircle(button.cx, button.cy, scaledRadius, bgPaint)
        canvas.drawCircle(button.cx, button.cy, scaledRadius, borderPaint)

        button.icon?.let { icon ->
            val iconSize = scaledRadius * 1.2f
            val left = button.cx - iconSize / 2f
            val top = button.cy - iconSize / 2f
            canvas.drawBitmap(icon, null, RectF(left, top, left + iconSize, top + iconSize), null)
        }

        // 动画文字
        if (labelScale > 0f && labelAlpha > 0f && button.label.isNotEmpty()) {
            labelPaint.textSize = calculateSize(vw) * 0.35f * labelScale
            labelPaint.alpha = (labelAlpha * 255).toInt().coerceIn(0, 255)
            val textY = button.cy - scaledRadius - calculateSize(vw) * 0.15f
            canvas.drawText(button.label, button.cx, textY, labelPaint)
        }
    }

    fun isHit(button: ButtonData, x: Float, y: Float): Boolean {
        val dx = x - button.cx
        val dy = y - button.cy
        return dx * dx + dy * dy <= button.radius * button.radius
    }
}