package com.rtnp.demo.combat.weapons

import com.rtnp.demo.UnitSystem
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.gpu.CameraMatrix
import com.rtnp.demo.gpu.ShaderProgram

interface WeaponSystem {
    /** 更新武器逻辑 */
    fun update(
        attacker: PlacedObject,
        ws: PlacedObject.WeaponSlot,
        host: UnitSystem,
        fixedDt: Float,
        range: Float,
        target: PlacedObject?
    )

    /** 初始化着色器（外部调用，在GL上下文就绪后） */
    fun initShader(vertSource: String, fragSource: String)

    /** 获取此系统当前帧所有实例的渲染数据（供调试） */
    fun getRenderData(attacker: PlacedObject, ws: PlacedObject.WeaponSlot): WeaponRenderData?

    /** 执行GPU渲染（由武器显示层调用） */
    fun render(cameraMatrix: CameraMatrix, units: List<PlacedObject>, host: UnitSystem)
}

data class WeaponRenderData(
    val type: String,
    val startX: Float, val startY: Float,
    val endX: Float?, val endY: Float?,
    val color: Int,
    val isMoving: Boolean = false,
    val extra: Map<String, Any> = emptyMap()
)