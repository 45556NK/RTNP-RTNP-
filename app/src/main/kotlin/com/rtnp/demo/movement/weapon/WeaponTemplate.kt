package com.rtnp.demo.movement.weapon

class WeaponTemplate {
    @JvmField var id: String = ""
    @JvmField var category: String = ""
    @JvmField var subType: String = ""
    @JvmField var displayName: String = ""
    @JvmField var range: Float = 0f
    @JvmField var cooldown: Float = 0f
    @JvmField var active: Boolean = true
    @JvmField var damage: Float = 0f
    @JvmField var bulletSpeed: Float = 0f
    @JvmField var minDamage: Float = 0f
    @JvmField var maxDamage: Float = 0f
    @JvmField var chargeTime: Float = 0f
    @JvmField var missileCooldown: Float = 0f
    @JvmField var minRange: Float = 0f
    @JvmField var fireEffectId: String = ""
    @JvmField var maxTargets: Int = 1
    @JvmField var energyDuration: Float = 5f
    @JvmField var energyRegenDelay: Float = 6f
    @JvmField var energyRegenSpeed: Float = 0.05f
    @JvmField var traitIds: MutableList<String> = mutableListOf()
    
    /**
     * 过滤白名单：特性ID -> 豁免的目标类型列表
     * 例如："filter_missile" -> ["标准导弹"]
     * 表示该武器过滤导弹，但"标准导弹"类型的导弹不会被过滤
     */
     @JvmField var missileTypeName: String = ""
     @JvmField var filterWhitelist: MutableMap<String, MutableList<String>> = mutableMapOf()
     
     @JvmField var iconPath: String? = null
}