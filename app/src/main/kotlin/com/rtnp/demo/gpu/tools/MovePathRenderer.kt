package com.rtnp.demo.gpu.render.tools

import android.opengl.GLES30
import com.rtnp.demo.gpu.CameraMatrix
import com.rtnp.demo.gpu.ShaderProgram
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MovePathRenderer {

    private var lineShader: ShaderProgram? = null
    private var dotShader: ShaderProgram? = null
    private val pathTool = MovePathTool()

    // 圆形顶点
    private val circleVerts: FloatArray

    init {
        val segs = 32
        circleVerts = FloatArray((segs + 1) * 2)
        for (i in 0..segs) {
            val angle = 2.0 * Math.PI * i / segs
            circleVerts[i * 2] = Math.cos(angle).toFloat()
            circleVerts[i * 2 + 1] = Math.sin(angle).toFloat()
        }
    }

    fun init() {
        // 线段着色器
        val lineVert = """
            #version 300 es
            layout(location = 0) in vec4 aPosition;
            uniform mat4 uMVPMatrix;
            void main() {
                gl_Position = uMVPMatrix * aPosition;
            }
        """.trimIndent()
        val lineFrag = """
            #version 300 es
            precision mediump float;
            out vec4 fragColor;
            uniform vec4 uColor;
            void main() {
                fragColor = uColor;
            }
        """.trimIndent()
        lineShader = ShaderProgram.create(lineVert, lineFrag)
            ?: throw RuntimeException("Failed line shader")

        // 圆点着色器
        val dotVert = """
            #version 300 es
            layout(location = 0) in vec2 aCirclePos;
            layout(location = 1) in vec2 aCenter;
            uniform mat4 uMVPMatrix;
            uniform float uRadius;
            void main() {
                vec2 worldPos = aCenter + aCirclePos * uRadius;
                gl_Position = uMVPMatrix * vec4(worldPos, 0.0, 1.0);
            }
        """.trimIndent()
        val dotFrag = """
            #version 300 es
            precision mediump float;
            out vec4 fragColor;
            uniform vec4 uColor;
            void main() {
                fragColor = uColor;
            }
        """.trimIndent()
        dotShader = ShaderProgram.create(dotVert, dotFrag)
            ?: throw RuntimeException("Failed dot shader")
    }

    fun build(units: List<com.rtnp.demo.core.PlacedObject>) {
        pathTool.buildMovePaths(units)
    }

    fun drawLines(cameraMatrix: CameraMatrix, color: FloatArray) {
        val s = lineShader ?: return
        s.use()
        s.setMat4("uMVPMatrix", cameraMatrix.getMVPMatrix())
        s.setVec4("uColor", color)
        val verts = pathTool.getMoveVertices2D()
        if (verts.isEmpty()) return
        val buf = ByteBuffer.allocateDirect(verts.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(verts)
            .apply { position(0) }
        val posHandle = s.getAttribLocation("aPosition")
        GLES30.glVertexAttribPointer(posHandle, 2, GLES30.GL_FLOAT, false, 0, buf)
        GLES30.glEnableVertexAttribArray(posHandle)
        GLES30.glLineWidth(5f)
        GLES30.glDrawArrays(GLES30.GL_LINES, 0, verts.size / 2)
        GLES30.glDisableVertexAttribArray(posHandle)
    }

    fun drawDots(cameraMatrix: CameraMatrix, color: FloatArray, radius: Float = 2.5f) {
        val s = dotShader ?: return
        s.use()
        s.setMat4("uMVPMatrix", cameraMatrix.getMVPMatrix())
        s.setVec4("uColor", color)
        s.setFloat("uRadius", radius)
        val dots = pathTool.getPreviewDots()
        if (dots.isEmpty()) return

        val circleBuf = ByteBuffer.allocateDirect(circleVerts.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(circleVerts)
            .apply { position(0) }

        val cPosHandle = s.getAttribLocation("aCirclePos")
        GLES30.glVertexAttribPointer(cPosHandle, 2, GLES30.GL_FLOAT, false, 0, circleBuf)
        GLES30.glEnableVertexAttribArray(cPosHandle)

        val centerBuf = ByteBuffer.allocateDirect(dots.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(dots)
            .apply { position(0) }
        val centerHandle = s.getAttribLocation("aCenter")
        GLES30.glVertexAttribPointer(centerHandle, 2, GLES30.GL_FLOAT, false, 0, centerBuf)
        GLES30.glEnableVertexAttribArray(centerHandle)
        GLES30.glVertexAttribDivisor(centerHandle, 1)

        GLES30.glDrawArraysInstanced(GLES30.GL_TRIANGLE_FAN, 0, 33, dots.size / 2)

        GLES30.glDisableVertexAttribArray(cPosHandle)
        GLES30.glDisableVertexAttribArray(centerHandle)
        GLES30.glVertexAttribDivisor(centerHandle, 0)
    }
}