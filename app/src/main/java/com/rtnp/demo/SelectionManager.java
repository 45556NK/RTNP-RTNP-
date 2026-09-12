package com.rtnp.demo;

import android.graphics.*;
import android.os.Handler;
import android.os.Looper;

import com.rtnp.demo.core.PlacedObject;
import com.rtnp.demo.logic.AutoLaunchTimer;
import com.rtnp.demo.logic.DockingSystem;
import com.rtnp.demo.logic.MovementSystem;
import com.rtnp.demo.render.RenderControl;
import com.rtnp.demo.ui.ToastTool;
import com.rtnp.demo.ui.panel.RightPanel;
import com.rtnp.demo.render.RenderControl;

import java.util.*;

public class SelectionManager {
    private final UnitSystem host;

    public SelectionManager(UnitSystem host) {
        this.host = host;
    }

    public void handleSelectTap(float worldX, float worldY) {
        if (host.multiSelectManager != null && host.multiSelectManager.isActive()) {
            clearAllSelections();
            RightPanel.INSTANCE.unlock();
            return;
        }

        if (host.missileSystem != null) {
            host.missileSystem.unwatchAll();
        }

        PlacedObject hit = hitTest(worldX, worldY);
        if (hit != null && ("unit".equals(hit.type) || "base".equals(hit.type) || "environment".equals(hit.type))) {
            clearAllSelections();
            host.selectedUnit = hit;
            host.selectedUnit.isSelected = true;
            host.actionLocked = false;
            host.showDetails = false;

            RightPanel.INSTANCE.lockUnit(hit);

            if ("missile".equals(hit.category) && host.missileSystem != null) {
                host.missileSystem.watch(hit);
            }
        } else {
            clearAllSelections();
            host.selectedUnit = null;
            host.actionLocked = false;
            host.showDetails = false;
            RightPanel.INSTANCE.unlock();
        }
    }

    public PlacedObject hitTest(float worldX, float worldY) {
        for (int i = host.placedObjects.size() - 1; i >= 0; i--) {
            PlacedObject o = host.placedObjects.get(i);
            if (!"unit".equals(o.type) && !"base".equals(o.type) && !"environment".equals(o.type)) continue;

            if ("missile".equals(o.category)) {
                float half = Math.max(o.size / 2f, 60f);
                if (Math.abs(worldX - o.worldX) <= half && Math.abs(worldY - o.worldY) <= half) return o;
                continue;
            }

            if (host.isPointInObject(worldX, worldY, o)) return o;
        }
        return null;
    }

    private void clearAllSelections() {
        if (host.multiSelectManager != null) {
            host.multiSelectManager.clearSelection();
        }
        if (host.selectedUnit != null) {
            host.selectedUnit.isSelected = false;
            host.selectedUnit = null;
        }
    }

    public void deselectUnit() {
        if (host.selectedUnit != null) {
            if (host.missileSystem != null) {
                host.missileSystem.unwatchAll();
            }
            host.selectedUnit.isSelected = false;
            host.selectedUnit = null;
        }
        RenderControl.INSTANCE.setHideStopButtonForAntimatter(false);
        host.actionLocked = false;
        host.showDetails = false;
        host.actionPending = false;
        host.actionDock = false;
    }

    public void setSelectedUnitDirect(PlacedObject unit) {
        if (unit != null && host.placedObjects.contains(unit)) {
            host.selectedUnit = unit;
            host.actionLocked = false;
            host.showDetails = false;
        }
    }

    public PlacedObject getSelectedUnit() { return host.selectedUnit; }
    public boolean isActionLocked() { return host.actionLocked; }
    public boolean isShowDetails() { return host.showDetails; }
    public void setShowDetails(boolean show) { host.showDetails = show; }

    public boolean isMissileSelected() {
        return host.selectedUnit != null && "missile".equals(host.selectedUnit.category);
    }

    public boolean isActionPending() { return host.actionPending; }

    public boolean isUnitMovingOrStopping() {
        return host.selectedUnit != null && (host.selectedUnit.isMoving || host.selectedUnit.isStopping);
    }

    public boolean isDockAvailable() {
        if (host.selectedUnit == null || !host.actionPending) return false;
        PlacedObject target = host.getDockTargetAt(host.selectedUnit.targetX, host.selectedUnit.targetY, host.selectedUnit);
        return target != null;
    }

