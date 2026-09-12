// app/src/main/kotlin/com/rtnp/demo/ui/panel/tool/LayerAlphaTool.kt
package com.rtnp.demo.ui.panel.tool

object LayerAlphaTool {

    /**
     * 根据是否激活返回对应的透明度
     * @return Pair(背景alpha, 边框/图标alpha)
     */
    fun getAlphaForState(isActive: Boolean): Pair<Int, Int> {
        return if (isActive) {
            Pair(255, 255)
        } else {
            Pair(64, 128)
        }
    }
}