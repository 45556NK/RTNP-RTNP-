// app/src/main/kotlin/com/rtnp/demo/ui/panel/data/UnitNameComponent.kt
package com.rtnp.demo.ui.panel.data

import com.rtnp.demo.core.PlacedObject

/**
 * 单位名称组件
 */
object UnitNameComponent {

    /**
     * 创建单位名称文本组件
     * @param unit 单位
     * @return 名称文本组件，名称为空则返回 null
     */
    fun create(unit: PlacedObject): PanelComponent.Text? {
        val name = unit.displayName ?: unit.name
        if (name.isEmpty()) return null
        return PanelComponent.Text(content = name, textSize = 32f)
    }
}