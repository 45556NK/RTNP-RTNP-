package com.rtnp.demo.ai.automation

import com.rtnp.demo.ai.AiTemplate
import com.rtnp.demo.ai.AiTools
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.logic.HexGridManager
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class ZoneDefense : AiTemplate {

    override val id = "zone_defense"
    override val displayName = "区域防守"
    override val description = "在本区块内搜索并攻击敌方单位，不跨区块行动"
    override val enabledByDefault = false

    private var timer = 0f
    private val searchInterval = 0.5f
    private var currentTarget: PlacedObject? = null
    private var moveTargetX = 0f
    private var moveTargetY = 0f

    override fun onUpdate(host: PlacedObject, deltaTime: Float, tools: AiTools) {
        timer += deltaTime

        if (tools.isDocked(host)) return

        val tileId = HexGridManager.getTileIdForUnit(host) ?: return

        // 检查是否在战斗中
        var inCombat = false
        for (ws in host.weaponSlots) {
            val target = tools.getAttackTarget(host, host.weaponSlots.indexOf(ws))
            if (target != null && tools.getHealth(target) > 0) {
                inCombat = true
                break
            }
        }

        // ★ 战斗中：不主动移动，也不强制停止
        if (inCombat) {
            currentTarget = null
            timer = searchInterval
            return
        }

        // 非战斗状态：定期搜索区块内敌方
        if (timer >= searchInterval) {
            timer = 0f

            val unitsInTile = HexGridManager.getUnitsInTile(tileId)

            var bestTarget: PlacedObject? = null
            var bestDist = Float.MAX_VALUE
            for (u in unitsInTile) {
                if (u.faction == host.faction || u.faction == 0) continue
                val hp = tools.getHealth(u)
                if (hp <= 0) continue
                val dist = tools.distance(host, u)
                if (dist < bestDist) {
                    bestDist = dist
                    bestTarget = u
                }
            }

            if (bestTarget != null) {
                currentTarget = bestTarget
                val refRadius = bestTarget.referenceVolume?.radius ?: 100f
                val randomAngle = Random.nextFloat() * 2f * Math.PI.toFloat()
                val approachDist = refRadius + 50f
                moveTargetX = bestTarget.worldX + cos(randomAngle) * approachDist
                moveTargetY = bestTarget.worldY + sin(randomAngle) * approachDist
                val tile = HexGridManager.getTileById(tileId)
                if (tile != null) {
                    val dx = moveTargetX - tile.worldX
                    val dy = moveTargetY - tile.worldY
                    val maxDist = HexGridManager.hexSide * 0.8f
                    val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                    if (dist > maxDist) {
                        moveTargetX = tile.worldX + dx / dist * maxDist
                        moveTargetY = tile.worldY + dy / dist * maxDist
                    }
                }
                tools.moveTo(host, moveTargetX, moveTargetY)
            } else {
                currentTarget = null
            }
        }
    }
}