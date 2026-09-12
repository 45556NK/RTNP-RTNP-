// app/src/main/kotlin/com/rtnp/demo/core/GameText.kt
package com.rtnp.demo.core

/**
 * 通用游戏文本翻译类
 */
object GameText {

    // ========== 阵营 ==========
    fun faction(faction: Int): String = when (faction) {
        0 -> "中立"
        1 -> "敌对"
        2 -> "我方"
        else -> "未知"
    }

    // ========== 武器类型 ==========
    fun weaponType(type: String): String = when (type) {
        "railgun" -> "轨道炮"
        "laser" -> "激光"
        "missile" -> "导弹"
        "pulse" -> "脉冲"
        "?" -> "滚木"
        else -> type
    }

    // ========== 状态 ==========
    fun enabled(isEnabled: Boolean): String = if (isEnabled) "开" else "关"

    // ========== 单位属性名 ==========
    fun attributeName(key: String): String = when (key) {
        "health" -> "血量"
        "originalHealth" -> "初始血量"
        "speed" -> "速度"
        "acceleration" -> "加速度"
        "maxSpeed" -> "最大速度"
        "deceleration" -> "减速度"
        "range" -> "射程"
        "damage" -> "伤害"
        "weaponType" -> "武器类型"
        "weaponActive" -> "武器开关"
        "faction" -> "阵营"
        "category" -> "种类"
        "missileHealth" -> "导弹血量"
        "missileDamage" -> "导弹伤害"
        "missileLifetime" -> "导弹寿命"
        "explosionRange" -> "爆炸范围"
        "missileSpeed" -> "导弹速度"
        "missileAcceleration" -> "导弹加速度"
        "effectSize" -> "特效大小"
        "missileMinRange" -> "最小射程"
        "cooldown" -> "冷却时间"
        "chargeTime" -> "充能时间"
        "bulletSpeed" -> "子弹速度"
        "minDamage" -> "最小伤害"
        "maxDamage" -> "最大伤害"
        "active" -> "启用"
        "storedItem" -> "储存物品"
        "storage" -> "仓储"
        "miningSpeed" -> "采矿速度"
        "miningEnabled" -> "矿机状态"
        "collectedItems" -> "已采集"
        "maxDockingSlots" -> "最大停泊数"
        "dockRadius" -> "停泊半径"
        "currentSpeed" -> "当前速度"
        "heading" -> "朝向角度"
        "worldX" -> "世界X坐标"
        "worldY" -> "世界Y坐标"
        "targetX" -> "目标X坐标"
        "targetY" -> "目标Y坐标"
        "isMoving" -> "是否移动中"
        "isStopping" -> "是否停止中"
        "isDocked" -> "是否停泊"
        "isUndocking" -> "是否出港中"
        "undockProgress" -> "出港进度"
        "lifetime" -> "存活时间"
        "dockedAt" -> "停泊目标"
        "dockSlotIndex" -> "停泊槽位"
        "isMining" -> "是否采矿中"
        "laserCharge" -> "激光充能"
        "missileCooldown" -> "导弹冷却"
        else -> key
    }
}