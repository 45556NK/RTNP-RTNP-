package com.rtnp.demo.module

import com.rtnp.demo.core.PlacedObject

/**
 * 单位配件管理器：统一管理武器槽和策略槽（策略槽暂未实现）。
 * 兼容旧的武器槽直接访问，同时提供统一的配件视图。
 */
object UnitModuleManager {

    /** 配件类型 */
    enum class ModuleType { WEAPON, STRATEGY }

    /** 配件描述 */
    data class ModuleInfo(
        val type: ModuleType,
        val index: Int,
        val label: String,
        val active: Boolean,
        val subText: String? = null,
        val data: Any? = null   // WeaponSlot 或 Strategy 对象
    )

    /** 获取单位的所有配件列表（武器槽 + 策略槽占位） */
    fun getModules(unit: PlacedObject): List<ModuleInfo> {
        val list = mutableListOf<ModuleInfo>()

        // 武器槽
        for (i in unit.weaponSlots.indices) {
            val ws = unit.weaponSlots[i]
            list.add(ModuleInfo(
                type = ModuleType.WEAPON,
                index = i,
                label = getWeaponLabel(ws),
                active = ws.active,
                subText = getWeaponSubText(ws),
                data = ws
            ))
        }

        // 策略槽（暂未实现）
        // for (i in 0 until unit.strategySlots) { ... }

        return list
    }

    private fun getWeaponLabel(ws: PlacedObject.WeaponSlot): String {
        return when (ws.type) {
            "laser" -> "激光"
            "missile" -> "导弹"
            "railgun" -> "轨道炮"
            else -> "?"
        }
    }

    private fun getWeaponSubText(ws: PlacedObject.WeaponSlot): String? {
        val id = ws.templateId ?: return null
        val t = com.rtnp.demo.movement.weapon.WeaponManager.getTemplate(id)
        return t?.subType?.takeIf { it.isNotEmpty() }
    }
}