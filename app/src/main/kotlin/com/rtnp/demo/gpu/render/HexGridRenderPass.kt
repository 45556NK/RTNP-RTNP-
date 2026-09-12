package com.rtnp.demo.gpu.render

import android.opengl.GLES30
import com.rtnp.demo.gpu.ShaderProgram
import com.rtnp.demo.gpu.CameraMatrix
import com.rtnp.demo.logic.HexGridManager
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class HexGridRenderPass {
    private var shader: ShaderProgram? = null
    private var vertexBuffer: FloatBuffer? = null
    private var vertexCount: Int = 0

    // 六边形颜色：半透明蓝色 (0, 100, 255, 128)
    private val gridColor = floatArrayOf(0f, 100f / 255f, 1f, 128f / 255f)

    fun init() {
        val vertexShaderCode = """
            #version 300 es
            layout(location = 0) in vec4 aPosition;
            uniform mat4 uMVPMatrix;
            void main() {
                gl_Position = uMVPMatrix * aPosition;
            }
        """.trimIndent()

        val fragmentShaderCode = """
            #version 300 es
            precision mediump float;
            out vec4 fragColor;
            uniform vec4 uColor;
            void main() {
                fragColor = uColor;
            }
        """.trimIndent()

        shader = ShaderProgram.create(vertexShaderCode, fragmentShaderCode)
            ?: throw RuntimeException("Failed to create hex grid shader")

        buildHexLines()
    }

    private fun buildHexLines() {
        val tiles = HexGridManager.getAllTiles()
        if (tiles.isEmpty()) return

        val verticesPerHex = 6
        val floatsPerVertex = 3 // x, y, z (z=0)
        val totalFloats = tiles.size * verticesPerHex * floatsPerVertex

        val buffer = ByteBuffer.allocateDirect(totalFloats * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        for (tile in tiles) {
            val cx = tile.worldX
            val cy = tile.worldY
            val side = HexGridManager.hexSide

            for (i in 0 until 6) {
                val angle = Math.PI / 3.0 * i - Math.PI / 2.0
                val x = cx + side * Math.cos(angle).toFloat()
                val y = cy + side * Math.sin(angle).toFloat()
                buffer.put(x)
                buffer.put(y)
                buffer.put(0f)
            }
        }

        buffer.position(0)
        vertexBuffer = buffer
        vertexCount = tiles.size * verticesPerHex
    }

    fun draw(cameraMatrix: CameraMatrix) {
        if (vertexCount == 0) return

        shader?.use()

        // 传入 MVP 矩阵
        shader?.setMat4("uMVPMatrix", cameraMatrix.getMVPMatrix())

        // 传入颜色
        shader?.setVec4("uColor", gridColor)
        GLES30.glLineWidth(3f)  // 固定3像素

        vertexBuffer?.let { buf ->
            buf.position(0)
            val posHandle = shader?.getAttribLocation("aPosition") ?: return
            GLES30.glVertexAttribPointer(posHandle, 3, GLES30.GL_FLOAT, false, 0, buf)
            GLES30.glEnableVertexAttribArray(posHandle)

            // 每个六边形6个顶点，绘制为连续的线段（不闭合，需要最后画一条回到第一个顶点的线）
            // 但我们的数据每个六边形是独立的6个顶点，如果用 LINE_LOOP 会导致所有六边形连在一起。
            // 解决方案：每个六边形使用 glDrawArrays(GL_LINE_LOOP, offset, 6)
            for (i in 0 until vertexCount / 6) {
                GLES30.glDrawArrays(GLES30.GL_LINE_LOOP, i * 6, 6)
            }

            GLES30.glDisableVertexAttribArray(posHandle)
        }
    }
}