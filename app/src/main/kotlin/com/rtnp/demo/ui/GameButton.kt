// app/src/main/kotlin/com/rtnp/demo/ui/GameButton.kt
package com.rtnp.demo.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

class GameButton(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    private val text: String
) {
    private var currentX: Float = x
    private var currentY: Float = y
    private val currentWidth: Float = width
    private val currentHeight: Float = height

    private var borderPaint: Paint? = null
    private var borderNormalColor: Int = Color.TRANSPARENT
    private var borderPressedColor: Int = Color.TRANSPARENT
    private var hasBorder = false

    private var bgPaint: Paint? = null

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    private var isPressed = false
    private var triggerOnRelease = false

    private var onClick: (() -> Unit)? = null

    private val rect = RectF()

    init {
        updateRect()
    }

    fun setPosition(x: Float, y: Float) {
        this.currentX = x
        this.currentY = y
        updateRect()
    }

    fun setTextSize(size: Float) {
        textPaint.textSize = size
    }

    fun setTextColor(color: Int) {
        textPaint.color = color
    }

    fun setBackgroundColor(color: Int) {
        if (bgPaint == null) {
            bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        }
        bgPaint!!.color = color
    }

    fun setBorder(normalColor: Int, pressedColor: Int, strokeWidth: Float = 3f) {
        hasBorder = true
        borderNormalColor = normalColor
        borderPressedColor = pressedColor
        triggerOnRelease = true
        if (borderPaint == null) {
            borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
            }
        }
        borderPaint!!.strokeWidth = strokeWidth
        borderPaint!!.color = normalColor
    }

    fun setOnClick(listener: () -> Unit) {
        this.onClick = listener
    }

    fun contains(px: Float, py: Float): Boolean = rect.contains(px, py)

    fun onTouchDown(x: Float, y: Float): Boolean {
        if (!contains(x, y)) return false
        isPressed = true
        if (hasBorder) {
            borderPaint?.color = borderPressedColor
        }
        if (!triggerOnRelease) {
            onClick?.invoke()
        }
        return true
    }

    fun onTouchUp(x: Float, y: Float): Boolean {
        if (!isPressed) return false
        isPressed = false
        if (hasBorder) {
            borderPaint?.color = borderNormalColor
        }
        if (triggerOnRelease && contains(x, y)) {
            onClick?.invoke()
            return true
        }
        return false
    }

    fun cancel() {
        isPressed = false
        if (hasBorder) {
            borderPaint?.color = borderNormalColor
        }
    }

    fun draw(canvas: Canvas) {
        bgPaint?.let { canvas.drawRect(rect, it) }
        borderPaint?.let { canvas.drawRect(rect, it) }
        val textY = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(text, rect.centerX(), textY, textPaint)
    }

    private fun updateRect() {
        rect.set(currentX, currentY, currentX + currentWidth, currentY + currentHeight)
    }

    companion object {
        // ==================== 正方形 ====================

        fun create(
            shape: String,
            reference: String,
            percent: Float,
            text: String,
            posX: Any,
            posY: Any,
            screenW: Int,
            screenH: Int
        ): GameButton {
            require(shape == "square")
            val size = when (reference) {
                "width" -> screenW * percent / 100f
                "height" -> screenH * percent / 100f
                else -> throw IllegalArgumentException("reference 必须是 width 或 height")
            }
            val x = parseCoordinate(posX, screenW, size)
            val y = parseCoordinate(posY, screenH, size)
            return GameButton(x, y, size, size, text)
        }

        fun create(
            shape: String,
            reference: String,
            percent: Float,
            text: String,
            posX: Any,
            posY: Any
        ): GameButton {
            val metrics = android.content.res.Resources.getSystem().displayMetrics
            return create(shape, reference, percent, text, posX, posY, metrics.widthPixels, metrics.heightPixels)
        }

        // ==================== 长方形 ====================

        fun create(
            shape: String,
            widthFraction: Float,
            heightFraction: Float,
            text: String,
            posX: Any,
            posY: Any,
            screenW: Int,
            screenH: Int
        ): GameButton {
            require(shape == "rectangle")
            val w = screenW * widthFraction
            val h = screenH * heightFraction
            val x = parseCoordinate(posX, screenW, w)
            val y = parseCoordinate(posY, screenH, h)
            return GameButton(x, y, w, h, text)
        }

        fun create(
            shape: String,
            widthFraction: Float,
            heightFraction: Float,
            text: String,
            posX: Any,
            posY: Any
        ): GameButton {
            val metrics = android.content.res.Resources.getSystem().displayMetrics
            return create(shape, widthFraction, heightFraction, text, posX, posY, metrics.widthPixels, metrics.heightPixels)
        }

        private fun parseCoordinate(value: Any, screenSize: Int, buttonSize: Float): Float {
            return when (value) {
                is Number -> value.toFloat()
                is String -> {
                    val fraction = value.toFloatOrNull()
                    if (fraction != null && fraction > 0f && fraction <= 1f) {
                        screenSize * fraction - buttonSize / 2f
                    } else {
                        value.toFloat()
                    }
                }
                else -> 0f
            }
        }
    }
}