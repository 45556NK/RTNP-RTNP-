// app/src/main/kotlin/com/rtnp/demo/data/units/TeleportAnchorTemplate.kt
package com.rtnp.demo.data.units

import android.graphics.Color
import com.rtnp.demo.core.InventoryItem
import com.rtnp.demo.core.UnitTemplateProvider

class TeleportAnchorTemplate : UnitTemplateProvider {
    override fun provide(): InventoryItem {
        return InventoryItem().apply {
            type = "base"
            name = "传送锚点"
            shape = "circle"
            size = 40
            health = 1600
            faction = 2
            category = "临时建筑"
            color = Color.rgb(128, 0, 128)  // 紫色
            description = "装备了，专门调试过后的区域传送，不过储能有限，只能使用一次 "
            allowWeaponChange = false
            allowStrategyChange = false
            speed = 80
            acceleration = 20f
            maxSpeed = 160f
            deceleration = 40f
            turnRate = 180f
            lifetime = 60f
            costItem = "金属"
            costAmount = 5
            maxDockingSlots = 0
            dockRadius = 0f
            physicsMass = 5f
            weaponSlotCount = 0
            strategySlots = 1
            refVolumeShape = "circle"
            refVolumeRadius = 40f  // 根据需要设置
            strategyIds.add("area_teleport")
        }
    }
}