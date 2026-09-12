package com.rtnp.demo.trait.templates

import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.trait.TraitTemplate
import com.rtnp.demo.trait.TraitTools

class PulseDisableTrait : TraitTemplate() {

    override val id = "pulse_disable"
    override val displayName = "脉冲瘫痪"
    override val description = "脉冲武器无法充能与冷却"

    override fun onUpdate(host: PlacedObject, deltaTime: Float, tools: TraitTools) {
        for (ws in host.weaponSlots) {
            if (ws.type == "pulse" && ws.pulseWeapon != null) {
                val pw = ws.pulseWeapon!!

                // 禁止能量恢复
                pw.disableEnergyRegen = true

                // 如果能量归零，禁止冷却恢复
                if (pw.getEnergyProgress() <= 0f) {
                    pw.disableCooldownRecovery = true
                }
            }
        }
    }
}