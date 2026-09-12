package com.rtnp.demo.gpu.render.tools

import android.opengl.GLES30
import com.rtnp.demo.gpu.ShaderProgram
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.cos
import kotlin.math.sin

object RenderToolkit {

    private val circleVertexBuffer: java.nio.FloatBuffer by lazy {
        val verts = mutableListOf<Float>()
        verts.add(0f); verts.add(0f)
        for (i in 0..64) {
            val angle = 2.0 * Math.PI * i / 64
            verts.add(cos(angle).toFloat())
            verts.add(sin(angle).toFloat())
        }
        ByteBuffer.allocateDirect(verts.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
            .put(verts.toFloatArray()).apply { position(0) }
    }

    fun drawCircle(s: ShaderProgram, cx: Float, cy: Float, r: Float, color: FloatArray) {
        val buf = circleVertexBuffer
        buf.position(0)
        val posHandle = s.getAttribLocation("aPosition")
        GLES30.glVertexAttribPointer(posHandle, 2, GLES30.GL_FLOAT, false, 0, buf)
        GLES30.glEnableVertexAttribArray(posHandle)
        s.setVec4("uColor", color)
        s.setVec2("uTranslation", floatArrayOf(cx, cy))
        s.setFloat("uScale", r)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, 66)
        GLES30.glDisableVertexAttribArray(posHandle)
    }

    fun drawCircleOutline(s: ShaderProgram, cx: Float, cy: Float, r: Float, color: FloatArray, strokeWidth: Float = 2f) {
        // 保留旧方法，但内部调用新的三角形环，保证粗细可靠
        drawThickCircleOutline(s, cx, cy, r, strokeWidth, color)
    }

