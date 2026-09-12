package com.rtnp.demo.gpu.render

import com.rtnp.demo.UnitSystem
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.gpu.CameraMatrix
import com.rtnp.demo.gpu.GpuTextureProvider
import com.rtnp.demo.gpu.render.tools.DockingPointRenderer
import com.rtnp.demo.gpu.render.tools.RangeDisplaySystem
import com.rtnp.demo.gpu.render.tools.BuildPreviewRenderer

class LowDisplayPass(private val textureProvider: GpuTextureProvider) {

    private val rangeSystem = RangeDisplaySystem()
    private val dockingPointRenderer = DockingPointRenderer()
    private val buildPreviewRenderer = BuildPreviewRenderer(textureProvider)

    fun init() {
        rangeSystem.init()
        dockingPointRenderer.init()
        buildPreviewRenderer.init()
    }

    fun draw(units: List<PlacedObject>, unitSystem: UnitSystem?, cameraMatrix: CameraMatrix, zoom: Float) {
        rangeSystem.update(unitSystem)
        rangeSystem.render(cameraMatrix)
        dockingPointRenderer.render(cameraMatrix, units, unitSystem, zoom)

        // ★ 建造预览与建造进程（进度环在预览之上，所以在此统一绘制）
        buildPreviewRenderer.draw(cameraMatrix, zoom)
    }
}