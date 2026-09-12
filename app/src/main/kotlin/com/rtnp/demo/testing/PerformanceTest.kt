package com.rtnp.demo.testing

import com.rtnp.demo.UnitSystem
import com.rtnp.demo.core.InventoryItem
import com.rtnp.demo.core.TemplateRegistry
import kotlin.math.sqrt

/**
 * 性能测试工具：在地图中心生成大量单位，用于测试渲染和逻辑帧率。
 * 支持自定义密度，控制单位之间的间距。
 */
object PerformanceTest {

    /**
     * 在地图中心生成指定数量的“测试单位”（默认模板名 "测试单位"）。
     *
     * @param unitSystem   游戏单位系统实例
     * @param count        要生成的单位数量
     * @param worldWidth   世界宽度
     * @param worldHeight  世界高度
     * @param density      密度值 (0.0 ~ 1.0)。
     *                     1.0 = 最高密度（单位紧密排列，占据区域较小）
     *                     0.1 = 最低密度（单位分散排列，占据整个地图）
     *                     默认 0.5 为适中密度
     */
    @JvmStatic
    fun spawnUnitsAtCenter(
        unitSystem: UnitSystem,
        count: Int = 800,
        worldWidth: Float = 3000f,
        worldHeight: Float = 5000f,
        density: Float = 0.5f
    ) {
        // 查找模板（必须是已注册的 InventoryItem）
        val template: InventoryItem? = TemplateRegistry.templates
            .firstOrNull { it.name == "测试单位" }

        if (template == null) {
            android.util.Log.e("PerformanceTest", "模板 '测试单位' 未找到，请检查 TemplateRegistry")
            return
        }

        // 将密度限制在 0.1 ~ 1.0 范围内
        val clampedDensity = density.coerceIn(0.1f, 1.0f)

        // 根据密度计算实际占用的区域
        // 密度越高，区域越小，单位越密集
        val regionWidth = worldWidth * (1.0f - clampedDensity * 0.6f)
        val regionHeight = worldHeight * (1.0f - clampedDensity * 0.6f)
        val startX = (worldWidth - regionWidth) / 2f
        val startY = (worldHeight - regionHeight) / 2f

        // 自动计算网格列数，尽量接近正方形排列
        val cols = sqrt(count.toDouble()).toInt().coerceAtLeast(1)
        val rows = (count + cols - 1) / cols
        val spacingX = regionWidth / cols
        val spacingY = regionHeight / rows

        // 单位占位偏移（使它们居中排列）
        val halfCellX = spacingX / 2f
        val halfCellY = spacingY / 2f

        for (i in 0 until count) {
            val row = i / cols
            val col = i % cols
            val worldX = startX + halfCellX + col * spacingX
            val worldY = startY + halfCellY + row * spacingY

            unitSystem.addObjectFromItem(template, worldX, worldY)
        }

        android.util.Log.d(
            "PerformanceTest",
            "已生成 $count 个单位，密度: $clampedDensity，区域: ${regionWidth.toInt()}×${regionHeight.toInt()}"
        )
    }
}