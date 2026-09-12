package com.rtnp.demo.render

import android.graphics.Path
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 六边形网格缓存工具
 * 预计算六边形的 Path，避免每帧重复创建。
 */
object HexGridCache {

    private var cachedPath: Path? = null
    private var cachedSide: Float = -1f

    /** 获取指定边长的六边形 Path（自动缓存） */
    fun getHexPath(sideLength: Float): Path {
        if (cachedPath != null && cachedSide == sideLength) {
            return cachedPath!!
        }
        val path = Path()
        for (i in 0 until 6) {
            val angle = PI / 3 * i - PI / 2
            val x = (sideLength * cos(angle)).toFloat()
            val y = (sideLength * sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y)
            else path.lineTo(x, y)
        }
        path.close()
        cachedPath = path
        cachedSide = sideLength
        return path
    }
}