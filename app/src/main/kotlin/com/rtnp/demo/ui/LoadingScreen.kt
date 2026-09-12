package com.rtnp.demo.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.view.View
import com.rtnp.demo.MainActivity

/**
 * 加载界面
 */
class LoadingScreen(context: Context) : View(context) {

    private val borderPaint: Paint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 20f
        isAntiAlias = true
    }

    private val fillPaint: Paint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val textPaint: Paint = Paint().apply {
        color = Color.WHITE
        textSize = 30f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    private var progress = 0f
    private var currentTask = "初始化..."
    private val handler = Handler(Looper.getMainLooper())

    init {
        setBackgroundColor(Color.BLACK)
        startLoadingTasks()
    }

    private fun startLoadingTasks() {
        val tasks = arrayOf("加载系统...", "加载地图...", "加载单位资源...", "加载武器数据...", "初始化港口...")
        val step = 1f / tasks.size

        Thread {
            for (i in tasks.indices) {
                try {
                    Thread.sleep(500)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                val index = i
                handler.post {
                    progress = (index + 1) * step
                    currentTask = tasks[index]
                    invalidate()
                }
            }
            // 加载完成，延迟一下通知 Activity
            handler.postDelayed({
                (context as? MainActivity)?.onLoadingComplete()
            }, 300)
        }.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = width
        val height = height

        // 外边框
        val outerWidth = width * 0.8f
        val outerHeight = 100f
        val outerLeft = (width - outerWidth) / 2
        val outerTop = (height - outerHeight) / 2
        canvas.drawRect(outerLeft, outerTop, outerLeft + outerWidth, outerTop + outerHeight, borderPaint)

        // 内部填充矩形
        val innerLeft = outerLeft + 15
        val innerRight = outerLeft + outerWidth - 15
        val innerTop = outerTop + 15
        val innerBottom = outerTop + outerHeight - 15
        val innerWidth = innerRight - innerLeft
        val fillRight = innerLeft + innerWidth * progress
        canvas.drawRect(innerLeft, innerTop, fillRight, innerBottom, fillPaint)

        // 进度文字
        canvas.drawText("加载进度: ${(progress * 100).toInt()}%", width / 2f, outerTop + outerHeight + 50, textPaint)
        canvas.drawText(currentTask, width / 2f, outerTop + outerHeight + 90, textPaint)
    }
}