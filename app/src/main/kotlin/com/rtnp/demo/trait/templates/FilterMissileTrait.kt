package com.rtnp.demo.trait.templates

import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.trait.TraitTemplate
import com.rtnp.demo.trait.TraitTools
import com.rtnp.demo.trait.TargetFilter

class FilterMissileTrait : TraitTemplate() {
    override val id = "filter_missile"
    override val displayName = "导弹过滤"
    override val description = "索敌时忽略导弹目标"

    override fun onUpdate(host: PlacedObject, deltaTime: Float, tools: TraitTools) {
        for (ws in host.weaponSlots) {
            val target = ws.lockedTarget
            if (target != null && TargetFilter.shouldFilter(ws, target)) {
                ws.lockedTarget = null
                ws.nextSearchTime = 0L
            }
        }
    }
}