// app/src/main/kotlin/com/rtnp/demo/testing/tools/ToggleInvincibleFunction.kt
package com.rtnp.demo.testing.tools

import com.rtnp.demo.UnitSystem

class ToggleInvincibleFunction(private val unitSystem: UnitSystem) : DevToolFunction {
    override val id = "toggle_invincible"
    override val name = "切换无敌(需要提前选中单位)"
    override val type = FunctionType.BUTTON

    override fun execute() {
        val unit = unitSystem.selectedUnit ?: return
        val currentHp = unitSystem.healthMap[unit] ?: unit.health.toFloat()

        if (currentHp >= 9999999f) {
            // 取消无敌：恢复原始血量
            unitSystem.healthMap[unit] = unit.originalHealth.toFloat()
            unit.health = unit.originalHealth
        } else {
            // 开启无敌
            unitSystem.healthMap[unit] = 9999999f
            unit.health = 9999999
            unit.originalHealth = 9999999  // 防止血量条显示异常
        }
    }

    override fun getDisplayValue(): String {
        val unit = unitSystem.selectedUnit ?: return ""
        val hp = unitSystem.healthMap[unit] ?: return ""
        return if (hp >= 999999f) "ON" else "OFF"
    }
}