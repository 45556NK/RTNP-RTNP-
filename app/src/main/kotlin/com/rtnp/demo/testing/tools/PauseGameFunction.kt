package com.rtnp.demo.testing.tools

import com.rtnp.demo.UnitSystem
import com.rtnp.demo.testing.DevTools

/**
 * 切换游戏暂停（不弹出暂停菜单）。
 */
class PauseGameFunction : DevToolFunction {
    override val id = "pause_game"
    override val name = "暂停游戏"
    override val type = FunctionType.TOGGLE

    private var isPaused = false

    override fun execute() {
        isPaused = !isPaused
        DevTools.onPauseToggleListener?.onPauseToggle(isPaused)
    }

    override fun getDisplayValue(): String {
        return if (isPaused) "ON" else "OFF"
    }
}