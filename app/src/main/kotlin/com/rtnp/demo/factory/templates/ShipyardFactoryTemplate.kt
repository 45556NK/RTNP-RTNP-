package com.rtnp.demo.factory.templates

import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.core.TemplateRegistry
import com.rtnp.demo.factory.FactorySlotData
import com.rtnp.demo.factory.FactoryTemplate
import com.rtnp.demo.factory.FactoryTools
import com.rtnp.demo.logic.DockingSystem
import com.rtnp.demo.strategy.CooldownMode
import com.rtnp.demo.ui.WarningMessage
import com.rtnp.demo.logger.Logger

/**
 * 船坞
 * 消耗一定的资源来生产舰船
 */
class ShipyardFactoryTemplate : FactoryTemplate() {

    override val id = "shipyard"
    override val displayName = "船坞"
    override val description = "消耗一定的资源来生产舰船"

    override val cooldown = 0f
    override val cooldownMode = CooldownMode.MANUAL

    override val defaultParams: Map<String, Any> = mapOf(
        "productionRate" to 100f  // 每秒生产多少建造量
    )

    override fun onUpdate(
        host: PlacedObject,
        deltaTime: Float,
        tools: FactoryTools,
        slotData: FactorySlotData
    ) {
        // 读取参数
        val productionRate = slotData.getFloat("productionRate") ?: 100f

        // 获取要生产的单位模板
        val targetTemplate = TemplateRegistry.templates.firstOrNull { it.name == "测试单位" }
        val buildAmount = targetTemplate?.buildAmount?.takeIf { it > 0f } ?: 1000f

        // 读取状态
        var isProducing = slotData.getBoolean("_isProducing") ?: false
        var progress = slotData.getFloat("_progress") ?: 0f
        var warnedFull = slotData.getBoolean("_warnedFull") ?: false

        // 获取宿主停泊点占用情况
        val totalSlots = DockingSystem.getSlotCount(host)
        val occupiedSlots = host.dockedList.size + host.dockingList.size
        val hasFreeSlot = totalSlots > 0 && occupiedSlots < totalSlots
        
        // 在 onUpdate 开头
        //Logger.d("Shipyard", "deltaTime=$deltaTime, productionRate=$productionRate, progress=$progress, isProducing=$isProducing")

        if (!isProducing) {
            // 未在生产，检查是否可以开始
            if (!hasFreeSlot) {
                if (!warnedFull) {
                    WarningMessage.show("停泊点已被占满")
                    warnedFull = true
                    slotData.put("_warnedFull", true)
                }
                return
            }
            // 有空闲，开始生产
            isProducing = true
            progress = 0f
            warnedFull = false
            slotData.put("_isProducing", true)
            slotData.put("_progress", 0f)
            slotData.put("_warnedFull", false)
        }

        // 正在生产
        if (!hasFreeSlot) {
            // 停泊点满，暂停推进
            if (!warnedFull) {
                WarningMessage.show("停泊点已被占满")
                warnedFull = true
                slotData.put("_warnedFull", true)
            }
            // 保留进度但不超过 buildAmount - 微小量
            if (progress >= buildAmount - 0.01f) {
                progress = buildAmount - 0.01f
                slotData.put("_progress", progress)
            }
            return
        } else {
            // 有空闲，如果之前警告过则重置警告
            if (warnedFull) {
                warnedFull = false
                slotData.put("_warnedFull", false)
            }
        }

        // 推进生产
        progress += deltaTime * productionRate
        if (progress >= buildAmount) {
            // 生产完成
            isProducing = false
            progress = 0f
            slotData.put("_isProducing", false)
            slotData.put("_progress", 0f)

            // 生成单位
            val newUnit = tools.spawnUnit("测试单位", host.worldX, host.worldY)
            //Logger.d("Shipyard", "生产完成！耗时由 progress 累计 = $progress")
            if (newUnit != null) {
                // 尝试停泊到宿主
                DockingSystem.requestDock(newUnit, host)
            }
        } else {
            slotData.put("_progress", progress)
        }
    }

    override fun onClicked(
        host: PlacedObject,
        tools: FactoryTools,
        slotIndex: Int,
        slotData: FactorySlotData
    ) {
        // 船坞为持续运作型，无需点击触发
    }
}