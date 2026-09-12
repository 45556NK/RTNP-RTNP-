package com.rtnp.demo.gpu.render.tools

import android.opengl.GLES30
import com.rtnp.demo.core.InventoryItem
import com.rtnp.demo.core.TempUnitData
import com.rtnp.demo.gpu.CameraMatrix
import com.rtnp.demo.gpu.ShaderProgram
import com.rtnp.demo.logic.BuildManager
import com.rtnp.demo.gpu.GpuTextureProvider

class BuildPreviewRenderer(private val textureProvider: GpuTextureProvider) {

    private var shader: ShaderProgram? = null

    fun init() {
        val vert = """
            #version 300 es
            in vec2 aPosition;
            uniform mat4 uMVPMatrix;
            uniform vec2 uTranslation;
            uniform float uScale;
            void main() {
                vec2 pos = aPosition * uScale + uTranslation;
                gl_Position = uMVPMatrix * vec4(pos, 0.0, 1.0);
            }
        """.trimIndent()
        val frag = """
            #version 300 es
            precision mediump float;
            out vec4 fragColor;
            uniform vec4 uColor;
            void main() {
                fragColor = uColor;
            }
        """.trimIndent()
        shader = ShaderProgram.create(vert, frag)
            ?: throw RuntimeException("BuildPreviewRenderer shader failed")
    }

    fun draw(cameraMatrix: CameraMatrix, zoom: Float) {
        val s = shader ?: return
        s.use()
        s.setMat4("uMVPMatrix", cameraMatrix.getMVPMatrix())

        val wasBlendEnabled = GLES30.glIsEnabled(GLES30.GL_BLEND)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        // 绘制预览单位
        for (data in BuildManager.getPreviews()) {
            drawPreviewUnit(s, data)
        }

        // 绘制建造进程（进度环 + 半透明形状）
        for (process in BuildManager.getBuildProcesses()) {
            drawBuildProcess(s, process)
        }

        if (!wasBlendEnabled) {
            GLES30.glDisable(GLES30.GL_BLEND)
        }
    }

    private fun drawPreviewUnit(s: ShaderProgram, data: TempUnitData) {
        val item = data.item
        val alpha = getPulsingAlpha() / 255f
        val color = floatArrayOf(
            android.graphics.Color.red(item.color) / 255f,
            android.graphics.Color.green(item.color) / 255f,
            android.graphics.Color.blue(item.color) / 255f,
            alpha
        )
        drawShape(s, data.worldX, data.worldY, item, color)
    }

    private fun drawBuildProcess(s: ShaderProgram, process: BuildManager.BuildProcess) {
        val item = process.item
        val alpha = 128f / 255f
        val color = floatArrayOf(
            android.graphics.Color.red(item.color) / 255f,
            android.graphics.Color.green(item.color) / 255f,
            android.graphics.Color.blue(item.color) / 255f,
            alpha
        )
        drawShape(s, process.worldX, process.worldY, item, color)

        val progress = (process.progress / item.buildAmount).coerceIn(0f, 1f)
        val radius = item.refVolumeRadius.takeIf { it > 0f } ?: 30f

        RenderToolkit.drawThickCircleOutline(
            s, process.worldX, process.worldY, radius,
            10f,
            floatArrayOf(100f / 255f, 180f / 255f, 1f, 100f / 255f)
        )

        if (progress > 0f) {
            RenderToolkit.drawThickArc(
                s, process.worldX, process.worldY, radius,
                6f,
                -90f, 360f * progress,
                floatArrayOf(1f, 1f, 0f, 1f)
            )
        }
    }

    private fun drawShape(s: ShaderProgram, cx: Float, cy: Float, item: InventoryItem, color: FloatArray) {
        when (item.shape) {
            "circle" -> RenderToolkit.drawCircle(s, cx, cy, item.size / 2f, color)
            "square" -> {
                val half = item.size / 2f
                RenderToolkit.drawRect(s, cx - half, cy - half, cx + half, cy + half, color)
            }
            "rectangle" -> {
                val hw = item.width / 2f
                val hh = item.height / 2f
                RenderToolkit.drawRect(s, cx - hw, cy - hh, cx + hw, cy + hh, color)
            }
            else -> {
                val hw = item.width / 2f
                val hh = item.height / 2f
                RenderToolkit.drawRect(s, cx - hw, cy - hh, cx + hw, cy + hh, color)
            }
        }
    }

    private fun getPulsingAlpha(): Float {
        val now = System.currentTimeMillis()
        val phase = (now % 2500L) / 2500f
        val progress = if (phase <= 0.5f) phase * 2f else (1f - phase) * 2f
        return 64f + (211f - 64f) * progress
    }
}