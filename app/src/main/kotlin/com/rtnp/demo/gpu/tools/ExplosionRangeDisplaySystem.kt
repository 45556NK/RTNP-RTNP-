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

object ExplosionRangeDisplaySystem {

    private var shader: ShaderProgram? = null
    private var discVertexBuffer: java.nio.FloatBuffer? = null
    private val circleSegments = 64

    private val discColor = floatArrayOf(1f, 131f / 255f, 131f / 255f, 86f / 255f)
    private val ringColor = floatArrayOf(138f / 255f, 0f, 0f, 1f)

    private val nextId = AtomicLong(0)
    private const val MAX_PER_POSITION = 2
    private val explosionRanges = mutableMapOf<Long, ExplosionRange>()

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

    @JvmStatic
    fun addExplosionRange(worldX: Float, worldY: Float, radius: Float): Long {
        val samePositionCount = explosionRanges.values.count { range ->
            val dx = range.worldX - worldX
            val dy = range.worldY - worldY
            sqrt(dx * dx + dy * dy) < 10f
        }
        
        if (samePositionCount >= MAX_PER_POSITION) {
            return -1L
        }
        
        val id = nextId.incrementAndGet()
        explosionRanges[id] = ExplosionRange(worldX, worldY, radius)
        return id
    }

    @JvmStatic
    fun removeExplosionRange(id: Long) {
        explosionRanges.remove(id)
    }

    @JvmStatic
    fun clear() {
        explosionRanges.clear()
    }

    fun render(cameraMatrix: CameraMatrix) {
        if (explosionRanges.isEmpty()) return

        val mvp = cameraMatrix.getMVPMatrix()
        val s = shader ?: return
        val buf = discVertexBuffer ?: return

        // ★ 保存当前状态
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

        for ((_, range) in explosionRanges) {
            s.setVec4("uColor", discColor)
            s.setVec2("uCenter", floatArrayOf(range.worldX, range.worldY))
            s.setFloat("uRadius", range.radius)
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, 2 + circleSegments)

            s.setVec4("uColor", ringColor)
            s.setFloat("uRadius", range.radius + 1f)
            GLES30.glDrawArrays(GLES30.GL_LINE_LOOP, 1, circleSegments)
        }

        GLES30.glDisableVertexAttribArray(posHandle)

        // ★ 恢复之前的状态
        GLES30.glLineWidth(prevLineWidth[0])
        if (!blendEnabled) {
            GLES30.glDisable(GLES30.GL_BLEND)
        }
    }

    private data class ExplosionRange(
        val worldX: Float,
        val worldY: Float,
        val radius: Float
    )
}