// app/src/main/kotlin/com/rtnp/demo/testing/tools/SpawnUnitsFunction.kt
package com.rtnp.demo.testing.tools

import android.text.InputType
import com.rtnp.demo.UnitSystem
import com.rtnp.demo.core.InventoryItem
import com.rtnp.demo.core.TemplateRegistry

class SpawnUnitsFunction(private val unitSystem: UnitSystem) : MultiInputFunction {
    override val id = "spawn_units"
    override val name = "生成单位"
    override val type = FunctionType.INPUT

    override fun getTitle() = "生成单位"

    override fun getInputFields(): List<InputField> {
        val unitNames = TemplateRegistry.templates.map { it.name }
        return listOf(
            InputField("单位名称", "测试单位", InputType.TYPE_CLASS_TEXT, "点击选择或输入", unitNames),
            InputField("生成数量", "10", InputType.TYPE_CLASS_NUMBER, "数量"),
            InputField("生成密度", "0.5", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL, "0.1 ~ 1.0"),
            InputField("X坐标", "1500", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL, "世界X坐标"),
            InputField("Y坐标", "2500", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL, "世界Y坐标")
        )
    }

    override fun onComplete(values: List<String>): MultiInputFunction? {
        val name = values.getOrElse(0) { "测试单位" }
        val count = values.getOrElse(1) { "10" }.toIntOrNull() ?: 10
        val density = values.getOrElse(2) { "0.5" }.toFloatOrNull() ?: 0.5f
        val centerX = values.getOrElse(3) { "1500" }.toFloatOrNull() ?: 1500f
        val centerY = values.getOrElse(4) { "2500" }.toFloatOrNull() ?: 2500f

        val template = TemplateRegistry.templates.firstOrNull { it.name == name }
        if (template == null) {
            android.util.Log.e("SpawnUnits", "单位模板 '$name' 未找到")
            return null
        }

        spawnUnits(template, count, density, centerX, centerY)
        return null
    }

    override fun execute() {}
    override fun getDisplayValue() = ""

    private fun spawnUnits(template: InventoryItem, count: Int, density: Float, cx: Float, cy: Float) {
        val clampedDensity = density.coerceIn(0.1f, 1.0f)
        val spread = 1000f * (1.0f - clampedDensity * 0.8f)

        val cols = kotlin.math.sqrt(count.toDouble()).toInt().coerceAtLeast(1)
        val rows = (count + cols - 1) / cols
        val spacingX = spread / cols
        val spacingY = spread / rows

        val startX = cx - spread / 2f + spacingX / 2f
        val startY = cy - spread / 2f + spacingY / 2f

        for (i in 0 until count) {
            val row = i / cols
            val col = i % cols
            val x = startX + col * spacingX
            val y = startY + row * spacingY
            unitSystem.addObjectFromItem(template, x, y)
        }

        android.util.Log.d("SpawnUnits", "已生成 $count 个 ${template.name}")
    }
}