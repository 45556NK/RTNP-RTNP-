// app/src/main/kotlin/com/rtnp/demo/testing/tools/ShowPlacementGridFunction.kt
package com.rtnp.demo.testing.tools

import com.rtnp.demo.testing.DevTools

/**
 * 显示可放置区域（六边形网格覆盖层）
 */
class ShowPlacementGridFunction : DevToolFunction {
    override val id = "show_placement_grid"
    override val name = "区块显示"
    override val type = FunctionType.TOGGLE

    override fun execute() {
        DevTools.showPlacementGrid = !DevTools.showPlacementGrid
    }

    override fun getDisplayValue(): String {
        return if (DevTools.showPlacementGrid) "ON" else "OFF"
    }
}