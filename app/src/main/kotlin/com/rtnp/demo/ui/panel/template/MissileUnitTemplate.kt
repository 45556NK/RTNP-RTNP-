package com.rtnp.demo.ui.panel.template

import android.content.Context
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.ui.panel.RightPanel
import com.rtnp.demo.ui.panel.data.PanelComponent
import com.rtnp.demo.ui.panel.data.UnitNameComponent
import com.rtnp.demo.ui.panel.tool.ExitButtonTool
import com.rtnp.demo.ui.panel.tool.HealthBarTool
import com.rtnp.demo.ui.panel.tool.LifetimeBarTool
import com.rtnp.demo.ui.panel.tool.SpeedBarTool

object MissileUnitTemplate {

    fun build(unit: PlacedObject, vw: Int, context: Context): List<PanelComponent> {
        val components = mutableListOf<PanelComponent>()

        UnitNameComponent.create(unit)?.let { components.add(it) }

        LifetimeBarTool.create(unit)?.let { components.add(it) }

        components.add(HealthBarTool.create(unit))

        components.add(SpeedBarTool.create(unit))

        components.add(PanelComponent.Spacer(60f))

        components.add(ExitButtonTool.create(vw, context) {
            RightPanel.unlock()
        })

        return components
    }
}