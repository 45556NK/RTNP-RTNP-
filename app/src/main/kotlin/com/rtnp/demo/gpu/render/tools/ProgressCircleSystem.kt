package com.rtnp.demo.gpu.render.tools

import android.opengl.GLES30
import com.rtnp.demo.gpu.CameraMatrix
import com.rtnp.demo.gpu.ShaderProgram
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.*

object ProgressCircleSystem {

    private var shader: ShaderProgram? = null
    private var fullCircleBuffer: java.nio.FloatBuffer? = null
    private var fullCircleVertCount = 0

    private val instances = mutableListOf<ProgressCircle>()
    private val nextId = AtomicLong(0)

    data class ProgressCircle(
        val id: Long,
        var cx: Float,
        var cy: Float,
        val radius: Float,
        val bgColor: FloatArray,
        val progressColor: FloatArray,
        var progress: Float,
        val duration: Float,
        val thickness: Float,
        val onFinished: (() -> Unit)?
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

        val segments = 64
        val fullVerts = mutableListOf<Float>()
        fullVerts.add(0f); fullVerts.add(0f)
        for (i in 0..segments) {
            val angle = 2.0 * PI * i / segments
            fullVerts.add(cos(angle).toFloat())
            fullVerts.add(sin(angle).toFloat())
        }
        fullCircleVertCount = fullVerts.size / 2
        fullCircleBuffer = ByteBuffer.allocateDirect(fullVerts.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
            .put(fullVerts.toFloatArray()).apply { position(0) }
    }

    @JvmStatic
    @JvmOverloads
    fun addProgressCircle(
        cx: Float, cy: Float, radius: Float,
        bgColor: FloatArray, progressColor: FloatArray,
        duration: Float = 1f,
        thickness: Float = 6f,
        onFinished: (() -> Unit)? = null
    ): Long {
        val dur = if (duration <= 0f) 1f else duration
        val id = nextId.incrementAndGet()
        instances.add(ProgressCircle(id, cx, cy, radius, bgColor, progressColor, 0f, dur, thickness, onFinished))
        return id
    }

    @JvmStatic
    fun removeProgressCircle(id: Long) {
        instances.removeAll { it.id == id }
    }

    fun updateCirclePosition(id: Long, cx: Float, cy: Float) {
        instances.find { it.id == id }?.let { it.cx = cx; it.cy = cy }
    }

    fun update(deltaTime: Float) {
        val dt = deltaTime.coerceAtMost(0.1f)
        val finished = mutableListOf<Long>()
        for (inst in instances.toList()) {
            if (inst.progress >= 1f) continue
            inst.progress += dt / inst.duration
            if (inst.progress >= 1f) {
                inst.progress = 1f
                finished.add(inst.id)
            }
        }
        for (id in finished) {
            val inst = instances.find { it.id == id } ?: continue
            inst.onFinished?.invoke()
            instances.remove(inst)
        }
    }

    fun render(cameraMatrix: CameraMatrix) {
        if (instances.isEmpty()) return
        val s = shader ?: return
        s.use()
        s.setMat4("uMVPMatrix", cameraMatrix.getMVPMatrix())

        val wasBlendEnabled = GLES30.glIsEnabled(GLES30.GL_BLEND)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        val fullBuf = fullCircleBuffer ?: return
        fullBuf.position(0)
        val posHandle = s.getAttribLocation("aPosition")
        GLES30.glVertexAttribPointer(posHandle, 2, GLES30.GL_FLOAT, false, 0, fullBuf)
        GLES30.glEnableVertexAttribArray(posHandle)

        for (inst in instances) {
            if (inst.bgColor[3] > 0.001f) {
                s.setVec4("uColor", inst.bgColor)
                s.setVec2("uTranslation", floatArrayOf(inst.cx, inst.cy))
                s.setFloat("uScale", inst.radius)
                GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, fullCircleVertCount)
            }
        }

        for (inst in instances) {
            if (inst.progress <= 0f) continue
            val outerR = inst.radius + inst.thickness / 2f
            val innerR = inst.radius - inst.thickness / 2f
            val segments = 64
            val maxAngle = -PI / 2.0 + 2.0 * PI * inst.progress.coerceIn(0f, 1f)
            val startAngle = -PI / 2.0

            val vertCount = (segments * inst.progress).toInt().coerceAtLeast(1) + 1
            val stripVerts = FloatArray(vertCount * 4)
            var idx = 0
            for (i in 0 until vertCount) {
                val t = i.toFloat() / segments
                val angle = startAngle + (maxAngle - startAngle) * t
                val cosA = cos(angle).toFloat()
                val sinA = sin(angle).toFloat()
                stripVerts[idx++] = cosA * outerR
                stripVerts[idx++] = sinA * outerR
                stripVerts[idx++] = cosA * innerR
                stripVerts[idx++] = sinA * innerR
            }
            val stripBuf = ByteBuffer.allocateDirect(stripVerts.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(stripVerts)
            stripBuf.position(0)

            GLES30.glVertexAttribPointer(posHandle, 2, GLES30.GL_FLOAT, false, 0, stripBuf)
            s.setVec4("uColor", inst.progressColor)
            s.setVec2("uTranslation", floatArrayOf(inst.cx, inst.cy))
            s.setFloat("uScale", 1f)
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, vertCount * 2)
        }

        GLES30.glDisableVertexAttribArray(posHandle)

        if (!wasBlendEnabled) {
            GLES30.glDisable(GLES30.GL_BLEND)
        }
    }
}