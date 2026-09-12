package com.rtnp.demo.camera

/**
 * 相机控制器（纯工具类）
 * 
 * 封装地图滚动和缩放的计算逻辑，不持有状态，输入输出均为纯函数。
 * 方便 GPU 渲染器和 UI 层共享相同的相机变换公式。
 */
object CameraController {

    /** 世界尺寸常量 */
    const val WORLD_WIDTH = 3000f
    const val WORLD_HEIGHT = 5000f

    /** 缩放限制 */
    const val MIN_ZOOM = 0.05f
    const val MAX_ZOOM = 5f

    /**
     * 计算滚动后的新相机位置
     * @param cameraX 当前相机X
     * @param cameraY 当前相机Y
     * @param distanceX 手指移动的X距离（像素）
     * @param distanceY 手指移动的Y距离（像素）
     * @param zoom 当前缩放
     * @return Pair<新X, 新Y>
     */
    fun scroll(cameraX: Float, cameraY: Float, distanceX: Float, distanceY: Float, zoom: Float): Pair<Float, Float> {
        val newX = cameraX + distanceX / zoom
        val newY = cameraY + distanceY / zoom
        return Pair(newX, newY)
    }

    /**
     * 计算缩放后的新相机状态
     * @param cameraX 当前相机X
     * @param cameraY 当前相机Y
     * @param focusX 缩放焦点屏幕X
     * @param focusY 缩放焦点屏幕Y
     * @param scaleFactor 缩放因子（如 1.1）
     * @param zoom 当前缩放
     * @param screenWidth 屏幕宽度
     * @param screenHeight 屏幕高度
     * @return Triple<新X, 新Y, 新zoom>
     */
    fun scale(
        cameraX: Float, cameraY: Float,
        focusX: Float, focusY: Float,
        scaleFactor: Float, zoom: Float,
        screenWidth: Int, screenHeight: Int
    ): Triple<Float, Float, Float> {
        val newZoom = (zoom * scaleFactor).coerceIn(MIN_ZOOM, MAX_ZOOM)
        val centerX = screenWidth / 2f
        val centerY = screenHeight / 2f
        val dx = focusX - centerX
        val dy = focusY - centerY
        val newX = cameraX + dx * (1f / zoom - 1f / newZoom)
        val newY = cameraY + dy * (1f / zoom - 1f / newZoom)
        return Triple(newX, newY, newZoom)
    }

    /**
     * 屏幕坐标转世界坐标
     */
    fun screenToWorldX(screenX: Float, cameraX: Float, zoom: Float, screenWidth: Int): Float {
        if (zoom <= 0f) return Float.NaN
        return (screenX - screenWidth / 2f) / zoom + cameraX
    }

    fun screenToWorldY(screenY: Float, cameraY: Float, zoom: Float, screenHeight: Int): Float {
        if (zoom <= 0f) return Float.NaN
        return (screenY - screenHeight / 2f) / zoom + cameraY
    }
}