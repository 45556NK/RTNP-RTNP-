package com.rtnp.demo.trait

import com.rtnp.demo.core.PlacedObject

object TargetFilter {

    private const val FILTER_PREFIX = "filter_"

    /**
     * 判断目标是否应该被该武器槽过滤
     * 先检查是否命中过滤规则，再检查是否在白名单中豁免
     */
    @JvmStatic
    fun shouldFilter(ws: PlacedObject.WeaponSlot, target: PlacedObject): Boolean {
        for (traitId in ws.traitIds) {
            if (!traitId.startsWith(FILTER_PREFIX)) continue
            
            val filterType = traitId.removePrefix(FILTER_PREFIX)
            val isFiltered = (filterType == target.category || filterType == target.type)
            
            if (isFiltered) {
                // 检查该特性是否有白名单豁免
                val whitelist = ws.filterWhitelist[traitId]
                if (whitelist != null && whitelist.contains(target.missileTypeName)) {
                    return false  // 在白名单中，不过滤
                }
                return true  // 不在白名单中，过滤
            }
        }
        return false
    }
}