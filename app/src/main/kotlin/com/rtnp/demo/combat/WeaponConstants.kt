package com.rtnp.demo.combat

object WeaponConstants {
    @JvmField var launchAngleOffset: Float = 0f
    @JvmField var targetSearchInterval: Long = 400L
    @JvmField var bulletSpeedMultiplier: Float = 1f
    @JvmField var damageMultiplier: Float = 1f
    @JvmField var rangeMultiplier: Float = 1f
    @JvmField var cooldownMultiplier: Float = 1f  // ★ 新增：冷却时间倍率
}