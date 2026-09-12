// app/src/main/kotlin/com/rtnp/demo/logic/MultiSelectManager.kt
package com.rtnp.demo.logic

import android.graphics.RectF
import com.rtnp.demo.core.PlacedObject
import com.rtnp.demo.UnitSystem
import com.rtnp.demo.ui.panel.PanelLogic
import com.rtnp.demo.ui.panel.RightPanel

class MultiSelectManager(private val unitSystem: UnitSystem) {

    private val selectedUnits = mutableSetOf<PlacedObject>()
    private var cameraX = 0f
    private var cameraY = 0f
    private var zoom = 1f
    private var screenWidth = 0
    private var screenHeight = 0
    private var active = false
    private var isSelecting = false
    private var startWorldX = 0f
    private var startWorldY = 0f
    private val currentWorldRect = RectF()
    private var formationMode = false

    fun setFormationMode(mode: Boolean) {
        formationMode = mode
    }

    fun isFormationMode(): Boolean = formationMode

    fun setActive(active: Boolean) {
        if (!active) {
            clearSelection()
            RightPanel.unlock()
        }
        this.active = active
        if (active) {
            formationMode = false
            RightPanel.lockMultiUnits(selectedUnits)
        }
    }

    fun removeUnit(unit: PlacedObject) {
        if (selectedUnits.remove(unit)) {
            unit.isSelected = false
        }
    }

    fun isActive(): Boolean = active

    fun updateCamera(camX: Float, camY: Float, zoom: Float, screenW: Int, screenH: Int) {
        cameraX = camX
        cameraY = camY
        this.zoom = zoom
        screenWidth = screenW
        screenHeight = screenH
    }

    fun startSelection(screenX: Float, screenY: Float) {
        if (!active) return
        isSelecting = true
        startWorldX = screenToWorldX(screenX)
        startWorldY = screenToWorldY(screenY)
        currentWorldRect.set(startWorldX, startWorldY, startWorldX, startWorldY)
    }

    fun updateSelection(screenX: Float, screenY: Float) {
        if (!active || !isSelecting) return
        val worldX = screenToWorldX(screenX)
        val worldY = screenToWorldY(screenY)
        val left = minOf(startWorldX, worldX)
        val top = minOf(startWorldY, worldY)
        val right = maxOf(startWorldX, worldX)
        val bottom = maxOf(startWorldY, worldY)
        currentWorldRect.set(left, top, right, bottom)
    }

    fun endSelection(append: Boolean) {
        if (!active || !isSelecting) return
        isSelecting = false

        if (currentWorldRect.width() > 5 && currentWorldRect.height() > 5) {
            if (!append) {
                for (unit in selectedUnits) {
                    unit.isSelected = false
                }
                selectedUnits.clear()
            }

            for (unit in unitSystem.allUnits) {
                if (unit.faction != 2) continue
                val bounds = getUnitBounds(unit)
                if (RectF.intersects(currentWorldRect, bounds)) {
                    if (selectedUnits.add(unit)) {
                        unit.isSelected = true
                    }
                } else if (!append) {
                    if (selectedUnits.remove(unit)) {
                        unit.isSelected = false
                    }
                }
            }
        } else {
            if (!append) {
                for (unit in selectedUnits) {
                    unit.isSelected = false
                }
                selectedUnits.clear()
            }
        }

        val first = selectedUnits.firstOrNull()
        unitSystem.setSelectedUnitDirect(first)

        val currentState = RightPanel.getCurrentState()
        val resolvedState = PanelLogic.resolveMultiSelect(currentState, active, selectedUnits.toList())
        RightPanel.applyState(resolvedState)
    }

    fun cancelSelection() {
        isSelecting = false
    }

    fun isSelecting(): Boolean = isSelecting

    fun getCurrentWorldRect(): RectF = currentWorldRect

    fun clearSelection() {
        for (unit in selectedUnits) {
            unit.isSelected = false
        }
        selectedUnits.clear()
    }

    fun addSingleSelection(unit: PlacedObject?) {
        clearSelection()
        if (unit != null) {
            selectedUnits.add(unit)
            unit.isSelected = true
        }
    }

    fun getSelectedUnits(): Set<PlacedObject> = selectedUnits

    fun getSelectedCount(): Int = selectedUnits.size

    private fun screenToWorldX(screenX: Float): Float {
        return (screenX - screenWidth / 2f) / zoom + cameraX
    }

    private fun screenToWorldY(screenY: Float): Float {
        return (screenY - screenHeight / 2f) / zoom + cameraY
    }

    private fun getUnitBounds(unit: PlacedObject): RectF {
        return when (unit.shape) {
            "circle" -> {
                val r = unit.size / 2f
                RectF(unit.worldX - r, unit.worldY - r, unit.worldX + r, unit.worldY + r)
            }
            "square" -> {
                val half = unit.size / 2f
                RectF(unit.worldX - half, unit.worldY - half, unit.worldX + half, unit.worldY + half)
            }
            else -> {
                val hw = unit.width / 2f
                val hh = unit.height / 2f
                RectF(unit.worldX - hw, unit.worldY - hh, unit.worldX + hw, unit.worldY + hh)
            }
        }
    }
}