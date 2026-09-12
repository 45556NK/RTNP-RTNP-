package com.rtnp.demo.gpu.render.tools

import android.graphics.RectF
import android.opengl.GLES30
import com.rtnp.demo.gpu.CameraMatrix
import com.rtnp.demo.gpu.ShaderProgram
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SelectionBoxSystem {

    private var shader: ShaderProgram? = null

    // 青色线框
    private val boxColor = floatArrayOf(0f, 1f, 1f, 1f)

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

        shader = ShaderProgram.create(vert, frag)
            ?: throw RuntimeException("SelectionBox shader failed")
    }

    fun render(cameraMatrix: CameraMatrix, rect: RectF) {
        if (rect.width() <= 0 || rect.height() <= 0) return

        val s = shader ?: return
        s.use()
        s.setMat4("uMVPMatrix", cameraMatrix.getMVPMatrix())
        s.setVec4("uColor", boxColor)

        // 构建矩形四个顶点（世界坐标）
        val vertices = floatArrayOf(
            rect.left,  rect.top,
            rect.right, rect.top,
            rect.right, rect.bottom,
            rect.left,  rect.bottom
        )

        val buf = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices)
            .apply { position(0) }

        val posHandle = s.getAttribLocation("aPosition")
        if (posHandle == -1) return

        GLES30.glVertexAttribPointer(posHandle, 2, GLES30.GL_FLOAT, false, 0, buf)
        GLES30.glEnableVertexAttribArray(posHandle)

        // 设置线宽（注意：某些设备可能不支持宽线，但通常支持）
        GLES30.glLineWidth(3f)

        // 绘制线框
        GLES30.glDrawArrays(GLES30.GL_LINE_LOOP, 0, 4)

        GLES30.glDisableVertexAttribArray(posHandle)
    }
}