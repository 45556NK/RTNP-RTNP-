package com.rtnp.demo.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.rtnp.demo.core.GameConstants

class ToastTool {

    companion object {
        @JvmStatic var globalInstance: ToastTool? = null
        @JvmStatic fun showGlobal(text: String, onExit: (() -> Unit)? = null) { globalInstance?.show(text, onExit) }
        @JvmStatic fun hideGlobal() { globalInstance?.hide() }
    }

    class ToastButton(
        val text: String = "退出",
        val color: Int = Color.rgb(139, 0, 0),
        val onClick: (() -> Unit)? = null
    )

    var isVisible: Boolean = false
        private set

    private var message: String = ""
    private var button: ToastButton? = null
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0

    var offsetX: Float = 0f
    var offsetY: Float = 0f

    // 按钮动画状态
    private var isPressed: Boolean = false
    private var pressStartTime: Long = 0L
    private var releaseStartTime: Long = 0L
    private var isRecovering: Boolean = false
    private val animDuration = 0.1f
    private val pressScale = 0.8f

    private val panelBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(200, 30, 30, 30)
        style = Paint.Style.FILL
    }
    private val panelBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    // 使用大字体，固定大小，不再动态切换
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
    }

    private val buttonBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val buttonTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
    }
    private val buttonBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private var buttonRect: RectF = RectF()

    fun updateScreenSize(w: Int, h: Int) {
        screenWidth = w
        screenHeight = h
        // 使用 GameConstants 大字体
        textPaint.textSize = GameConstants.getLargeFontSize(h)
        buttonTextPaint.textSize = GameConstants.getSmallFontSize(h)
    }

    fun show(text: String, onExit: (() -> Unit)? = null) {
        this.message = text
        if (onExit != null) {
            this.button = ToastButton(onClick = onExit)
        } else {
            this.button = null
        }
        isVisible = true
        isPressed = false
        isRecovering = false
    }

    fun hide() {
        isVisible = false
        button = null
        offsetX = 0f
        offsetY = 0f
        isPressed = false
        isRecovering = false
    }

    fun setOffset(x: Float, y: Float) {
        offsetX = x
        offsetY = y
    }

    fun isButtonHit(x: Float, y: Float): Boolean {
        return isVisible && button != null && buttonRect.contains(x, y)
    }

    /** 按钮按下事件，由 GameTouchHandler 调用 */
    fun onTouchDown(x: Float, y: Float): Boolean {
        if (!isVisible || button == null) return false
        if (buttonRect.contains(x, y)) {
            isPressed = true
            isRecovering = false
            pressStartTime = System.currentTimeMillis()
            return true
        }
        return false
    }

    /** 按钮移动事件（用于判断是否移出） */
    fun onTouchMove(x: Float, y: Float) {
        if (!isPressed) return
        if (!buttonRect.contains(x, y)) {
            // 移出按钮，取消按下状态
            isPressed = false
            isRecovering = true
            releaseStartTime = System.currentTimeMillis()
        }
    }

    /** 按钮抬起事件，返回是否触发回调 */
    fun onTouchUp(x: Float, y: Float): Boolean {
        if (!isPressed) return false
        isPressed = false
        isRecovering = true
        releaseStartTime = System.currentTimeMillis()
        if (buttonRect.contains(x, y)) {
            button?.onClick?.invoke()
            return true
        }
        return false
    }

    /** 取消按钮触摸 */
    fun cancelTouch() {
        isPressed = false
        isRecovering = true
        releaseStartTime = System.currentTimeMillis()
    }

    fun draw(canvas: Canvas) {
        if (!isVisible || screenWidth <= 0 || screenHeight <= 0) return

        val panelWidth = screenWidth * 6 / 8f
        val panelHeight = screenHeight / 5f
        val marginX = screenWidth / 8f
        val marginBottom = screenWidth / 8f

        val left = marginX + offsetX
        val top = screenHeight - marginBottom - panelHeight + offsetY
        val right = left + panelWidth
        val bottom = top + panelHeight
        val cornerRadius = 16f

        // 绘制背景和边框
        canvas.drawRoundRect(RectF(left, top, right, bottom), cornerRadius, cornerRadius, panelBgPaint)
        canvas.drawRoundRect(RectF(left, top, right, bottom), cornerRadius, cornerRadius, panelBorderPaint)

        // 计算按钮缩放动画
        val now = System.currentTimeMillis()
        var btnScale = 1f
        if (isPressed) {
            val elapsed = (now - pressStartTime) / 1000f
            val progress = (elapsed / animDuration).coerceIn(0f, 1f)
            btnScale = 1f + (pressScale - 1f) * progress
        } else if (isRecovering) {
            val elapsed = (now - releaseStartTime) / 1000f
            val progress = (elapsed / animDuration).coerceIn(0f, 1f)
            btnScale = pressScale + (1f - pressScale) * progress
            if (progress >= 1f) isRecovering = false
        }

        if (button != null) {
            val btnWidth = panelWidth / 3f
            val btnLeft = right - btnWidth
            buttonRect.set(btnLeft, top, right, bottom)

            // 以按钮中心缩放
            val btnCenterX = buttonRect.centerX()
            val btnCenterY = buttonRect.centerY()
            canvas.save()
            canvas.scale(btnScale, btnScale, btnCenterX, btnCenterY)

            buttonBgPaint.color = button!!.color
            canvas.drawRoundRect(buttonRect, cornerRadius, cornerRadius, buttonBgPaint)
            canvas.drawRoundRect(buttonRect, cornerRadius, cornerRadius, buttonBorderPaint)

            val btnTextY = btnCenterY - (buttonTextPaint.descent() + buttonTextPaint.ascent()) / 2f
            canvas.drawText(button!!.text, btnCenterX, btnTextY, buttonTextPaint)

            canvas.restore()

            // 文本区域（左侧2/3），固定使用大字体
            val textAreaLeft = left + 20f
            val textAreaRight = btnLeft - 20f
            val textCx = (textAreaLeft + textAreaRight) / 2f
            val textCy = (top + bottom) / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(message, textCx, textCy, textPaint)
        } else {
            val textCx = (left + right) / 2f
            val textCy = (top + bottom) / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(message, textCx, textCy, textPaint)
        }
    }
}