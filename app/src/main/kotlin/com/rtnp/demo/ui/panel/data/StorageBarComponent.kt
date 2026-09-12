// app/src/main/kotlin/com/rtnp/demo/ui/panel/data/StorageBarComponent.kt
package com.rtnp.demo.ui.panel.data

import android.graphics.Bitmap

class StorageBarComponent(
    val currentStorage: Int,
    val maxStorage: Int,
    val tooltipText: String? = null,
    val tooltipLines: List<String> = emptyList(),  // ★ 多行文本
    val tooltipBitmap: Bitmap? = null,
    val tooltipWidth: Float = 0f,
    val tooltipHeight: Float = 0f
) : PanelComponent() {
    var isPressed: Boolean = false
}