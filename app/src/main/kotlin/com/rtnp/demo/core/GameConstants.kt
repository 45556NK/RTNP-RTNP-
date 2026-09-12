// app/src/main/kotlin/com/rtnp/demo/core/GameConstants.kt
package com.rtnp.demo.core

import android.graphics.Color

object GameConstants {
    // 阵营
    const val FACTION_NEUTRAL = 0
    const val FACTION_ENEMY = 1
    const val FACTION_PLAYER = 2

    fun factionToName(faction: Int): String = when (faction) {
        FACTION_NEUTRAL -> "中立"
        FACTION_ENEMY -> "敌方"
        FACTION_PLAYER -> "我方"
        else -> "未知"
    }

    val FACTION_BORDER_COLOR_NEUTRAL = floatArrayOf(0.5f, 0.5f, 0.5f)
    val FACTION_BORDER_COLOR_ENEMY = floatArrayOf(139f/255f, 0f, 0f)
    val FACTION_BORDER_COLOR_PLAYER = floatArrayOf(0f, 0f, 139f/255f)

    fun getFactionBorderColor(faction: Int): FloatArray {
        return when (faction) {
            FACTION_NEUTRAL -> FACTION_BORDER_COLOR_NEUTRAL
            FACTION_ENEMY -> FACTION_BORDER_COLOR_ENEMY
            FACTION_PLAYER -> FACTION_BORDER_COLOR_PLAYER
            else -> floatArrayOf(1f, 1f, 1f)
        }
    }

    val TOOL_CIRCLE_COLOR = Color.rgb(100, 150, 255)
    val TOOL_OUT_OF_RANGE_COLOR = Color.argb(80, 255, 150, 150)

    // ★ 全局字体大小
    const val FONT_SIZE_LARGE_RATIO = 1f / 52f   // 大字体
    const val FONT_SIZE_SMALL_RATIO = 1f / 58f   // 小字体

    fun getLargeFontSize(screenHeight: Int): Float = screenHeight * FONT_SIZE_LARGE_RATIO
    fun getSmallFontSize(screenHeight: Int): Float = screenHeight * FONT_SIZE_SMALL_RATIO
}