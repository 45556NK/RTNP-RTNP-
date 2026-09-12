// app/src/main/kotlin/com/rtnp/demo/testing/tools/ModifyUnitFunction.kt
package com.rtnp.demo.testing.tools

import android.text.InputType
import com.rtnp.demo.UnitSystem
import com.rtnp.demo.core.PlacedObject

class ModifyUnitFunction(private val unitSystem: UnitSystem) : MultiInputFunction {
    override val id = "modify_unit"
    override val name = "修改单位数据"
    override val type = FunctionType.INPUT

    override fun getPages(): List<InputPage> {
        val allUnits = unitSystem.placedObjects.toList()
        val choices = allUnits.map { "${it.displayName ?: it.name} (${it.hashCode()})" }
        return listOf(
            InputPage(
                title = "选择要修改的单位",
                fields = listOf(
                    InputField(
                        label = "单位",
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

        val allUnits = unitSystem.placedObjects.toList()
        val target = allUnits.find { "${it.displayName ?: it.name} (${it.hashCode()})" == selected }
        return if (target != null) ModifyAttributesStep(unitSystem, target) else null
    }

    override fun getTitle() = "修改单位数据"
    override fun execute() {}
    override fun getDisplayValue() = ""
}

class ModifyAttributesStep(
    private val unitSystem: UnitSystem,
    private val target: PlacedObject
) : MultiInputFunction {
    override val id = "modify_attributes"
    override val name = "修改属性"
    override val type = FunctionType.INPUT

    private val orderedKeys: List<String>
    private val skipKeys = setOf(
        "dockedAt", "currentTarget", "laserTarget",
        "miningTarget", "dockTarget", "dockingTarget",
        "isMoving", "isStopping", "isDocked", "isUndocking",
        "isMining", "miningEnabled", "weaponActive"
    )

    init {
        orderedKeys = UnitSystem.attrNames.keys.toList()
    }

    override fun getPages(): List<InputPage> {
        val fields = mutableListOf<InputField>()
        val attrMap = UnitSystem.attrNames

        for (key in orderedKeys) {
            if (key in skipKeys) continue
            val label = attrMap[key] ?: key
            val value = getCurrentValue(key)
            if (value != null) {
                fields.add(
                    InputField(
                        label = label,
                        defaultValue = value,
                        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                    )
                )
            }
        }

        return listOf(
            InputPage(
                title = "修改属性 - ${target.displayName ?: target.name}",
                fields = fields
            )
        )
    }

    private fun getCurrentValue(key: String): String? {
        return try {
            if (key == "health") {
                val hp = unitSystem.healthMap[target] ?: target.health.toFloat()
                return hp.toInt().toString()
            }

            if (key == "cooldown") {
                if (target.weaponSlots.isNotEmpty()) {
                    return target.weaponSlots[0].cooldown.toInt().toString()
                }
                return null
            }

            val field = target.javaClass.getDeclaredField(key)
            field.isAccessible = true
            val value = field.get(target)

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
        var valueIndex = 0
        for (key in orderedKeys) {
            if (key in skipKeys) continue
            if (getCurrentValue(key) == null) continue
            if (valueIndex >= values.size) break

            val rawVal = values[valueIndex]
            val newVal = rawVal.toFloatOrNull()

            if (newVal != null) {
                try {
                    val field = target.javaClass.getDeclaredField(key)
                    field.isAccessible = true

                    when (field.type) {
                        Float::class.java -> field.set(target, newVal)
                        Int::class.java -> field.set(target, newVal.toInt())
                        Long::class.java -> field.set(target, newVal.toLong())
                        Double::class.java -> field.set(target, newVal.toDouble())
                        Boolean::class.java -> field.set(target, newVal != 0f)
                        String::class.java -> field.set(target, rawVal)
                        else -> {}
                    }

                    if (key == "health") {
                        unitSystem.healthMap[target] = newVal
                    }
                    if (key == "cooldown" && target.weaponSlots.isNotEmpty()) {
                        target.weaponSlots[0].cooldownRemaining = newVal
                    }
                } catch (e: NoSuchFieldException) {
                } catch (e: Exception) {
                }
            }

            valueIndex++
        }

        target.syncCurrentWeapon()
        return null
    }

    override fun getTitle() = "修改属性"
    override fun execute() {}
    override fun getDisplayValue() = ""
}