package com.rtnp.demo;

import com.rtnp.demo.core.PlacedObject;
import com.rtnp.demo.core.InventoryItem;

public class FactoryManager {
    private final UnitSystem host;

    public FactoryManager(UnitSystem host) {
        this.host = host;
    }

    public void registerItemTemplate(InventoryItem item) {
        // 旧工厂系统已移除，无操作
    }

    public PlacedObject findNearbyMineral(float wx, float wy, PlacedObject mover) {
        return null;
    }

    public PlacedObject findNearbyMineral(PlacedObject unit) {
        return null;
    }

    public PlacedObject createShipFromName(String name) {
        return null;
    }

    public int getTotalStored(PlacedObject unit) {
        return 0;
    }

    public void toggleMiningEnabled() {
        // 旧工厂系统已移除，无操作
    }
}