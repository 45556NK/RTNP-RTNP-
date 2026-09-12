package com.rtnp.demo.logic
import com.rtnp.demo.core.PlacedObject
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * 编队管理器：负责计算阵型偏移量并指挥单位编队移动。
 * 纯静态方法工具类
 */
object FormationCommander {

    // 阵型类型常量
    const val FORMATION_SQUARE = 0
    const val FORMATION_LINE = 1

    /**
     * 执行编队移动指令
     * @param units 需要移动的单位列表
     * @param targetX 编队中心的目标 X 坐标（世界坐标）
     * @param targetY 编队中心的目标 Y 坐标（世界坐标）
     * @param formationType 阵型类型，如 FORMATION_SQUARE / FORMATION_LINE
     * @param spacing 单位间距（世界单位）
     */
    @JvmStatic
    fun orderFormationMove(units: List<PlacedObject>?, targetX: Float, targetY: Float,
                           formationType: Int, spacing: Float) {
        if (units == null || units.isEmpty()) return

        // 计算每个单位在阵型中的偏移量
        val offsets = calculateFormationOffsets(units.size, formationType, spacing)

        // 为每个单位设置移动目标
        for (i in units.indices) {
            val unit = units[i]
            val offset = offsets[i]
            val finalTargetX = targetX + offset[0]
            val finalTargetY = targetY + offset[1]

            // 清除该单位所有待处理行为（停泊、采矿等）
            clearPendingActions(unit)

            // 设置移动目标
            unit.targetX = finalTargetX
            unit.targetY = finalTargetY
            unit.isMoving = true
            unit.currentSpeed = 0f   // 重新加速，避免瞬间移动
        }
    }

    /**
     * 计算指定数量单位的阵型偏移量列表（相对于编队中心）
     * @param count 单位数量
     * @param formationType 阵型类型
     * @param spacing 间距
     * @return 偏移量列表，每个元素为 [offsetX, offsetY]
     */
    @JvmStatic
    fun calculateFormationOffsets(count: Int, formationType: Int, spacing: Float): List<FloatArray> {
        val offsets = mutableListOf<FloatArray>()
        if (count <= 0) return offsets

        when (formationType) {
            FORMATION_SQUARE -> {
                // 方形阵：尽可能接近正方形排列
                val side = ceil(sqrt(count.toDouble())).toInt()
                for (i in 0 until count) {
                    val row = i / side
                    val col = i % side
                    val offsetX = (col - (side - 1) / 2f) * spacing
                    val offsetY = (row - (side - 1) / 2f) * spacing
                    offsets.add(floatArrayOf(offsetX, offsetY))
                }
            }
            FORMATION_LINE -> {
                // 直线阵：水平排列，Y 偏移为 0
                val startX = -(count - 1) * spacing / 2f
                for (i in 0 until count) {
                    offsets.add(floatArrayOf(startX + i * spacing, 0f))
                }
            }
            else -> {
                // 默认方形
                val side = ceil(sqrt(count.toDouble())).toInt()
                for (i in 0 until count) {
                    val row = i / side
                    val col = i % side
                    val offsetX = (col - (side - 1) / 2f) * spacing
                    val offsetY = (row - (side - 1) / 2f) * spacing
                    offsets.add(floatArrayOf(offsetX, offsetY))
                }
            }
        }
        return offsets
    }

    /**
     * 清除单个单位的所有待处理行为（停泊目标、采矿、移动等）
     */
    private fun clearPendingActions(unit: PlacedObject) {
        unit.dockTarget = null
        unit.miningTarget = null
        unit.isMining = false
        unit.isUndocking = false
        if (unit.isMoving) {
            unit.isMoving = false
            unit.isStopping = false
            unit.currentSpeed = 0f
        }
    }
}