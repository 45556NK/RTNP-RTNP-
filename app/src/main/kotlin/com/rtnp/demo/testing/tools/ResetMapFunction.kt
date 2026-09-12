// app/src/main/kotlin/com/rtnp/demo/testing/tools/ResetMapFunction.kt
package com.rtnp.demo.testing.tools

import com.rtnp.demo.UnitSystem
import com.rtnp.demo.logic.HexGridManager
import com.rtnp.demo.testing.PlacementGridRenderer

class ResetMapFunction(private val unitSystem: UnitSystem) : DevToolFunction {
    override val id = "reset_map"
    override val name = "重置地图"
    override val type = FunctionType.BUTTON

    override fun execute() {
        // 清除导弹
        unitSystem.missileSystem?.clearAll()
        // 清除所有单位
        unitSystem.placedObjects.clear()
        unitSystem.healthMap.clear()
        unitSystem.bullets.clear()
        unitSystem.deselectUnit()

        // 使用当前 maxLayer 重新生成六边形网格
        HexGridManager.init(
            HexGridManager.worldCenterX,
            HexGridManager.worldCenterY,
            HexGridManager.maxLayer,
            HexGridManager.hexSide
        )

        // 强制清除放置网格缓存
        PlacementGridRenderer.invalidateCache()
    }
}