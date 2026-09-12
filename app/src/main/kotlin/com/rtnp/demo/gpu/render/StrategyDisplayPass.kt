package com.rtnp.demo.gpu.render

import com.rtnp.demo.UnitSystem
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.gpu.CameraMatrix
import com.rtnp.demo.gpu.render.tools.ProgressCircleSystem
import com.rtnp.demo.gpu.render.tools.StrategyRenderSystem
import com.rtnp.demo.gpu.render.tools.TeleportPathSystem
import com.rtnp.demo.strategy.StrategyManager

class StrategyDisplayPass {

    fun init() {
        StrategyRenderSystem.init()
        ProgressCircleSystem.init()
        TeleportPathSystem.init()
    }

    fun draw(
        cameraMatrix: CameraMatrix,
        deltaTime: Float,
        zoom: Float,
        units: List<PlacedObject>,
        unitSystem: UnitSystem?
    ) {
        ProgressCircleSystem.update(deltaTime)
        ProgressCircleSystem.render(cameraMatrix)

        TeleportPathSystem.update(deltaTime)
        TeleportPathSystem.render(cameraMatrix, zoom)

        StrategyRenderSystem.clearCommands()
        if (unitSystem != null) {
            StrategyManager.renderGpuStrategies(units, StrategyRenderSystem)
        }
        StrategyRenderSystem.render(cameraMatrix)
    }
}