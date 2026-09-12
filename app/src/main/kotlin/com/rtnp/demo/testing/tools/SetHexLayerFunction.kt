// app/src/main/kotlin/com/rtnp/demo/testing/tools/SetHexLayerFunction.kt
package com.rtnp.demo.testing.tools

import android.text.InputType
import com.rtnp.demo.logic.HexGridManager
import com.rtnp.demo.UnitSystem

/**
 * 设置六边形网格层数，在下一次重置地图时生效
 */
class SetHexLayerFunction(private val unitSystem: UnitSystem) : MultiInputFunction {
    override val id = "set_hex_layer"
    override val name = "区块生成"
    override val type = FunctionType.INPUT

    override fun getTitle() = "设置区块层数"

    override fun getInputFields(): List<InputField> {
        return listOf(
            InputField(
                label = "区块层数",
                defaultValue = HexGridManager.maxLayer.toString(),
                inputType = InputType.TYPE_CLASS_NUMBER,
                hint = "当前: ${HexGridManager.maxLayer}"
            )
        )
    }

    // app/src/main/kotlin/com/rtnp/demo/testing/tools/SetHexLayerFunction.kt
// 找到 onComplete 方法，改为：

    override fun onComplete(values: List<String>): MultiInputFunction? {
        val layer = values.getOrElse(0) { HexGridManager.maxLayer.toString() }.toIntOrNull()
        if (layer != null && layer >= 0) {
            HexGridManager.maxLayer = layer
        }
        return null
    }

    override fun execute() {}
    override fun getDisplayValue() = "${HexGridManager.maxLayer}层"
}