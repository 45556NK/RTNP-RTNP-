// app/src/main/kotlin/com/rtnp/demo/testing/tools/CreateCustomWeaponFunction.kt
package com.rtnp.demo.testing.tools

import android.text.InputType
import com.rtnp.demo.UnitSystem
import com.rtnp.demo.movement.weapon.WeaponManager
import com.rtnp.demo.movement.weapon.WeaponTemplate

class CreateCustomWeaponFunction(private val unitSystem: UnitSystem) : MultiInputFunction {
    override val id = "create_custom_weapon"
    override val name = "自定义武器"
    override val type = FunctionType.INPUT

    // 第一页：基本属性
    private val page1Keys = listOf("id", "category", "subType", "displayName")
    private val page1Labels = mapOf(
        "id" to "武器ID",
        "category" to "类型(railgun/laser/missile)",
        "subType" to "子型号",
        "displayName" to "显示名称"
    )
    private val page1Defaults = mapOf(
        "id" to "custom_weapon",
        "category" to "railgun",
        "subType" to "X1",
        "displayName" to "自定义武器"
    )

    // 第二页：战斗属性
    private val page2Keys = listOf(
        "range", "cooldown", "damage", "bulletSpeed",
        "minDamage", "maxDamage", "chargeTime", "minRange"
    )
    private val page2Labels = mapOf(
        "range" to "射程",
        "cooldown" to "冷却时间",
        "damage" to "伤害(轨道炮)",
        "bulletSpeed" to "子弹速度(轨道炮)",
        "minDamage" to "最小伤害(激光)",
        "maxDamage" to "最大伤害(激光)",
        "chargeTime" to "充能时间(激光)",
        "minRange" to "最小射程(导弹)"
    )
    private val page2Defaults = mapOf(
        "range" to "500",
        "cooldown" to "1.0",
        "damage" to "100",
        "bulletSpeed" to "800",
        "minDamage" to "50",
        "maxDamage" to "200",
        "chargeTime" to "10",
        "minRange" to "100"
    )

    // 第三页：特效属性
    private val page3Keys = listOf("fireEffectId")
    private val page3Labels = mapOf("fireEffectId" to "开火特效ID(留空=无)")
    private val page3Defaults = mapOf("fireEffectId" to "")

    private val allValues = mutableMapOf<String, String>()

    override fun getPages(): List<InputPage> {
        allValues.clear()
        return listOf(
            InputPage("基本属性 - 第1/3页", createFields(page1Keys, page1Labels, page1Defaults)),
            InputPage("战斗属性 - 第2/3页", createFields(page2Keys, page2Labels, page2Defaults)),
            InputPage("特效属性 - 第3/3页", createFields(page3Keys, page3Labels, page3Defaults))
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

        createAndRegisterWeapon()
        return null
    }

    private fun createAndRegisterWeapon() {
        val id = allValues["id"] ?: "custom_weapon"

        if (WeaponManager.getTemplate(id) != null) {
            android.util.Log.e("CreateWeapon", "武器ID '$id' 已存在")
            return
        }

        val template = WeaponTemplate()
        template.id = id
        template.category = allValues["category"] ?: "railgun"
        template.subType = allValues["subType"] ?: "X1"
        template.displayName = allValues["displayName"] ?: "自定义武器"
        template.range = (allValues["range"]?.toFloatOrNull() ?: 500f)
        template.cooldown = (allValues["cooldown"]?.toFloatOrNull() ?: 1.0f)
        template.damage = (allValues["damage"]?.toFloatOrNull() ?: 100f)
        template.bulletSpeed = (allValues["bulletSpeed"]?.toFloatOrNull() ?: 800f)
        template.minDamage = (allValues["minDamage"]?.toFloatOrNull() ?: 50f)
        template.maxDamage = (allValues["maxDamage"]?.toFloatOrNull() ?: 200f)
        template.chargeTime = (allValues["chargeTime"]?.toFloatOrNull() ?: 10f)
        template.minRange = (allValues["minRange"]?.toFloatOrNull() ?: 100f)
        template.active = true
        template.fireEffectId = allValues["fireEffectId"]?.takeIf { it.isNotEmpty() } ?: ""

        WeaponManager.register(template)

        try {
            unitSystem.redrawListener?.requestRedraw()
        } catch (e: Exception) {}

        android.util.Log.d("CreateWeapon", "自定义武器 '$id' (${template.displayName}) 创建成功")
    }

    override fun getTitle() = "自定义武器"
    override fun execute() {}
    override fun getDisplayValue() = ""
}