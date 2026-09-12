// app/src/main/kotlin/com/rtnp/demo/ui/panel/tool/ButtonAnimTool.kt
package com.rtnp.demo.ui.panel.tool

object ButtonAnimTool {

    private const val PRESS_DURATION = 0.1f
    private const val RELEASE_DURATION = 0.15f
    private const val PRESS_SCALE = 0.8f

    data class AnimState(
        var scale: Float = 1f,
        var rotation: Float = 0f,
        var targetRotation: Float = 0f,
        var isPressed: Boolean = false,
        var isRecovering: Boolean = false,
        var pressStartTime: Long = 0L,
        var releaseStartTime: Long = 0L,
        var labelAlpha: Float = 0f,
        var labelScale: Float = 0f,
        var labelOffsetX: Float = 0f
    )

    private val states = mutableMapOf<Int, AnimState>()

    fun getScale(index: Int): Float = states[index]?.scale ?: 1f
    fun getRotation(index: Int): Float = states[index]?.rotation ?: 0f
    fun getLabelAlpha(index: Int): Float = states[index]?.labelAlpha ?: 0f
    fun getLabelScale(index: Int): Float = states[index]?.labelScale ?: 0f
    fun getLabelOffsetX(index: Int): Float = states[index]?.labelOffsetX ?: 0f

    fun setRotationTarget(index: Int, target: Float) {
        val s = states.getOrPut(index) { AnimState() }
        s.targetRotation = target
    }

    fun update(buttonCount: Int) {
        val now = System.currentTimeMillis()
        for (i in 0 until buttonCount) {
            val s = states[i] ?: continue

            if (s.isPressed && !s.isRecovering) {
                val elapsed = (now - s.pressStartTime) / 1000f
                val progress = (elapsed / PRESS_DURATION).coerceIn(0f, 1f)
                s.scale = 1f + (PRESS_SCALE - 1f) * progress
                s.labelScale = 0.01f + (1f - 0.01f) * progress
                s.labelAlpha = progress
                if (s.targetRotation != 0f) {
                    s.rotation = s.targetRotation * progress
                }
            } else if (s.isRecovering) {
                val elapsed = (now - s.releaseStartTime) / 1000f
                val progress = (elapsed / RELEASE_DURATION).coerceIn(0f, 1f)
                s.scale = PRESS_SCALE + (1f - PRESS_SCALE) * progress
                s.labelScale = 1f - (1f - 0.01f) * progress
                s.labelAlpha = 1f - progress
                if (s.targetRotation != 0f) {
                    s.rotation = s.targetRotation * (1f - progress)
                }
                if (progress >= 1f) {
                    states.remove(i)
                }
            }
        }
    }

    fun onPress(index: Int) {
        states[index] = AnimState(
            scale = 1f,
            isPressed = true,
            pressStartTime = System.currentTimeMillis(),
            labelAlpha = 0f,
            labelScale = 0.01f
        )
    }

    fun onRelease(index: Int): Boolean {
        val s = states[index] ?: return false
        if (!s.isPressed) return false
        val elapsed = System.currentTimeMillis() - s.pressStartTime
        val shouldFire = elapsed <= 3000L
        s.isPressed = false
        s.isRecovering = true
        s.releaseStartTime = System.currentTimeMillis()
        return shouldFire
    }

    fun cancel(index: Int) {
        states.remove(index)
    }

    /**
     * ★ 默认动画：缩放 + 动画文字，无旋转
     */
    fun defaultOnPress(index: Int) {
        onPress(index)
    }

    fun defaultOnRelease(index: Int): Boolean {
        return onRelease(index)
    }

    fun defaultOnCancel(index: Int) {
        cancel(index)
    }
}