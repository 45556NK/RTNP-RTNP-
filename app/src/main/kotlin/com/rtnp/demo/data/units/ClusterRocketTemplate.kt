// app/src/main/kotlin/com/rtnp/demo/data/units/ClusterRocketTemplate.kt
package com.rtnp.demo.data.units

import android.graphics.Color
import com.rtnp.demo.core.InventoryItem
import com.rtnp.demo.core.UnitTemplateProvider

class ClusterRocketTemplate : UnitTemplateProvider {
    override fun provide(): InventoryItem {
        return InventoryItem().apply {
            type = "unit"
            name = "集群火箭"
            shape = "rectangle"
            width = 12
            height = 4
            health = 2000
            faction = 2
            category = "火箭"
            color = Color.rgb(255, 165, 0)
            description = "高速集群火箭，不可操控，到达目标区块后发射集群导弹"
            allowWeaponChange = false
            allowStrategyChange = false
            playerControllable = false
            speed = 80
            acceleration = 20f
            maxSpeed = 120f
            deceleration = 40f
            turnRate = 180f
            lifetime = 120f
            costItem = "金属"
            costAmount = 5
            maxDockingSlots = 0
            dockRadius = 0f
            physicsMass = 2f
            weaponSlotCount = 0
            strategySlots = 1
            traitIds.add("suppression_explosion")
            strategyIds.add("cluster_bomb")
            suppressionEffectId = "suppression_explosion"
            refVolumeShape = "circle"
            refVolumeRadius = 40f  // 根据需要设置
        }
    }
}