// app/src/main/kotlin/com/rtnp/demo/data/units/RepairShipTemplate.kt
package com.rtnp.demo.data.units

import android.graphics.Color
import com.rtnp.demo.core.InventoryItem
import com.rtnp.demo.core.UnitTemplateProvider

class RepairShipTemplate : UnitTemplateProvider {
    override fun provide(): InventoryItem {
        return InventoryItem().apply {
            type = "unit"
            name = "尘埃重组无人机"
            shape = "square"
            size = 40
            health = 1500
            faction = 2
            category = "工程"
            color = Color.rgb(255, 182, 193)  // 粉色
            description = "停靠在小行星周围时，会开启金属工厂生成修复模块，给附近的友方单位进行修复 "
            allowWeaponChange = false
            allowStrategyChange = false
            speed = 80
            acceleration = 20f
            maxSpeed = 160f
            deceleration = 40f
            turnRate = 180f
            miningSpeed = 2f  // 测试工程的一半
            storageCapacity = 200
            miningEnabled = true
            lifetime = 180f
            costItem = "金属"
            costAmount = 3
            maxDockingSlots = 0
            dockRadius = 0f
            physicsMass = 5f
            strategySlots = 1
            weaponSlotCount = 0
            strategyIds.add("repair_beam")
            aiIds.add("auto_mine")
            playerControllable = false
            refVolumeShape = "circle"
            refVolumeRadius = 40f  // 根据需要设置
            weaponSlots.clear()
        }
    }
}