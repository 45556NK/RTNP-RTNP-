// app/src/main/kotlin/com/rtnp/demo/ui/WarningMessage.kt
package com.rtnp.demo.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.WindowManager

object WarningMessage {

    private val bgColor = Color.rgb(139, 0, 0)
    private val borderColor = Color.rgb(255, 100, 100)
    private val textColor = Color.WHITE

    private enum class Phase {
        IDLE,
        APPEAR,    // 0.1秒：0.8倍→1.2倍，上移1/10屏高
        FADE_OUT   // 0.8秒：1.2倍→1.0倍，移到顶端，透明度降低
    }

    private var phase = Phase.IDLE
    private var phaseTime = 0f
    private var message = ""
    private var screenWidth = 0
    private var screenHeight = 0

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textSize = 36f
        textAlign = Paint.Align.CENTER
    }

    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = android.util.DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
    }

    fun show(text: String) {
        message = text
        phase = Phase.APPEAR
        phaseTime = 0f
    }

    fun isActive(): Boolean = phase != Phase.IDLE

    fun update(deltaTime: Float) {
        if (phase == Phase.IDLE) return
        phaseTime += deltaTime
        when (phase) {
            Phase.APPEAR -> if (phaseTime >= 0.1f) { phaseTime = 0f; phase = Phase.FADE_OUT }
            Phase.FADE_OUT -> if (phaseTime >= 0.8f) { phase = Phase.IDLE }
            Phase.IDLE -> {}
        }
    }

    fun draw(canvas: Canvas) {
        if (phase == Phase.IDLE || screenWidth <= 0 || screenHeight <= 0) return

        val panelWidth = screenWidth / 3f
        val textHeight = textPaint.textSize + 40f
        val panelHeight = textHeight

        // 起点：屏幕1/4处（反转后）
        val startY = screenHeight * 1f / 4f
        // 终点：屏幕顶端以上
        val endY = -panelHeight

        val (scale, centerY, alpha) = when (phase) {
            Phase.APPEAR -> {
                val t = (phaseTime / 0.1f).coerceIn(0f, 1f)
                val s = 0.8f + 0.4f * t
                val y = startY - screenHeight * 0.1f * t
                Triple(s, y, 255)
            }
            Phase.FADE_OUT -> {
                val t = (phaseTime / 0.8f).coerceIn(0f, 1f)
                val s = 1.2f - 0.2f * t
                val y = startY - screenHeight * 0.1f + (endY - (startY - screenHeight * 0.1f)) * t
                val a = (255 * (1f - t)).toInt()
                Triple(s, y, a)
            }
            Phase.IDLE -> return
        }

        val centerX = screenWidth / 2f

        canvas.save()
        canvas.translate(centerX, centerY)
        canvas.scale(scale, scale)

        val left = -panelWidth / 2f
        val top = -panelHeight / 2f
        val right = panelWidth / 2f
        val bottom = panelHeight / 2f
        val cornerRadius = 10f

        bgPaint.color = Color.argb(alpha, 139, 0, 0)
        canvas.drawRoundRect(RectF(left, top, right, bottom), cornerRadius, cornerRadius, bgPaint)

        borderPaint.color = Color.argb(alpha, 255, 100, 100)
        canvas.drawRoundRect(RectF(left, top, right, bottom), cornerRadius, cornerRadius, borderPaint)

        textPaint.alpha = alpha
        val textY = (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(message, 0f, -textY, textPaint)

        canvas.restore()
    }
}