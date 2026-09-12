package com.rtnp.demo.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent

/**
 * 底部操作确认按钮（取消 / 确定）
 *
 * 位置：紧贴底部栏左侧，与底部栏底部对齐
 * 大小：正方形，边长 = 底部栏宽度 / 3
 * 样式：白色边框（5px），按下变蓝，松开恢复白色
 * 文字：取消、确定
 */
class ActionConfirmButtons {

    // 两个按钮的矩形区域
    private val cancelRect = RectF()
    private val confirmRect = RectF()

    // 按钮边长（屏幕宽度的 1/6）
    private var buttonSize = 0f

    // 是否已布局
    private var isLayouted = false

    // 是否可见（暂留空，默认可见）
    @JvmField
    var visible: Boolean = false

    // 按下状态
    private var cancelPressed = false
    private var confirmPressed = false

    // 边框画笔
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = Color.WHITE
    }

    // 按下时边框颜色
    private val pressedBorderColor = Color.BLUE

    // 填充画笔（半透明背景，可选）
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(180, 30, 30, 30)
    }

    // 文字画笔
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 40f
        textAlign = Paint.Align.CENTER
    }

    // 回调接口
    interface OnActionListener {
        fun onCancel()
        fun onConfirm()
    }

    private var listener: OnActionListener? = null

    fun setOnActionListener(listener: OnActionListener) {
        this.listener = listener
    }

    /**
     * 根据屏幕尺寸和底部栏位置计算按钮布局
     *
     * @param screenWidth   屏幕宽度
     * @param screenHeight  屏幕高度
     * @param bottomBarLeft 底部栏左边缘坐标
     */
    fun layout(screenWidth: Int, screenHeight: Int, bottomBarLeft: Int) {
        // 底部栏宽度 = screenWidth / 2（与 BottomBar 逻辑一致）
        val barWidth = screenWidth / 2f
        buttonSize = barWidth / 3f

        // 按钮底部与屏幕底部对齐
        val bottom = screenHeight.toFloat()

        // 两个按钮右边缘紧贴底部栏左边缘
        val right = bottomBarLeft.toFloat()

        // 确认按钮在右边，取消在左边
        confirmRect.set(
            right - buttonSize,
            bottom - buttonSize,
            right,
            bottom
        )
        cancelRect.set(
            confirmRect.left - buttonSize,
            bottom - buttonSize,
            confirmRect.left,
            bottom
        )

        isLayouted = true
    }

    /**
     * 处理触摸事件，返回 true 表示事件被消费
     */
    fun onTouchEvent(event: MotionEvent): Boolean {
        if (!visible || !isLayouted) return false

        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (cancelRect.contains(x, y)) {
                    cancelPressed = true
                    return true
                }
                if (confirmRect.contains(x, y)) {
                    confirmPressed = true
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                // 如果手指移出按钮，取消按下状态
                if (cancelPressed && !cancelRect.contains(x, y)) {
                    cancelPressed = false
                }
                if (confirmPressed && !confirmRect.contains(x, y)) {
                    confirmPressed = false
                }
                // 只要在任何一个按钮区域内就消费事件
                if (cancelPressed || confirmPressed) return true
            }
            MotionEvent.ACTION_UP -> {
                if (cancelPressed && cancelRect.contains(x, y)) {
                    cancelPressed = false
                    listener?.onCancel()
                    return true
                }
                if (confirmPressed && confirmRect.contains(x, y)) {
                    confirmPressed = false
                    listener?.onConfirm()
                    return true
                }
                cancelPressed = false
                confirmPressed = false
            }
            MotionEvent.ACTION_CANCEL -> {
                cancelPressed = false
                confirmPressed = false
            }
        }
        return false
    }

    /**
     * 绘制按钮（应在底部栏和舰队面板之后调用以保证覆盖层级）
     */
    fun draw(canvas: Canvas) {
        if (!visible || !isLayouted) return

        // 绘制取消按钮
        drawButton(canvas, cancelRect, "取消", cancelPressed)

        // 绘制确定按钮
        drawButton(canvas, confirmRect, "确定", confirmPressed)
    }

    private fun drawButton(canvas: Canvas, rect: RectF, text: String, pressed: Boolean) {
        // 背景
        canvas.drawRect(rect, bgPaint)

        // 边框（按下时变蓝）
        borderPaint.color = if (pressed) pressedBorderColor else Color.WHITE
        canvas.drawRect(rect, borderPaint)

        // 文字
        val cx = rect.centerX()
        val cy = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(text, cx, cy, textPaint)
    }
}