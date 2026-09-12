package com.rtnp.demo.ui

import kotlin.math.sqrt

/**
 * 六边形网格工具类
 * 纯静态方法，使用 object 单例
 */
object HexGridUtil {

    /**
     * 生成尖顶朝上的蜂窝网格坐标（不考虑边框）
     */
    @JvmStatic
    fun generateHexCoords(count: Int, sideLength: Float, centerX: Float, centerY: Float): List<FloatArray> {
        return generateHexCoordsWithBorder(count, sideLength, 0f, centerX, centerY)
    }

    /**
     * 生成尖顶朝上的蜂窝网格坐标，考虑边框宽度，使六边形外边缘刚好接触
     * @param sideLength 中心到顶点的距离（内半径）
     * @param borderWidth 边框宽度（居中绘制，外扩 borderWidth/2）
     */
    @JvmStatic
    fun generateHexCoordsWithBorder(count: Int, sideLength: Float, borderWidth: Float,
                                    centerX: Float, centerY: Float): List<FloatArray> {
        val coords = mutableListOf<FloatArray>()
        if (count <= 0) return coords

        // 实际占用的外半径
        val effectiveRadius = sideLength + borderWidth / 2f
        val horizSpacing = sqrt(3f) * effectiveRadius
        val vertSpacing = effectiveRadius * 1.5f

        coords.add(floatArrayOf(centerX, centerY))
        if (count == 1) return coords

        var ring = 1
        var generated = 1
        while (generated < count) {
            var q = ring
            var r = -ring
            val dirs = arrayOf(
                intArrayOf(0, 1, -1), intArrayOf(-1, 1, 0), intArrayOf(-1, 0, 1),
                intArrayOf(0, -1, 1), intArrayOf(1, -1, 0), intArrayOf(1, 0, -1)
            )
            for (d in 0 until 6) {
                for (i in 0 until ring) {
                    if (generated >= count) break
                    val x = centerX + horizSpacing * (q + r / 2f)
                    val y = centerY + vertSpacing * r
                    coords.add(floatArrayOf(x, y))
                    generated++
                    q += dirs[d][0]
                    r += dirs[d][1]
                }
                if (generated >= count) break
            }
            ring++
        }
        return coords
    }
}