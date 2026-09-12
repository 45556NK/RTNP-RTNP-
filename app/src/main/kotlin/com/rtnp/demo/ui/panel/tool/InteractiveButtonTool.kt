// app/src/main/kotlin/com/rtnp/demo/ui/panel/tool/InteractiveButtonTool.kt
package com.rtnp.demo.ui.panel.tool

object InteractiveButtonTool {

    /** 按钮与面板左右边的间距 */
    const val BUTTON_MARGIN_H = 5f

    /** 圆角半径 */
    const val CORNER_RADIUS = 12f

    /**
     * 计算按钮尺寸
     * 正方形，边长为屏幕宽的 1/8
     */
    fun calculateSize(vw: Int): Float {
        return vw / 6f
    }
}