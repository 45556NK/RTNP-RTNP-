// app/src/main/kotlin/com/rtnp/demo/gpu/render/tools/TileSelectorRenderSystem.kt
package com.rtnp.demo.gpu.render.tools

import android.opengl.GLES30
import com.rtnp.demo.gpu.CameraMatrix
import com.rtnp.demo.gpu.ShaderProgram
import com.rtnp.demo.logic.HexGridManager
import com.rtnp.demo.strategy.TileSelectorTool
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class TileSelectorRenderSystem {

    private var hexShader: ShaderProgram? = null
    private var hexVertexBuffer: java.nio.FloatBuffer? = null
    private var hexIndexCount: Int = 0

    private val goldColor = floatArrayOf(1f, 215f / 255f, 0f, 1f)
    private val redColor = floatArrayOf(1f, 0f, 0f, 1f)

    fun init() {
        val vert = """
            #version 300 es
            in vec2 aPosition;
            uniform mat4 uMVPMatrix;
            void main() {
                gl_Position = uMVPMatrix * vec4(aPosition, 0.0, 1.0);
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
        hexShader = ShaderProgram.create(vert, frag)
            ?: throw RuntimeException("TileSelectorRenderSystem: hex shader failed")

        val verts = mutableListOf<Float>()
        for (i in 0..6) {
            val angle = PI / 3.0 * i - PI / 2.0
            verts.add(cos(angle).toFloat())
            verts.add(sin(angle).toFloat())
        }
        hexIndexCount = verts.size / 2
        hexVertexBuffer = ByteBuffer.allocateDirect(verts.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
            .put(verts.toFloatArray()).apply { position(0) }
    }

    fun render(cameraMatrix: CameraMatrix, zoom: Float) {
        if (!TileSelectorTool.isActive) return

        val tile = TileSelectorTool.getSelectedTile() ?: return

        val s = hexShader ?: return
        s.use()
        s.setMat4("uMVPMatrix", cameraMatrix.getMVPMatrix())

        val buf = hexVertexBuffer ?: return
        buf.position(0)
        val posHandle = s.getAttribLocation("aPosition")
        if (posHandle == -1) return

        GLES30.glVertexAttribPointer(posHandle, 2, GLES30.GL_FLOAT, false, 0, buf)
        GLES30.glEnableVertexAttribArray(posHandle)

        // ★ 保存混合状态
        val blendEnabled = GLES30.glIsEnabled(GLES30.GL_BLEND)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        val animScale = getAnimScale()
        val hexSide = HexGridManager.hexSide * animScale

        val baseStroke = 8f / zoom
        val strokeWidth = (baseStroke * animScale).coerceIn(10f, 100f)
        GLES30.glLineWidth(strokeWidth)

        val color = if (TileSelectorTool.isSelectedTileRestricted()) redColor else goldColor
        s.setVec4("uColor", color)

        val verts = buildHexVertices(tile.worldX, tile.worldY, hexSide)
        val vbuf = ByteBuffer.allocateDirect(verts.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
            .put(verts).apply { position(0) }

        GLES30.glVertexAttribPointer(posHandle, 2, GLES30.GL_FLOAT, false, 0, vbuf)
        GLES30.glDrawArrays(GLES30.GL_LINE_LOOP, 0, 6)

        GLES30.glDisableVertexAttribArray(posHandle)

        // ★ 恢复混合状态
        if (!blendEnabled) {
            GLES30.glDisable(GLES30.GL_BLEND)
        }
    }

    private fun buildHexVertices(cx: Float, cy: Float, side: Float): FloatArray {
        val verts = FloatArray(12)
        for (i in 0 until 6) {
            val angle = PI / 3.0 * i - PI / 2.0
            verts[i * 2] = cx + side * cos(angle).toFloat()
            verts[i * 2 + 1] = cy + side * sin(angle).toFloat()
        }
        return verts
    }

    private fun getAnimScale(): Float {
        val animStartTime = TileSelectorTool.animStartTime
        val now = System.currentTimeMillis()
        val animDurationMs = 100L
        val animMaxScale = 1.2f

        val animElapsed = (now - animStartTime).coerceAtMost(animDurationMs)
        val animT = animElapsed.toFloat() / animDurationMs.toFloat()
        return 1f + (animMaxScale - 1f) * (1f - animT) * sin(animT * PI.toFloat()).toFloat()
    }
}