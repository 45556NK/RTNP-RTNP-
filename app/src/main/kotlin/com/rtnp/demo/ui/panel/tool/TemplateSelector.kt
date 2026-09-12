// app/src/main/kotlin/com/rtnp/demo/ui/panel/tool/TemplateSelector.kt
package com.rtnp.demo.ui.panel.tool

import android.content.Context
import com.rtnp.demo.ui.panel.PanelState
import com.rtnp.demo.ui.panel.PanelTemplate
import com.rtnp.demo.ui.panel.data.PanelComponent
import com.rtnp.demo.ui.panel.template.NormalUnitTemplate
import com.rtnp.demo.ui.panel.template.NonControllableUnitTemplate
import com.rtnp.demo.ui.panel.template.EnvironmentUnitTemplate
import com.rtnp.demo.ui.panel.template.BaseUnitTemplate
import com.rtnp.demo.ui.panel.template.BuildingUnitTemplate
import com.rtnp.demo.ui.panel.template.MissileUnitTemplate

object TemplateSelector {

    fun selectByTemplate(template: PanelTemplate, state: PanelState, vw: Int, context: Context): List<PanelComponent> {
        return when (template) {
            PanelTemplate.NORMAL_UNIT -> {
                val unit = state.lockedUnits.firstOrNull()
                if (unit != null) NormalUnitTemplate.build(unit, vw, context) else emptyList()
            }
            PanelTemplate.MISSILE -> {
                val unit = state.lockedUnits.firstOrNull()
                if (unit != null) MissileUnitTemplate.build(unit, vw, context) else emptyList()
            }
            PanelTemplate.BASE -> {
                val unit = state.lockedUnits.firstOrNull()
                if (unit != null) BaseUnitTemplate.build(unit, vw, context) else emptyList()
            }
            // TemplateSelector.kt 中修改
            PanelTemplate.ENVIRONMENT -> {
                val unit = state.lockedUnits.firstOrNull()
                if (unit != null) EnvironmentUnitTemplate.build(unit, vw, context) else emptyList()
            }
            PanelTemplate.MULTI_SELECT -> emptyList()
            PanelTemplate.DETAILS -> emptyList()
            // TemplateSelector.kt 的 selectByTemplate 中增加
            PanelTemplate.NON_CONTROLLABLE_UNIT -> {
                val unit = state.lockedUnits.firstOrNull()
                if (unit != null) NonControllableUnitTemplate.build(unit, vw, context) else emptyList()
            }
            PanelTemplate.BUILDING_UNIT -> {
                val unit = state.lockedUnits.firstOrNull()
                if (unit != null) BuildingUnitTemplate.build(unit, vw, context) else emptyList()
            }
        }
    }
}