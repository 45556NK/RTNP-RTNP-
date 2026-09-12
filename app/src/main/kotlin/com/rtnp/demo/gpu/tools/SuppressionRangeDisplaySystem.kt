// app/src/main/kotlin/com/rtnp/demo/gpu/render/tools/SuppressionRangeDisplaySystem.kt
package com.rtnp.demo.gpu.render.tools

import android.opengl.GLES30
import com.rtnp.demo.gpu.CameraMatrix
import com.rtnp.demo.gpu.ShaderProgram
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object SuppressionRangeDisplaySystem {

    private var shader: ShaderProgram? = null
    private var discVertexBuffer: java.nio.FloatBuffer? = null
    private val circleSegments = 64

    // 圆盘颜色：与爆炸范围底相同 (r255, g131, b131, a86)
    private val discColor = floatArrayOf(1f, 131f / 255f, 131f / 255f, 86f / 255f)
    // 圆环颜色：更深的红色 (r138, g0, b0, a255)
    private val ringColor = floatArrayOf(138f / 255f, 0f, 0f, 1f)

    private val nextId = AtomicLong(0)
    private const val MAX_PER_POSITION = 2
    private val suppressionRanges = mutableMapOf<Long, SuppressionRange>()

    private var initialized = false

    fun init() {

        val vert = """
            #version 300 es
            in vec2 aPosition;
            uniform mat4 uMVPMatrix;
            uniform vec2 uCenter;
            uniform float uRadius;
            void main() {
                vec2 pos = aPosition * uRadius + uCenter;
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

        val verts = mutableListOf<Float>()
        verts.add(0f); verts.add(0f)
        for (i in 0..circleSegments) {
            val angle = 2.0 * Math.PI * i / circleSegments
            verts.add(cos(angle).toFloat())
            verts.add(sin(angle).toFloat())
        }
        discVertexBuffer = ByteBuffer.allocateDirect(verts.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
            .put(verts.toFloatArray()).apply { position(0) }
    }

    /**
     * 添加压制范围显示
     * @param worldX 压制范围中心世界X坐标
     * @param worldY 压制范围中心世界Y坐标
     * @param radius 压制范围半径（世界单位）
     * @return 压制范围ID，用于后续更新位置或移除，-1表示添加失败
     */
    @JvmStatic
    fun addSuppressionRange(worldX: Float, worldY: Float, radius: Float): Long {
        val samePositionCount = suppressionRanges.values.count { range ->
            val dx = range.worldX - worldX
            val dy = range.worldY - worldY
            sqrt(dx * dx + dy * dy) < 10f
        }
        
        if (samePositionCount >= MAX_PER_POSITION) {
            return -1L
        }
        
        val id = nextId.incrementAndGet()
        suppressionRanges[id] = SuppressionRange(worldX, worldY, radius)
        return id
    }

    /**
     * 更新压制范围的位置（用于跟随导弹移动）
     */
    @JvmStatic
    fun updateSuppressionRange(id: Long, worldX: Float, worldY: Float) {
        suppressionRanges[id]?.let {
            it.worldX = worldX
            it.worldY = worldY
        }
    }

    /**
     * 移除指定ID的压制范围显示
     */
    @JvmStatic
    fun removeSuppressionRange(id: Long) {
        suppressionRanges.remove(id)
    }

    /**
     * 清除所有压制范围显示
     */
    @JvmStatic
    fun clear() {
        suppressionRanges.clear()
    }

    fun render(cameraMatrix: CameraMatrix) {
        if (suppressionRanges.isEmpty()) return

        val mvp = cameraMatrix.getMVPMatrix()
        val s = shader ?: return
        val buf = discVertexBuffer ?: return

        // 保存当前OpenGL状态
        val prevLineWidth = FloatArray(1)
        GLES30.glGetFloatv(GLES30.GL_LINE_WIDTH, prevLineWidth, 0)
        val blendEnabled = GLES30.glIsEnabled(GLES30.GL_BLEND)

        s.use()
        buf.position(0)
        val posHandle = s.getAttribLocation("aPosition")
        if (posHandle == -1) return

        GLES30.glVertexAttribPointer(posHandle, 2, GLES30.GL_FLOAT, false, 0, buf)
        GLES30.glEnableVertexAttribArray(posHandle)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        GLES30.glLineWidth(3f)
        s.setMat4("uMVPMatrix", mvp)

        for ((_, range) in suppressionRanges) {
            // 圆盘
            s.setVec4("uColor", discColor)
            s.setVec2("uCenter", floatArrayOf(range.worldX, range.worldY))
            s.setFloat("uRadius", range.radius)
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, 2 + circleSegments)

            // 圆环
            s.setVec4("uColor", ringColor)
            s.setFloat("uRadius", range.radius + 1f)
            GLES30.glDrawArrays(GLES30.GL_LINE_LOOP, 1, circleSegments)
        }

        GLES30.glDisableVertexAttribArray(posHandle)
        GLES30.glLineWidth(prevLineWidth[0])
        if (!blendEnabled) {
            GLES30.glDisable(GLES30.GL_BLEND)
        }
    }

    private data class SuppressionRange(
        var worldX: Float,
        var worldY: Float,
        val radius: Float
    )
}