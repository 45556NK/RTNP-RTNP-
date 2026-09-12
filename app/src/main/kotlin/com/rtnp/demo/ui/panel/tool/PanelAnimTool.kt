// app/src/main/kotlin/com/rtnp/demo/ui/panel/tool/PanelAnimTool.kt
package com.rtnp.demo.ui.panel.tool

import android.graphics.Canvas
import com.rtnp.demo.ui.panel.data.PanelComponent

object PanelAnimTool {

    private const val SHOW_ANIM_DURATION = 0.1f

    private var animating: Boolean = false
    private var animStart: Long = 0L

    fun start() {
        animating = true
        animStart = System.currentTimeMillis()
    }

    fun stop() {
        animating = false
    }

    fun isAnimating(): Boolean = animating

    fun draw(canvas: Canvas, components: List<PanelComponent>, vw: Int, vh: Int, drawFn: (Canvas, List<PanelComponent>) -> Unit) {
        if (!animating) {
            drawFn(canvas, components)
            return
        }

        val now = System.currentTimeMillis()
        val elapsed = (now - animStart) / 1000f
        if (elapsed >= SHOW_ANIM_DURATION) {
            animating = false
            drawFn(canvas, components)
            return
        }

        val progress = (elapsed / SHOW_ANIM_DURATION).coerceIn(0f, 1f)
        val scale = 0.01f + (1f - 0.01f) * progress
        val finalRect = PanelBox.rect()
        val startCx = vw.toFloat()
        val startCy = vh / 2f
        val finalCx = finalRect.centerX()
        val finalCy = finalRect.centerY()
        val curCx = startCx + (finalCx - startCx) * progress
        val curCy = startCy + (finalCy - startCy) * progress

        canvas.save()
        canvas.translate(curCx, curCy)
        canvas.scale(scale, scale)
        canvas.translate(-finalCx, -finalCy)
        drawFn(canvas, components)
        canvas.restore()
    }
}