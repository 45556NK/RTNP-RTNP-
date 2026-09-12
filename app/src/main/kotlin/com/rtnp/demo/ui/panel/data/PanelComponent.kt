// app/src/main/kotlin/com/rtnp/demo/ui/panel/data/PanelComponent.kt
package com.rtnp.demo.ui.panel.data

import android.graphics.Bitmap

/**
 * 右侧栏组件数据
 */
sealed class PanelComponent {

    /** 组件高度（像素），由布局引擎计算后填入 */
    var computedHeight: Float = 0f

    /** 组件在面板内的 Y 坐标（由布局引擎计算后填入） */
    var layoutY: Float = 0f

    /** 纯文本组件 */
    data class Text(
        val content: String,
        val textSize: Float = 28f
    ) : PanelComponent()

    /** 静态贴图组件 */
    data class Image(
        val bitmap: Bitmap?,
        val width: Float,
        val height: Float
    ) : PanelComponent()

    /** 可互动按钮组件 */
    data class Button(
        val label: String,
        val isActive: Boolean = false,
        val activeColor: Int = android.graphics.Color.YELLOW,
        val subText: String? = null,
        val cooldownProgress: Float? = null
    ) : PanelComponent()
    
    // PanelComponent.kt 中增加
    data class Spacer(val height: Float = 10f) : PanelComponent()
}