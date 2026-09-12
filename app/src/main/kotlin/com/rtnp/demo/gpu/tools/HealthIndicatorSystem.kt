package com.rtnp.demo.gpu.render.tools

import android.opengl.GLES30
import com.rtnp.demo.UnitSystem
import com.rtnp.demo.core.GameConstants
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.gpu.CameraMatrix
import com.rtnp.demo.gpu.ShaderProgram
import java.nio.ByteBuffer
import java.nio.ByteOrder

class HealthIndicatorSystem {

    private var shader: ShaderProgram? = null
    private var vertexBuffer: java.nio.FloatBuffer? = null

    // 矩形顶点（单位矩形，中心在原点）
    private val rectVertices = floatArrayOf(
        -0.5f, -0.5f,
         0.5f, -0.5f,
        -0.5f,  0.5f,
         0.5f,  0.5f
    )

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
            ?: throw RuntimeException("HealthIndicator shader failed")

        vertexBuffer = ByteBuffer.allocateDirect(rectVertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(rectVertices)
            .apply { position(0) }
    }

    fun render(
        cameraMatrix: CameraMatrix,
        units: List<PlacedObject>,
        unitSystem: UnitSystem?,
        zoom: Float
    ) {
        if (unitSystem == null || units.isEmpty()) return

        // ★ 复制列表防止并发修改
        val safeUnits = units.toList()

        val s = shader ?: return
        s.use()
        val vp = vertexBuffer ?: return
        vp.position(0)
        val posHandle = s.getAttribLocation("aPosition")
        if (posHandle == -1) return

        GLES30.glVertexAttribPointer(posHandle, 2, GLES30.GL_FLOAT, false, 0, vp)
        GLES30.glEnableVertexAttribArray(posHandle)

        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        val mvp = cameraMatrix.getMVPMatrix()
        s.setMat4("uMVPMatrix", mvp)

        // 收集选中的单位ID（用于多选判定）
        val selectedUnit = unitSystem.selectedUnit
        val multiSelUnits = if (unitSystem.multiSelectManager?.isActive() == true) {
            unitSystem.multiSelectManager.getSelectedUnits()
        } else emptySet()

        for (unit in safeUnits) {
            if (unit.category == "missile") continue

            val isSelected = unit == selectedUnit || unit in multiSelUnits
            val health = unit.health.toFloat()
            val maxHealth = unit.originalHealth.toFloat()
            if (maxHealth <= 0f) continue

            val isFullHealth = health >= maxHealth

            // 选中且满血：不渲染
            if (isSelected && isFullHealth) continue

            // 未选中且缩放 < 3：不渲染
            if (!isSelected && zoom < 3f) continue

            // 计算填充颜色（红 -> 绿）
            val ratio = (health / maxHealth).coerceIn(0f, 1f)
            val r = 1f - ratio
            val g = ratio
            val b = 0f
            val fillColor = floatArrayOf(r, g, b, 1.0f)

            // 阵营边框颜色
            val borderColors = GameConstants.getFactionBorderColor(unit.faction)

            // 计算大小（世界单位，保持屏幕像素近似恒定）
            val baseSize = 40f / zoom
            val size = baseSize.coerceIn(15f, 60f)
            val borderStroke = (20f / zoom).coerceIn(4f, 30f)  // 边框厚度翻倍
            val outerSize = size + borderStroke

            val cx = unit.worldX
            val cy = unit.worldY

            // 先绘制边框（稍大，作为底色）
            s.setVec4("uColor", floatArrayOf(borderColors[0], borderColors[1], borderColors[2], 1.0f))
            s.setVec2("uTranslation", floatArrayOf(cx, cy))
            s.setFloat("uScale", outerSize)
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)

            // 再绘制内部填充（略小）
            s.setVec4("uColor", fillColor)
            s.setFloat("uScale", size)
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        }

        GLES30.glDisableVertexAttribArray(posHandle)
    }
}