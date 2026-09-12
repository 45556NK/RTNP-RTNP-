// app/src/main/kotlin/com/rtnp/demo/ui/settings/DebugSettingsScreen.kt
package com.rtnp.demo.ui.settings

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import com.rtnp.demo.ui.HexButton
import com.rtnp.demo.ui.HexGridUtil
import com.rtnp.demo.ui.ToastTool
import java.io.File

class DebugSettingsScreen(
    context: Context,
    private val onBack: () -> Unit
) : View(context) {

    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.YELLOW
        textSize = 80f
        textAlign = Paint.Align.CENTER
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 36f
    }

    // ★ 清除按钮（长方形）
    private val clearBtnBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val clearBtnBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val clearBtnBorderPressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.YELLOW
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val clearBtnTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 32f
        textAlign = Paint.Align.CENTER
    }

    private var backBtn: HexButton? = null

    // ★ 清除按钮位置
    private var clearBtnX = 0f
    private var clearBtnY = 0f
    private var clearBtnW = 0f
    private var clearBtnH = 0f
    private var clearBtnPressed = false

    private var pressedHexButton: HexButton? = null

    init {
        setBackgroundColor(Color.BLACK)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // 返回按钮（六边形，单独一个）
        backBtn = HexButton(w / 2f, h - 200f, 110f, "返回").apply {
            fillColor = Color.rgb(255, 165, 0)
            strokeColor = Color.WHITE
            pressedStrokeColor = Color.YELLOW
            textColor = Color.BLACK
            fontSize = 36f
            buildPaths()
        }

        // ★ 清除按钮（长方形）
        clearBtnW = w * 0.3f
        clearBtnH = 70f
        clearBtnX = w * 0.6f
        clearBtnY = h * 0.35f - clearBtnH / 2f
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x; val y = event.y
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // 检查六边形返回按钮
                if (backBtn?.isPointInside(x, y) == true) {
                    backBtn?.onTouchDown(); pressedHexButton = backBtn; return true
                }
                // ★ 检查长方形清除按钮
                if (x >= clearBtnX && x <= clearBtnX + clearBtnW &&
                    y >= clearBtnY && y <= clearBtnY + clearBtnH) {
                    clearBtnPressed = true; invalidate(); return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                pressedHexButton?.let {
                    if (!it.isPointInside(x, y)) { it.cancel(); pressedHexButton = null }
                }
                // 清除按钮移出时取消按下状态
                if (clearBtnPressed &&
                    !(x >= clearBtnX && x <= clearBtnX + clearBtnW &&
                      y >= clearBtnY && y <= clearBtnY + clearBtnH)) {
                    clearBtnPressed = false; invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                // 六边形按钮
                pressedHexButton?.let {
                    it.onTouchUp()
                    if (it.isPointInside(x, y)) {
                        onBack()
                    }
                    invalidate()
                    pressedHexButton = null
                    return true
                }
                // ★ 长方形清除按钮
                if (clearBtnPressed) {
                    clearBtnPressed = false
                    if (x >= clearBtnX && x <= clearBtnX + clearBtnW &&
                        y >= clearBtnY && y <= clearBtnY + clearBtnH) {
                        clearLogs()
                        postDelayed({ ToastTool.hideGlobal() }, 2000)
                    }
                    invalidate()
                    return true
                }
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawText("调试设置", width / 2f, height * 0.12f, titlePaint)

        val labelY = height * 0.35f + 20f
        canvas.drawText("清除日志", width * 0.1f, labelY, labelPaint)

        // ★ 清除按钮（长方形）
        clearBtnBgPaint.color = Color.rgb(200, 50, 50)
        canvas.drawRect(clearBtnX, clearBtnY, clearBtnX + clearBtnW, clearBtnY + clearBtnH, clearBtnBgPaint)
        canvas.drawRect(clearBtnX, clearBtnY, clearBtnX + clearBtnW, clearBtnY + clearBtnH,
            if (clearBtnPressed) clearBtnBorderPressedPaint else clearBtnBorderPaint)
        val textY = clearBtnY + clearBtnH / 2f - (clearBtnTextPaint.descent() + clearBtnTextPaint.ascent()) / 2f
        canvas.drawText("清除", clearBtnX + clearBtnW / 2f, textY, clearBtnTextPaint)

        // 返回按钮（六边形）
        backBtn?.draw(canvas)
    }

    private fun clearLogs() {
        try {
            val logDir = File("/storage/emulated/0/RTNP/logs")
            if (logDir.isDirectory) {
                logDir.listFiles()?.forEach { it.delete() }
            }
            val fallbackDir = File(context.getExternalFilesDir(null), "logs")
            if (fallbackDir.isDirectory) {
                fallbackDir.listFiles()?.forEach { it.delete() }
            }
        } catch (e: Exception) {
            postDelayed({ ToastTool.hideGlobal() }, 2000)
        }
    }
}