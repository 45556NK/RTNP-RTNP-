package com.rtnp.demo.ui.panel.data

import android.graphics.Bitmap
import android.graphics.Color
import com.rtnp.demo.ui.panel.tool.CooldownRenderTool
import com.rtnp.demo.ui.panel.tool.EnergyRingRenderTool

class InteractiveButtonComponent(
    val label: String,
    val width: Float,
    val height: Float,
    val backgroundColor: Int = Color.BLACK,
    val borderBitmap: Bitmap? = null,
    val iconBitmap: Bitmap? = null,
    val animLabel: String = "",
    var onClick: (() -> Unit)? = null,      // ★ 改为 var
    val onPressed: ((Int) -> Unit)? = null,
    val onReleased: ((Int) -> Boolean)? = null,
    val onCancelled: ((Int) -> Unit)? = null
) : PanelComponent() {

    var animScale: Float = 1f
    var customData: Any? = null
    var cooldownProgress: Float = 0f
    var cooldownColor: Int = Color.argb(180, 0, 0, 0)
    var cooldownDirection: CooldownRenderTool.Direction = CooldownRenderTool.Direction.BOTTOM_UP
    var energyProgress: Float = 0f
    var energyColor: Int = Color.argb(200, 100, 200, 255)
    var energyDirection: EnergyRingRenderTool.Direction = EnergyRingRenderTool.Direction.CLOCKWISE
}