package com.rtnp.demo.ui.settings

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import com.rtnp.demo.audio.AudioManager
import com.rtnp.demo.ui.HexButton
import com.rtnp.demo.ui.MenuEvent

/**
 * 音频设置界面（独立绘制，不添加到视图层级）
 */
class AudioSettingsScreen(
    context: Context,
    private val onBack: () -> Unit,
    private val onStateChanged: () -> Unit = {}  // 添加状态变更回调
) : View(context) {

    private val titlePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.YELLOW
        textSize = 80f
        textAlign = Paint.Align.CENTER
    }

    private val labelPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 36f
        textAlign = Paint.Align.CENTER
    }

    private val percentPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    private val rectPaint: Paint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = 4f
    }

    private val fillPaint: Paint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val linePaint: Paint = Paint().apply {
        color = Color.GRAY
        strokeWidth = 6f
    }

    private val sliderPaint: Paint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private var isMusicOn = AudioManager.isMusicOn()
    private var currentVolume = AudioManager.getVolume()
    private var draggingSlider = false

    // 布局参数
    private var switchRectLeft = 0f
    private var switchRectTop = 0f
    private var switchRectWidth = 0f
    private var switchRectHeight = 0f
    private var switchBlockWidth = 0f

    private var volumeBarLeft = 0f
    private var volumeBarRight = 0f
    private var volumeBarY = 0f
    private var sliderSize = 0f
    private var sliderX = 0f

    private lateinit var backButton: HexButton

    init {
        setBackgroundColor(Color.BLACK)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        computeLayout(w, h)
    }

    private fun computeLayout(w: Int, h: Int) {
        val barW = w * 0.8f
        val barH = barW / 6f

        switchRectWidth = barW / 5f
        switchRectHeight = barH / 2f
        switchBlockWidth = switchRectWidth / 2f
        sliderSize = barH / 2f
        val btnDiameter = 160f

        switchRectLeft = (w - switchRectWidth) / 2f
        volumeBarLeft = (w - barW) / 2f
        volumeBarRight = volumeBarLeft + barW

        var currentY = h * 0.1f

        // 标题
        currentY += 80f + 20f

        // 音乐标签
        currentY += 36f + 5f

        // 开关矩形
        switchRectTop = currentY
        currentY += switchRectHeight + 20f

        // 总音量标签
        currentY += 36f + 5f

        // 滑块
        volumeBarY = currentY + sliderSize / 2f
        currentY += sliderSize + 5f

        // 百分比
        currentY += 28f + 20f

        // 返回按钮
        val btnCenterY = currentY + btnDiameter / 2f
        backButton = HexButton(w / 2f, btnCenterY, btnDiameter / 2f, "返回").apply {
            fillColor = Color.rgb(173, 216, 230)
            strokeColor = Color.WHITE
            pressedStrokeColor = Color.YELLOW
            fontSize = 45f
            buildPaths()
            setOnClick(MenuEvent { _, _ ->
                onBack()
            })
        }

        sliderX = volumeBarLeft + (volumeBarRight - volumeBarLeft) * currentVolume
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // 开关点击
                if (x >= switchRectLeft && x <= switchRectLeft + switchRectWidth &&
                    y >= switchRectTop && y <= switchRectTop + switchRectHeight) {
                    isMusicOn = !isMusicOn
                    AudioManager.setMusicOn(isMusicOn)
                    onStateChanged()  // 通知外部刷新
                    return true
                }

                // 滑块点击
                if (x >= volumeBarLeft - sliderSize / 2 && x <= volumeBarRight + sliderSize / 2 &&
                    y >= volumeBarY - sliderSize / 2 && y <= volumeBarY + sliderSize / 2) {
                    draggingSlider = true
                    updateSliderPosition(x)
                    onStateChanged()  // 通知外部刷新
                    return true
                }

                // 返回按钮
                if (backButton.isPointInside(x, y)) {
                    backButton.onTouchDown()
                    onStateChanged()  // 通知外部刷新
                    return true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (draggingSlider) {
                    updateSliderPosition(x)
                    onStateChanged()  // 通知外部刷新
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (draggingSlider) {
                    draggingSlider = false
                    onStateChanged()  // 通知外部刷新
                }
                if (backButton.isPointInside(x, y)) {
                    backButton.onTouchUp()
                    backButton.performClick(null)
                } else {
                    backButton.cancel()
                }
                onStateChanged()  // 通知外部刷新
                return true
            }
        }
        return true
    }

    private fun updateSliderPosition(x: Float) {
        sliderX = x.coerceIn(volumeBarLeft, volumeBarRight)
        currentVolume = (sliderX - volumeBarLeft) / (volumeBarRight - volumeBarLeft)
        AudioManager.setVolume(currentVolume)
    }

    override fun onDraw(canvas: Canvas) {
        // 背景
        canvas.drawColor(Color.BLACK)

        // 标题
        canvas.drawText("音频设置", width / 2f, height * 0.1f + 80f, titlePaint)

        // 音乐标签
        canvas.drawText("音乐", switchRectLeft + switchRectWidth / 2f, switchRectTop - 5f, labelPaint)

        // 开关背景
        canvas.drawRect(switchRectLeft, switchRectTop, switchRectLeft + switchRectWidth, switchRectTop + switchRectHeight, rectPaint)

        // 开关填充
        if (isMusicOn) {
            canvas.drawRect(switchRectLeft, switchRectTop, switchRectLeft + switchBlockWidth, switchRectTop + switchRectHeight, fillPaint)
        } else {
            canvas.drawRect(switchRectLeft + switchBlockWidth, switchRectTop, switchRectLeft + switchRectWidth, switchRectTop + switchRectHeight, fillPaint)
        }

        // 总音量标签
        canvas.drawText("总音量", (volumeBarLeft + volumeBarRight) / 2f, volumeBarY - sliderSize / 2 - 5f, labelPaint)

        // 音量横线
        canvas.drawLine(volumeBarLeft, volumeBarY, volumeBarRight, volumeBarY, linePaint)

        // 音量滑块
        canvas.drawRect(sliderX - sliderSize / 2, volumeBarY - sliderSize / 2,
                        sliderX + sliderSize / 2, volumeBarY + sliderSize / 2, sliderPaint)

        // 百分比
        canvas.drawText("${(currentVolume * 100).toInt()}%", width / 2f, volumeBarY + sliderSize / 2 + 25f, percentPaint)

        // 返回按钮
        backButton.draw(canvas)
    }
}