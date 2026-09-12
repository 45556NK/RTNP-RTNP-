// app/src/main/kotlin/com/rtnp/demo/testing/PlacementGridRenderer.kt
package com.rtnp.demo.testing

import android.graphics.*
import com.rtnp.demo.logic.HexGridManager
import com.rtnp.demo.render.HexGridCache

object PlacementGridRenderer {

    /** 上次采样时的参数，用于检测是否需要重新采样 */
    private var lastLayer = -1
    private var lastHexSide = -1f
    private var lastCenterX = -1f
    private var lastCenterY = -1f

    /** 缓存的采样 Bitmap */
    private var cachedBitmap: Bitmap? = null

    /** Bitmap 对应的世界坐标范围 */
    private var cachedWorldLeft = 0f
    private var cachedWorldTop = 0f
    private var cachedWorldRight = 0f
    private var cachedWorldBottom = 0f

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.YELLOW
        style = Paint.Style.STROKE
    }

    private val crossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLUE
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLUE
        textAlign = Paint.Align.CENTER
    }

    fun draw(canvas: Canvas, zoom: Float) {
        ensureSampled()

        // 绘制采样结果
        cachedBitmap?.let { bitmap ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = 80 }
            val src = Rect(0, 0, bitmap.width, bitmap.height)
            val dst = RectF(cachedWorldLeft, cachedWorldTop, cachedWorldRight, cachedWorldBottom)
            canvas.drawBitmap(bitmap, src, dst, paint)
        }

        // 绘制六边形边框、中心十字和编号
        strokePaint.strokeWidth = 2f / zoom
        crossPaint.strokeWidth = 3f / zoom
        textPaint.textSize = 30f / zoom

        for (tile in HexGridManager.getAllTiles()) {
            val wx = tile.worldX
            val wy = tile.worldY

            drawHexagon(canvas, wx, wy, strokePaint)

            val crossLen = 15f / zoom
            canvas.drawLine(wx - crossLen, wy, wx + crossLen, wy, crossPaint)
            canvas.drawLine(wx, wy - crossLen, wx, wy + crossLen, crossPaint)

            canvas.drawText(tile.id.toString(), wx, wy - crossLen - 10f / zoom, textPaint)
        }
    }

    /**
     * 检查是否需要重新采样，如果需要则执行采样。
     * 当区块层数、六边形边长、世界中心任一变化时触发重新采样。
     */
    private fun ensureSampled() {
        val currentLayer = HexGridManager.maxLayer
        val currentHexSide = HexGridManager.hexSide
        val currentCenterX = HexGridManager.worldCenterX
        val currentCenterY = HexGridManager.worldCenterY

        if (cachedBitmap != null &&
            lastLayer == currentLayer &&
            lastHexSide == currentHexSide &&
            lastCenterX == currentCenterX &&
            lastCenterY == currentCenterY
        ) {
            return // 参数未变，复用缓存
        }

        // 参数变化，重新采样
        lastLayer = currentLayer
        lastHexSide = currentHexSide
        lastCenterX = currentCenterX
        lastCenterY = currentCenterY

        performSampling()
    }

    /**
     * 执行逐点采样。
     * 先获取所有六边形的实际边界，在边界内逐行逐列检测每个点是否在六边形网格内。
     */
    private fun performSampling() {
        val tiles = HexGridManager.getAllTiles()
        if (tiles.isEmpty()) {
            cachedBitmap = null
            return
        }

        // 1. 计算所有六边形的实际世界边界
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        for (tile in tiles) {
            val wx = tile.worldX
            val wy = tile.worldY
            val r = HexGridManager.hexSide  // 外接圆半径

            if (wx - r < minX) minX = wx - r
            if (wy - r < minY) minY = wy - r
            if (wx + r > maxX) maxX = wx + r
            if (wy + r > maxY) maxY = wy + r
        }

        // 2. 外扩一点余量
        val margin = HexGridManager.hexSide * 0.1f
        cachedWorldLeft = minX - margin
        cachedWorldTop = minY - margin
        cachedWorldRight = maxX + margin
        cachedWorldBottom = maxY + margin

        val worldW = cachedWorldRight - cachedWorldLeft
        val worldH = cachedWorldBottom - cachedWorldTop

        // 3. 采样步长：固定用小步长确保逐个探点完全覆盖
        val sampleStep = 5f

        val cols = (worldW / sampleStep).toInt() + 1
        val rows = (worldH / sampleStep).toInt() + 1

        // 限制 Bitmap 最大尺寸，防止超大网格时内存溢出
        val maxDimension = 4096
        val actualCols = cols.coerceAtMost(maxDimension)
        val actualRows = rows.coerceAtMost(maxDimension)
        val actualStepX = if (cols > maxDimension) worldW / maxDimension else sampleStep
        val actualStepY = if (rows > maxDimension) worldH / maxDimension else sampleStep

        val bitmap = Bitmap.createBitmap(actualCols, actualRows, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(255, 255, 255, 0)
            style = Paint.Style.FILL
        }

        // 4. 逐点采样
        for (col in 0 until actualCols) {
            val x = cachedWorldLeft + col * actualStepX
            for (row in 0 until actualRows) {
                val y = cachedWorldTop + row * actualStepY
                if (HexGridManager.isPointInHexGrid(x, y)) {
                    canvas.drawPoint(col.toFloat(), row.toFloat(), dotPaint)
                }
            }
        }

        cachedBitmap?.recycle()
        cachedBitmap = bitmap
    }

    private fun drawHexagon(canvas: Canvas, cx: Float, cy: Float, paint: Paint) {
        val path = HexGridCache.getHexPath(HexGridManager.hexSide)
        canvas.save()
        canvas.translate(cx, cy)
        canvas.drawPath(path, paint)
        canvas.restore()
    }

    /**
     * 强制使缓存失效，外部可在重置地图或修改网格参数后调用。
     */
    fun invalidateCache() {
        cachedBitmap?.recycle()
        cachedBitmap = null
        lastLayer = -1
        lastHexSide = -1f
        lastCenterX = -1f
        lastCenterY = -1f
    }
}