    fun drawLine(s: ShaderProgram, x1: Float, y1: Float, x2: Float, y2: Float, color: FloatArray, width: Float = 2f) {
        val verts = floatArrayOf(x1, y1, x2, y2)
        val buf = ByteBuffer.allocateDirect(verts.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(verts).apply { position(0) }
        val posHandle = s.getAttribLocation("aPosition")
        GLES30.glVertexAttribPointer(posHandle, 2, GLES30.GL_FLOAT, false, 0, buf)
        GLES30.glEnableVertexAttribArray(posHandle)
        s.setVec4("uColor", color)
        s.setVec2("uTranslation", floatArrayOf(0f, 0f))
        s.setFloat("uScale", 1f)
        GLES30.glLineWidth(width)
        GLES30.glDrawArrays(GLES30.GL_LINES, 0, 2)
        GLES30.glDisableVertexAttribArray(posHandle)
    }

    fun drawTriangle(s: ShaderProgram, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, color: FloatArray) {
        val verts = floatArrayOf(x1, y1, x2, y2, x3, y3)
        val buf = ByteBuffer.allocateDirect(verts.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(verts).apply { position(0) }
        val posHandle = s.getAttribLocation("aPosition")
        GLES30.glVertexAttribPointer(posHandle, 2, GLES30.GL_FLOAT, false, 0, buf)
        GLES30.glEnableVertexAttribArray(posHandle)
        s.setVec4("uColor", color)
        s.setVec2("uTranslation", floatArrayOf(0f, 0f))
        s.setFloat("uScale", 1f)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 3)
        GLES30.glDisableVertexAttribArray(posHandle)
    }

    /** 绘制轴对齐矩形（填充），顶点为世界坐标 */
    fun drawRect(s: ShaderProgram, left: Float, top: Float, right: Float, bottom: Float, color: FloatArray) {
        val verts = floatArrayOf(
            left, top,
            right, top,
            left, bottom,
            right, bottom
        )
        val buf = ByteBuffer.allocateDirect(verts.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(verts).apply { position(0) }
        val posHandle = s.getAttribLocation("aPosition")
        GLES30.glVertexAttribPointer(posHandle, 2, GLES30.GL_FLOAT, false, 0, buf)
        GLES30.glEnableVertexAttribArray(posHandle)
        s.setVec4("uColor", color)
        s.setVec2("uTranslation", floatArrayOf(0f, 0f))
        s.setFloat("uScale", 1f)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glDisableVertexAttribArray(posHandle)
    }

    /** 绘制圆弧（描边），从 startAngle 开始，顺时针 sweepAngle 度，角度为度数 */
    fun drawArc(s: ShaderProgram, cx: Float, cy: Float, r: Float, startAngle: Float, sweepAngle: Float, color: FloatArray, width: Float = 6f) {
        // 保留旧方法，内部改为三角形弧
        drawThickArc(s, cx, cy, r, width, startAngle, sweepAngle, color)
    }

    /**
     * 使用三角形条带绘制指定世界宽度的圆环
     * @param strokeWidth 世界单位，实际像素宽度 = strokeWidth * zoom
     */
    fun drawThickCircleOutline(s: ShaderProgram, cx: Float, cy: Float, radius: Float, strokeWidth: Float, color: FloatArray) {
        val segs = 64
        val halfWidth = strokeWidth / 2f
        val verts = ArrayList<Float>(segs * 4 + 4)
        for (i in 0..segs) {
            val angle = 2.0 * Math.PI * i / segs
            val cosA = cos(angle).toFloat()
            val sinA = sin(angle).toFloat()
            val outerX = cx + (radius + halfWidth) * cosA
            val outerY = cy + (radius + halfWidth) * sinA
            val innerX = cx + (radius - halfWidth) * cosA
            val innerY = cy + (radius - halfWidth) * sinA
            verts.add(outerX); verts.add(outerY)
            verts.add(innerX); verts.add(innerY)
        }
        val buf = ByteBuffer.allocateDirect(verts.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(verts.toFloatArray()).apply { position(0) }
        val posHandle = s.getAttribLocation("aPosition")
        GLES30.glVertexAttribPointer(posHandle, 2, GLES30.GL_FLOAT, false, 0, buf)
        GLES30.glEnableVertexAttribArray(posHandle)
        s.setVec4("uColor", color)
        s.setVec2("uTranslation", floatArrayOf(0f, 0f))
        s.setFloat("uScale", 1f)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, (segs + 1) * 2)
        GLES30.glDisableVertexAttribArray(posHandle)
    }

    /**
     * 使用三角形条带绘制指定世界宽度的圆弧
     */
    fun drawThickArc(s: ShaderProgram, cx: Float, cy: Float, radius: Float, strokeWidth: Float, startAngle: Float, sweepAngle: Float, color: FloatArray) {
        val segs = 64
        val halfWidth = strokeWidth / 2f
        val startRad = Math.toRadians(startAngle.toDouble())
        val sweepRad = Math.toRadians(sweepAngle.toDouble())
        val verts = ArrayList<Float>()
        for (i in 0..segs) {
            val theta = startRad + sweepRad * i / segs
            val cosA = cos(theta).toFloat()
            val sinA = sin(theta).toFloat()
            val outerX = cx + (radius + halfWidth) * cosA
            val outerY = cy + (radius + halfWidth) * sinA
            val innerX = cx + (radius - halfWidth) * cosA
            val innerY = cy + (radius - halfWidth) * sinA
            verts.add(outerX); verts.add(outerY)
            verts.add(innerX); verts.add(innerY)
        }
        val buf = ByteBuffer.allocateDirect(verts.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(verts.toFloatArray()).apply { position(0) }
        val posHandle = s.getAttribLocation("aPosition")
        GLES30.glVertexAttribPointer(posHandle, 2, GLES30.GL_FLOAT, false, 0, buf)
        GLES30.glEnableVertexAttribArray(posHandle)
        s.setVec4("uColor", color)
        s.setVec2("uTranslation", floatArrayOf(0f, 0f))
        s.setFloat("uScale", 1f)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, (segs + 1) * 2)
        GLES30.glDisableVertexAttribArray(posHandle)
    }
}