package com.rtnp.demo.ui

import android.graphics.*

/**
 * 六边形按钮
 * 使用 @JvmField 保证 Java 代码可以直接访问字段
 * 支持文字换行（\n）
 */
class HexButton(
    @JvmField var centerX: Float,
    @JvmField var centerY: Float,
    @JvmField var size: Float,
    @JvmField var label: String
) {
    @JvmField var textColor: Int = Color.BLACK
    @JvmField var fillColor: Int = Color.rgb(255, 165, 0)
    @JvmField var strokeColor: Int = Color.WHITE
    @JvmField var strokeWidth: Float = 8f
    @JvmField var fontSize: Float = 50f
    @JvmField var pressedStrokeColor: Int = Color.rgb(255, 215, 0)

    private var pressed = false
    private var event: MenuEvent? = null
    private var targetDirectory: String? = null
    private var fillPath: Path? = null
    private var strokePath: Path? = null
    private var savedStrokeColor: Int = strokeColor

    init {
        buildPaths()
    }

    fun setLayout(cx: Float, cy: Float, size: Float, label: String) {
        this.centerX = cx
        this.centerY = cy
        this.size = size
        this.label = label
        buildPaths()
    }

    fun buildPaths() {
        fillPath = Path()
        strokePath = Path()

        for (i in 0 until 6) {
            val angle = Math.PI / 3 * i - Math.PI / 2
            val x = (centerX + size * Math.cos(angle)).toFloat()
            val y = (centerY + size * Math.sin(angle)).toFloat()
            if (i == 0) strokePath?.moveTo(x, y)
            else strokePath?.lineTo(x, y)
        }
        strokePath?.close()

        val fillSize = size - strokeWidth / 2f
        for (i in 0 until 6) {
            val angle = Math.PI / 3 * i - Math.PI / 2
            val x = (centerX + fillSize * Math.cos(angle)).toFloat()
            val y = (centerY + fillSize * Math.sin(angle)).toFloat()
            if (i == 0) fillPath?.moveTo(x, y)
            else fillPath?.lineTo(x, y)
        }
        fillPath?.close()
    }

    fun setOnClick(event: MenuEvent?) {
        this.event = event
    }

    fun setTargetDirectory(dir: String?) {
        this.targetDirectory = dir
    }

    fun isPointInside(x: Float, y: Float): Boolean {
        val dx = x - centerX
        val dy = y - centerY
        return dx * dx + dy * dy <= size * size
    }

    fun onTouchDown() {
        if (!pressed) {
            pressed = true
            savedStrokeColor = strokeColor
            strokeColor = pressedStrokeColor
        }
    }

    fun onTouchUp() {
        if (pressed) {
            pressed = false
            strokeColor = savedStrokeColor
        }
    }

    fun cancel() {
        if (pressed) {
            pressed = false
            strokeColor = savedStrokeColor
        }
    }

    fun performClick(manager: MenuManager?) {
        event?.execute(this, manager)
    }

    fun draw(canvas: Canvas) {
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = fillColor
        }
        fillPath?.let { canvas.drawPath(it, fill) }

        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = strokeColor
            strokeWidth = this@HexButton.strokeWidth
        }
        strokePath?.let { canvas.drawPath(it, stroke) }

        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = fontSize
            textAlign = Paint.Align.CENTER
        }

        // ★ 支持换行
        if (label.contains("\n")) {
            val lines = label.split("\n")
            val lineHeight = fontSize * 1.2f
            val totalHeight = lines.size * lineHeight
            var y = centerY - totalHeight / 2f + lineHeight / 2f
            for (line in lines) {
                canvas.drawText(line, centerX, y, text)
                y += lineHeight
            }
        } else {
            canvas.drawText(label, centerX, centerY + fontSize / 3, text)
        }
    }
}