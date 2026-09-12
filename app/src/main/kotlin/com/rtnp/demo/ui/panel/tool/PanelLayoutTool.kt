// app/src/main/kotlin/com/rtnp/demo/ui/panel/tool/PanelLayoutTool.kt
package com.rtnp.demo.ui.panel.tool

import android.graphics.Paint
import com.rtnp.demo.ui.panel.data.HealthBarComponent
import com.rtnp.demo.ui.panel.data.InteractiveButtonComponent
import com.rtnp.demo.ui.panel.data.PanelComponent
import com.rtnp.demo.ui.panel.data.SpeedBarComponent
import com.rtnp.demo.ui.panel.data.LifetimeBarComponent
import com.rtnp.demo.ui.panel.data.StorageBarComponent
import com.rtnp.demo.ui.panel.data.BuildProgressComponent
import com.rtnp.demo.ui.panel.data.BuildReserveComponent

object PanelLayoutTool {

    const val COMPONENT_SPACING = 5f
    const val PADDING_TOP = 16f

    fun layout(components: List<PanelComponent>): Float {
        for (comp in components) {
            comp.computedHeight = when (comp) {
                is PanelComponent.Text -> TextLayoutTool.calculateHeight(comp.textSize)
                is PanelComponent.Image -> comp.height
                is InteractiveButtonComponent -> comp.height
                is HealthBarComponent -> HealthBarLayoutTool.calculateHeight()
                is SpeedBarComponent -> SpeedBarLayoutTool.calculateHeight()
                is LifetimeBarComponent -> SpeedBarLayoutTool.calculateHeight()
                is StorageBarComponent -> SpeedBarLayoutTool.calculateHeight()
                is PanelComponent.Spacer -> comp.height
                is BuildProgressComponent -> BuildProgressLayoutTool.calculateHeight()
                is BuildReserveComponent -> BuildReserveLayoutTool.calculateHeight()
                else -> 0f
            }
        }

        var currentY = PADDING_TOP
        for (comp in components) {
            comp.layoutY = currentY
            currentY += comp.computedHeight + COMPONENT_SPACING
        }

        if (components.isNotEmpty()) {
            currentY -= COMPONENT_SPACING
        }

        return currentY
    }
}