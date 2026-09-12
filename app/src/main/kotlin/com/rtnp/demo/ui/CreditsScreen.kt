package com.rtnp.demo.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View

/**
 * 致敬名单界面
 */
class CreditsScreen(
    context: Context,
    private val backListener: OnBackListener
) : View(context) {

    interface OnBackListener {
        fun onBack()
    }

    private val titlePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLUE
        textSize = 80f
        textAlign = Paint.Align.CENTER
    }

    private val textPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 40f
        textAlign = Paint.Align.CENTER
    }

    private val musicTitlePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.YELLOW
        textSize = 60f
        textAlign = Paint.Align.CENTER
    }

    private val musicTextPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 35f
        textAlign = Paint.Align.CENTER
    }

    private lateinit var backButton: HexButton
    private var titleY = 0f
    private var codeY = 0f
    private var musicTitleY = 0f
    private var musicY1 = 0f
    private var musicY2 = 0f

    init {
        setBackgroundColor(Color.BLACK)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        val cx = w / 2f
        titleY = h * 0.15f
        codeY = titleY + 100
        musicTitleY = codeY + 150
        musicY1 = musicTitleY + 80
        musicY2 = musicY1 + 50

        backButton = HexButton(cx, h - 150f, 80f, "返回").apply {
            fillColor = Color.rgb(255, 165, 0)
            strokeColor = Color.WHITE
            pressedStrokeColor = Color.YELLOW
            fontSize = 40f
            buildPaths()
            setOnClick { _, _ ->
                backListener.onBack()
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (backButton.isPointInside(x, y)) {
                    backButton.onTouchDown()
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (!backButton.isPointInside(x, y)) {
                    backButton.cancel()
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                if (backButton.isPointInside(x, y)) {
                    backButton.onTouchUp()
                    backButton.performClick(null)
                } else {
                    backButton.cancel()
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                backButton.cancel()
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 标题
        canvas.drawText("致敬名单", width / 2f, titleY, titlePaint)

        // 代码
        canvas.drawText("代码: 不知道", width / 2f, codeY, textPaint)

        // 音乐来源
        canvas.drawText("音乐来源", width / 2f, musicTitleY, musicTitlePaint)
        canvas.drawText("Music: \"Miracle\" by Sappheiros", width / 2f, musicY1, musicTextPaint)
        canvas.drawText("https://www.youtube.com/@sappheiros", width / 2f, musicY2, musicTextPaint)

        backButton.draw(canvas)
    }
}