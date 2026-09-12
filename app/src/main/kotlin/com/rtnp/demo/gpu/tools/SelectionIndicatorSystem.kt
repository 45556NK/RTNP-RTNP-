package com.rtnp.demo.gpu.render.tools

import android.opengl.GLES30
import com.rtnp.demo.UnitSystem
import com.rtnp.demo.core.GameConstants
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.gpu.CameraMatrix
import com.rtnp.demo.gpu.ShaderProgram
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SelectionIndicatorSystem {

    private var shader: ShaderProgram? = null
    private var vertexBuffer: java.nio.FloatBuffer? = null

    // 三角形顶点：上顶点为定位点 (0,0)，向右下和左下延伸
    private val triangleVertices = floatArrayOf(
         0.0f,  0.0f,   // v0: 尖
         0.866f, 1.5f,   // v1: 右下
        -0.866f, 1.5f    // v2: 左下
    )

    fun init() {
        val vert = """
            #version 300 es
            in vec2 aPosition;
            uniform mat4 uMVPMatrix;
            uniform vec2 uTranslation;
            uniform float uScale;
            out vec2 vLocalPos;
            void main() {
                vec2 pos = aPosition * uScale + uTranslation;
                vLocalPos = aPosition;
                gl_Position = uMVPMatrix * vec4(pos, 0.0, 1.0);
            }
        """.trimIndent()

        val frag = """
            #version 300 es
            precision mediump float;
            in vec2 vLocalPos;
            out vec4 fragColor;
            uniform vec3 uColor;
            uniform float uGlowRadius;
            uniform float uBaseAlpha;
            uniform float uTime;
            
            void main() {
                // 三角形顶点（与传入的顶点数据完全一致）
                vec2 v0 = vec2(0.0, 0.0);
                vec2 v1 = vec2(0.866, 1.5);
                vec2 v2 = vec2(-0.866, 1.5);
                
                // 正确的重心坐标计算：每条边对应的叉积
                // d0: 边 v1v2 与点 P 的关系 = (v1 - v0) × (P - v0)
                // d1: 边 v2v0 与点 P 的关系 = (v2 - v1) × (P - v1)
                // d2: 边 v0v1 与点 P 的关系 = (v0 - v2) × (P - v2)
                float d0 = (v1.x - v0.x)*(vLocalPos.y - v0.y) - (v1.y - v0.y)*(vLocalPos.x - v0.x);
                float d1 = (v2.x - v1.x)*(vLocalPos.y - v1.y) - (v2.y - v1.y)*(vLocalPos.x - v1.x);
                float d2 = (v0.x - v2.x)*(vLocalPos.y - v2.y) - (v0.y - v2.y)*(vLocalPos.x - v2.x);
                
                // 逆时针三角形内部点三个值都 >= 0
                bool inside = (d0 >= 0.0 && d1 >= 0.0 && d2 >= 0.0);
                
                // 计算点到三条边的垂直距离（用于光晕）
                float dist0 = abs(d0) / length(v1 - v0);
                float dist1 = abs(d1) / length(v2 - v1);
                float dist2 = abs(d2) / length(v0 - v2);
                float minDist = min(min(dist0, dist1), dist2);
                
                float alpha;
                if (inside) {
                    // 三角形内部：完全实心
                    alpha = 1.0;
                } else {
                    // 外部光晕：距离越远越透明（向外扩散）
                    float glow = 1.0 - minDist / uGlowRadius;
                    glow = clamp(glow, 0.0, 1.0);
                    float breathe = 0.8 + 0.2 * sin(uTime * 3.0);
                    alpha = uBaseAlpha * glow * breathe;
                }
                
                if (alpha < 0.01) discard;
                fragColor = vec4(uColor, alpha);
            }
        """.trimIndent()

        shader = ShaderProgram.create(vert, frag)
            ?: throw RuntimeException("SelectionIndicator shader failed")

        vertexBuffer = ByteBuffer.allocateDirect(triangleVertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(triangleVertices)
            .apply { position(0) }
    }

    fun render(
        cameraMatrix: CameraMatrix,
        units: List<PlacedObject>,
        unitSystem: UnitSystem?,
        zoom: Float
    ) {
        if (unitSystem == null) return

        val s = shader ?: return
        s.use()
        val vp = vertexBuffer ?: return
        vp.position(0)
        val posHandle = s.getAttribLocation("aPosition")
        if (posHandle == -1) return

        GLES30.glVertexAttribPointer(posHandle, 2, GLES30.GL_FLOAT, false, 0, vp)
        GLES30.glEnableVertexAttribArray(posHandle)

        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        val mvp = cameraMatrix.getMVPMatrix()
        s.setMat4("uMVPMatrix", mvp)

        val selectedUnit = unitSystem.selectedUnit
        val multiSelUnits = if (unitSystem.multiSelectManager?.isActive() == true) {
            unitSystem.multiSelectManager.getSelectedUnits()
        } else emptySet()

        val time = System.currentTimeMillis() / 1000f

        for (unit in units) {
            if (!unit.isSelected) continue
            if (unit == selectedUnit || unit in multiSelUnits) {
                val ref = unit.referenceVolume
                val baseRadius = if (ref != null && ref.radius > 0) {
                    ref.radius
                } else {
                    maxOf(unit.width.toFloat(), unit.height.toFloat(), unit.size.toFloat()) / 2f
                }

                val breathScale = getBreathScale()
                val triangleSize = baseRadius * 2f / 3f * breathScale

                // 定位点 = 单位正下方，偏移 = 参考体积半径 + 10像素
                val pixelOffset = 10f / zoom
                val tipX = unit.worldX
                val tipY = unit.worldY + baseRadius + pixelOffset

                val color = getFactionColor(unit.faction)

                // 光晕扩散半径（世界单位转归一化）
                val glowPixel = 60f / zoom
                val glowNorm = (glowPixel / triangleSize).coerceIn(0.15f, 1.5f)
                val baseAlpha = 0.5f

                s.setVec3("uColor", color)
                s.setFloat("uGlowRadius", glowNorm)
                s.setFloat("uBaseAlpha", baseAlpha)
                s.setFloat("uTime", time)
                s.setVec2("uTranslation", floatArrayOf(tipX, tipY))
                s.setFloat("uScale", triangleSize)

                GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 3)
            }
        }

        GLES30.glDisableVertexAttribArray(posHandle)
    }

    private fun getFactionColor(faction: Int): FloatArray {
        return when (faction) {
            GameConstants.FACTION_PLAYER -> floatArrayOf(0f, 100f/255f, 1f)
            GameConstants.FACTION_ENEMY -> floatArrayOf(220f/255f, 20f/255f, 60f/255f)
            else -> floatArrayOf(0.5f, 0.5f, 0.5f)
        }
    }

    private fun getBreathScale(): Float {
        val elapsed = System.currentTimeMillis() % 1000L
        val phase = elapsed / 1000f
        return if (phase <= 0.5f) 1.0f + (phase / 0.5f) * 0.25f
        else 1.25f - ((phase - 0.5f) / 0.5f) * 0.25f
    }

    private fun ShaderProgram.setVec3(name: String, value: FloatArray) {
        GLES30.glUniform3fv(getUniformLocation(name), 1, value, 0)
    }
}