    public void confirmAction() {
        RenderControl.INSTANCE.setShowUnitInfoPanel(true);
        ToastTool.hideGlobal();
        RenderControl.INSTANCE.setTouchDisabledUnitInfoPanel(false);
        RenderControl.INSTANCE.setTouchDisabledBottomBar(false);
        RenderControl.INSTANCE.setTouchDisabledMultiSelectButton(false);
        AutoLaunchTimer.INSTANCE.cancel(host.selectedUnit);
        if (host.selectedUnit != null && host.actionPending) {
            if (host.selectedUnit.actionDock && host.selectedUnit.dockTarget != null) {
                PlacedObject target = host.selectedUnit.dockTarget;
                int slotIndex = host.selectedUnit.dockSlotIndex;

                java.util.List<DockingSystem.DockSector> sectors = DockingSystem.INSTANCE.getDockingSectors(target);
                DockingSystem.DockSector sector = null;
                for (DockingSystem.DockSector s : sectors) {
                    if (s.getIndex() == slotIndex) {
                        sector = s;
                        break;
                    }
                }

                boolean success = DockingSystem.INSTANCE.requestDock(
                    host.selectedUnit, target, slotIndex
                );
                if (!success) return;

                if (sector != null) {
                    host.selectedUnit.targetX = sector.getCenterX();
                    host.selectedUnit.targetY = sector.getCenterY();
                }
            }

            host.selectedUnit.hasPreviewTarget = false;
            host.selectedUnit.isRotating = false;
            host.selectedUnit.isMoving = true;
            host.selectedUnit.currentSpeed = 0f;
            host.selectedUnit.canMove = true;
            host.actionPending = false;
        }
    }

    public void confirmDockAction() {
        if (host.selectedUnit != null && host.actionPending && host.actionDock) {
            host.selectedUnit.hasPreviewTarget = false;
            host.selectedUnit.isRotating = false;
            host.selectedUnit.isMoving = true;
            host.selectedUnit.actionDock = true;
            host.actionPending = false;
            host.actionDock = true;
        }
    }

    public void cancelAction() {
        RenderControl.INSTANCE.setShowUnitInfoPanel(true);
        ToastTool.hideGlobal();
        RenderControl.INSTANCE.setTouchDisabledUnitInfoPanel(false);
        RenderControl.INSTANCE.setTouchDisabledBottomBar(false);
        RenderControl.INSTANCE.setTouchDisabledMultiSelectButton(false);
        AutoLaunchTimer.INSTANCE.cancel(host.selectedUnit);
        if (host.selectedUnit != null && host.actionPending) {
            host.selectedUnit.hasPreviewTarget = false;
            host.selectedUnit.isMoving = false;
            host.selectedUnit.targetX = host.selectedUnit.worldX;
            host.selectedUnit.targetY = host.selectedUnit.worldY;
            host.selectedUnit.dockTarget = null;
            host.selectedUnit.dockSlotIndex = -1;
            host.selectedUnit.dockOffsetX = 0;
            host.selectedUnit.dockOffsetY = 0;
            host.actionPending = false;
            host.actionDock = false;
        }
    }

    public void stopMoving() {
        if (host.selectedUnit == null) return;
        if (!host.selectedUnit.isMoving && !host.selectedUnit.isStopping) return;

        if ("missile".equals(host.selectedUnit.category)) {
            PlacedObject missile = host.selectedUnit;

            host.selectedUnit.isSelected = false;
            host.selectedUnit = null;
            host.actionLocked = false;
            host.showDetails = false;
            host.actionPending = false;
            host.actionDock = false;

            RightPanel.INSTANCE.unlock();

            if (host.missileSystem != null) {
                try {
                    host.missileSystem.selfDestructWatched();
                } catch (Exception e) {}
            }
            return;
        }

        host.selectedUnit.isMoving = false;
        host.selectedUnit.isStopping = true;
        host.selectedUnit.targetX = host.selectedUnit.worldX;
        host.selectedUnit.targetY = host.selectedUnit.worldY;
        host.selectedUnit.dockTarget = null;
        host.actionDock = false;
    }

    public void showTargetIndicator(float worldX, float worldY, int color) {
        host.targetIndicators.clear();
        host.targetIndicators.add(new TargetIndicator(worldX, worldY, color));
        host.requestRedraw();
    }

    public void showActionDeniedIndicator(float worldX, float worldY) {
        host.targetIndicators.clear();
        host.targetIndicators.add(new TargetIndicator(worldX, worldY, Color.RED));
        host.requestRedraw();
    }

    public void setOnRedrawListener(UnitSystem.OnRedrawListener listener) {
        host.redrawListener = listener;
    }

    public Set<PlacedObject> getLockedUnits() {
        Set<PlacedObject> locked = new HashSet<>();
        if (host.selectedUnit != null) {
            locked.add(host.selectedUnit);
        }
        if (host.multiSelectManager != null && host.multiSelectManager.isActive()) {
            locked.addAll(host.multiSelectManager.getSelectedUnits());
        }
        return locked;
    }
}