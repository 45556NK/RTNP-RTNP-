package com.rtnp.demo.combat

import com.rtnp.demo.UnitSystem
import com.rtnp.demo.combat.weapons.*
import com.rtnp.demo.core.PlacedObject

object WeaponLoop {

    // 公开的系统实例，供 WeaponDisplayPass 调用渲染
    val pulseSystem = PulseWeaponSystem()
    val missileSystem = MissileWeaponSystem()
    val laserSystem = LaserWeaponSystem()
    val railgunSystem = RailgunWeaponSystem()

    fun update(host: UnitSystem, fixedDt: Float) {
        val nowMillis = System.currentTimeMillis()
        val interval = WeaponConstants.targetSearchInterval
        val safeCopy = ArrayList(host.placedObjects)

        for (attacker in safeCopy) {
            if (!host.placedObjects.contains(attacker)) continue
            if (attacker.category == "missile") continue
            if (attacker.faction != 1 && attacker.faction != 2) continue

            val weaponSlotsCopy: List<PlacedObject.WeaponSlot>
            try {
                weaponSlotsCopy = ArrayList(attacker.weaponSlots)
            } catch (e: Exception) { continue }
            if (weaponSlotsCopy.isEmpty()) continue

            for (ws in weaponSlotsCopy) {
                if (ws == null || !ws.active) continue
                val range = ws.range * WeaponConstants.rangeMultiplier
                if (range <= 0) continue

                // 索敌
                var target: PlacedObject? = null
                var needSearch = false
                val currentLocked = ws.lockedTarget
                if (currentLocked != null) {
                    val targetHp = host.healthMap[currentLocked]
                    if (targetHp == null || targetHp <= 0
                        || currentLocked.faction == attacker.faction
                        || currentLocked.faction == 0) {
                        ws.lockedTarget = null; needSearch = true
                    } else {
                        val dx = currentLocked.worldX - attacker.worldX
                        val dy = currentLocked.worldY - attacker.worldY
                        if (dx * dx + dy * dy > range * range) {
                            ws.lockedTarget = null; needSearch = true
                        } else target = currentLocked
                    }
                } else needSearch = true

                if (needSearch && nowMillis >= ws.nextSearchTime) {
                    target = host.combatManager.findTargetForWeaponSlot(attacker, ws, range)
                    ws.nextSearchTime = nowMillis + interval
                }
                if (target == null && ws.type == "laser") {
                    ws.laserTarget = null; ws.laserCharge = 0f
                }

                // 调度到对应武器系统
                val system = when (ws.type) {
                    "pulse" -> pulseSystem
                    "missile" -> missileSystem
                    "laser" -> laserSystem
                    "railgun" -> railgunSystem
                    else -> continue
                }
                system.update(attacker, ws, host, fixedDt, range, target)
            }
        }
    }
}