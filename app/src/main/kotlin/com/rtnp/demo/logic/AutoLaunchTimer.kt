// app/src/main/kotlin/com/rtnp/demo/logic/AutoLaunchTimer.kt
package com.rtnp.demo.logic

import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.render.RenderControl
import com.rtnp.demo.ui.ToastTool

/**
 * 自动出动倒计时管理器
 * 当单位有待确认的移动目标且退出选择时，5秒后自动出动
 */
object AutoLaunchTimer {

    private const val DEFAULT_DURATION = 5f

    /**
     * 启动倒计时
     */
    fun start(unit: PlacedObject, duration: Float = DEFAULT_DURATION) {
        unit.autoLaunchTimer = duration
    }

    /**
     * 取消倒计时
     */
    fun cancel(unit: PlacedObject) {
        unit.autoLaunchTimer = -1f
    }

    /**
     * 是否正在倒计时
     */
    fun isCountingDown(unit: PlacedObject): Boolean {
        return unit.autoLaunchTimer > 0f
    }

    /**
     * 获取剩余时间（秒），-1 表示未启用
     */
    fun getRemaining(unit: PlacedObject): Float {
        return unit.autoLaunchTimer
    }

    /**
     * 每帧更新所有单位的倒计时
     * @return 本轮到期的单位列表
     */
    fun update(units: List<PlacedObject>, deltaTime: Float): List<PlacedObject> {
        val launched = mutableListOf<PlacedObject>()
        val effectiveDt = deltaTime * com.rtnp.demo.combat.WeaponConstants.cooldownMultiplier
        for (unit in units) {
            if (unit.autoLaunchTimer > 0f) {
                unit.autoLaunchTimer -= effectiveDt
                if (unit.autoLaunchTimer <= 0f) {
                    unit.autoLaunchTimer = -1f
                    launched.add(unit)
                }
            }
        }
        return launched
    }

    /**
     * 执行自动出动
     */
    fun execute(unit: PlacedObject) {
        unit.hasPreviewTarget = false
        unit.isRotating = false
        unit.isMoving = true
        unit.currentSpeed = 0f
        unit.canMove = true
        // ★ 关闭弹窗，恢复 UI
        RenderControl.showUnitInfoPanel = true
        RenderControl.touchDisabledUnitInfoPanel = false
        RenderControl.touchDisabledBottomBar = false
        RenderControl.touchDisabledMultiSelectButton = false
        ToastTool.hideGlobal()
    }
}