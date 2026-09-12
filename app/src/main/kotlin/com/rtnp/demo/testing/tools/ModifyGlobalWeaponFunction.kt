// app/src/main/kotlin/com/rtnp/demo/testing/tools/ModifyGlobalWeaponFunction.kt
package com.rtnp.demo.testing.tools

import android.text.InputType
import com.rtnp.demo.UnitSystem
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.movement.weapon.WeaponManager
import com.rtnp.demo.movement.weapon.WeaponTemplate
import com.rtnp.demo.movement.ModuleManager

class ModifyGlobalWeaponFunction(private val unitSystem: UnitSystem) : MultiInputFunction {
    override val id = "modify_global_weapon"
    override val name = "修改全局武器"
    override val type = FunctionType.INPUT

    override fun getPages(): List<InputPage> {
        val allWeapons = WeaponManager.getAllTemplates()
        val choices = allWeapons.keys.toList().map { id ->
            val t = allWeapons[id]!!
            "${t.displayName} ${t.subType} ($id)"
        }
        return listOf(
            InputPage(
                title = "选择要修改的武器模板",
                fields = listOf(
                    InputField(
                        label = "武器模板",
                        defaultValue = "",
                        hint = "点击选择",
                        choices = choices
                    )
                )
            )
        )
    }

    override fun onComplete(values: List<String>): MultiInputFunction? {
        val selected = values.getOrElse(0) { "" }
        if (selected.isEmpty()) return null

        // 从选项中提取武器 ID（括号中的内容）
        val id = selected.substringAfter("(").substringBefore(")")
        val template = WeaponManager.getTemplate(id)
        return if (template != null) ModifyGlobalWeaponAttributesStep(unitSystem, template) else null
    }

    override fun getTitle() = "修改全局武器"
    override fun execute() {}
    override fun getDisplayValue() = ""
}

class ModifyGlobalWeaponAttributesStep(
    private val unitSystem: UnitSystem,
    private val template: WeaponTemplate
) : MultiInputFunction {
    override val id = "modify_global_weapon_attributes"
    override val name = "修改武器属性"
    override val type = FunctionType.INPUT

    // 武器模板可修改的属性键列表
    private val weaponKeys = listOf(
        "range", "cooldown", "damage", "bulletSpeed",
        "minDamage", "maxDamage", "chargeTime", "minRange"
    )

    // 中文翻译
    private val weaponLabels = mapOf(
        "range" to "射程",
        "cooldown" to "冷却时间",
        "damage" to "伤害",
        "bulletSpeed" to "子弹速度",
        "minDamage" to "最小伤害",
        "maxDamage" to "最大伤害",
        "chargeTime" to "充能时间",
        "minRange" to "最小射程"
    )

    override fun getPages(): List<InputPage> {
        val fields = mutableListOf<InputField>()

        for (key in weaponKeys) {
            val value = getWeaponValue(key) ?: continue
            val label = weaponLabels[key] ?: key
            fields.add(
                InputField(
                    label = label,
                    defaultValue = value,
                    inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                )
            )
        }

        return listOf(
            InputPage(
                title = "修改武器 - ${template.displayName} ${template.subType}",
                fields = fields
            )
        )
    }

    private fun getWeaponValue(key: String): String? {
        return try {
            val field = template.javaClass.getDeclaredField(key)
            field.isAccessible = true
            val value = field.get(template)

            when (value) {
                is Float -> value.toInt().toString()
                is Int -> value.toString()
                is Long -> value.toString()
                is Double -> value.toInt().toString()
                is Boolean -> value.toString()
                is String -> value
                else -> value?.toString() ?: "0"
            }
        } catch (e: NoSuchFieldException) {
            null
        } catch (e: Exception) {
            "0"
        }
    }

    override fun onComplete(values: List<String>): MultiInputFunction? {
        for (i in weaponKeys.indices) {
            val key = weaponKeys[i]
            if (getWeaponValue(key) == null) continue
            if (i >= values.size) break

            val rawVal = values[i]
            val newVal = rawVal.toFloatOrNull() ?: continue

            try {
                val field = template.javaClass.getDeclaredField(key)
                field.isAccessible = true

                when (field.type) {
                    Float::class.java -> field.set(template, newVal)
                    Int::class.java -> field.set(template, newVal.toInt())
                    Long::class.java -> field.set(template, newVal.toLong())
                    Double::class.java -> field.set(template, newVal.toDouble())
                    else -> {}
                }
            } catch (e: NoSuchFieldException) {
            } catch (e: Exception) {
            }
        }

        // ★ 刷新所有已装配此武器的单位
        refreshAllUnitsWithWeapon()

        return null
    }

    /**
     * 遍历所有单位，找到使用此武器模板的武器槽，重新创建槽位以应用新属性。
     */
    private fun refreshAllUnitsWithWeapon() {
        for (unit in unitSystem.placedObjects) {
            for (i in unit.weaponSlots.indices) {
                val slot = unit.weaponSlots[i]
                if (slot.templateId == template.id) {
                    // 重新创建武器槽位（保留激活状态）
                    val wasActive = slot.active
                    val lockedTarget = slot.lockedTarget
                    val newSlot = ModuleManager.createSlotFromTemplate(template.id)
                    if (newSlot != null) {
                        newSlot.active = wasActive
                        newSlot.lockedTarget = lockedTarget
                        unit.weaponSlots[i] = newSlot
                    }
                }
            }
            unit.syncCurrentWeapon()
        }
    }

    override fun getTitle() = "修改武器属性"
    override fun execute() {}
    override fun getDisplayValue() = ""
}