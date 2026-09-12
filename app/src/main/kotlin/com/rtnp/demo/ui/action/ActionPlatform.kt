// app/src/main/kotlin/com/rtnp/demo/ui/action/ActionPlatform.kt
package com.rtnp.demo.ui.action

import android.content.Context
import android.graphics.Canvas
import com.rtnp.demo.image.ImageManager

object ActionPlatform {

    enum class Mode {
        NONE,
        CONFIRM_CANCEL,
        STOP
    }

    private const val BOTTOM_OFFSET_RATIO = 1f / 8f
    private const val BUTTON_GAP = 10f

    private const val CONFIRM_ICON = "images/ui/confirm_btn.png"
    private const val CANCEL_ICON = "images/ui/cancel_btn.png"
    private const val STOP_ICON = "images/ui/stop_btn.png"

    private const val PRESS_DURATION = 0.1f
    private const val PRESS_SCALE = 0.8f
    private const val TIMEOUT_MS = 3000L

    private var mode: Mode = Mode.NONE
    private var buttons: List<ActionButtonTool.ButtonData> = emptyList()
    private var onButtonClicked: ((String) -> Unit)? = null

    private var pressedIndex: Int = -1
    private var pressStartTime: Long = 0L
    private var isRecovering: Boolean = false
    private var releaseStartTime: Long = 0L
    
    // ActionPlatform.kt 增加
    fun getMode(): Mode = mode

    fun showConfirmCancel(vw: Int, vh: Int, context: Context, callback: ((String) -> Unit)? = null) {
        mode = Mode.CONFIRM_CANCEL
        onButtonClicked = callback
        val size = ActionButtonTool.calculateSize(vw)
        val cy = vh - vh * BOTTOM_OFFSET_RATIO
        val totalWidth = size * 2 + BUTTON_GAP
        val startX = (vw - totalWidth) / 2f + size / 2f

        val imgMgr = ImageManager.getInstance(context)
        buttons = listOf(
            ActionButtonTool.createButton("confirm", "出动", startX, cy, vw, imgMgr.getBitmap(CONFIRM_ICON)),
            ActionButtonTool.createButton("cancel", "取消", startX + size + BUTTON_GAP, cy, vw, imgMgr.getBitmap(CANCEL_ICON))
        )
    }

    fun showStop(vw: Int, vh: Int, context: Context, callback: ((String) -> Unit)? = null) {
        mode = Mode.STOP
        onButtonClicked = callback
        val size = ActionButtonTool.calculateSize(vw)
        val cy = vh - vh * BOTTOM_OFFSET_RATIO
        val cx = vw / 2f

        val imgMgr = ImageManager.getInstance(context)
        buttons = listOf(
            ActionButtonTool.createButton("stop", "停止", cx, cy, vw, imgMgr.getBitmap(STOP_ICON))
        )
    }

    fun hide() {
        mode = Mode.NONE
        buttons = emptyList()
        onButtonClicked = null
        pressedIndex = -1
        isRecovering = false
    }

    // ==================== 渲染 ====================

    fun draw(canvas: Canvas, vw: Int) {
        if (mode == Mode.NONE) return

        val now = System.currentTimeMillis()

        for (i in buttons.indices) {
            val scale: Float
            val labelScale: Float
            val labelAlpha: Float

            if (i == pressedIndex && !isRecovering) {
                val elapsed = (now - pressStartTime) / 1000f
                val progress = (elapsed / PRESS_DURATION).coerceIn(0f, 1f)
                scale = 1f + (PRESS_SCALE - 1f) * progress
                labelScale = 0.01f + (1f - 0.01f) * progress
                labelAlpha = progress
            } else if (i == pressedIndex && isRecovering) {
                val elapsed = (now - releaseStartTime) / 1000f
                val progress = (elapsed / PRESS_DURATION).coerceIn(0f, 1f)
                scale = PRESS_SCALE + (1f - PRESS_SCALE) * progress
                labelScale = 1f - (1f - 0.01f) * progress
                labelAlpha = 1f - progress
            } else {
                scale = 1f
                labelScale = 0f
                labelAlpha = 0f
            }

            ActionButtonTool.draw(canvas, buttons[i], vw, scale, labelScale, labelAlpha)
        }
    }

    // ==================== 触摸 ====================

    fun onTouch(x: Float, y: Float, action: Int): Boolean {
        if (mode == Mode.NONE) return false

        when (action) {
            0 -> {
                for (i in buttons.indices) {
                    if (ActionButtonTool.isHit(buttons[i], x, y)) {
                        pressedIndex = i
                        pressStartTime = System.currentTimeMillis()
                        isRecovering = false
                        return true
                    }
                }
            }
            1 -> {
                if (pressedIndex >= 0) {
                    val elapsed = System.currentTimeMillis() - pressStartTime
                    val hit = ActionButtonTool.isHit(buttons[pressedIndex], x, y)
                    isRecovering = true
                    releaseStartTime = System.currentTimeMillis()

                    if (hit && elapsed <= TIMEOUT_MS) {
                        onButtonClicked?.invoke(buttons[pressedIndex].id)
                    }
                    pressedIndex = -1
                    return true
                }
            }
            3 -> {
                pressedIndex = -1
                isRecovering = false
                return true
            }
        }
        return false
    }
}