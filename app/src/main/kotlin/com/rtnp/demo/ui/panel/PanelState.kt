// app/src/main/kotlin/com/rtnp/demo/ui/panel/PanelState.kt
package com.rtnp.demo.ui.panel

import com.rtnp.demo.core.PlacedObject

data class PanelState(
    val mode: PanelMode = PanelMode.HIDDEN,
    val lockedUnits: List<PlacedObject> = emptyList(),
    val customLayout: List<LayoutItem>? = null,
    val showDetails: Boolean = false,
    val template: PanelTemplate? = null   // ★ 外部指定的模板，null 表示自行判断
)