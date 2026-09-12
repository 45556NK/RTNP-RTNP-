// app/src/main/kotlin/com/rtnp/demo/ui/panel/data/SpeedBarComponent.kt
package com.rtnp.demo.ui.panel.data

import android.graphics.Bitmap

class SpeedBarComponent(
    val currentSpeed: Float,
    val maxSpeed: Float,
    val tooltipText: String? = null,
    val tooltipBitmap: Bitmap? = null,
    val tooltipWidth: Float = 0f,
    val tooltipHeight: Float = 0f
) : PanelComponent() {
    var isPressed: Boolean = false
}