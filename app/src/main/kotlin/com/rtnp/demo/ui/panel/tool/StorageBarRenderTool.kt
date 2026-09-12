// app/src/main/kotlin/com/rtnp/demo/ui/panel/tool/StorageBarRenderTool.kt
package com.rtnp.demo.ui.panel.tool

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import com.rtnp.demo.ui.panel.data.StorageBarComponent

object StorageBarRenderTool {

    fun draw(canvas: Canvas, comp: StorageBarComponent, panelRect: RectF) {
        val ratio = if (comp.maxStorage > 0) (comp.currentStorage.toFloat() / comp.maxStorage).coerceIn(0f, 1f) else 0f
        ProgressBarRenderTool.draw(
            canvas, panelRect, comp.layoutY, comp.computedHeight,
            ratio, Color.rgb(200, 180, 0),
            "${comp.currentStorage}/${comp.maxStorage}"
        )
    }
}