package com.rtnp.demo.ui.settings

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import com.rtnp.demo.ui.HexButton
import com.rtnp.demo.ui.HexGridUtil

class SettingScreen(
    context: Context,
    private val backListener: OnBackListener
) : View(context) {

    interface OnBackListener {
        fun onBack()
    }

    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.YELLOW
        textSize = 80f
        textAlign = Paint.Align.CENTER
    }

    // 主菜单按钮
    private val mainButtons = mutableListOf<HexButton>()

    // 子页面
    private var showAudioSettings = false
    private var audioSettingsScreen: AudioSettingsScreen? = null
    private var showDebugSettings = false
    private var debugSettingsScreen: DebugSettingsScreen? = null

    init {
        setBackgroundColor(Color.BLACK)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        if (showDebugSettings) {
            if (debugSettingsScreen == null) {
                debugSettingsScreen = DebugSettingsScreen(context, onBack = {
                    showDebugSettings = false; debugSettingsScreen = null; rebuildMainMenu(w, h); invalidate()
                })
            }
            debugSettingsScreen?.layout(0, 0, w, h)
        } else if (showAudioSettings) {
            if (audioSettingsScreen == null) {
                audioSettingsScreen = AudioSettingsScreen(context,
                    onBack = { showAudioSettings = false; audioSettingsScreen = null; rebuildMainMenu(w, h); invalidate() },
                    onStateChanged = { invalidate() })
            }
            audioSettingsScreen?.layout(0, 0, w, h)
        } else {
            rebuildMainMenu(w, h)
        }
    }

    private fun rebuildMainMenu(w: Int, h: Int) {
        mainButtons.clear()
        val labels = listOf("返回", "音频", "调试")
        val coords = HexGridUtil.generateHexCoordsWithBorder(labels.size, 120f, 8f, w / 2f, h / 2f)

        for (i in labels.indices) {
            val btn = HexButton(coords[i][0], coords[i][1], 120f, labels[i]).apply {
                fillColor = Color.rgb(255, 165, 0)
                strokeColor = Color.WHITE
                pressedStrokeColor = Color.YELLOW
                textColor = Color.BLACK
                fontSize = 40f
                buildPaths()
            }
            when (labels[i]) {
                "返回" -> btn.setOnClick { _, _ -> backListener.onBack() }
                "音频" -> btn.setOnClick { _, _ ->
                    showAudioSettings = true
                    audioSettingsScreen = AudioSettingsScreen(context,
                        onBack = { showAudioSettings = false; audioSettingsScreen = null; rebuildMainMenu(w, h); invalidate() },
                        onStateChanged = { invalidate() })
                    audioSettingsScreen?.layout(0, 0, w, h)
                    invalidate()
                }
                "调试" -> btn.setOnClick { _, _ ->
                    showDebugSettings = true
                    debugSettingsScreen = DebugSettingsScreen(context, onBack = {
                        showDebugSettings = false; debugSettingsScreen = null; rebuildMainMenu(w, h); invalidate()
                    })
                    debugSettingsScreen?.layout(0, 0, w, h)
                    invalidate()
                }
            }
            mainButtons.add(btn)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (showDebugSettings) return debugSettingsScreen?.onTouchEvent(event) ?: true
        if (showAudioSettings) return audioSettingsScreen?.onTouchEvent(event) ?: true

        val x = event.x; val y = event.y
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                for (btn in mainButtons) {
                    if (btn.isPointInside(x, y)) { btn.onTouchDown(); return true }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                for (btn in mainButtons) {
                    if (!btn.isPointInside(x, y)) btn.cancel()
                }
            }
            MotionEvent.ACTION_UP -> {
                for (btn in mainButtons) {
                    if (btn.isPointInside(x, y)) {
                        btn.onTouchUp(); btn.performClick(null); invalidate(); return true
                    }
                }
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (showDebugSettings) { debugSettingsScreen?.draw(canvas); return }
        if (showAudioSettings) { audioSettingsScreen?.draw(canvas); return }

        canvas.drawText("设置", width / 2f, 100f, titlePaint)
        for (btn in mainButtons) btn.draw(canvas)
    }
}