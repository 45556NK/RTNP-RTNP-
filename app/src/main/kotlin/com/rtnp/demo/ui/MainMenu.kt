package com.rtnp.demo.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import com.rtnp.demo.MainActivity

class MainMenu(context: Context) : View(context) {

    private val titlePaint: Paint = Paint().apply {
        color = Color.YELLOW
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        textSize = 200f
    }

    private val rtnpPaint: Paint = Paint().apply {
        color = Color.rgb(40, 40, 255)
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        textSize = 130f
    }

    private val menuManager = MenuManager(context)

    init {
        setBackgroundColor(Color.BLACK)  // ← 添加这一行
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        menuManager.screenWidth = w
        menuManager.screenHeight = h
        menuManager.loadFromAssets("menu/main_menu.json")

        for (btn in menuManager.getHexButtons()) {
            when (btn.label) {
                "开始" -> {
                    btn.setOnClick { _, _ ->
                        (context as? MainActivity)?.startGame()
                    }
                }
                "设置" -> {
                    btn.setOnClick { _, _ ->
                        (context as? MainActivity)?.showSettings(this@MainMenu)
                    }
                }
                "致敬名单" -> {
                    btn.setOnClick { _, _ ->
                        (context as? MainActivity)?.showCredits(MainMenu(context))
                    }
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (menuManager.onTouchEvent(event)) {
            invalidate()
            return true
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawText("环星计划", width / 2f, 200f, titlePaint)
        canvas.drawText("RTNP", width / 2f, 330f, rtnpPaint)
        menuManager.draw(canvas)
    }
}