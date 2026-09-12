package com.rtnp.demo.testing.tools

import com.rtnp.demo.UnitSystem
import com.rtnp.demo.combat.WeaponConstants

class SetSpeedMultiplierFunction(private val unitSystem: UnitSystem) : DevToolFunction {
    override val id = "set_speed_multiplier"
    override val name = "游戏倍速"
    override val type = FunctionType.INPUT

    private var currentValue = 1.0f
    private val baseDt = 1f / 60f

    override fun getDisplayValue(): String = "${currentValue}x"

    override fun execute() {}

    fun applyValue(value: Float) {
        currentValue = value.coerceIn(0.1f, 10.0f)
        
        // 统一修改逻辑速度（时间步长）
        UnitSystem.setFixedDt(baseDt * currentValue)
        
        // 同步修改渲染相关速度（子弹、伤害）
        WeaponConstants.bulletSpeedMultiplier = currentValue
        //WeaponConstants.damageMultiplier = currentValue
        WeaponConstants.cooldownMultiplier = currentValue
    }
}