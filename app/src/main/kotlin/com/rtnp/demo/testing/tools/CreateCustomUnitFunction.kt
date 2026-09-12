// app/src/main/kotlin/com/rtnp/demo/testing/tools/CreateCustomUnitFunction.kt
package com.rtnp.demo.testing.tools

import android.graphics.Color
import android.text.InputType
import com.rtnp.demo.UnitSystem
import com.rtnp.demo.core.InventoryItem
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.core.TemplateRegistry
import com.rtnp.demo.ui.InventoryPanel

class CreateCustomUnitFunction(private val unitSystem: UnitSystem) : MultiInputFunction {
    override val id = "create_custom_unit"
    override val name = "自定义单位"
    override val type = FunctionType.INPUT

    // 第一页：基础属性
    private val page1Keys = listOf(
        "name", "type", "shape", "width", "height", "size",
        "health", "faction", "category", "color"
    )
    private val page1Labels = mapOf(
        "name" to "名称",
        "type" to "类型(unit/base/environment)",
        "shape" to "形状(rectangle/circle/square)",
        "width" to "宽度",
        "height" to "高度",
        "size" to "尺寸",
        "health" to "血量",
        "faction" to "阵营(0中立/1敌对/2我方)",
        "category" to "分类",
        "color" to "颜色(#RRGGBB)"
    )
    private val page1Defaults = mapOf(
        "name" to "自定义单位",
        "type" to "unit",
        "shape" to "rectangle",
        "width" to "90",
        "height" to "30",
        "size" to "50",
        "health" to "5000",
        "faction" to "2",
        "category" to "",
        "color" to "#FFFFFF"
    )

    // 第二页：移动属性
    private val page2Keys = listOf(
        "speed", "maxSpeed", "acceleration", "deceleration", "turnRate"
    )
    private val page2Labels = mapOf(
        "speed" to "基础速度",
        "maxSpeed" to "最大速度",
        "acceleration" to "加速度",
        "deceleration" to "减速度",
        "turnRate" to "转向速率"
    )
    private val page2Defaults = mapOf(
        "speed" to "80",
        "maxSpeed" to "160",
        "acceleration" to "20",
        "deceleration" to "40",
        "turnRate" to "180"
    )

    // 第三页：武器系统选择 + 旧版单武器属性
    private val page3Keys = listOf(
        "weaponSystemType", "weaponCount",
        "weaponType", "range", "damage", "bulletSpeed",
        "chargeTime", "minDamage", "maxDamage"
    )
    private val page3Labels = mapOf(
        "weaponSystemType" to "武器系统(旧版/新版)",
        "weaponCount" to "武器数量(仅新版,>=1)",
        "weaponType" to "武器类型(?/railgun/laser/missile)",
        "range" to "射程",
        "damage" to "伤害",
        "bulletSpeed" to "子弹速度",
        "chargeTime" to "充能时间",
        "minDamage" to "最小伤害",
        "maxDamage" to "最大伤害"
    )
    private val page3Defaults = mapOf(
        "weaponSystemType" to "旧版",
        "weaponCount" to "1",
        "weaponType" to "?",
        "range" to "0",
        "damage" to "0",
        "bulletSpeed" to "0",
        "chargeTime" to "0",
        "minDamage" to "0",
        "maxDamage" to "0"
    )

    // 第四页：经济/采集属性
    private val page4Keys = listOf(
        "costItem", "costAmount", "miningSpeed", "storageCapacity",
        "storedItemName", "storedItemCount", "maxMiners"
    )
    private val page4Labels = mapOf(
        "costItem" to "消耗资源",
        "costAmount" to "消耗数量",
        "miningSpeed" to "采矿速度",
        "storageCapacity" to "仓储容量",
        "storedItemName" to "储存物品",
        "storedItemCount" to "储存数量",
        "maxMiners" to "最大矿工数"
    )
    private val page4Defaults = mapOf(
        "costItem" to "金属",
        "costAmount" to "5",
        "miningSpeed" to "0",
        "storageCapacity" to "0",
        "storedItemName" to "",
        "storedItemCount" to "0",
        "maxMiners" to "0"
    )

    private val allValues = mutableMapOf<String, String>()

    override fun getPages(): List<InputPage> {
        allValues.clear()
        return listOf(
            InputPage("基础属性 - 第1/4页", createFields(page1Keys, page1Labels, page1Defaults)),
            InputPage("移动属性 - 第2/4页", createFields(page2Keys, page2Labels, page2Defaults)),
            InputPage("武器系统 - 第3/4页", createFields(page3Keys, page3Labels, page3Defaults)),
            InputPage("经济/采集 - 第4/4页", createFields(page4Keys, page4Labels, page4Defaults))
        )
    }

    private fun createFields(
        keys: List<String>,
        labels: Map<String, String>,
        defaults: Map<String, String>
    ): List<InputField> {
        return keys.map { key ->
            InputField(
                label = labels[key] ?: key,
                defaultValue = defaults[key] ?: "",
                inputType = InputType.TYPE_CLASS_TEXT
            )
        }
    }

    override fun onComplete(values: List<String>): MultiInputFunction? {
        var index = 0
        for (key in page1Keys) {
            allValues[key] = values.getOrElse(index++) { page1Defaults[key] ?: "" }
        }
        for (key in page2Keys) {
            allValues[key] = values.getOrElse(index++) { page2Defaults[key] ?: "" }
        }
        for (key in page3Keys) {
            allValues[key] = values.getOrElse(index++) { page3Defaults[key] ?: "" }
        }
        for (key in page4Keys) {
            allValues[key] = values.getOrElse(index++) { page4Defaults[key] ?: "" }
        }

        createAndRegisterUnit()
        return null
    }

