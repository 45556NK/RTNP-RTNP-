// app/src/main/kotlin/com/rtnp/demo/ui/render/button/ProgressBarTool.kt
package com.rtnp.demo.ui.render.button

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

class ProgressBarTool(
    @JvmField var totalDuration: Float = 1f,
    initialProgress: Float = 0f
) {
    var progress: Float = initialProgress
        private set

    @JvmField var speedMultiplier: Float = 1f
    @JvmField var autoAdvance: Boolean = true
    val isFinished: Boolean get() = progress >= 1f
    @JvmField var isPaused: Boolean = false

    @JvmField var bounds: RectF = RectF(0f, 0f, 200f, 20f)
    @JvmField var backgroundColor: Int = Color.argb(150, 60, 60, 60)
    @JvmField var progressColor: Int = Color.rgb(0, 200, 100)
    @JvmField var finishedColor: Int = Color.rgb(255, 215, 0)
    @JvmField var borderColor: Int = Color.WHITE
    @JvmField var borderWidth: Float = 2f
    @JvmField var cornerRadius: Float = 8f
    @JvmField var showPercentText: Boolean = false
    @JvmField var textColor: Int = Color.WHITE
    @JvmField var textSize: Float = 14f
    @JvmField var textSuffix: String = "%"
    @JvmField var leftToRight: Boolean = true
    var onFinished: (() -> Unit)? = null

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private var wasFinished = false

    fun update(deltaTime: Float) {
        if (isPaused || !autoAdvance || isFinished) return
        val step = (deltaTime * speedMultiplier) / totalDuration
        progress = (progress + step).coerceIn(0f, 1f)
        if (isFinished && !wasFinished) {
            wasFinished = true
            onFinished?.invoke()
        }
    }

    fun addProgress(amount: Float) {
        progress = (progress + amount).coerceIn(0f, 1f)
        if (isFinished && !wasFinished) {
            wasFinished = true
            onFinished?.invoke()
        }
    }

    fun reduceProgress(amount: Float) {
        progress = (progress - amount).coerceIn(0f, 1f)
    }

    fun setProgress(value: Float) {
        progress = value.coerceIn(0f, 1f)
    }

    fun speedUp(factor: Float = 2f) { speedMultiplier *= factor }
    fun slowDown(factor: Float = 2f) { speedMultiplier /= factor }

    fun reset() {
        progress = 0f
        speedMultiplier = 1f
        isPaused = false
        wasFinished = false
    }

    fun draw(canvas: Canvas) {
        val left = bounds.left
        val top = bounds.top
        val right = bounds.right
        val bottom = bounds.bottom
        val width = bounds.width()
        val height = bounds.height()

        bgPaint.color = backgroundColor
        canvas.drawRoundRect(left, top, right, bottom, cornerRadius, cornerRadius, bgPaint)

        val progressWidth = width * progress
        progressPaint.color = if (isFinished) finishedColor else progressColor
        if (leftToRight) {
            canvas.drawRoundRect(left, top, left + progressWidth, bottom, cornerRadius, cornerRadius, progressPaint)
        } else {
            canvas.drawRoundRect(right - progressWidth, top, right, bottom, cornerRadius, cornerRadius, progressPaint)
        }

        borderPaint.color = borderColor
        borderPaint.strokeWidth = borderWidth
        canvas.drawRoundRect(left, top, right, bottom, cornerRadius, cornerRadius, borderPaint)

        if (showPercentText) {
            textPaint.color = textColor
            textPaint.textSize = textSize
            val percent = (progress * 100).toInt()
            val text = "$percent$textSuffix"
            val textY = top + height / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(text, left + width / 2f, textY, textPaint)
        }
    }
}