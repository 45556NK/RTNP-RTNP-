// app/src/main/kotlin/com/rtnp/demo/data/units/DriftingBeaconTemplate.kt
package com.rtnp.demo.data.units

import android.graphics.Color
import com.rtnp.demo.core.InventoryItem
import com.rtnp.demo.core.UnitTemplateProvider

class DriftingBeaconTemplate : UnitTemplateProvider {
    override fun provide(): InventoryItem {
        return InventoryItem().apply {
            type = "base"
            name = "漂泊信标"
            shape = "circle"
            size = 40
            health = 1200
            faction = 2
            category = "临时建筑"
            color = Color.GRAY
            description = "微型破雾系统，允许远处船只锁定其坐标进行停泊"
            allowWeaponChange = false
            allowStrategyChange = false
            playerControllable = false
            lifetime = 300f
            maxDockingSlots = 3
            dockRadius = 80f
            weaponSlotCount = 0
            strategySlots = 0
        }
    }
}