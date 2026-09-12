package com.rtnp.demo.testing.tools

import com.rtnp.demo.testing.DevTools

class ShowUnitBoundsFunction : DevToolFunction {
    override val id = "show_unit_bounds"
    override val name = "显示单位碰撞箱"
    override val type = FunctionType.TOGGLE

    override fun execute() {
        DevTools.showUnitBounds = !DevTools.showUnitBounds
    }

    override fun getDisplayValue(): String {
        return if (DevTools.showUnitBounds) "ON" else "OFF"
    }
}