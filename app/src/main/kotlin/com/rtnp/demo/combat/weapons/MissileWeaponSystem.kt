// app/src/main/kotlin/com/rtnp/demo/combat/weapons/MissileWeaponSystem.kt
package com.rtnp.demo.combat.weapons

import android.graphics.Color
import android.opengl.GLES30
import com.rtnp.demo.UnitSystem
import com.rtnp.demo.combat.WeaponConstants
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.gpu.CameraMatrix
import com.rtnp.demo.gpu.ShaderProgram
import com.rtnp.demo.movement.MissileSystem
import com.rtnp.demo.ui.panel.tool.CooldownRenderTool
import com.rtnp.demo.ui.panel.tool.WeaponButtonTool
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MissileWeaponSystem : WeaponSystem {

    private var shader: ShaderProgram? = null

    override fun initShader(vertSource: String, fragSource: String) {
        shader = ShaderProgram.create(vertSource, fragSource)
    }

    override fun update(
        attacker: PlacedObject,
        ws: PlacedObject.WeaponSlot,
        host: UnitSystem,
        fixedDt: Float,
        range: Float,
        target: PlacedObject?
    ) {
        if (ws.cooldown > 0) {
            val bar = ws.getOrCreateCooldownBar()
            bar.totalDuration = ws.cooldown
            bar.speedMultiplier = WeaponConstants.cooldownMultiplier
            bar.addProgress(fixedDt / ws.cooldown * WeaponConstants.cooldownMultiplier)
        }
        if (target == null) return
        val bar = ws.getOrCreateCooldownBar()
        val minCheck = if (ws.minRange > 0f) {
            val dx = target.worldX - attacker.worldX
            val dy = target.worldY - attacker.worldY
            (dx * dx + dy * dy) >= ws.minRange * ws.minRange
        } else true
        if (!bar.isFinished || !minCheck) return
        if (!host.placedObjects.contains(attacker) || !host.placedObjects.contains(target)) return
        if ((host.healthMap[target] ?: 0f) <= 0 || (host.healthMap[attacker] ?: 0f) <= 0) return
        if (host.missileSystem == null) {
            host.missileSystem = MissileSystem()
            host.missileSystem.setHost(host)
        }
        host.missileSystem.launchMissile(attacker, target)
        bar.reset()
        WeaponButtonTool.setCooldownConfig(ws, Color.argb(89, 255, 130, 130), CooldownRenderTool.Direction.BOTTOM_UP)
    }

    override fun getRenderData(attacker: PlacedObject, ws: PlacedObject.WeaponSlot): WeaponRenderData? = null

    override fun render(cameraMatrix: CameraMatrix, units: List<PlacedObject>, host: UnitSystem) {
        val ms = host.missileSystem ?: return
        val s = shader ?: return
        s.use()
        s.setMat4("uMVPMatrix", cameraMatrix.getMVPMatrix())
        for (m in ms.getMissiles().toList()) {
            if (m.isMoving) {
                drawLine(s, m.worldX, m.worldY, m.targetX, m.targetY, 0xFFFFFFFF.toInt())
            }
        }
    }

    private fun drawRect(s: ShaderProgram, cx: Float, cy: Float, size: Float, color: Int) {
        val h = size / 2f
        val v = floatArrayOf(cx - h, cy - h, 0f, cx + h, cy - h, 0f, cx - h, cy + h, 0f, cx + h, cy + h, 0f)
        draw(s, v, GLES30.GL_TRIANGLE_STRIP, color)
    }

    private fun drawLine(s: ShaderProgram, x1: Float, y1: Float, x2: Float, y2: Float, color: Int) {
        draw(s, floatArrayOf(x1, y1, 0f, x2, y2, 0f), GLES30.GL_LINES, color)
    }

    private fun drawCircle(s: ShaderProgram, cx: Float, cy: Float, r: Float, color: Int) {
        if (r <= 0) return
        val seg = 32
        val v = mutableListOf<Float>()
        for (i in 0..seg) {
            val angle = 2.0 * Math.PI * i / seg
            v.add(cx + r * Math.cos(angle).toFloat())
            v.add(cy + r * Math.sin(angle).toFloat())
            v.add(0f)
        }
        draw(s, v.toFloatArray(), GLES30.GL_LINE_LOOP, color)
    }

    private fun draw(s: ShaderProgram, vertices: FloatArray, mode: Int, color: Int) {
        val buf = ByteBuffer.allocateDirect(vertices.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(vertices).apply { position(0) }
        val pos = s.getAttribLocation("aPosition")
        GLES30.glVertexAttribPointer(pos, 3, GLES30.GL_FLOAT, false, 0, buf)
        GLES30.glEnableVertexAttribArray(pos)
        val r = ((color shr 16) and 0xFF) / 255f
        val g = ((color shr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        val a = ((color shr 24) and 0xFF) / 255f
        s.setVec4("uColor", floatArrayOf(r, g, b, a))
        GLES30.glDrawArrays(mode, 0, vertices.size / 3)
        GLES30.glDisableVertexAttribArray(pos)
    }
}