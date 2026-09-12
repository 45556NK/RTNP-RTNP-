// app/src/main/kotlin/com/rtnp/demo/ui/panel/PanelRenderer.kt
package com.rtnp.demo.ui.panel

import android.graphics.Canvas
import com.rtnp.demo.ui.panel.data.PanelComponent
import com.rtnp.demo.ui.panel.tool.ComponentRenderTool
import com.rtnp.demo.ui.panel.tool.PanelRenderTool

object PanelRenderer {

    fun render(canvas: Canvas, components: List<PanelComponent>) {
        // 背景
        PanelRenderTool.drawPanelBackground(canvas)

        // 组件
        ComponentRenderTool.drawComponents(canvas, components)
    }
}