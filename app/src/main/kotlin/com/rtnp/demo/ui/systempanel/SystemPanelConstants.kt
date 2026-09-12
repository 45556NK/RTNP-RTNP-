package com.rtnp.demo.ui.systempanel

import com.rtnp.demo.TranslationTable
import com.rtnp.demo.core.GameConstants
import com.rtnp.demo.ui.systempanel.SystemPanelConstants


/**
 * 新系统管理面板常量数据类
 * 储存面板所需的尺寸、比例等常量
 */
object SystemPanelConstants {

    /** 六边形按钮边长 = 屏幕宽度的 1/10 */
    const val HEX_SIZE_RATIO = 1f / 10f

    /** 获取六边形按钮边长 */
    fun getHexSize(screenWidth: Int): Float = screenWidth * HEX_SIZE_RATIO

    // ==================== 面板布局常量 ====================

    /** 面板四方向边距比例（相对于屏幕宽度） */
    const val PANEL_MARGIN_HORIZONTAL_RATIO = 1f / 8f
    const val PANEL_MARGIN_VERTICAL_RATIO = 1f / 3f

    /** 面板圆角半径 */
    const val PANEL_CORNER_RADIUS = 16f

    // ==================== 字体 ====================

    /** 获取全局小字体大小 */
    fun getSmallFontSize(screenHeight: Int): Float = GameConstants.getSmallFontSize(screenHeight)

    // ==================== 文本获取 ====================

    /**
     * 从全局翻译表获取文本
     * 未找到时返回原始 key
     */
    fun getText(key: String): String {
        val translation = TranslationTable.get(key)
        return if (translation.isNotEmpty()) translation else key
    }
}