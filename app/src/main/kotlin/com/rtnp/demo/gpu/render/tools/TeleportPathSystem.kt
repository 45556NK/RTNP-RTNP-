package com.rtnp.demo.gpu.render.tools

import android.opengl.GLES30
import com.rtnp.demo.gpu.CameraMatrix
import com.rtnp.demo.gpu.ShaderProgram
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.*

object TeleportPathSystem {

    private var shader: ShaderProgram? = null
    private var circleBuffer: java.nio.FloatBuffer? = null
    private var circleVertexCount = 0

    private val instances = mutableListOf<TeleportPathInstance>()
    private val nextId = AtomicLong(0)

    private var currentTime = 0f

    data class TeleportPathInstance(
        val id: Long,
        val startX: Float,
        val startY: Float,
        val endX: Float,
        val endY: Float,
        val color: FloatArray,
        val pulseDuration: Float,
        val startMarkerRadius: Float = 0f
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

        val segments = 32
        val verts = mutableListOf<Float>()
        verts.add(0f); verts.add(0f)
        for (i in 0..segments) {
            val angle = 2.0 * PI * i / segments
            verts.add(cos(angle).toFloat())
            verts.add(sin(angle).toFloat())
        }
        circleVertexCount = verts.size / 2
        circleBuffer = ByteBuffer.allocateDirect(verts.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
            .put(verts.toFloatArray()).apply { position(0) }
    }

    @JvmStatic
    fun addTeleportPath(
        startX: Float, startY: Float,
        endX: Float, endY: Float,
        color: FloatArray,
        pulseDuration: Float,
        startMarkerRadius: Float = 0f
    ): Long {
        val id = nextId.incrementAndGet()
        instances.add(
            TeleportPathInstance(
                id, startX, startY, endX, endY,
                color.copyOf(), pulseDuration.coerceAtLeast(0.01f),
                startMarkerRadius
            )
        )
        return id
    }

    @JvmStatic
    fun removeTeleportPath(id: Long) {
        instances.removeAll { it.id == id }
    }

    @JvmStatic
    fun clear() {
        instances.clear()
        currentTime = 0f
    }

    fun update(deltaTime: Float) {
        currentTime += deltaTime.coerceAtMost(0.1f)
    }

    fun render(cameraMatrix: CameraMatrix, zoom: Float) {
        if (instances.isEmpty()) return
        val s = shader ?: return
        s.use()
        s.setMat4("uMVPMatrix", cameraMatrix.getMVPMatrix())

        val wasBlendEnabled = GLES30.glIsEnabled(GLES30.GL_BLEND)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        for (inst in instances) {
            drawPath(s, inst, zoom)
        }

        if (!wasBlendEnabled) {
            GLES30.glDisable(GLES30.GL_BLEND)
        }
    }

    private fun drawPath(s: ShaderProgram, inst: TeleportPathInstance, zoom: Float) {
        // 先绘制路径圆点
        val points = generatePathPoints(inst.startX, inst.startY, inst.endX, inst.endY)
        if (points.isNotEmpty()) {
            val count = points.size
            val pulsePeriod = inst.pulseDuration
            val activeIndex = floor(currentTime / pulsePeriod).toInt() % count
            val progress = (currentTime / pulsePeriod) % 1.0

            val scale: Float
            val alpha: Float
            if (progress <= 0.5f) {
                val p = progress / 0.5f
                scale = 1f + (1.5f - 1f) * p.toFloat()
                alpha = 0.4f + (0.9f - 0.4f) * p.toFloat()
            } else {
                val p = (progress - 0.5f) / 0.5f
                scale = 1.5f - (1.5f - 1f) * p.toFloat()
                alpha = 0.9f - (0.9f - 0.4f) * p.toFloat()
            }

            val worldRadius = (5f / zoom).coerceIn(6f, 24f)
            val circleBuf = circleBuffer ?: return
            circleBuf.position(0)
            val posHandle = s.getAttribLocation("aPosition")
            GLES30.glVertexAttribPointer(posHandle, 2, GLES30.GL_FLOAT, false, 0, circleBuf)
            GLES30.glEnableVertexAttribArray(posHandle)

            for (i in 0 until count) {
                val (px, py) = points[i]
                val r = worldRadius * if (i == activeIndex) scale else 1f
                val a = if (i == activeIndex) alpha else 0.4f
                val color = floatArrayOf(inst.color[0], inst.color[1], inst.color[2], a)
                s.setVec4("uColor", color)
                s.setVec2("uTranslation", floatArrayOf(px, py))
                s.setFloat("uScale", r)
                GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, circleVertexCount)
            }
            GLES30.glDisableVertexAttribArray(posHandle)
        }

        // 最后绘制起始标记
        if (inst.startMarkerRadius > 0f) {
            drawStartMarker(s, inst)
        }
    }

    private fun drawStartMarker(s: ShaderProgram, inst: TeleportPathInstance) {
        val cx = inst.startX
        val cy = inst.startY
        val radius = inst.startMarkerRadius + 1f

        val arcSweep = Math.PI / 3.0   // 1/6圆周 = 60度
        val speed = 2.0 * Math.PI / 1.8f  // 1.8秒一圈
        val baseAngle = (currentTime * speed) % (2.0 * Math.PI)

        val segStartAngles = listOf(0.0, 2.0 * Math.PI / 3.0, 4.0 * Math.PI / 3.0)

        for (startOffset in segStartAngles) {
            val start = baseAngle + startOffset
            RenderToolkit.drawThickArc(
                s, cx, cy, radius,
                2f,
                Math.toDegrees(start).toFloat(),
                Math.toDegrees(arcSweep).toFloat(),
                floatArrayOf(inst.color[0], inst.color[1], inst.color[2], 1f)
            )
        }
    }

    private fun generatePathPoints(startX: Float, startY: Float, endX: Float, endY: Float): List<Pair<Float, Float>> {
        val dx = endX - startX
        val dy = endY - startY
        val dist = sqrt(dx * dx + dy * dy)
        if (dist <= 0.0001f) return listOf(startX to startY)

        val step = 40f
        val points = mutableListOf<Pair<Float, Float>>()
        points.add(startX to startY)

        var traveled = step
        while (traveled < dist) {
            val t = traveled / dist
            points.add((startX + dx * t) to (startY + dy * t))
            traveled += step
        }
        return points
    }
}