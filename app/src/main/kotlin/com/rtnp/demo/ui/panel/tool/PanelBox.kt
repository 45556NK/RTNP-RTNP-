// app/src/main/kotlin/com/rtnp/demo/ui/panel/tool/PanelBox.kt
package com.rtnp.demo.ui.panel.tool

import android.graphics.RectF

/**
 * 右侧栏面板盒子工具
 * 提供面板边界矩形的计算和坐标查询能力
 */
object PanelBox {

    /** 面板距屏幕右侧的缝隙比例 */
    private const val RIGHT_MARGIN_RATIO = 1f / 100f

    /** 面板顶部和底部的间距比例（相对于屏幕宽度） */
    private const val VERTICAL_MARGIN_RATIO = 1f / 8f

    /** 当前面板边界矩形（每次 draw 时更新） */
    private var currentRect: RectF = RectF()

    /** 当前屏幕尺寸 */
    private var currentVw: Int = 0
    private var currentVh: Int = 0

    /**
     * 更新面板尺寸（屏幕尺寸变化时调用）
     */
    fun update(vw: Int, vh: Int) {
        if (vw == currentVw && vh == currentVh) return
        currentVw = vw
        currentVh = vh

        val marginRight = vw * RIGHT_MARGIN_RATIO
        val verticalMargin = vw * VERTICAL_MARGIN_RATIO
        val panelWidth = vw / 3f

        currentRect.set(
            vw - panelWidth - marginRight,
            verticalMargin,
            vw - marginRight,
            vh - verticalMargin
        )
    }

    /** 获取当前面板矩形 */
    fun rect(): RectF = currentRect

    /** 面板左边界 */
    fun left(): Float = currentRect.left

    /** 面板右边界 */
    fun right(): Float = currentRect.right

    /** 面板顶边界 */
    fun top(): Float = currentRect.top

    /** 面板底边界 */
    fun bottom(): Float = currentRect.bottom

    /** 面板宽度 */
    fun width(): Float = currentRect.width()

    /** 面板高度 */
    fun height(): Float = currentRect.height()

    /** 面板中心 X */
    fun centerX(): Float = currentRect.centerX()

    /**
     * 判断屏幕坐标是否在面板内
     */
    fun contains(x: Float, y: Float): Boolean {
        return x >= currentRect.left && x <= currentRect.right &&
               y >= currentRect.top && y <= currentRect.bottom
    }
}