// app/src/main/kotlin/com/rtnp/demo/trait/templates/SuppressionExplosionTrait.kt
package com.rtnp.demo.trait.templates

import com.rtnp.demo.combat.ExplosionDamageTool
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.gpu.effect.GpuEffectSystem
import com.rtnp.demo.gpu.render.tools.SuppressionRangeDisplaySystem
import com.rtnp.demo.trait.TraitTemplate
import com.rtnp.demo.trait.TraitTools

class SuppressionExplosionTrait : TraitTemplate() {

    override val id = "suppression_explosion"
    override val displayName = "压制爆炸"
    override val description = "死亡时造成范围压制伤害"
    override val isGlobal = true

    companion object {
        const val SUPPRESSION_RANGE = 200
        const val SUPPRESSION_DAMAGE = 1600
    }

    override fun onUpdate(host: PlacedObject, deltaTime: Float, tools: TraitTools) {
        if (!host.hasSuppression) {
            host.hasSuppression = true
            host.suppressionRange = SUPPRESSION_RANGE
            host.suppressionDamage = SUPPRESSION_DAMAGE
            ExplosionDamageTool.registerSuppression(host)

            host.suppressionRangeDisplayId = SuppressionRangeDisplaySystem.addSuppressionRange(
                host.worldX, host.worldY, SUPPRESSION_RANGE.toFloat()
            )
        }

        if (host.suppressionRangeDisplayId >= 0L) {
            SuppressionRangeDisplaySystem.updateSuppressionRange(
                host.suppressionRangeDisplayId, host.worldX, host.worldY
            )
        }
    }

    override fun onAfterDeath(host: PlacedObject, deltaTime: Float, tools: TraitTools): Boolean {
        if (host.suppressionRangeDisplayId >= 0L) {
            SuppressionRangeDisplaySystem.removeSuppressionRange(host.suppressionRangeDisplayId)
        }

        // 从单位数据中获取压制爆炸特效ID
        val effectId = host.suppressionEffectId
        if (!effectId.isNullOrEmpty()) {
            GpuEffectSystem.spawn(effectId, host.worldX, host.worldY, host)
        }

        return true
    }

    override fun needsAfterDeath(): Boolean = true
}