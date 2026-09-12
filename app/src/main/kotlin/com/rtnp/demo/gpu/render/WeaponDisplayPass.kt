package com.rtnp.demo.gpu.render

import com.rtnp.demo.UnitSystem
import com.rtnp.demo.combat.WeaponLoop
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.gpu.CameraMatrix
import com.rtnp.demo.gpu.effect.ShaderToolRegistry

class WeaponDisplayPass {

    private var initialized = false

    fun init() {
        val systems = listOf(
            WeaponLoop.pulseSystem,
            WeaponLoop.missileSystem,
            WeaponLoop.laserSystem,
            WeaponLoop.railgunSystem
        )
        for (sys in systems) {
            val name = sys::class.simpleName?.removeSuffix("WeaponSystem")?.lowercase() ?: "default"
            var vert = ShaderToolRegistry.getVertexShader("weapon_$name") ?: defaultVert()
            var frag = ShaderToolRegistry.getFragmentShader("weapon_$name") ?: defaultFrag()
            sys.initShader(vert, frag)
        }
        initialized = true
    }

    fun draw(units: List<PlacedObject>, host: UnitSystem, cameraMatrix: CameraMatrix) {
        if (!initialized) return
        // 各武器系统自行渲染
        WeaponLoop.pulseSystem.render(cameraMatrix, units, host)
        WeaponLoop.laserSystem.render(cameraMatrix, units, host)
        WeaponLoop.missileSystem.render(cameraMatrix, units, host)
        WeaponLoop.railgunSystem.render(cameraMatrix, units, host)
    }

    private fun defaultVert() = """
        #version 300 es
        layout(location = 0) in vec4 aPosition;
        uniform mat4 uMVPMatrix;
        void main() {
            gl_Position = uMVPMatrix * aPosition;
        }
    """.trimIndent()

    private fun defaultFrag() = """
        #version 300 es
        precision mediump float;
        out vec4 fragColor;
        uniform vec4 uColor;
        void main() {
            fragColor = uColor;
        }
    """.trimIndent()
}