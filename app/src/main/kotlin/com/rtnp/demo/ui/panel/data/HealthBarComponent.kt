// app/src/main/kotlin/com/rtnp/demo/ui/panel/data/HealthBarComponent.kt
package com.rtnp.demo.ui.panel.data

import android.graphics.Bitmap

class HealthBarComponent(
    val currentHealth: Float,
    val maxHealth: Float,
    val tooltipText: String? = null,
    val tooltipBitmap: Bitmap? = null,
    val tooltipWidth: Float = 0f,
    val tooltipHeight: Float = 0f
) : PanelComponent() {
    var isPressed: Boolean = false
}