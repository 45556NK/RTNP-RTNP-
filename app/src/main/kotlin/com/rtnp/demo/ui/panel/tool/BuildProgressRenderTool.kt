package com.rtnp.demo.ui.panel.tool

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import com.rtnp.demo.ui.panel.data.BuildProgressComponent

object BuildProgressRenderTool {

    fun draw(canvas: Canvas, comp: BuildProgressComponent, panelRect: RectF) {
        val ratio = if (comp.maxBuild > 0f) (comp.currentBuild / comp.maxBuild).coerceIn(0f, 1f) else 0f
        ProgressBarRenderTool.draw(
            canvas,
            panelRect,
            comp.layoutY,
            comp.computedHeight,
            ratio,
            Color.WHITE,
            "${comp.currentBuild.toInt()}/${comp.maxBuild.toInt()}",
            textColor = Color.BLACK
        )
    }
}