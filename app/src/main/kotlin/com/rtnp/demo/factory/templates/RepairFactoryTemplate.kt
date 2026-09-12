package com.rtnp.demo.factory.templates

import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.factory.FactorySlotData
import com.rtnp.demo.factory.FactoryTemplate
import com.rtnp.demo.factory.FactoryTools
import com.rtnp.demo.strategy.CooldownMode

/**
 * 修复工厂
 * 拼装系统使用预制模块快速拼装战列舰使其更快的投入战场
 */
class RepairFactoryTemplate : FactoryTemplate() {

    override val id = "repair_factory"
    override val displayName = "修复工厂"
    override val description = "拼装系统使用预制模块快速拼装战列舰使其更快的投入战场"

    override val cooldown = 0f
    override val cooldownMode = CooldownMode.MANUAL

    override val defaultParams: Map<String, Any> = mapOf(
        "costAmount" to 10,      // 消耗量（金属）
        "repairAmount" to 100    // 修复量（血量）
    )

    // 使用宿主单位的 effectTriggerSpeed 字段作为计时器
    // 或者使用 FactorySlotData 中的自定义字段

    override fun onUpdate(
        host: PlacedObject,
        deltaTime: Float,
        tools: FactoryTools,
        slotData: FactorySlotData
    ) {
        // 读取参数
        val costAmount = slotData.getInt("costAmount") ?: 10
        val repairAmount = slotData.getInt("repairAmount") ?: 100

        // ★ 计时器：使用 slotData 中的 Float 参数存储累计时间
        var timer = slotData.getFloat("_timer") ?: 0f
        timer += deltaTime

        // 未满1秒，不执行修复
        if (timer < 1.0f) {
            slotData.put("_timer", timer)
            return
        }

        // 满1秒，重置计时器（保留多余时间）
        timer -= 1.0f
        slotData.put("_timer", timer)

        // 获取宿主的停泊列表
        val dockedUnits = host.dockedList.mapNotNull { entry ->
            tools.getAllUnits().firstOrNull { it.uniqueId == entry.unitId }
        }

        if (dockedUnits.isEmpty()) return

        // 找到第一个血量不满的单位
        val target = dockedUnits.firstOrNull { unit ->
            val hp = tools.getHealth(unit)
            hp < unit.originalHealth
        } ?: return

        // 检查宿主是否有足够金属
        val metalCount = host.collectedItems?.get("金属") ?: 0
        if (metalCount < costAmount) return

        // 扣除金属
        host.collectedItems?.put("金属", metalCount - costAmount)

        // 增加血量（不超过上限）
        val currentHp = tools.getHealth(target)
        val newHp = (currentHp + repairAmount).coerceAtMost(target.originalHealth.toFloat())
        tools.setHealth(target, newHp)
    }

    override fun onClicked(
        host: PlacedObject,
        tools: FactoryTools,
        slotIndex: Int,
        slotData: FactorySlotData
    ) {
        // 修复工厂为持续运作型，无需点击触发
    }
}