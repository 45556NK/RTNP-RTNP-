// app/src/main/kotlin/com/rtnp/demo/ui/panel/data/LifetimeBarComponent.kt
package com.rtnp.demo.ui.panel.data

import android.graphics.Bitmap

class LifetimeBarComponent(
    val currentLifetime: Float,
    val maxLifetime: Float,
    val tooltipText: String? = null,
    val tooltipBitmap: Bitmap? = null,
    val tooltipWidth: Float = 0f,
    val tooltipHeight: Float = 0f
) : PanelComponent() {
    var isPressed: Boolean = false
}