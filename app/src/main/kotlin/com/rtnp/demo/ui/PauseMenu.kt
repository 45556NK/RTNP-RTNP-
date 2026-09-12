package com.rtnp.demo.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent

/**
 * 暂停菜单
 */
class PauseMenu(
    private val context: Context,
    private val listener: OnMenuActionListener
) {

    interface OnMenuActionListener {
        fun onResume()
        fun onQuitToMain()
        fun onSettings()
    }

    private val bgPaint: Paint = Paint().apply {
        color = Color.argb(200, 80, 80, 80)
    }

    private val titlePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(180, 0, 180)
        textAlign = Paint.Align.CENTER
    }

    private val menuManager = MenuManager(context)
    private var screenW = 0
    private var screenH = 0
    private var titleY = 0f

    fun setScreenSize(w: Int, h: Int) {
        screenW = w
        screenH = h
        val titleSize = (w / 4f).coerceAtMost(200f)
        titlePaint.textSize = titleSize
        titleY = h * 0.2f

        menuManager.screenWidth = w
        menuManager.screenHeight = h
        menuManager.loadFromAssets("menu/pause_menu.json")

        // 绑定事件
        for (btn in menuManager.getHexButtons()) {
            when (btn.label) {
                "回到游戏" -> {
                    btn.setOnClick { _, _ ->
                        listener.onResume()
                    }
                }
                "返回主菜单" -> {
                    btn.setOnClick { _, _ ->
                        listener.onQuitToMain()
                    }
                }
                "设置" -> {
                    btn.setOnClick { _, _ ->
                        listener.onSettings()
                    }
                }
            }
        }
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        return menuManager.onTouchEvent(event)
    }

    fun draw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, screenW.toFloat(), screenH.toFloat(), bgPaint)
        canvas.drawText("游戏菜单", screenW / 2f, titleY, titlePaint)
        menuManager.draw(canvas)
    }
}