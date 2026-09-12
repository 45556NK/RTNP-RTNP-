package com.rtnp.demo.gpu.render.tools

import android.opengl.GLES30
import com.rtnp.demo.gpu.CameraMatrix
import com.rtnp.demo.gpu.ShaderProgram

object StrategyRenderSystem {

    private var shader: ShaderProgram? = null

    private data class CircleCmd(val cx: Float, val cy: Float, val r: Float, val color: FloatArray)
    private data class RingCmd(val cx: Float, val cy: Float, val outerR: Float, val innerR: Float, val color: FloatArray)
    private data class LineCmd(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val color: FloatArray, val width: Float)
    private data class TriangleCmd(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val x3: Float, val y3: Float, val color: FloatArray)

    private val circles = mutableListOf<CircleCmd>()
    private val rings = mutableListOf<RingCmd>()
    private val lines = mutableListOf<LineCmd>()
    private val triangles = mutableListOf<TriangleCmd>()

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
    }

    fun addCircle(cx: Float, cy: Float, r: Float, color: FloatArray) {
        circles.add(CircleCmd(cx, cy, r, color))
    }

    fun addRing(cx: Float, cy: Float, outerR: Float, innerR: Float, color: FloatArray) {
        rings.add(RingCmd(cx, cy, outerR, innerR, color))
    }

    fun addLine(x1: Float, y1: Float, x2: Float, y2: Float, color: FloatArray, width: Float = 2f) {
        lines.add(LineCmd(x1, y1, x2, y2, color, width))
    }

    fun addTriangle(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, color: FloatArray) {
        triangles.add(TriangleCmd(x1, y1, x2, y2, x3, y3, color))
    }

    /** ★ 清空所有命令，在渲染线程开始生成命令前调用 */
    fun clearCommands() {
        circles.clear()
        rings.clear()
        lines.clear()
        triangles.clear()
    }

    fun render(cameraMatrix: CameraMatrix) {
        if (circles.isEmpty() && rings.isEmpty() && lines.isEmpty() && triangles.isEmpty()) return

        val s = shader ?: return
        s.use()
        s.setMat4("uMVPMatrix", cameraMatrix.getMVPMatrix())

        // 保存混合状态
        val blendEnabled = GLES30.glIsEnabled(GLES30.GL_BLEND)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        for (c in circles) RenderToolkit.drawCircle(s, c.cx, c.cy, c.r, c.color)
        for (r in rings) {
            RenderToolkit.drawCircleOutline(s, r.cx, r.cy, r.outerR, r.color)
            RenderToolkit.drawCircleOutline(s, r.cx, r.cy, r.innerR, r.color)
        }
        for (l in lines) RenderToolkit.drawLine(s, l.x1, l.y1, l.x2, l.y2, l.color, l.width)
        for (t in triangles) RenderToolkit.drawTriangle(s, t.x1, t.y1, t.x2, t.y2, t.x3, t.y3, t.color)

        // 恢复混合状态
        if (!blendEnabled) {
            GLES30.glDisable(GLES30.GL_BLEND)
        }

        // 绘制完成后清空，避免残留
        circles.clear()
        rings.clear()
        lines.clear()
        triangles.clear()
    }
}