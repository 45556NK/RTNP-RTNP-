package com.rtnp.demo.core

class MissileType {
    @JvmField var name: String = ""
    @JvmField var health: Int = 0
    @JvmField var damage: Int = 0
    @JvmField var startSpeed: Int = 0
    @JvmField var maxSpeed: Int = 0
    @JvmField var acceleration: Float = 0f
    @JvmField var lifetime: Int = 0
    @JvmField var explosionRange: Int = 0
    @JvmField var effectSize: Int = 0
    @JvmField var cooldownMax: Int = 0
    @JvmField var minRange: Int = 0
    @JvmField var size: Int = 8
    @JvmField var shape: String = "square"
    @JvmField var color: Int = 0xFF00FFFF.toInt()
    
    // ★ 参考体积（特效定位用）
    @JvmField var refVolumeShape: String = ""
    @JvmField var refVolumeRadius: Float = 0f
    
    // 在 MissileType 类中添加
    @JvmField var hasSuppression: Boolean = false
    @JvmField var suppressionRange: Int = 0
    @JvmField var suppressionDamage: Int = 0
    @JvmField var suppressionDuration: Float = 0f
    @JvmField var suppressionEffectId: String? = null
    
    @JvmField var explosionEffectId: String = "explosion"
}