    private fun createAndRegisterUnit() {
        val item = InventoryItem()
        val name = allValues["name"] ?: "自定义单位"

        if (TemplateRegistry.templates.any { it.name == name }) {
            android.util.Log.e("CreateUnit", "单位名称 '$name' 已存在")
            return
        }

        // 基础属性
        item.name = name
        item.type = allValues["type"] ?: "unit"
        item.shape = allValues["shape"] ?: "rectangle"
        item.width = (allValues["width"]?.toIntOrNull() ?: 90)
        item.height = (allValues["height"]?.toIntOrNull() ?: 30)
        item.size = (allValues["size"]?.toIntOrNull() ?: 50)
        item.health = (allValues["health"]?.toIntOrNull() ?: 5000)
        item.faction = (allValues["faction"]?.toIntOrNull() ?: 2)
        item.category = allValues["category"] ?: ""

        val colorStr = allValues["color"] ?: "#FFFFFF"
        item.color = try {
            if (colorStr.startsWith("#")) Color.parseColor(colorStr) else Color.WHITE
        } catch (e: Exception) {
            Color.WHITE
        }

        // 移动属性
        item.speed = (allValues["speed"]?.toIntOrNull() ?: 80)
        item.maxSpeed = (allValues["maxSpeed"]?.toFloatOrNull() ?: 160f)
        item.acceleration = (allValues["acceleration"]?.toFloatOrNull() ?: 20f)
        item.deceleration = (allValues["deceleration"]?.toFloatOrNull() ?: 40f)
        item.turnRate = (allValues["turnRate"]?.toFloatOrNull() ?: 180f)

        // 武器属性
        val weaponSystemType = allValues["weaponSystemType"] ?: "旧版"
        val wt = allValues["weaponType"] ?: "?"
        val range = (allValues["range"]?.toFloatOrNull() ?: 0f)
        val damage = (allValues["damage"]?.toFloatOrNull() ?: 0f)
        val bulletSpeed = (allValues["bulletSpeed"]?.toFloatOrNull() ?: 0f)
        val chargeTime = (allValues["chargeTime"]?.toFloatOrNull() ?: 0f)
        val minDamage = (allValues["minDamage"]?.toFloatOrNull() ?: 0f)
        val maxDamage = (allValues["maxDamage"]?.toFloatOrNull() ?: 0f)

        item.weaponType = wt
        item.range = range
        item.damage = damage
        item.bulletSpeed = bulletSpeed
        item.chargeTime = chargeTime
        item.minDamage = minDamage
        item.maxDamage = maxDamage

        // 根据武器系统类型设置武器槽
        item.weaponSlots.clear()

        if (weaponSystemType == "新版") {
            // 新版多武器系统：根据武器数量创建对应数量的槽位
            val weaponCount = (allValues["weaponCount"]?.toIntOrNull() ?: 1).coerceAtLeast(1)

            if (wt == "railgun" || wt == "laser" || wt == "missile") {
                for (i in 0 until weaponCount) {
                    val slot = PlacedObject.WeaponSlot()
                    slot.type = wt
                    slot.range = range
                    slot.damage = damage
                    slot.bulletSpeed = bulletSpeed
                    slot.chargeTime = chargeTime
                    slot.minDamage = minDamage
                    slot.maxDamage = maxDamage
                    slot.active = (i == 0) // 第一个槽位默认激活
                    item.weaponSlots.add(slot)
                }
            } else {
                // 空武器类型：创建空槽位
                for (i in 0 until weaponCount) {
                    val slot = PlacedObject.WeaponSlot()
                    slot.type = "?"
                    slot.active = false
                    item.weaponSlots.add(slot)
                }
            }
        } else {
            // 旧版单武器系统：只创建一个槽位
            item.weaponActive = true
            if (wt == "railgun" || wt == "laser" || wt == "missile") {
                val slot = PlacedObject.WeaponSlot()
                slot.type = wt
                slot.range = range
                slot.damage = damage
                slot.bulletSpeed = bulletSpeed
                slot.chargeTime = chargeTime
                slot.minDamage = minDamage
                slot.maxDamage = maxDamage
                slot.active = true
                item.weaponSlots.add(slot)
            }
        }

        // 经济/采集属性
        item.costItem = allValues["costItem"]?.takeIf { it.isNotEmpty() }
        item.costAmount = (allValues["costAmount"]?.toIntOrNull() ?: 0)
        item.miningSpeed = (allValues["miningSpeed"]?.toFloatOrNull() ?: 0f)
        item.storageCapacity = (allValues["storageCapacity"]?.toIntOrNull() ?: 0)
        item.storedItemName = allValues["storedItemName"]?.takeIf { it.isNotEmpty() }
        item.storedItemCount = (allValues["storedItemCount"]?.toIntOrNull() ?: 0)
        item.maxMiners = (allValues["maxMiners"]?.toIntOrNull() ?: 0)
        item.maxDockingSlots = 12
        item.dockRadius = 80f

        // 注册
        TemplateRegistry.addTemplate(item)
        InventoryPanel.registerTemplate(item)

        try {
            unitSystem.redrawListener?.requestRedraw()
        } catch (e: Exception) {}

        //android.util.Log.d("CreateUnit", "自定义单位 '$name' 创建成功 (武器系统: $weaponSystemType)")
    }

    override fun getTitle() = "自定义单位"
    override fun execute() {}
    override fun getDisplayValue() = ""
}