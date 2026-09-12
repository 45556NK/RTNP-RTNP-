// app/src/main/kotlin/com/rtnp/demo/testing/tools/ModifyGlobalUnitFunction.kt
package com.rtnp.demo.testing.tools

import android.text.InputType
import com.rtnp.demo.UnitSystem
import com.rtnp.demo.core.InventoryItem
import com.rtnp.demo.core.TemplateRegistry
import com.rtnp.demo.ui.InventoryPanel

class ModifyGlobalUnitFunction(private val unitSystem: UnitSystem) : MultiInputFunction {
    override val id = "modify_global_unit"
    override val name = "修改全局单位"
    override val type = FunctionType.INPUT

    override fun getPages(): List<InputPage> {
        val templates = TemplateRegistry.templates.toList()
        val choices = templates.map { it.name }
        return listOf(
            InputPage(
                title = "选择要修改的单位模板",
                fields = listOf(
                    InputField(
                        label = "单位模板",
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

        val template = TemplateRegistry.templates.find { it.name == selected }
        return if (template != null) ModifyGlobalAttributesStep(unitSystem, template) else null
    }

    override fun getTitle() = "修改全局单位"
    override fun execute() {}
    override fun getDisplayValue() = ""
}

class ModifyGlobalAttributesStep(
    private val unitSystem: UnitSystem,
    private val template: InventoryItem
) : MultiInputFunction {
    override val id = "modify_global_attributes"
    override val name = "修改全局属性"
    override val type = FunctionType.INPUT

    override fun getPages(): List<InputPage> {
        val fields = mutableListOf<InputField>()
        val attrMap = UnitSystem.attrNames

        for ((key, label) in attrMap) {
            val value = getTemplateValue(key)
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
                title = "修改全局属性 - ${template.name}",
                fields = fields
            )
        )
    }

    private fun getTemplateValue(key: String): String? {
        return try {
            // 特殊处理
            if (key == "health") {
                return template.health.toString()
            }

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
        val attrMap = UnitSystem.attrNames
        val keys = attrMap.keys.toList()
        var valueIndex = 0

        for (key in keys) {
            if (getTemplateValue(key) == null) continue
            if (valueIndex >= values.size) break

            val rawVal = values[valueIndex]
            val newVal = rawVal.toFloatOrNull()

            if (newVal != null) {
                try {
                    val field = template.javaClass.getDeclaredField(key)
                    field.isAccessible = true

                    when (field.type) {
                        Float::class.java -> field.set(template, newVal)
                        Int::class.java -> field.set(template, newVal.toInt())
                        Long::class.java -> field.set(template, newVal.toLong())
                        Double::class.java -> field.set(template, newVal.toDouble())
                        Boolean::class.java -> field.set(template, newVal != 0f)
                        String::class.java -> field.set(template, rawVal)
                        else -> {}
                    }
                } catch (e: NoSuchFieldException) {
                } catch (e: Exception) {
                }
            }

            valueIndex++
        }

        // 同步更新已注册的模板（确保下次获取时是新值）
        InventoryPanel.registerTemplate(template)

        return null
    }

    override fun getTitle() = "修改全局属性"
    override fun execute() {}
    override fun getDisplayValue() = ""
}