package com.rtnp.demo.testing.tools

import com.rtnp.demo.UnitSystem
import com.rtnp.demo.core.PlacedObject

/**
 * 删除当前选中的单位。
 */
class DeleteSelectedUnitFunction(private val unitSystem: UnitSystem) : DevToolFunction {
    override val id = "delete_selected_unit"
    override val name = "删除选中单位"
    override val type = FunctionType.BUTTON

    override fun execute() {
        val unit = unitSystem.selectedUnit ?: return
        // 从世界中移除
        unitSystem.placedObjects.remove(unit)
        unitSystem.healthMap.remove(unit)
        // 清除选中状态
        unitSystem.deselectUnit()
    }
}