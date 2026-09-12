package com.rtnp.demo.ui.panel.template

import android.content.Context
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.ui.panel.data.PanelComponent
import com.rtnp.demo.ui.panel.data.UnitNameComponent
import com.rtnp.demo.ui.panel.tool.BuildProgressTool
import com.rtnp.demo.ui.panel.tool.ExitButtonTool
import com.rtnp.demo.ui.panel.RightPanel
import com.rtnp.demo.ui.panel.tool.BuildReserveTool

object BuildingUnitTemplate {

    fun build(unit: PlacedObject, vw: Int, context: Context): List<PanelComponent> {
        val components = mutableListOf<PanelComponent>()

        UnitNameComponent.create(unit)?.let { components.add(it) }

        BuildProgressTool.create(unit)?.let { components.add(it) }
        
        BuildReserveTool.create(unit)?.let { components.add(it) }
        
        components.add(PanelComponent.Spacer(60f))

        components.add(ExitButtonTool.create(vw, context) {
            RightPanel.unlock()
        })

        return components
    }
}