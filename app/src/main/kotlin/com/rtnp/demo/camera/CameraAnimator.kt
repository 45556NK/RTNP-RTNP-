package com.rtnp.demo.camera

import android.os.Handler
import android.os.Looper

object CameraAnimator {

    private val handler = Handler(Looper.getMainLooper())
    private var animRunnable: Runnable? = null

    interface CameraState {
        fun getCameraX(): Float
        fun setCameraX(value: Float)
        fun getCameraY(): Float
        fun setCameraY(value: Float)
        fun getZoom(): Float
        fun setZoom(value: Float)
    }

    private var cameraState: CameraState? = null
    private var onUpdate: (() -> Unit)? = null

    var isAnimating: Boolean = false
        private set

    fun init(state: CameraState, onUpdate: () -> Unit) {
        this.cameraState = state
        this.onUpdate = onUpdate
    }

    fun animateZoom(targetZoom: Float, durationMs: Long) {
        val state = cameraState ?: return
        val startZoom = state.getZoom()
        val startTime = System.currentTimeMillis()

        cancelAnimation()
        isAnimating = true

        animRunnable = object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
                val easedProgress = easeOut(progress)
                state.setZoom((startZoom + (targetZoom - startZoom) * easedProgress).coerceIn(0.05f, 5f))
                onUpdate?.invoke()

                if (progress < 1f) {
                    handler.postDelayed(this, 16)
                } else {
                    state.setZoom(targetZoom.coerceIn(0.05f, 5f))
                    onUpdate?.invoke()
                    isAnimating = false
                }
            }
        }
        handler.post(animRunnable!!)
    }

    fun animateMove(targetX: Float, targetY: Float, durationMs: Long) {
        val state = cameraState ?: return
        val startX = state.getCameraX()
        val startY = state.getCameraY()
        val startTime = System.currentTimeMillis()

        cancelAnimation()
        isAnimating = true

        animRunnable = object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
                val easedProgress = easeOut(progress)
                state.setCameraX(startX + (targetX - startX) * easedProgress)
                state.setCameraY(startY + (targetY - startY) * easedProgress)
                onUpdate?.invoke()

                if (progress < 1f) {
                    handler.postDelayed(this, 16)
                } else {
                    state.setCameraX(targetX)
                    state.setCameraY(targetY)
                    onUpdate?.invoke()
                    isAnimating = false
                }
            }
        }
        handler.post(animRunnable!!)
    }

    fun animateZoomAndMove(targetX: Float, targetY: Float, targetZoom: Float, durationMs: Long) {
        val state = cameraState ?: return
        val startX = state.getCameraX()
        val startY = state.getCameraY()
        val startZoom = state.getZoom()
        val startTime = System.currentTimeMillis()

        cancelAnimation()
        isAnimating = true

        animRunnable = object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
                val easedProgress = easeOut(progress)
                state.setCameraX(startX + (targetX - startX) * easedProgress)
                state.setCameraY(startY + (targetY - startY) * easedProgress)
                state.setZoom((startZoom + (targetZoom - startZoom) * easedProgress).coerceIn(0.05f, 5f))
                onUpdate?.invoke()

                if (progress < 1f) {
                    handler.postDelayed(this, 16)
                } else {
                    state.setCameraX(targetX)
                    state.setCameraY(targetY)
                    state.setZoom(targetZoom.coerceIn(0.05f, 5f))
                    onUpdate?.invoke()
                    isAnimating = false
                }
            }
        }
        handler.post(animRunnable!!)
    }

    fun cancelAnimation() {
        animRunnable?.let { handler.removeCallbacks(it) }
        animRunnable = null
        isAnimating = false
    }

    /**
     * 平滑缓出曲线：整个动画速度连续变化
     * 前2/3时间移动总距离的较少部分，后1/3移动较多部分
     * 使用五次方曲线实现平滑过渡，无分段割裂感
     */
    private fun easeOut(t: Float): Float {
        // 使用三次方缓出：1 - (1-t)^3
        // 前2/3时间完成约70%，后1/3完成剩余30%
        // 全程速度平滑变化，无跳跃
        val oneMinusT = 1f - t
        return 1f - oneMinusT * oneMinusT * oneMinusT
    }
}