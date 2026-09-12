package com.rtnp.demo.data.units

import android.graphics.Color
import com.rtnp.demo.core.InventoryItem
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.core.UnitTemplateProvider
import com.rtnp.demo.movement.ModuleManager

class PulseTowerTemplate : UnitTemplateProvider {

    override fun provide(): InventoryItem {
        return InventoryItem().apply {
            type = "base"
            name = "脉冲塔"
            shape = "circle"
            size = 50                    // 直径100，半径50
            health = 1600
            faction = 2                   // 我方
            category = "临时建筑"
            color = Color.rgb(100, 150, 255)
            autoControl = true
            description = "寿命较短的防御性炮塔，用于紧急情况避险"
            allowWeaponChange = false     // 不可更改武器
            allowStrategyChange = false   // 不可更改策略
            maxDockingSlots = 0
            dockRadius = 0f
            useTexture = true
            texturePath = "images/units/pulsess.png"
            textureDisplayHeight = 50f    // 贴图渲染高度
            weaponSlots.add(ModuleManager.createSlotFromTemplate("pulse_default_x1") ?: PlacedObject.WeaponSlot())
            lifetime = 32f
            traitIds.add("pulse_disable")
            physicsMass = 3f
            refVolumeShape = "circle"
            refVolumeRadius = 60f  // 根据需要设置
        }
    }
}