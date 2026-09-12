package com.rtnp.demo.ui.panel.data

import android.graphics.Bitmap

class BuildReserveComponent(
    val currentReserve: Int,
    val maxReserve: Int,
    val tooltipText: String? = null,
    val tooltipLines: List<String> = emptyList(),
    val tooltipBitmap: Bitmap? = null,
    val tooltipWidth: Float = 0f,
    val tooltipHeight: Float = 0f
) : PanelComponent() {
    var isPressed: Boolean = false
}