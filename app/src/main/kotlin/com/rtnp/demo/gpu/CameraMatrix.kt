package com.rtnp.demo.gpu

import android.opengl.Matrix

class CameraMatrix {
    private val mvpMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)

    var surfaceWidth: Int = 1
    var surfaceHeight: Int = 1

    var worldWidth: Float = 3000f
    var worldHeight: Float = 5000f

    fun updateSurfaceSize(width: Int, height: Int) {
        surfaceWidth = width
        surfaceHeight = height
    }

    fun update(cameraX: Float, cameraY: Float, zoom: Float) {
        // 计算视口在世界空间中的范围
        val halfViewWidth = (surfaceWidth / 2f) / zoom
        val halfViewHeight = (surfaceHeight / 2f) / zoom

        val left = cameraX - halfViewWidth
        val right = cameraX + halfViewWidth
        val bottom = cameraY + halfViewHeight   // 世界坐标 Y 轴向下
        val top = cameraY - halfViewHeight

        // 正交投影矩阵：直接裁剪世界坐标
        Matrix.orthoM(projectionMatrix, 0, left, right, bottom, top, -1f, 1f)

        // 视图矩阵保持单位矩阵（因为投影已经包含了摄像机变换）
        Matrix.setIdentityM(viewMatrix, 0)

        // MVP = Projection × View × Model（Model 为单位矩阵）
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
    }

    fun getMVPMatrix(): FloatArray = mvpMatrix
}